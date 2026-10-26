package com.inventory.repository;


import com.inventory.entity.SeatHoldItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SeatHoldItemRepository
        extends JpaRepository<SeatHoldItem, Long> {

    @Query("""
        SELECT i
        FROM SeatHoldItem i
        WHERE i.hold.id = :holdId
        """)
    List<SeatHoldItem> findByHoldId(@Param("holdId") Long holdId);
}
