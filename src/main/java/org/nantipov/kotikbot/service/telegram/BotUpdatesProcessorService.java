package org.nantipov.kotikbot.service.telegram;

import org.nantipov.kotikbot.domain.RoomProvider;
import org.nantipov.kotikbot.domain.UserRequest;
import org.nantipov.kotikbot.domain.UserRequestType;
import org.nantipov.kotikbot.domain.UserResponse;
import org.nantipov.kotikbot.domain.entity.Room;
import org.nantipov.kotikbot.respository.RoomRepository;
import org.nantipov.kotikbot.service.UserRequestService;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.objects.Update;

import java.time.OffsetDateTime;
import java.util.Optional;

@Service
public class BotUpdatesProcessorService { //TODO move to TelegramBot/Service?

    private final UserRequestService userRequestService;
    private final RoomRepository roomRepository;

    public BotUpdatesProcessorService(UserRequestService userRequestService,
                                      RoomRepository roomRepository) {
        this.userRequestService = userRequestService;
        this.roomRepository = roomRepository;
    }

    public Optional<UserResponse> processUpdate(Update update) {
        var req = toUserRequest(update);
        var resp = userRequestService.process(req);
        return Optional.of(resp);
    }

    private UserRequest toUserRequest(Update update) {
        if (update.hasMessage()) {
            var message = update.getMessage();
            var room = checkChatSubscription(message.getChatId());
            if (message.hasText() && message.getText().startsWith("/")) {
                //        userRequestService.process(message.getChatId(), message.getText());
            }
            return new UserRequest(UserRequestType.ACTION, "", room.getId());
        }
        return new UserRequest(UserRequestType.ACTION, "", null);
    }

    private Room checkChatSubscription(long chatId) {
        var room = roomRepository.findByProviderAndProviderRoomKey(RoomProvider.TELEGRAM, String.valueOf(chatId))
                                 .orElseGet(() -> new Room(RoomProvider.TELEGRAM, String.valueOf(chatId)));
        room.setReceivedAt(OffsetDateTime.now());
        return roomRepository.save(room);
    }
}
