package com.inventory.service;


import com.inventory.entity.SeatHold;
import com.inventory.entity.SeatHoldItem;
import com.inventory.entity.ShowSeatInventory;
import com.inventory.enums.HoldStatus;
import com.inventory.enums.SeatStatus;
import com.inventory.repository.SeatHoldItemRepository;
import com.inventory.repository.SeatHoldRepository;
import com.inventory.repository.ShowSeatInventoryRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Service
public class SeatHoldExpiryService {

    private final SeatHoldRepository holdRepository;
    private final SeatHoldItemRepository holdItemRepository;
    private final ShowSeatInventoryRepository inventoryRepository;

    public SeatHoldExpiryService(
            SeatHoldRepository holdRepository,
            SeatHoldItemRepository holdItemRepository,
            ShowSeatInventoryRepository inventoryRepository) {
        this.holdRepository = holdRepository;
        this.holdItemRepository = holdItemRepository;
        this.inventoryRepository = inventoryRepository;
    }

    /**
     * Process expired holds in batches.
     */
    public int expireHolds() {
        List<Long> expiredHoldIds = holdRepository.findExpiredActiveHoldIds(Instant.now(),HoldStatus.ACTIVE);

        int count = 0;

        for (Long holdId : expiredHoldIds) {
            if (expireSingleHold(holdId)) {
                count++;
            }
        }

        return count;
    }

    @Transactional
    public boolean expireSingleHold(Long holdId) {
        SeatHold hold = holdRepository.findByIdForUpdate(holdId)
                .orElse(null);

        if (hold == null
                || hold.getStatus() != HoldStatus.ACTIVE
                || hold.getExpiresAt().isAfter(Instant.now())) {
            return false;
        }

        List<SeatHoldItem> items =
                holdItemRepository.findByHoldId(holdId);

        List<Long> seatIds = items.stream()
                .map(item -> item.getShowSeatInventory().getSeatId())
                .sorted()
                .toList();

        List<ShowSeatInventory> seats =
                inventoryRepository.findByShowIdAndSeatIdInOrderBySeatIdForUpdate(
                        hold.getShowId(), seatIds);

        for (ShowSeatInventory seat : seats) {
            if (seat.getStatus() == SeatStatus.HELD
                    && holdId.equals(seat.getHoldId())) {
                seat.setStatus(SeatStatus.AVAILABLE);
                seat.setHoldId(null);
                inventoryRepository.save(seat);
            }
        }

        hold.setStatus(HoldStatus.EXPIRED);
        holdRepository.save(hold);

        return true;
    }
}
