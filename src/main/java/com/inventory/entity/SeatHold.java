package com.inventory.entity;

import com.inventory.enums.HoldStatus;
import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(
    name = "seat_holds",
    uniqueConstraints = {
        @UniqueConstraint(
            name = "uk_hold_reference",
            columnNames = "hold_reference"
        )
    },
    indexes = {
        @Index(name = "idx_hold_booking_reference", columnList = "booking_reference"),
        @Index(name = "idx_hold_show_status", columnList = "show_id,status"),
        @Index(name = "idx_hold_status_expiry", columnList = "status,expires_at")
    }
)
public class SeatHold {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(columnDefinition = "BIGINT UNSIGNED")
    private Long id;

    @Column(name = "hold_reference", nullable = false, unique = true, length = 64)
    private String holdReference;

    @Column(name = "booking_reference", nullable = false, length = 64)
    private String bookingReference;

    @Column(name = "show_id", nullable = false, columnDefinition = "BIGINT UNSIGNED")
    private Long showId;

    @Enumerated(EnumType.STRING)
    @Column(
        name = "status",
        nullable = false,
        columnDefinition = "ENUM('ACTIVE','CONFIRMED','EXPIRED','RELEASED')"
    )
    private HoldStatus status = HoldStatus.ACTIVE;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected SeatHold() {
    }

    public SeatHold(
        String holdReference,
        String bookingReference,
        Long showId,
        HoldStatus status,
        Instant expiresAt
    ) {
        this.holdReference = holdReference;
        this.bookingReference = bookingReference;
        this.showId = showId;
        this.status = status;
        this.expiresAt = expiresAt;
    }

    @PrePersist
    protected void onCreate() {
        Instant now = Instant.now();
        createdAt = now;
        updatedAt = now;

        if (status == null) {
            status = HoldStatus.ACTIVE;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = Instant.now();
    }

    public Long getId() {
        return id;
    }

    public String getHoldReference() {
        return holdReference;
    }

    public void setHoldReference(String holdReference) {
        this.holdReference = holdReference;
    }

    public String getBookingReference() {
        return bookingReference;
    }

    public void setBookingReference(String bookingReference) {
        this.bookingReference = bookingReference;
    }

    public Long getShowId() {
        return showId;
    }

    public void setShowId(Long showId) {
        this.showId = showId;
    }

    public HoldStatus getStatus() {
        return status;
    }

    public void setStatus(HoldStatus status) {
        this.status = status;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(Instant expiresAt) {
        this.expiresAt = expiresAt;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
