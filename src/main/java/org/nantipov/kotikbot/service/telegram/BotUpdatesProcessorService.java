package org.nantipov.kotikbot.service.telegram;

import org.nantipov.kotikbot.domain.RoomAction;
import org.nantipov.kotikbot.domain.RoomProvider;
import org.nantipov.kotikbot.domain.RoomQuery;
import org.nantipov.kotikbot.domain.RoomQueryResponse;
import org.nantipov.kotikbot.domain.RoomQueryType;
import org.nantipov.kotikbot.domain.entity.Room;
import org.nantipov.kotikbot.respository.RoomRepository;
import org.nantipov.kotikbot.service.RoomRequestService;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.objects.Update;

import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class BotUpdatesProcessorService { //TODO move to TelegramBot/Service?

    private final RoomRequestService roomRequestService;
    private final RoomRepository roomRepository;

    public BotUpdatesProcessorService(RoomRequestService roomRequestService,
                                      RoomRepository roomRepository) {
        this.roomRequestService = roomRequestService;
        this.roomRepository = roomRepository;
    }

    public Optional<RoomQueryResponse> processUpdate(Update update) {
        return toUserRequest(update)
                .flatMap(roomRequestService::process);
    }

    private Optional<RoomQuery> toUserRequest(Update update) {
        if (update.hasMessage()) {
            var message = update.getMessage();
            var room = checkChatSubscription(message.getChatId());
            if (message.hasText() && message.getText().length() > 1 && message.getText().startsWith("/")) {
                var args = message.getText().trim().split(" ");
                return Optional.of(
                        RoomQuery.builder()
                                 .roomId(room.getId())
                                 .rawText(message.getText())
                                 .queryType(RoomQueryType.ACTION)
                                 .action(RoomAction.fromString(args[0].substring(1).trim()))
                                 .args(
                                         Arrays.stream(args)
                                               .skip(1)
                                               .map(String::trim)
                                               .collect(Collectors.toList())
                                 )
                                 .build()
                );
            }
        }
        return Optional.empty();
    }

    private Room checkChatSubscription(long chatId) {
        var room = roomRepository.findByProviderAndProviderRoomKey(RoomProvider.TELEGRAM, String.valueOf(chatId))
                                 .orElseGet(() -> new Room(RoomProvider.TELEGRAM, String.valueOf(chatId)));
        room.setReceivedAt(OffsetDateTime.now());
        return roomRepository.save(room);
    }
}
