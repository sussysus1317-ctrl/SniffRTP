/*
 * Decompiled with CFR 0.152.
 */
package dev.deepslate.sniffrtp.core;

import dev.deepslate.sniffrtp.core.Chat;
import dev.deepslate.sniffrtp.core.ConfigStore;
import dev.deepslate.sniffrtp.core.Geometry;
import dev.deepslate.sniffrtp.core.PreparationFuture;
import java.util.ArrayDeque;
import java.util.Collections;
import java.util.Deque;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.LongSupplier;

public final class RtpEngine<P, W> {
    private final Platform<P, W> platform;
    private final LongSupplier clock;
    private final ConcurrentMap<UUID, Session> sessions = new ConcurrentHashMap<UUID, Session>();
    private final Set<UUID> busy = ConcurrentHashMap.newKeySet();
    private final AtomicInteger active = new AtomicInteger();
    private final ConcurrentMap<UUID, Long> cooldowns = new ConcurrentHashMap<UUID, Long>();
    private final Map<String, Deque<Geometry.Point>> recent = Collections.synchronizedMap(new LinkedHashMap<String, Deque<Geometry.Point>>(this, 128, 0.75f, true){
        final /* synthetic */ RtpEngine this$0;
        {
            RtpEngine rtpEngine = this$0;
            Objects.requireNonNull(rtpEngine);
            this.this$0 = rtpEngine;
            super(arg0, arg1, arg2);
        }

        @Override
        protected boolean removeEldestEntry(Map.Entry<String, Deque<Geometry.Point>> e) {
            return this.size() > 10000;
        }
    });
    private volatile boolean closed;

    public RtpEngine(Platform<P, W> platform) {
        this(platform, System::currentTimeMillis);
    }

