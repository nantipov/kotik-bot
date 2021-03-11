package org.nantipov.kotikbot.domain;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class RoomQueryResponse {
    String providerRoomKey;
    SupplierMessage message;
}
