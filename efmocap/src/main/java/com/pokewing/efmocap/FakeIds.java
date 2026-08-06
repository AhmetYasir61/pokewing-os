package com.pokewing.efmocap;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * Entity ids for client-only actors and cameras. Kept well outside the range
 * the server hands out so they can never collide with real entities.
 */
public final class FakeIds {
    private FakeIds() {}

    private static final AtomicInteger NEXT = new AtomicInteger(1_900_000_000);

    public static int next() {
        return NEXT.getAndIncrement();
    }
}
