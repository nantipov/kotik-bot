package org.nantipov.kotikbot.service;

import com.google.common.collect.Sets;

import java.util.Set;
import java.util.stream.Collectors;

public class Utils {

    private static final Set<Character> MARKDOWN_RESERVED_CHARACTERS = Sets.newHashSet(
            '_', '*', '[', ']', '(', ')', '~', '`', '>', '#', '+', '-', '=', '|', '{', '}', '.', '!'
    );

    private Utils() {
    }

    public static String escapeReservedCharacters(String markdownText) {
        //TODO optimize
        return markdownText.chars()
                           .mapToObj(charCode -> {
                                         if (MARKDOWN_RESERVED_CHARACTERS.contains((char) charCode)) {
                                             return "\\" + (char) charCode;
                                         } else {
                                             return String.valueOf((char) charCode);
                                         }
                                     }
                           )
                           .collect(Collectors.joining());
    }
}
