package com.inventory.entity;


import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(
    name = "seat_hold_items",
    uniqueConstraints = {
        @UniqueConstraint(
            name = "uk_hold_inventory",
            columnNames = {"hold_id", "show_seat_inventory_id"}
        )
    },
    indexes = {
        @Index(name = "idx_hold_item_hold", columnList = "hold_id"),
        @Index(name = "idx_hold_item_inventory", columnList = "show_seat_inventory_id")
    }
)
public class SeatHoldItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(columnDefinition = "BIGINT UNSIGNED")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
        name = "hold_id",
        nullable = false,
        foreignKey = @ForeignKey(name = "fk_hold_item_hold")
    )
    private SeatHold hold;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
        name = "show_seat_inventory_id",
        nullable = false,
        foreignKey = @ForeignKey(name = "fk_hold_item_inventory")
    )
    private ShowSeatInventory showSeatInventory;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected SeatHoldItem() {
    }

    public SeatHoldItem(SeatHold hold, ShowSeatInventory showSeatInventory) {
        this.hold = hold;
        this.showSeatInventory = showSeatInventory;
    }

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
    }

    public Long getId() {
        return id;
    }

    public SeatHold getHold() {
        return hold;
    }

    public void setHold(SeatHold hold) {
        this.hold = hold;
    }

    public Long getHoldId() {
        return hold != null ? hold.getId() : null;
    }

    public ShowSeatInventory getShowSeatInventory() {
        return showSeatInventory;
    }

    public void setShowSeatInventory(ShowSeatInventory showSeatInventory) {
        this.showSeatInventory = showSeatInventory;
    }

    public Long getShowSeatInventoryId() {
        return showSeatInventory != null ? showSeatInventory.getId() : null;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
