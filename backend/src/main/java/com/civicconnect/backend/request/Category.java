package com.civicconnect.backend.request;

<<<<<<< Updated upstream
/** One entry of the controlled category list (FR-002). Inactive categories cannot be chosen. */
public record Category(Long id, String name, boolean active) {
=======
import java.util.UUID;

public record Category(UUID id, String name, boolean active, int overdueAfterHours) {
>>>>>>> Stashed changes
}
