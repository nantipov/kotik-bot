package org.nantipov.kotikbot.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Strings;
import org.nantipov.kotikbot.domain.RoomLanguage;
import org.nantipov.kotikbot.domain.RoomSettings;
import org.nantipov.kotikbot.domain.entity.Room;
import org.nantipov.kotikbot.respository.RoomRepository;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

@Service
public class RoomSettingsService {

    private final static RoomSettings DEFAULT_SETTINGS =
            RoomSettings.builder()
                        .languages(List.of(RoomLanguage.EN))
                        .timeZoneId(ZoneId.of("UTC"))
                        .build();

    private final RoomRepository roomRepository;
    private final ObjectMapper objectMapper;

    public RoomSettingsService(RoomRepository roomRepository,
                               @Qualifier("kotik") ObjectMapper objectMapper) {
        this.roomRepository = roomRepository;
        this.objectMapper = objectMapper;
    }

    public RoomSettings getSettings(long roomId) {
        return roomRepository.findById(roomId)
                             .map(Room::getSettingsJson)
                             .map(Strings::nullToEmpty)
                             .filter(Predicate.not(String::isBlank))
                             .map(this::extractSettings)
                             .orElse(DEFAULT_SETTINGS);
    }

    public RoomSettings getSettingsFromString(String settingsJson) { //TODO hibernate type adapter
        if (Strings.isNullOrEmpty(settingsJson)) {
            return DEFAULT_SETTINGS;
        } else {
            return extractSettings(settingsJson);
        }
    }

    public void setFirstLanguage(long roomId, RoomLanguage language) {
        var oldSettings = getSettings(roomId);
        var languagesMod = new ArrayList<>(oldSettings.getLanguages());
        languagesMod.remove(language);
        languagesMod.add(0, language);
        var newSettings = oldSettings.toBuilder()
                                     .languages(languagesMod)
                                     .build();
        var room =
                roomRepository.findById(roomId)
                              .orElseThrow(() -> new IllegalArgumentException("Save settings for non existing room"));

        room.setSettingsJson(writeSettings(newSettings)); //TODO hibernate type adapter
        roomRepository.save(room);
    }

    public RoomLanguage getFirstLanguage(long roomId) {
        return getSettings(roomId).getLanguages().get(0);
    }

    public void setLanguages(long roomId, List<RoomLanguage> languages) {
        var newSettings = getSettings(roomId).toBuilder()
                                             .languages(languages)
                                             .build();
        var room =
                roomRepository.findById(roomId)
                              .orElseThrow(() -> new IllegalArgumentException("Save settings for non existing room"));

        room.setSettingsJson(writeSettings(newSettings)); //TODO hibernate type adapter
        roomRepository.save(room);
    }

    private RoomSettings extractSettings(String settingsJson) {
        try {
            return objectMapper.readValue(settingsJson, RoomSettings.class);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("Could not read settings data " + settingsJson, e);
        }
    }

    private String writeSettings(RoomSettings roomSettings) {
        try {
            return objectMapper.writeValueAsString(roomSettings);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("Could not write settings data " + roomSettings, e);
        }
    }
}
