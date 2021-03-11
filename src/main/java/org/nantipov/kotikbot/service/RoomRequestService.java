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

import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Predicate;

@Service
public class RoomRequestService {

    private static final Predicate<RoomQuery> PREDICATE_IS_ACTION = roomQuery -> roomQuery.getAction() != null;
    private static final Predicate<RoomQuery> PREDICATE_IS_ENG =
            PREDICATE_IS_ACTION.and(roomQuery -> roomQuery.getAction() == RoomAction.ENG);
    private static final Predicate<RoomQuery> PREDICATE_IS_RUS =
            PREDICATE_IS_ACTION.and(roomQuery -> roomQuery.getAction() == RoomAction.RUS);

    private final RoomSettingsService roomSettingsService;
    private final TranslationsService translationsService;
    private final RoomRepository roomRepository;

    private final List<ProcessingRoute> routes = List.of(
            ProcessingRoute.of(PREDICATE_IS_ENG, query -> changeMainLanguageHandler(query, RoomLanguage.EN)),
            ProcessingRoute.of(PREDICATE_IS_RUS, query -> changeMainLanguageHandler(query, RoomLanguage.RU))
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

    private RoomQueryResponse changeMainLanguageHandler(RoomQuery query, RoomLanguage language) {
        var room = //TODO rework
                roomRepository.findById(query.getRoomId())
                              .orElseThrow(() -> new IllegalArgumentException(
                                      "Unknown room " + query.getRoomId())
                              );
        var response = changeMainLanguage(room, language);
        var responseMessage = new SupplierMessage();
        responseMessage.setMarkdownText(response);
        return RoomQueryResponse.builder()
                                .providerRoomKey(room.getProviderRoomKey())
                                .message(responseMessage)
                                .build();
    }

    private String changeMainLanguage(Room room, RoomLanguage language) {
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
