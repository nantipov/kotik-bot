package org.nantipov.kotikbot.domain;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class SupplierMessage {
    private String markdownText;
    private List<MessageResource> resources = new ArrayList<>();
}
