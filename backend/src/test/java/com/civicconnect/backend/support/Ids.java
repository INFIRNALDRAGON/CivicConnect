package com.civicconnect.backend.support;

import java.util.UUID;

public final class Ids {

    private Ids() {
    }

    public static UUID of(long n) {
        return new UUID(0L, n);
    }
}
