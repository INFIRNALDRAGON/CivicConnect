package com.civicconnect.backend.request;

/** One entry of the controlled category list (FR-002). Inactive categories cannot be chosen. */
public record Category(Long id, String name, boolean active) {
}
