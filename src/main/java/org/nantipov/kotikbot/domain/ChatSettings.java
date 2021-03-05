package org.nantipov.kotikbot.domain;

import lombok.Data;

import java.time.ZoneId;

@Data
public class ChatSettings {
    private ChatLanguage language;
    private ZoneId timeZoneId;
}
