package org.nantipov.kotikbot.domain;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class MessageResource {
    private MessageResourceType type;
    private String url;
    private String caption;
}
