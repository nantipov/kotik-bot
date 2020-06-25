package org.nantipov.kotikbot;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.telegram.telegrambots.ApiContextInitializer;

@SpringBootApplication
public class KotikBotApplication {
    public static void main(String[] args) {
        ApiContextInitializer.init();
        SpringApplication.run(KotikBotApplication.class, args);
    }
}
