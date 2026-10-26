package com.inventory.service;

import com.inventory.dto.SeatAvailabilityResponse;
import com.inventory.entity.ShowSeatInventory;
import com.inventory.repository.ShowSeatInventoryRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.HashSet;
import java.util.List;

@Service
public class SeatInventoryService {

    private final ShowSeatInventoryRepository inventoryRepository;

    public SeatInventoryService(
            ShowSeatInventoryRepository inventoryRepository) {
        this.inventoryRepository = inventoryRepository;
    }

    @Transactional
    public void initializeInventory(Long showId, List<Long> seatIds) {
        if (showId == null || showId <= 0) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "Invalid show ID");
        }

        if (seatIds == null || seatIds.isEmpty()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "Seat IDs are required");
        }

        if (seatIds.stream().anyMatch(id -> id == null || id <= 0)) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "Seat IDs must be positive");
        }

        if (new HashSet<>(seatIds).size() != seatIds.size()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "Duplicate seat IDs supplied");
        }

        List<ShowSeatInventory> inventory = seatIds.stream()
                .map(seatId -> new ShowSeatInventory(showId, seatId))
                .toList();

        inventoryRepository.saveAll(inventory);
    }

    @Transactional(readOnly = true)
    public List<SeatAvailabilityResponse> getSeatAvailability(Long showId) {
        if (showId == null || showId <= 0) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "Invalid show ID");
        }

        List<ShowSeatInventory> inventory =
                inventoryRepository.findByShowIdOrderBySeatId(showId);

        if (inventory.isEmpty()) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "No seat inventory found for show " + showId);
        }

        return inventory.stream()
                .map(seat -> new SeatAvailabilityResponse(
                        seat.getSeatId(),
                        seat.getStatus()))
                .toList();
    }
}
