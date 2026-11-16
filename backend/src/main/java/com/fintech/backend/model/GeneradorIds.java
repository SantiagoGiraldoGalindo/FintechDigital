package com.fintech.backend.model;

import java.util.concurrent.atomic.AtomicLong;

final class GeneradorIds {

    private static final AtomicLong SIGUIENTE_ID = new AtomicLong(1);

    private GeneradorIds() {
    }

    static long siguienteId() {
        return SIGUIENTE_ID.getAndIncrement();
    }
}
