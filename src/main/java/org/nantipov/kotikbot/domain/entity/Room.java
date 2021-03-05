package org.nantipov.kotikbot.domain.entity;

import lombok.Data;
import lombok.NoArgsConstructor;
import org.nantipov.kotikbot.domain.RoomProvider;

import java.time.OffsetDateTime;
import java.util.List;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.OneToMany;

@Data
@Entity
@NoArgsConstructor
public class Room {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private OffsetDateTime receivedAt;
    private OffsetDateTime postedAt;

    @Enumerated(EnumType.STRING)
    private RoomProvider provider;
    private String providerRoomKey;

    @OneToMany(mappedBy = "room")
    private List<DistributedUpdate> distributedUpdates;

    public Room(RoomProvider provider, String key) {
        this.provider = provider;
        this.providerRoomKey = key;
    }
}
