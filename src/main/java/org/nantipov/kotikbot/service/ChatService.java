package org.nantipov.kotikbot.service;

import org.nantipov.kotikbot.domain.entity.Chat;
import org.nantipov.kotikbot.respository.ChatRepository;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;

@Service
public class ChatService {
    private final ChatRepository chatRepository;

    public ChatService(ChatRepository chatRepository) {
        this.chatRepository = chatRepository;
    }

    public void checkChatSubscription(long chatId) {
        var chat = chatRepository.findById(chatId)
                                 .orElseGet(() -> new Chat(chatId));
        chat.setReceivedAt(OffsetDateTime.now());
        chatRepository.save(chat);
    }
}
