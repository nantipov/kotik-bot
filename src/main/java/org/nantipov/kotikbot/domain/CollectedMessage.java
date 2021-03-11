package org.nantipov.kotikbot.domain;

import lombok.Value;

@Value
public class CollectedMessage {
    RoomLanguage language;
    SupplierMessage message;
}
