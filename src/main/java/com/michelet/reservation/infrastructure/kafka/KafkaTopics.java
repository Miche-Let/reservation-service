package com.michelet.reservation.infrastructure.kafka;

public final class KafkaTopics {

    public static final String RESERVATION_CREATED   = "reservation.created";
    public static final String RESERVATION_CANCELLED = "reservation.cancelled";
    public static final String RESERVATION_DELETED   = "reservation.deleted";
    public static final String RESERVATION_CHECKED_IN = "reservation.checked-in";
    public static final String WAITING_COMPLETED     = "waiting.completed";

    public static final String SLOT_DEDUCTED    = "slot.deducted";
    public static final String SLOT_UNAVAILABLE = "slot.unavailable";

    private KafkaTopics() {}
}
