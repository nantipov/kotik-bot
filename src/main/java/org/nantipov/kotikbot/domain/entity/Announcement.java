package org.nantipov.kotikbot.domain.entity;

import lombok.Data;
import org.nantipov.kotikbot.domain.RoomLanguage;

import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;

@Data
@Entity
public class Announcement {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    private String announcementMarkdown;

    @Enumerated(EnumType.STRING)
    private RoomLanguage language;

    private String groupId;
}
