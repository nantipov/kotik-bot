package org.nantipov.kotikbot.domain.entity;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;
import java.util.List;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.OneToMany;

@Data
@Entity
@NoArgsConstructor
public class Chat {
    @Id
    private long chatId;

    private OffsetDateTime receivedAt;
    private OffsetDateTime postedAt;

    @OneToMany(mappedBy = "chat")
    private List<DistributedUpdate> distributedUpdates;

    public Chat(long chatId) {
        this.chatId = chatId;
    }
}
