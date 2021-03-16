package org.nantipov.kotikbot.domain;

import lombok.Builder;
import lombok.Value;

import java.time.ZoneId;
import java.util.List;

@Value
@Builder(toBuilder = true)
public class RoomSettings {
    List<RoomLanguage> languages;
    ZoneId timeZoneId;
    Boolean acceptBetaFeatures;
}
