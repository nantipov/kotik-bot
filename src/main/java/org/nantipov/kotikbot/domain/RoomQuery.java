package org.nantipov.kotikbot.domain;

import lombok.Builder;
import lombok.Value;

import java.util.List;

@Value
@Builder
public class RoomQuery {
    RoomQueryType queryType;
    String rawText;
    Long roomId;
    RoomAction action;
    List<String> args;
}
