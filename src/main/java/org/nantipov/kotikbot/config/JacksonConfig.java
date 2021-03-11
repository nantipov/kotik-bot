package org.nantipov.kotikbot.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class JacksonConfig {
    @Bean("kotik")
    public ObjectMapper kotikObjectMapper() {
        return new ObjectMapper().setPropertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE)
                                 .findAndRegisterModules();
    }
}
