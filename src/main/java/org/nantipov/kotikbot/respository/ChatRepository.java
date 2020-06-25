package org.nantipov.kotikbot.respository;

import org.nantipov.kotikbot.domain.entity.Chat;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;

import java.util.List;

public interface ChatRepository extends CrudRepository<Chat, Long> {
    @Query(value =
            "SELECT c.* " +
            "FROM chat c LEFT JOIN distributed_update u ON (" +
            "  u.chat_id = c.chat_id AND u.collected_update_id = :updateId " +
            ") " +
            "WHERE u.chat_id IS NULL",
            nativeQuery = true)
    List<Chat> findUnreachedChats(long updateId);
}
