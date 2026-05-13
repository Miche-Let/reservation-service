package com.michelet.reservation.infrastructure.outbox;

import com.michelet.reservation.infrastructure.outbox.entity.OutboxEventJpaEntity;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface OutboxEventJpaRepository extends JpaRepository<OutboxEventJpaEntity, UUID> {

    @Query(
            value = "SELECT * FROM p_outbox_events"
                    + " WHERE status = 'PENDING'"
                    + " ORDER BY created_at"
                    + " LIMIT 50"
                    + " FOR UPDATE SKIP LOCKED",
            nativeQuery = true
    )
    List<OutboxEventJpaEntity> findPendingForProcessing();
}
