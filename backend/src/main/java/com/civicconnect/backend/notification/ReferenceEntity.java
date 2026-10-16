package com.civicconnect.backend.notification;

import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import org.hibernate.annotations.Immutable;

@Entity
@Immutable
@Table(name = "v_request_overview")
class ReferenceEntity {

    @Id
    UUID id;

    @Column(name = "reference_number")
    String referenceNumber;

    protected ReferenceEntity() {
    }
}
