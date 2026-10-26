package com.inventory.controller;

import com.inventory.dto.CreateHoldRequest;
import com.inventory.dto.HoldResponse;
import com.inventory.service.SeatHoldService;
import com.inventory.service.SeatHoldExpiryService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1")
public class SeatHoldController {

    private final SeatHoldService seatHoldService;
    private final SeatHoldExpiryService seatHoldExpiryService;

    public SeatHoldController(
            SeatHoldService seatHoldService,
            SeatHoldExpiryService seatHoldExpiryService) {
        this.seatHoldService = seatHoldService;
        this.seatHoldExpiryService = seatHoldExpiryService;
    }

    /**
     * Temporarily hold seats for a show.
     *
     * POST /api/v1/shows/501/holds
     */
    @PostMapping("/shows/{showId}/holds")
    @ResponseStatus(HttpStatus.CREATED)
    public HoldResponse createHold(
            @PathVariable @NotNull Long showId,
            @Valid @RequestBody CreateHoldRequest request) {

        return seatHoldService.createHold(showId, request);
    }

    /**
     * Retrieve the current status of a hold.
     *
     * GET /api/v1/holds/1001
     */
    @GetMapping("/holds/{holdId}")
    public HoldResponse getHold(
            @PathVariable @NotNull Long holdId) {

        return seatHoldService.getHold(holdId);
    }

    /**
     * Confirm a hold after the booking workflow
     * has authorized the booking/payment.
     *
     * POST /api/v1/holds/1001/confirm
     */
    @PostMapping("/holds/{holdId}/confirm")
    public HoldResponse confirmHold(
            @PathVariable @NotNull Long holdId) {

        return seatHoldService.confirmHold(holdId);
    }

    /**
     * Release a hold and make its seats available again.
     *
     * DELETE /api/v1/holds/1001
     */
    @DeleteMapping("/holds/{holdId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void releaseHold(
            @PathVariable @NotNull Long holdId) {

        seatHoldService.releaseHold(holdId);
    }

    /**
     * Trigger expiration processing.
     *
     * POST /api/v1/holds/expire
     *
     * Protect this endpoint with internal-service
     * authentication or restrict it to internal access.
     * Prefer a scheduled job for normal operation.
     */
    @PostMapping("/holds/expire")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void expireHolds() {
        seatHoldExpiryService.expireHolds();
    }
}
