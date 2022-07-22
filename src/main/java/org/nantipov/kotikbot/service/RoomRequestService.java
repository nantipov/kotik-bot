package org.nantipov.kotikbot.service;

import lombok.Value;
import org.nantipov.kotikbot.domain.RoomAction;
import org.nantipov.kotikbot.domain.RoomLanguage;
import org.nantipov.kotikbot.domain.RoomQuery;
import org.nantipov.kotikbot.domain.RoomQueryResponse;
import org.nantipov.kotikbot.domain.SupplierMessage;
import org.nantipov.kotikbot.domain.entity.Room;
import org.nantipov.kotikbot.respository.RoomRepository;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.channels.Channels;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import static com.google.common.base.Strings.nullToEmpty;

@Service
public class RoomRequestService {

    private static final Predicate<RoomQuery> PREDICATE_IS_ACTION = roomQuery -> roomQuery.getAction() != null;
    private static final Predicate<RoomQuery> PREDICATE_IS_ENG =
            PREDICATE_IS_ACTION.and(roomQuery -> roomQuery.getAction() == RoomAction.ENG);
    private static final Predicate<RoomQuery> PREDICATE_IS_RUS =
            PREDICATE_IS_ACTION.and(roomQuery -> roomQuery.getAction() == RoomAction.RUS);
    private static final Predicate<RoomQuery> PREDICATE_IS_LANGUAGES =
            PREDICATE_IS_ACTION.and(roomQuery -> roomQuery.getAction() == RoomAction.LANGUAGES);
    private static final Predicate<RoomQuery> PREDICATE_IS_BETA =
            PREDICATE_IS_ACTION.and(roomQuery -> roomQuery.getAction() == RoomAction.BETA);
    private static final Predicate<RoomQuery> PREDICATE_IS_STATS =
            PREDICATE_IS_ACTION.and(roomQuery -> roomQuery.getAction() == RoomAction.STATS);
    private static final Predicate<RoomQuery> PREDICATE_IS_PEEK =
            PREDICATE_IS_ACTION.and(roomQuery -> roomQuery.getAction() == RoomAction.PEEK);

    private final RoomSettingsService roomSettingsService;
    private final TranslationsService translationsService;
    private final RoomRepository roomRepository;
    private final ResourceLoader resourceLoader;

    private final List<ProcessingRoute> routes;

    public RoomRequestService(RoomSettingsService roomSettingsService,
                              TranslationsService translationsService,
                              RoomRepository roomRepository, ResourceLoader resourceLoader,
                              PeekQueryService peekQueryService) {
        this.roomSettingsService = roomSettingsService;
        this.translationsService = translationsService;
        this.roomRepository = roomRepository;
        this.resourceLoader = resourceLoader;

        routes = List.of(
                ProcessingRoute.of(PREDICATE_IS_ENG, query -> changeFirstLanguageHandler(query, RoomLanguage.EN)),
                ProcessingRoute.of(PREDICATE_IS_RUS, query -> changeFirstLanguageHandler(query, RoomLanguage.RU)),
                ProcessingRoute.of(PREDICATE_IS_LANGUAGES, this::changeLanguagesHandler),
                ProcessingRoute.of(PREDICATE_IS_BETA, this::changeBetaFlag),
                ProcessingRoute.of(PREDICATE_IS_STATS, this::stats),
                ProcessingRoute.of(PREDICATE_IS_PEEK, peekQueryService::peek)
        );
    }

    public Optional<RoomQueryResponse> process(RoomQuery roomQuery) {
        return routes.stream()
                     .sequential()
                     .filter(route -> route.getRulePredicate().test(roomQuery))
                     .map(route -> route.getHandler().apply(roomQuery))
                     .findFirst();
    }

    private RoomQueryResponse changeBetaFlag(RoomQuery query) {
        var room = getRoom(query.getRoomId());
        var firstLanguage = roomSettingsService.getFirstLanguage(query.getRoomId());
        var responseMessage = new SupplierMessage();
        if (!query.getArgs().isEmpty() &&
            nullToEmpty(query.getArgs().get(0)).trim().equalsIgnoreCase("off")) {
            roomSettingsService.setBeta(query.getRoomId(), false);
            responseMessage.setMarkdownText(translationsService.translation(
                    "settings.beta_disabled", firstLanguage.getLocale()
            ));
        } else {
            roomSettingsService.setBeta(query.getRoomId(), true);
            responseMessage.setMarkdownText(translationsService.translation(
                    "settings.beta_enabled", firstLanguage.getLocale()
            ));
        }
        return RoomQueryResponse.builder()
                                .providerRoomKey(room.getProviderRoomKey())
                                .message(responseMessage)
                                .build();
    }

