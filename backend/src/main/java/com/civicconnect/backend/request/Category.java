package com.civicconnect.backend.request;

import java.util.UUID;

public record Category(UUID id, String name, boolean active, int overdueAfterHours) {
}
