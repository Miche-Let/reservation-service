package com.michelet.reservation.infrastructure.client.dto;

import java.util.UUID;

public record RestaurantOwnerResponse(
    UUID restaurantId,
    UUID ownerId
) {}