    private RoomQueryResponse changeFirstLanguageHandler(RoomQuery query, RoomLanguage language) {
        var room = getRoom(query.getRoomId());
        var response = changeFirstLanguage(room, language);
        var responseMessage = new SupplierMessage();
        responseMessage.setMarkdownText(response);
        return RoomQueryResponse.builder()
                                .providerRoomKey(room.getProviderRoomKey())
                                .message(responseMessage)
                                .build();
    }

    private RoomQueryResponse changeLanguagesHandler(RoomQuery query) {
        var room = getRoom(query.getRoomId());
        var availableLanguagesHelp =
                Arrays.stream(RoomLanguage.values())
                      .map(
                              l -> "`" + l.name() + "` `" + l.getDisplayName() + "` " +
                                   l.getAliases()
                                    .stream()
                                    .map(alias -> "`" + alias + "`")
                                    .collect(Collectors.joining(" "))
                      )
                      .collect(Collectors.joining("\n"));

        if (query.getArgs().isEmpty()) {
            var firstLanguage = roomSettingsService.getFirstLanguage(query.getRoomId());
            var responseMessage = new SupplierMessage();
            responseMessage.setMarkdownText(
                    String.format(
                            translationsService.translation(
                                    "settings.languages_set_empty_args", firstLanguage.getLocale()
                            ),
                            availableLanguagesHelp
                    )
            );
            return RoomQueryResponse.builder()
                                    .providerRoomKey(room.getProviderRoomKey())
                                    .message(responseMessage)
                                    .build();
        }

        var languagesSet = new HashSet<RoomLanguage>();
        var languages = query.getArgs()
                             .stream()
                             .sequential()
                             .map(RoomLanguage::findLanguage)
                             .filter(languagesSet::add)
                             .collect(Collectors.toList());

        var firstLanguage = languages.get(0);
        roomSettingsService.setLanguages(room.getId(), languages);

        var responseMessage = new SupplierMessage();
        responseMessage.setMarkdownText(
                String.format(
                        translationsService.translation(
                                "settings.languages_set", firstLanguage.getLocale()
                        ),
                        languages.stream()
                                 .map(RoomLanguage::getDisplayName)
                                 .collect(Collectors.joining(", ")),
                        availableLanguagesHelp
                )
        );
        return RoomQueryResponse.builder()
                                .providerRoomKey(room.getProviderRoomKey())
                                .message(responseMessage)
                                .build();
    }

    private String changeFirstLanguage(Room room, RoomLanguage language) {
        roomSettingsService.setFirstLanguage(room.getId(), language);
        return String.format(translationsService.translation("settings.language_set", language.getLocale()),
                             language.getDisplayName());
    }

    private RoomQueryResponse stats(RoomQuery query) {
        var room = getRoom(query.getRoomId());
        var firstLanguage = roomSettingsService.getFirstLanguage(query.getRoomId());
        var responseMessage = new SupplierMessage();

        var version = "unknown";
        try (var versionReader = new BufferedReader(Channels.newReader(
                resourceLoader.getResource("classpath:version").readableChannel(),
                StandardCharsets.UTF_8))
        ) {
            version = versionReader.readLine().trim();
        } catch (IOException e) {
            // it is not necessary to handle it
        }

        var roomsQuantity = roomRepository.count();

        var response = Utils.escapeReservedCharacters(
                String.format(
                        translationsService.translation(
                                "stats.response", firstLanguage.getLocale()
                        ),
                        version.trim(),
                        roomsQuantity
                )
        );
        responseMessage.setMarkdownText(response);
        return RoomQueryResponse.builder()
                                .providerRoomKey(room.getProviderRoomKey())
                                .message(responseMessage)
                                .build();
    }

    private Room getRoom(long roomId) {
        return roomRepository.findById(roomId)
                             .orElseThrow(() -> new IllegalArgumentException(
                                     "Unknown room " + roomId)
                             );
    }

    @Value(staticConstructor = "of")
    private static class ProcessingRoute {
        Predicate<RoomQuery> rulePredicate;
        Function<RoomQuery, RoomQueryResponse> handler;
    }
}
