package com.civicconnect.backend.request;

/**
 * Staff queue criteria (FR-007). Every field is optional.
 *
 * @param text matches against the reference number or description, case-insensitive
 */
public record RequestFilter(Long categoryId, RequestStatus status, String text, RequestSort sort) {

    public static RequestFilter none() {
        return new RequestFilter(null, null, null, RequestSort.OLDEST_FIRST);
    }

    public RequestSort sortOrDefault() {
        return sort == null ? RequestSort.OLDEST_FIRST : sort;
    }
}
