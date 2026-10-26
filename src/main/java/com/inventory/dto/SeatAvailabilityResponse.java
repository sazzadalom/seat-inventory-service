package com.inventory.dto;

import com.inventory.enums.SeatStatus;

public record SeatAvailabilityResponse(

        Long seatId,

        SeatStatus status

) {
}
