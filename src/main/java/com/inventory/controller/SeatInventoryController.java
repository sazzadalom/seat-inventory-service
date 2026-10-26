package com.inventory.controller;

import com.inventory.dto.SeatAvailabilityResponse;
import com.inventory.service.SeatInventoryService;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/shows")
public class SeatInventoryController {

    private final SeatInventoryService seatInventoryService;

    public SeatInventoryController(
            SeatInventoryService seatInventoryService) {
        this.seatInventoryService = seatInventoryService;
    }

    /**
     * Initialize seat inventory for a show.
     *
     * Example:
     * POST /api/v1/shows/501/inventory
     * Body: [101, 102, 103, 104]
     */
    @PostMapping("/{showId}/inventory")
    @ResponseStatus(HttpStatus.CREATED)
    public void initializeInventory(
            @PathVariable @NotNull Long showId,
            @RequestBody @NotEmpty List<@NotNull Long> seatIds) {

        seatInventoryService.initializeInventory(showId, seatIds);
    }

    /**
     * Get availability of all seats for a show.
     *
     * Example:
     * GET /api/v1/shows/501/seats
     */
    @GetMapping("/{showId}/seats")
    public List<SeatAvailabilityResponse> getSeatAvailability(
            @PathVariable @NotNull Long showId) {

        return seatInventoryService.getSeatAvailability(showId);
    }
}
