package org.nantipov.kotikbot.domain;

public enum ChatLanguage {
    EN("English"),
    RU("Русский");

    String displayName;

    ChatLanguage(String displayName) {
        this.displayName = displayName;
    }
}
