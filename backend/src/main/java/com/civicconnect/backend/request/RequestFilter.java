package com.civicconnect.backend.request;

import java.util.UUID;

public record RequestFilter(UUID categoryId, RequestStatus status, String text, RequestSort sort) {

    public static RequestFilter none() {
        return new RequestFilter(null, null, null, RequestSort.OLDEST_FIRST);
    }

    public RequestSort sortOrDefault() {
        return sort == null ? RequestSort.OLDEST_FIRST : sort;
    }
}
