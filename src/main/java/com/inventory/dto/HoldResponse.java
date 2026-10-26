package com.inventory.dto;

import com.inventory.enums.HoldStatus;

import java.time.Instant;
import java.util.List;

public record HoldResponse(

        Long holdId,

        String holdReference,

        String bookingReference,

        Long showId,

        HoldStatus status,

        List<Long> seatIds,

        Instant expiresAt,

        Instant createdAt

) {
}
