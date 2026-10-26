package com.inventory.service;
import com.inventory.dto.CreateHoldRequest;
import com.inventory.dto.HoldResponse;
import com.inventory.entity.SeatHold;
import com.inventory.entity.SeatHoldItem;
import com.inventory.entity.ShowSeatInventory;
import com.inventory.enums.HoldStatus;
import com.inventory.enums.SeatStatus;
import com.inventory.repository.SeatHoldItemRepository;
import com.inventory.repository.SeatHoldRepository;
import com.inventory.repository.ShowSeatInventoryRepository;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
public class SeatHoldService {

    private static final int DEFAULT_HOLD_DURATION_SECONDS = 300;
    private static final int MIN_HOLD_DURATION_SECONDS = 60;
    private static final int MAX_HOLD_DURATION_SECONDS = 300;

    private final SeatHoldRepository holdRepository;
    private final SeatHoldItemRepository holdItemRepository;
    private final ShowSeatInventoryRepository inventoryRepository;

    public SeatHoldService(
            SeatHoldRepository holdRepository,
            SeatHoldItemRepository holdItemRepository,
            ShowSeatInventoryRepository inventoryRepository
    ) {
        this.holdRepository = holdRepository;
        this.holdItemRepository = holdItemRepository;
        this.inventoryRepository = inventoryRepository;
    }

