package com.michelet.reservation.infrastructure.client.dto;

import java.util.UUID;

public record TicketClientResponse(
    UUID courseId,
    int unitPrice
) {}
