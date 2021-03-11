package org.nantipov.kotikbot.domain.entity;

import lombok.Data;

import java.time.OffsetDateTime;
import java.util.List;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.OneToMany;

@Data
@Entity
public class CollectedUpdate {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private OffsetDateTime createdAt = OffsetDateTime.now();
    private OffsetDateTime actualTill;
    private String supplier;
    private String updateKey;
    private String messageJson;
    private String messagesJson;

    @OneToMany(mappedBy = "collectedUpdate")
    private List<DistributedUpdate> distributedUpdates;
}
