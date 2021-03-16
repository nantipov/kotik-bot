package org.nantipov.kotikbot.domain;

import java.util.Arrays;

public enum RoomAction {
    START("Start conversation"),
    LANGUAGES("Setup languages - ex. /languages rus eng"),
    ENG("Change the first language to English"),
    RUS("Сменить первый язык на Русский"),
    BETA("I accept beta features"),
    //    HELP("Help message"),
    //    TIMEZONE("Timezone - <City>"),
    UNKNOWN("n/a");

    String description;

    RoomAction(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }

    public static RoomAction fromString(String text) {
        return Arrays.stream(values())
                     .filter(a -> a.name().equalsIgnoreCase(text))
                     .findAny()
                     .orElse(UNKNOWN);
    }
}
