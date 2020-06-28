package org.nantipov.kotikbot.service.remote;

import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.ApiContext;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.generics.BotSession;

import java.util.function.Consumer;

public class TelegramBot extends TelegramLongPollingBot {

    private final String token;
    private final String name;
    private final Consumer<Update> updateHandler;

    public TelegramBot(String token, String name, Consumer<Update> updateHandler) {
        this.token = token;
        this.name = name;
        this.updateHandler = updateHandler;
        BotSession session = ApiContext.getInstance(BotSession.class);
        session.setToken(token);
        session.setOptions(this.getOptions());
        session.setCallback(this);
        session.start();
    }

    @Override
    public void onUpdateReceived(Update update) {
        updateHandler.accept(update);
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
