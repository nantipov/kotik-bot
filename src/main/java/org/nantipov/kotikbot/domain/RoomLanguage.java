package org.nantipov.kotikbot.domain;

import com.google.common.base.Strings;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;

public enum RoomLanguage {
    EN("English", Locale.ENGLISH, List.of("eng")),
    RU("Русский", Locale.forLanguageTag("ru-RU"), List.of("rus", "ру", "рус"));

    String displayName;
    Locale locale;
    List<String> aliases;

    RoomLanguage(String displayName, Locale locale, List<String> aliases) {
        this.displayName = displayName;
        this.locale = locale;
        this.aliases = aliases;
    }

    public String getDisplayName() {
        return displayName;
    }

    public Locale getLocale() {
        return locale;
    }

    public List<String> getAliases() {
        return aliases;
    }

    public static RoomLanguage findLanguage(String text) {
        return Arrays.stream(values())
                     .filter(l -> l.name().equalsIgnoreCase(text) ||
                                  l.displayName.equalsIgnoreCase(text) ||
                                  l.aliases.contains(Strings.nullToEmpty(text).toLowerCase())
                     )
                     .findFirst()
                     .orElse(EN);
    }
}
