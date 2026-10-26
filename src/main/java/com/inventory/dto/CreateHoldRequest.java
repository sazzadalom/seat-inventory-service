package com.inventory.dto;


import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

public record CreateHoldRequest(

        @NotBlank(message = "Booking reference is required")
        @Size(max = 64, message = "Booking reference cannot exceed 64 characters")
        String bookingReference,

        @NotEmpty(message = "At least one seat must be selected")
        @Size(max = 10, message = "A maximum of 10 seats can be held per request")
        List<@NotNull(message = "Seat ID cannot be null")
             @Min(value = 1, message = "Seat ID must be positive") Long> seatIds,

        @Min(value = 60, message = "Hold duration must be at least 60 seconds")
        @Max(value = 300, message = "Hold duration cannot exceed 300 seconds")
        Integer holdDurationSeconds

) {
}
