package com.michelet.reservation.infrastructure.outbox;

public enum OutboxStatus {
    PENDING, PROCESSING, PROCESSED, FAILED
}
