package com.petlog.events;

import com.petlog.pet.Pet;
import com.petlog.user.User;
import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.type.SqlTypes;

import java.time.OffsetDateTime;
import java.util.Map;

@Entity
@Table(name = "events")
public class Event {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by", nullable = false)
    private User createdBy;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "pet_id", nullable = false)
    private Pet pet;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 30)
    private EventType type;

    @Column(name = "occurred_at", nullable = false)
    private OffsetDateTime occurredAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "data", nullable = false, columnDefinition = "jsonb")
    private Map<String, Object> data;

    @Column(name = "deleted_at")
    private OffsetDateTime deletedAt;

    public Event() {
    }

    public Event(
        User createdBy,
        Pet pet,
        EventType type,
        OffsetDateTime occurredAt,
        Map<String, Object> data
    ) {
        this.createdBy = createdBy;
        this.pet = pet;
        this.type = type;
        this.occurredAt = occurredAt;
        this.data = data;
    }

    public User getCreatedBy() {
        return createdBy;
    }

    public Pet getPet() {
        return pet;
    }

    public EventType getType() {
        return type;
    }

    public OffsetDateTime getOccurredAt() {
        return occurredAt;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public OffsetDateTime getUpdatedAt() {
        return updatedAt;
    }

    public Map<String, Object> getData() {
        return data;
    }

    public Long getId() {
        return id;
    }

    public void setOccurredAt(OffsetDateTime occurredAt) {
        this.occurredAt = occurredAt;
    }

    public void setData(Map<String, Object> data) {
        this.data = data;
    }

    public OffsetDateTime getDeletedAt() {
        return deletedAt;
    }

    public void setDeletedAt(OffsetDateTime deletedAt) {
        this.deletedAt = deletedAt;
    }
}
