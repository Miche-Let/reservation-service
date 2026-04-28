package com.michelet.reservation.presentation.reservation.dto.response;

import java.util.UUID;

public record ReservationCourseResponse(
    UUID courseId,
    int quantity,
    int unitPrice
) {}