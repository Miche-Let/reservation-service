package com.michelet.reservation.application.port;

import java.util.UUID;

public interface OutboxEventPort {
    void record(UUID aggregateId, String aggregateType, String eventType, Object payload);
}
