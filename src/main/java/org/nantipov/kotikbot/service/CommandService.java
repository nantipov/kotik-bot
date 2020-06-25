package org.nantipov.kotikbot.service;

import org.springframework.stereotype.Service;

@Service
public class CommandService {

    private final ChatService chatService;

    public CommandService(ChatService chatService) {
        this.chatService = chatService;
    }

    public void command(long chatId, String text) {
        chatService.checkChatSubscription(chatId);
    }
}
