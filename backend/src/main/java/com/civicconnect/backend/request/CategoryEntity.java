package com.civicconnect.backend.request;

import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import org.hibernate.annotations.Immutable;

@Entity
@Immutable
@Table(name = "categories")
class CategoryEntity {

    @Id
    UUID id;

    @Column(nullable = false)
    String name;

    @Column(nullable = false)
    boolean active;

    @Column(name = "overdue_after_hours", nullable = false)
    int overdueAfterHours;

    protected CategoryEntity() {
    }
}
