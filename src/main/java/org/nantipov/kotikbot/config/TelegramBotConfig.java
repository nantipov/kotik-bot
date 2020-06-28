package org.nantipov.kotikbot.config;

import org.nantipov.kotikbot.service.BotUpdatesProcessor;
import org.nantipov.kotikbot.service.remote.TelegramBot;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.telegram.telegrambots.ApiContextInitializer;

@Configuration
public class TelegramBotConfig {
    @Bean
    public TelegramBot telegramBot(BotProperties botProperties, BotUpdatesProcessor botUpdatesProcessor) {
        init();
        return new TelegramBot(botProperties.getToken(), botProperties.getName(), botUpdatesProcessor::processUpdate);
    }

    private void init() {
        ApiContextInitializer.init();
    }
}
