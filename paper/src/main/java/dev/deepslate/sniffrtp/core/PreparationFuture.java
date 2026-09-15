/*
 * Decompiled with CFR 0.152.
 */
package dev.deepslate.sniffrtp.core;

import dev.deepslate.sniffrtp.core.RtpEngine;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;

public final class PreparationFuture
extends CompletableFuture<RtpEngine.Lease> {
    private final AtomicBoolean stopped = new AtomicBoolean();
    private final Runnable cleanup;

    public PreparationFuture(Runnable cleanup) {
        this.cleanup = cleanup;
    }

    public boolean stopped() {
        return this.stopped.get();
    }

    public void stopPreparation() {
        if (this.stopped.compareAndSet(false, true)) {
            this.cleanup.run();
        }
    }
}

