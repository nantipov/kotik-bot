package org.nantipov.kotikbot.domain;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class CollectedMessages {
    List<CollectedMessage> messages;
}
