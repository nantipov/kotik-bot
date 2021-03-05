package org.nantipov.kotikbot.respository;

import org.nantipov.kotikbot.domain.RoomProvider;
import org.nantipov.kotikbot.domain.entity.Room;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;

import java.util.List;
import java.util.Optional;

public interface RoomRepository extends CrudRepository<Room, Long> {
    @Query(value =
            "SELECT r.* " +
            "FROM room r LEFT JOIN distributed_update u ON (" +
            "  u.room_id = r.id AND u.collected_update_id = :updateId " +
            ") " +
            "WHERE u.room_id IS NULL",
            nativeQuery = true)
    List<Room> findUnreachedChats(long updateId);

    Optional<Room> findByProviderAndProviderRoomKey(RoomProvider roomProvider, String providerRoomKey);
}
