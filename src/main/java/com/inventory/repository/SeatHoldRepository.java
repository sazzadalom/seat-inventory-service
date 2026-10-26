
package com.inventory.repository;

import com.inventory.entity.SeatHold;
import com.inventory.enums.HoldStatus;

import jakarta.persistence.LockModeType;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface SeatHoldRepository
        extends JpaRepository<SeatHold, Long> {

    // Find a hold using its unique reference.
    Optional<SeatHold> findByHoldReference(String holdReference);

    // Find a hold using the booking reference.
    Optional<SeatHold> findByBookingReference(String bookingReference);

    // Lock a hold row before changing its status.
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
        SELECT h
        FROM SeatHold h
        WHERE h.id = :holdId
        """)
    Optional<SeatHold> findByIdForUpdate(
            @Param("holdId") Long holdId
    );

    // Find active holds whose expiry time has passed.
    @Query("""
        SELECT h.id
        FROM SeatHold h
        WHERE h.status = :status
          AND h.expiresAt <= :now
        ORDER BY h.expiresAt
        """)
    List<Long> findExpiredActiveHoldIds(
            @Param("now") Instant now,
            @Param("status") HoldStatus status
    );

    // Check whether a booking already has a hold in a given state.
    boolean existsByBookingReferenceAndStatus(
            String bookingReference,
            HoldStatus status
    );
}
