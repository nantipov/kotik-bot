package org.nantipov.kotikbot.service;

import lombok.Value;
import org.nantipov.kotikbot.domain.RoomAction;
import org.nantipov.kotikbot.domain.RoomLanguage;
import org.nantipov.kotikbot.domain.RoomQuery;
import org.nantipov.kotikbot.domain.RoomQueryResponse;
import org.nantipov.kotikbot.domain.SupplierMessage;
import org.nantipov.kotikbot.domain.entity.Room;
import org.nantipov.kotikbot.respository.RoomRepository;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

@Service
public class RoomRequestService {

    private static final Predicate<RoomQuery> PREDICATE_IS_ACTION = roomQuery -> roomQuery.getAction() != null;
    private static final Predicate<RoomQuery> PREDICATE_IS_ENG =
            PREDICATE_IS_ACTION.and(roomQuery -> roomQuery.getAction() == RoomAction.ENG);
    private static final Predicate<RoomQuery> PREDICATE_IS_RUS =
            PREDICATE_IS_ACTION.and(roomQuery -> roomQuery.getAction() == RoomAction.RUS);
    private static final Predicate<RoomQuery> PREDICATE_IS_LANGUAGES =
            PREDICATE_IS_ACTION.and(roomQuery -> roomQuery.getAction() == RoomAction.LANGUAGES);

    private final RoomSettingsService roomSettingsService;
    private final TranslationsService translationsService;
    private final RoomRepository roomRepository;

    private final List<ProcessingRoute> routes = List.of(
            ProcessingRoute.of(PREDICATE_IS_ENG, query -> changeFirstLanguageHandler(query, RoomLanguage.EN)),
            ProcessingRoute.of(PREDICATE_IS_RUS, query -> changeFirstLanguageHandler(query, RoomLanguage.RU)),
            ProcessingRoute.of(PREDICATE_IS_LANGUAGES, this::changeLanguagesHandler)
    );

    public RoomRequestService(RoomSettingsService roomSettingsService,
                              TranslationsService translationsService,
                              RoomRepository roomRepository) {
        this.roomSettingsService = roomSettingsService;
        this.translationsService = translationsService;
        this.roomRepository = roomRepository;
    }

    public Optional<RoomQueryResponse> process(RoomQuery roomQuery) {
        return routes.stream()
                     .sequential()
                     .filter(route -> route.getRulePredicate().test(roomQuery))
                     .map(route -> route.getHandler().apply(roomQuery))
                     .findFirst();
    }

    private RoomQueryResponse changeFirstLanguageHandler(RoomQuery query, RoomLanguage language) {
        var room = //TODO rework
                roomRepository.findById(query.getRoomId())
                              .orElseThrow(() -> new IllegalArgumentException(
                                      "Unknown room " + query.getRoomId())
                              );
        var response = changeFirstLanguage(room, language);
        var responseMessage = new SupplierMessage();
        responseMessage.setMarkdownText(response);
        return RoomQueryResponse.builder()
                                .providerRoomKey(room.getProviderRoomKey())
                                .message(responseMessage)
                                .build();
    }

    private RoomQueryResponse changeLanguagesHandler(RoomQuery query) {
        var room = //TODO rework
                roomRepository.findById(query.getRoomId())
                              .orElseThrow(() -> new IllegalArgumentException(
                                      "Unknown room " + query.getRoomId())
                              );

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

    @Value(staticConstructor = "of")
    private static class ProcessingRoute {
        Predicate<RoomQuery> rulePredicate;
        Function<RoomQuery, RoomQueryResponse> handler;
    }
}
