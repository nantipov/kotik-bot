package org.nantipov.kotikbot.service;

import org.springframework.stereotype.Service;

import java.util.Locale;
import java.util.ResourceBundle;

@Service
public class TranslationsService {

    private static final String BUNDLE_LOCATION = "translations/translations";

    public String translation(String key, Locale locale) {
        var bundle = ResourceBundle.getBundle(BUNDLE_LOCATION, locale);
        return bundle.getString(key);
    }
}
