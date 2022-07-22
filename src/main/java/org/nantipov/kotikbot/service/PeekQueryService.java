package org.nantipov.kotikbot.service;

import org.nantipov.kotikbot.domain.RoomLanguage;
import org.nantipov.kotikbot.domain.RoomQuery;
import org.nantipov.kotikbot.domain.RoomQueryResponse;
import org.nantipov.kotikbot.domain.SupplierMessage;
import org.nantipov.kotikbot.domain.entity.Room;
import org.nantipov.kotikbot.respository.RoomRepository;
import org.springframework.stereotype.Service;

@Service
public class PeekQueryService {

    private final RoomSettingsService roomSettingsService;
    private final TranslationsService translationsService;
    private final RoomRepository roomRepository;

    public PeekQueryService(RoomSettingsService roomSettingsService,
                            TranslationsService translationsService,
                            RoomRepository roomRepository) {
        this.roomSettingsService = roomSettingsService;
        this.translationsService = translationsService;
        this.roomRepository = roomRepository;
    }

    public RoomQueryResponse peek(RoomQuery roomQuery) {
        var room = roomRepository.findById(roomQuery.getRoomId())
                                 .orElseThrow();

        var firstLanguage = roomSettingsService.getFirstLanguage(roomQuery.getRoomId());

        if (roomQuery.getArgs().isEmpty()) {
            return help(room, firstLanguage);
        }

        var term = String.join(" ", roomQuery.getArgs());
        return null;
    }

    private RoomQueryResponse help(Room room, RoomLanguage firstLanguage) {
        var responseMessage = new SupplierMessage();
        var response = Utils.escapeReservedCharacters(
                translationsService.translation(
                        "peek.help", firstLanguage.getLocale()
                )
        );
        responseMessage.setMarkdownText(response);
        return RoomQueryResponse.builder()
                                .providerRoomKey(room.getProviderRoomKey())
                                .message(responseMessage)
                                .build();
    }
}
