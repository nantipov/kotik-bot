package org.nantipov.kotikbot.service.telegram;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.objects.Update;

@Component
public class TelegramBot extends TelegramLongPollingBot {

    private final String token;
    private final String name;
    private final BotUpdatesProcessorService botUpdatesProcessorService;

    public TelegramBot(@Value("${telegram.token}") String token,
                       @Value("${bot.name}") String name,
                       BotUpdatesProcessorService botUpdatesProcessorService) {
        this.token = token;
        this.name = name;
        this.botUpdatesProcessorService = botUpdatesProcessorService;
    }

    @Override
    public void onUpdateReceived(Update update) {
        var response = botUpdatesProcessorService.processUpdate(update);
    }

    @Override
    public String getBotUsername() {
        return name;
    }

    @Override
    public String getBotToken() {
        return token;
    }
}