    /**
     * Creates a temporary hold on the requested seats.
     */
    @Transactional
    public HoldResponse createHold(
            Long showId,
            CreateHoldRequest request
    ) {
        validateShowId(showId);

        if (request == null) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Hold request must not be null"
            );
        }

        if (request.bookingReference() == null
                || request.bookingReference().isBlank()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Booking reference is required"
            );
        }

        List<Long> seatIds = validateAndSortSeatIds(request.seatIds());

        int duration = request.holdDurationSeconds() == null
                ? DEFAULT_HOLD_DURATION_SECONDS
                : request.holdDurationSeconds();

        if (duration < MIN_HOLD_DURATION_SECONDS
                || duration > MAX_HOLD_DURATION_SECONDS) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Hold duration must be between 60 and 300 seconds"
            );
        }

        // Lock selected inventory rows within this transaction.
        List<ShowSeatInventory> seats =
                inventoryRepository
                        .findByShowIdAndSeatIdInOrderBySeatIdForUpdate(
                                showId,
                                seatIds
                        );

        if (seats.size() != seatIds.size()) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "One or more seats do not exist for show " + showId
            );
        }

        // Release expired holds that are blocking requested seats.
        Set<Long> expiredHoldIds = new HashSet<>();

        for (ShowSeatInventory seat : seats) {
            if (seat.getStatus() != SeatStatus.HELD
                    || seat.getHoldId() == null) {
                continue;
            }

            Long existingHoldId = seat.getHoldId();

            if (expiredHoldIds.contains(existingHoldId)) {
                continue;
            }

            SeatHold existingHold = holdRepository
                    .findById(existingHoldId)
                    .orElse(null);

            if (existingHold != null
                    && existingHold.getStatus() == HoldStatus.ACTIVE
                    && !existingHold.getExpiresAt().isAfter(Instant.now())) {

                releaseSeatsForHold(existingHold);
                existingHold.setStatus(HoldStatus.EXPIRED);
                holdRepository.save(existingHold);

                expiredHoldIds.add(existingHoldId);
            }
        }

        // Refresh the selected rows after releasing expired holds.
        seats = inventoryRepository
                .findByShowIdAndSeatIdInOrderBySeatIdForUpdate(
                        showId,
                        seatIds
                );

        for (ShowSeatInventory seat : seats) {
            if (seat.getStatus() != SeatStatus.AVAILABLE) {
                throw new ResponseStatusException(
                        HttpStatus.CONFLICT,
                        "Seat " + seat.getSeatId()
                                + " is not available for show " + showId
                );
            }
        }

        Instant now = Instant.now();

        SeatHold hold = new SeatHold(
                generateHoldReference(),
                request.bookingReference(),
                showId,
                HoldStatus.ACTIVE,
                now.plusSeconds(duration)
        );

        hold = holdRepository.save(hold);

        List<SeatHoldItem> items = new ArrayList<>();

        for (ShowSeatInventory seat : seats) {
            seat.setStatus(SeatStatus.HELD);
            seat.setHoldId(hold.getId());

            items.add(new SeatHoldItem(hold, seat));
        }

        inventoryRepository.saveAll(seats);
        holdItemRepository.saveAll(items);

        return toResponse(hold, items);
    }

    /**
     * Returns the details of a hold.
     */
    @Transactional(readOnly = true)
    public HoldResponse getHold(Long holdId) {
        SeatHold hold = getHoldOrThrow(holdId);

        List<SeatHoldItem> items =
                holdItemRepository.findByHoldId(holdId);

        return toResponse(hold, items);
    }

    /**
     * Confirms an active hold and marks its seats as booked.
     */
    @Transactional
    public HoldResponse confirmHold(Long holdId) {
        SeatHold hold = holdRepository
                .findByIdForUpdate(holdId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Hold not found: " + holdId
                ));

        List<SeatHoldItem> items =
                holdItemRepository.findByHoldId(holdId);

        if (hold.getStatus() == HoldStatus.CONFIRMED) {
            return toResponse(hold, items);
        }

        if (hold.getStatus() != HoldStatus.ACTIVE) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Cannot confirm a hold with status " + hold.getStatus()
            );
        }

        // Do not throw after changing the status: that would roll back expiry.
        if (!hold.getExpiresAt().isAfter(Instant.now())) {
            releaseSeatsForHold(hold);
            hold.setStatus(HoldStatus.EXPIRED);
            holdRepository.save(hold);

            return toResponse(hold, items);
        }

        List<Long> seatIds = items.stream()
                .map(item -> item.getShowSeatInventory().getSeatId())
                .sorted()
                .toList();

        List<ShowSeatInventory> seats =
                inventoryRepository
                        .findByShowIdAndSeatIdInOrderBySeatIdForUpdate(
                                hold.getShowId(),
                                seatIds
                        );

        if (seats.size() != seatIds.size()) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Some held seats no longer exist in inventory"
            );
        }

        for (ShowSeatInventory seat : seats) {
            if (seat.getStatus() != SeatStatus.HELD
                    || !holdId.equals(seat.getHoldId())) {
                throw new ResponseStatusException(
                        HttpStatus.CONFLICT,
                        "Seat " + seat.getSeatId()
                                + " is not held by hold " + holdId
                );
            }
        }

        for (ShowSeatInventory seat : seats) {
            seat.setStatus(SeatStatus.BOOKED);
            // Keep holdId to retain the relationship to the confirmed hold.
        }

        hold.setStatus(HoldStatus.CONFIRMED);

        inventoryRepository.saveAll(seats);
        holdRepository.save(hold);

        return toResponse(hold, items);
    }

    /**
     * Releases an active hold and makes its seats available again.
     */
    @Transactional
    public HoldResponse releaseHold(Long holdId) {
        SeatHold hold = holdRepository
                .findByIdForUpdate(holdId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Hold not found: " + holdId
                ));

        if (hold.getStatus() == HoldStatus.RELEASED) {
            return toResponse(
                    hold,
                    holdItemRepository.findByHoldId(holdId)
            );
        }

        if (hold.getStatus() != HoldStatus.ACTIVE) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Cannot release a hold with status " + hold.getStatus()
            );
        }

        List<SeatHoldItem> items =
                holdItemRepository.findByHoldId(holdId);

        releaseSeatsForHold(hold);

        hold.setStatus(HoldStatus.RELEASED);
        holdRepository.save(hold);

        return toResponse(hold, items);
    }

    /**
     * Releases inventory seats only when they still belong to this hold.
     */
    private void releaseSeatsForHold(SeatHold hold) {
        List<SeatHoldItem> items =
                holdItemRepository.findByHoldId(hold.getId());

        for (SeatHoldItem item : items) {
            ShowSeatInventory seat = item.getShowSeatInventory();

            if (seat.getStatus() == SeatStatus.HELD
                    && hold.getId().equals(seat.getHoldId())) {
                seat.setStatus(SeatStatus.AVAILABLE);
                seat.setHoldId(null);
            }
        }

        // Managed entities are updated by JPA dirty checking.
    }

    private SeatHold getHoldOrThrow(Long holdId) {
        if (holdId == null || holdId <= 0) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Hold ID must be positive"
            );
        }

        return holdRepository.findById(holdId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Hold not found: " + holdId
                ));
    }

    private void validateShowId(Long showId) {
        if (showId == null || showId <= 0) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Show ID must be positive"
            );
        }
    }

    private List<Long> validateAndSortSeatIds(List<Long> seatIds) {
        if (seatIds == null || seatIds.isEmpty()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "At least one seat must be selected"
            );
        }

        if (seatIds.size() > 10) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "A maximum of 10 seats can be held per request"
            );
        }

        if (seatIds.stream().anyMatch(
                id -> id == null || id <= 0)) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Seat IDs must be positive"
            );
        }

        List<Long> sortedSeatIds = seatIds.stream()
                .sorted()
                .toList();

        if (new HashSet<>(sortedSeatIds).size() != sortedSeatIds.size()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Duplicate seat IDs are not allowed"
            );
        }

        return sortedSeatIds;
    }

    private String generateHoldReference() {
        return "HOLD-" + UUID.randomUUID()
                .toString()
                .replace("-", "")
                .toUpperCase();
    }

    private HoldResponse toResponse(
            SeatHold hold,
            List<SeatHoldItem> items
    ) {
        List<Long> seatIds = items.stream()
                .map(item -> item.getShowSeatInventory().getSeatId())
                .sorted()
                .toList();

        return new HoldResponse(
                hold.getId(),
                hold.getHoldReference(),
                hold.getBookingReference(),
                hold.getShowId(),
                hold.getStatus(),
                seatIds,
                hold.getExpiresAt(),
                hold.getCreatedAt()
        );
    }
}
