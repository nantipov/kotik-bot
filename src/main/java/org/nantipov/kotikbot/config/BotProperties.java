package org.nantipov.kotikbot.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties("bot")
public class BotProperties {
    private String name;
    private String token;
}
