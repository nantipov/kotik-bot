package org.nantipov.kotikbot.respository;

import org.nantipov.kotikbot.domain.entity.CollectedUpdate;
import org.springframework.data.repository.CrudRepository;

import java.time.OffsetDateTime;
import java.util.List;

public interface CollectedUpdateRepository extends CrudRepository<CollectedUpdate, Long> {
    boolean existsBySupplierAndUpdateKey(String supplier, String updateKey);

    List<CollectedUpdate> findByActualTillAfter(OffsetDateTime date);
}
