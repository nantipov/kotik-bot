package org.nantipov.kotikbot.respository;

import org.nantipov.kotikbot.domain.entity.Announcement;
import org.springframework.data.repository.CrudRepository;

import java.util.List;

public interface AnnouncementsRepository extends CrudRepository<Announcement, Integer> {
    List<Announcement> findByIdGreaterThan(int lastId);
}