    public RtpEngine(Platform<P, W> platform, LongSupplier clock) {
        this.platform = platform;
        this.clock = clock;
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    private List<Geometry.Point> history(UUID id, W world) {
        Map<String, Deque<Geometry.Point>> map = this.recent;
        synchronized (map) {
            return List.copyOf(this.recent.getOrDefault(String.valueOf(id) + ":" + this.platform.worldId(world), new ArrayDeque()));
        }
    }

    public boolean pending(P p) {
        return this.sessions.containsKey(this.platform.id(p));
    }

    public boolean committing(P p) {
        Session s = (Session)this.sessions.get(this.platform.id(p));
        return s != null && s.committing;
    }

    public void start(P player, W world, String biome, ConfigStore.Settings c) {
        if (this.closed) {
            return;
        }
        UUID id = this.platform.id(player);
        boolean admin = this.platform.admin(player);
        if (this.sessions.containsKey(id)) {
            this.message(player, c, "already-pending", true, Map.of());
            return;
        }
        long remaining = this.cooldowns.getOrDefault(id, 0L) - this.clock.getAsLong();
        if (!(remaining <= 0L || admin && c.bool("adm-bypass-cooldown", true))) {
            this.message(player, c, "cooldown", true, Map.of("time", RtpEngine.seconds(remaining) + "s"));
            return;
        }
        long duration = admin ? (c.bool("midrtp-adm-stand-still-enabled", true) ? c.duration("midrtp-adm-stand-still-time", 0L) : 0L) : (c.bool("midrtp-stand-still-enabled", true) ? c.duration("midrtp-stand-still-time", 5000L) : 0L);
        Session s = new Session(this, player, world, biome, c, duration);
        if (this.sessions.putIfAbsent(id, s) != null) {
            return;
        }
        this.platform.clear(player);
        if (admin && c.bool("adm-bypass-cooldown", true)) {
            this.platform.ui(player, "admin-cooldown-bypass", Map.of(), c);
        }
        if (admin && duration == 0L) {
            this.platform.ui(player, "admin-countdown-bypass", Map.of(), c);
        }
        if (duration == 0L) {
            this.platform.ui(player, "instant-rtp", Map.of(), c);
        }
        this.platform.ui(player, "start", this.values(s, duration, null), c);
        this.tick(s);
    }

    private boolean live(Session s) {
        return !this.closed && !s.cancelled.get() && this.sessions.get(s.id) == s;
    }

    private void tick(Session s) {
        if (!this.live(s)) {
            return;
        }
        if (!this.platform.online(s.player)) {
            this.cancel(s.player, "quit", false, false);
            return;
        }
        long left = Math.max(0L, s.deadline - this.clock.getAsLong());
        int seconds = (int)((left + 999L) / 1000L);
        if (seconds != s.display || left == 0L && ++s.pulses % 20 == 0) {
            boolean changed = seconds != s.display;
            s.display = seconds;
            Map<String, String> shown = this.values(s, Math.max(1000L, left), null);
            shown.put("play_countdown_sound", Boolean.toString(changed && seconds > 0));
            this.platform.ui(s.player, "during", shown, s.config);
            if (changed && left == 0L && s.ready == null) {
                this.message(s.player, s.config, "searching", false, Map.of());
            }
        }
        if (!s.committing) {
            if (s.ready != null && left == 0L) {
                this.commit(s);
            } else if (s.ready == null && !s.loading) {
                this.search(s);
            } else if (s.loading && this.clock.getAsLong() - s.operationStart > s.config.duration("search.operation-timeout", 60000L)) {
                this.fail(s, "Chunk preparation timed out");
            }
        }
        if (this.live(s)) {
            this.platform.later(s.player, 1, () -> this.tick(s), () -> this.retire(s));
        }
    }

    private void search(Session s) {
        if (this.busy.contains(s.id)) {
            return;
        }
        int limit = s.config.integer("rtp-max-concurrent-chunk-loads", 4, 1, 32);
        if (this.active.get() >= limit) {
            return;
        }
        Geometry.Area area = this.platform.area(s.player, s.world, s.config);
        int batch = s.config.integer("search.candidates-per-batch", 100, 1, 100);
        for (int i = 0; i < batch && s.attempted < s.maximum; ++i) {
            CompletableFuture<Object> future;
            Geometry.Point p = Geometry.sample(area);
            ++s.attempted;
            long elapsed = Math.max(0L, this.clock.getAsLong() - (s.deadline - s.duration));
            double hardship = Math.max((double)Math.max(0, s.prepared - 6) * 0.06, (double)Math.max(0L, elapsed - 10000L) / 40000.0);
            int spacingAttempt = Math.max(s.attempted, (int)((double)s.maximum * Math.min(0.95, hardship)));
            if (p == null || !area.contains(p) || !Geometry.separated(p, s.history, area, spacingAttempt, s.maximum)) continue;
            if (!this.busy.add(s.id)) {
                return;
            }
            if (this.active.incrementAndGet() > limit) {
                this.active.decrementAndGet();
                this.busy.remove(s.id);
                return;
            }
            s.loading = true;
            ++s.prepared;
            s.operationStart = this.clock.getAsLong();
            try {
                future = this.platform.prepare(s.player, s.world, p, s.biome, s.config);
            }
            catch (Throwable e) {
                future = CompletableFuture.failedFuture(e);
            }
            s.preparation = future;
            future.whenComplete((lease, error) -> {
                this.active.decrementAndGet();
                this.busy.remove(s.id);
                this.platform.execute(s.player, () -> {
                    s.loading = false;
                    s.preparation = null;
                    if (!this.live(s)) {
                        this.release((Lease)lease);
                        return;
                    }
                    if (error != null) {
                        this.release((Lease)lease);
                        this.fail(s, "Chunk API failure: " + RtpEngine.root(error));
                        return;
                    }
                    s.ready = lease;
                }, () -> {
                    this.release((Lease)lease);
                    this.retire(s);
                });
            });
            return;
        }
        if (s.attempted >= s.maximum) {
            this.fail(s, null);
        }
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    private void commit(Session s) {
        CompletableFuture<Object> teleport;
        if (!this.live(s)) {
            return;
        }
        if (!this.platform.area(s.player, s.world, s.config).contains(s.ready.point())) {
            this.release(s.ready);
            s.ready = null;
            return;
        }
        Session session = s;
        synchronized (session) {
            if (!this.live(s)) {
                return;
            }
            s.committing = true;
        }
        try {
            teleport = this.platform.teleport(s.player, s.world, s.ready, s.config);
        }
        catch (Throwable error2) {
            teleport = CompletableFuture.failedFuture(error2);
        }
        teleport.whenComplete((ok, error) -> this.platform.execute(s.player, () -> {
            if (!this.live(s)) {
                this.release(s.ready);
                return;
            }
            if (error != null || !Boolean.TRUE.equals(ok)) {
                this.fail(s, error == null ? "Teleport rejected" : RtpEngine.root(error));
                return;
            }
            Geometry.Point p = s.ready.point();
            String coords = p.x() + " " + (int)s.ready.y() + " " + p.z();
            Map<String, Deque<Geometry.Point>> map = this.recent;
            synchronized (map) {
                Deque h = this.recent.computeIfAbsent(String.valueOf(s.id) + ":" + this.platform.worldId(s.world), k -> new ArrayDeque());
                h.addFirst(p);
                while (h.size() > s.config.integer("search.recent-history-size", 32, 5, 128)) {
                    h.removeLast();
                }
            }
            long cd = this.platform.admin(s.player) ? s.config.duration("default-adm-cooldown", 0L) : s.config.duration("default-cooldown", 20000L);
            this.cooldowns.put(s.id, this.clock.getAsLong() + cd);
            this.sessions.remove(s.id, s);
            s.cancelled.set(true);
            this.release(s.ready);
            s.ready = null;
            this.platform.clear(s.player);
            this.platform.ui(s.player, "after", this.values(s, 0L, coords), s.config);
        }, () -> this.retire(s)));
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public boolean cancel(P player, String reason, boolean manual, boolean automatic) {
        Session s = (Session)this.sessions.get(this.platform.id(player));
        if (s == null) {
            return false;
        }
        Session session = s;
        synchronized (session) {
            if (s.committing) {
                return false;
            }
            if (automatic && this.platform.admin(player) && s.config.bool("midrtp-adm-cancel-bypass", true)) {
                this.platform.execute(player, () -> this.platform.ui(player, "admin-cancel-bypass", Map.of("action", reason), s.config), () -> {});
                return false;
            }
            if (!s.cancelled.compareAndSet(false, true)) {
                return false;
            }
            this.sessions.remove(s.id, s);
        }
        this.stopPreparation(s);
        this.platform.execute(player, () -> {
            this.release(s.ready);
            s.ready = null;
            this.platform.clear(player);
            if (manual || automatic) {
                this.platform.ui(player, manual ? "canceled-manual" : "canceled-automatic", Map.of("action", reason, "reason", reason), s.config);
            }
        }, () -> {
            this.release(s.ready);
            s.ready = null;
        });
        return true;
    }

    public void trigger(P p, String key, String reason) {
        Session s = (Session)this.sessions.get(this.platform.id(p));
        if (s != null && s.config.bool(key, false)) {
            this.cancel(p, reason, false, true);
        }
    }

    private void fail(Session s, String detail) {
        if (!this.live(s)) {
            return;
        }
        s.cancelled.set(true);
        this.sessions.remove(s.id, s);
        this.stopPreparation(s);
        this.release(s.ready);
        s.ready = null;
        this.platform.clear(s.player);
        this.cooldowns.put(s.id, this.clock.getAsLong() + s.config.duration("default-failed-cooldown", 5000L));
        this.platform.log("[SniffRTP] RTP failed, no solid block found");
        if (detail != null) {
            this.platform.log("[SniffRTP] " + detail);
        }
        this.message(s.player, s.config, s.biome == null ? "no-safe-location" : "no-biome-location", true, Map.of("biome", s.biome == null ? "" : s.biome));
    }

    private void stopPreparation(Session s) {
        CompletableFuture<Lease> completableFuture = s.preparation;
        if (completableFuture instanceof PreparationFuture) {
            PreparationFuture future = (PreparationFuture)completableFuture;
            future.stopPreparation();
        }
    }

    private void retire(Session s) {
        s.cancelled.set(true);
        this.sessions.remove(s.id, s);
        this.stopPreparation(s);
        this.release(s.ready);
        s.ready = null;
    }

    private void release(Lease lease) {
        if (lease != null) {
            lease.close();
        }
    }

    private static String root(Throwable e) {
        while (e.getCause() != null) {
            e = e.getCause();
        }
        return e.getClass().getSimpleName() + ": " + e.getMessage();
    }

    private static String seconds(long ms) {
        return Long.toString((ms + 999L) / 1000L);
    }

    private Map<String, String> values(Session s, long left, String coords) {
        HashMap<String, String> v = new HashMap<String, String>();
        v.put("time", RtpEngine.seconds(left));
        v.put("total_time", RtpEngine.seconds(s.duration));
        v.put("progress", Double.toString(s.duration == 0L ? 0.0 : Math.min(1.0, (double)left / (double)s.duration)));
        if (coords != null) {
            v.put("coord", coords);
            v.put("coords", coords);
        }
        return v;
    }

    public void message(P p, ConfigStore.Settings c, String key, boolean error, Map<String, String> values) {
        this.platform.chat(p, Chat.fill(c.text("messages." + key, key), values), error, c);
    }

    public void close() {
        this.closed = true;
        for (Session s : List.copyOf(this.sessions.values())) {
            s.cancelled.set(true);
            this.sessions.remove(s.id, s);
            this.stopPreparation(s);
            this.platform.execute(s.player, () -> {
                this.release(s.ready);
                this.platform.clear(s.player);
            }, () -> this.release(s.ready));
        }
    }

    public int activeOperations() {
        return this.active.get();
    }

    public static interface Platform<P, W> {
        public UUID id(P var1);

        public boolean online(P var1);

        public boolean admin(P var1);

        public void execute(P var1, Runnable var2, Runnable var3);

        public void later(P var1, int var2, Runnable var3, Runnable var4);

        public Geometry.Area area(P var1, W var2, ConfigStore.Settings var3);

        public CompletableFuture<Lease> prepare(P var1, W var2, Geometry.Point var3, String var4, ConfigStore.Settings var5);

        public CompletableFuture<Boolean> teleport(P var1, W var2, Lease var3, ConfigStore.Settings var4);

        public String worldId(W var1);

        public void chat(P var1, String var2, boolean var3, ConfigStore.Settings var4);

        public void ui(P var1, String var2, Map<String, String> var3, ConfigStore.Settings var4);

        public void clear(P var1);

        public void log(String var1);
    }

    private final class Session {
        final P player;
        final W world;
        final UUID id;
        final ConfigStore.Settings config;
        final String biome;
        final long deadline;
        final long duration;
        final AtomicBoolean cancelled;
        final List<Geometry.Point> history;
        final int maximum;
        int attempted;
        int prepared;
        int display;
        int pulses;
        boolean loading;
        boolean committing;
        long operationStart;
        Lease ready;
        CompletableFuture<Lease> preparation;

        Session(RtpEngine rtpEngine, P player, W world, String biome, ConfigStore.Settings c, long duration) {
            Objects.requireNonNull(rtpEngine);
            this.cancelled = new AtomicBoolean();
            this.display = -1;
            this.player = player;
            this.world = world;
            this.id = rtpEngine.platform.id(player);
            this.biome = biome;
            this.config = c;
            this.duration = duration;
            this.deadline = rtpEngine.clock.getAsLong() + duration;
            this.maximum = Math.min(c.integer(biome == null ? "rtp-max-attempts" : "rtp-biome-max-attempts", 10000, 1, 10000), c.integer("search.candidates-per-batch", 100, 1, 100) * c.integer("search.max-batches", 100, 1, 100));
            this.history = rtpEngine.history(this.id, world);
        }
    }

    public static interface Lease
    extends AutoCloseable {
        public Geometry.Point point();

        public double y();

        @Override
        public void close();
    }
}

