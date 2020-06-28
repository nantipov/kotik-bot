package org.nantipov.kotikbot.service;

import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.objects.Update;

@Service
public class BotUpdatesProcessor {

    private final CommandService commandService;

    public BotUpdatesProcessor(CommandService commandService) {
        this.commandService = commandService;
    }

    public void processUpdate(Update update) {
        if (update.hasMessage()) {
            var message = update.getMessage();
            if (message.hasText() && message.getText().startsWith("/")) {
                commandService.command(message.getChatId(), message.getText());
            }
        }
    }
}
