package com.civicconnect.backend.dto;

import com.civicconnect.backend.request.RequestStatus;

/** Body of the "change status" call. {@code version} is the value the client last saw (ASR-03). */
public record StatusChangeDto(RequestStatus target, String comment, Long version) {
}
