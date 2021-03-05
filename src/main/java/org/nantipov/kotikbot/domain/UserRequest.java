package org.nantipov.kotikbot.domain;

import lombok.Value;

@Value
public class UserRequest {
    UserRequestType requestType;
    String rawText;
    Long roomId;
}
