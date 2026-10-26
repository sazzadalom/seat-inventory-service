package com.inventory.repository;

import com.inventory.entity.ShowSeatInventory;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;

public interface ShowSeatInventoryRepository
        extends JpaRepository<ShowSeatInventory, Long> {

    List<ShowSeatInventory> findByShowIdOrderBySeatId(Long showId);

    @Query("""
        SELECT s
        FROM ShowSeatInventory s
        WHERE s.showId = :showId
          AND s.seatId IN :seatIds
        ORDER BY s.seatId
        """)
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    List<ShowSeatInventory> findByShowIdAndSeatIdInOrderBySeatIdForUpdate(
            @Param("showId") Long showId,
            @Param("seatIds") Collection<Long> seatIds
    );

    boolean existsByShowIdAndSeatId(Long showId, Long seatId);

    long countByShowId(Long showId);
}
