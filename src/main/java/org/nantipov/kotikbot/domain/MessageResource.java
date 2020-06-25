package org.nantipov.kotikbot.domain;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class MessageResource {
    private MessageResourceType type;
    private String url;
    private String caption;
}
