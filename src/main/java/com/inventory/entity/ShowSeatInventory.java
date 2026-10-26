package com.inventory.entity;

import com.inventory.enums.SeatStatus;
import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(
    name = "show_seat_inventory",
    uniqueConstraints = {
        @UniqueConstraint(
            name = "uk_show_seat",
            columnNames = {"show_id", "seat_id"}
        )
    },
    indexes = {
        @Index(name = "idx_show_status", columnList = "show_id,status"),
        @Index(name = "idx_inventory_hold", columnList = "hold_id")
    }
)
public class ShowSeatInventory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(columnDefinition = "BIGINT UNSIGNED")
    private Long id;

    @Column(name = "show_id", nullable = false, columnDefinition = "BIGINT UNSIGNED")
    private Long showId;

    @Column(name = "seat_id", nullable = false, columnDefinition = "BIGINT UNSIGNED")
    private Long seatId;

    @Enumerated(EnumType.STRING)
    @Column(
        name = "status",
        nullable = false,
        columnDefinition = "ENUM('AVAILABLE','HELD','BOOKED')"
    )
    private SeatStatus status = SeatStatus.AVAILABLE;

    @Column(name = "hold_id", columnDefinition = "BIGINT UNSIGNED")
    private Long holdId;

    @Version
    @Column(name = "version", nullable = false)
    private Long version = 0L;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected ShowSeatInventory() {
    }

    public ShowSeatInventory(Long showId, Long seatId) {
        this.showId = showId;
        this.seatId = seatId;
        this.status = SeatStatus.AVAILABLE;
    }

    @PrePersist
    protected void onCreate() {
        Instant now = Instant.now();
        createdAt = now;
        updatedAt = now;

        if (status == null) {
            status = SeatStatus.AVAILABLE;
        }
        if (version == null) {
            version = 0L;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = Instant.now();
    }

    public Long getId() {
        return id;
    }

    public Long getShowId() {
        return showId;
    }

    public void setShowId(Long showId) {
        this.showId = showId;
    }

    public Long getSeatId() {
        return seatId;
    }

    public void setSeatId(Long seatId) {
        this.seatId = seatId;
    }

    public SeatStatus getStatus() {
        return status;
    }

    public void setStatus(SeatStatus status) {
        this.status = status;
    }

    public Long getHoldId() {
        return holdId;
    }

    public void setHoldId(Long holdId) {
        this.holdId = holdId;
    }

    public Long getVersion() {
        return version;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
