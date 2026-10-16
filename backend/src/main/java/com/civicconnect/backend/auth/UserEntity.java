package com.civicconnect.backend.auth;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;

@Entity
@Table(name = "users")
class UserEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    UUID id;

    @Column(nullable = false, length = 100)
    String name;

    @Column(nullable = false, length = 254)
    String email;

    @Column(name = "password_hash", nullable = false, length = 100)
    String passwordHash;

    @Column(nullable = false, length = 20)
    String role;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "user_category", joinColumns = @JoinColumn(name = "user_id"))
    @Column(name = "category_id")
    Set<UUID> categoryIds = new HashSet<>();

    protected UserEntity() {
    }
}
