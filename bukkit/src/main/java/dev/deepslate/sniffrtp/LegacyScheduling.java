/*
 * Decompiled with CFR 0.152.
 */
package dev.deepslate.sniffrtp;

import dev.deepslate.sniffrtp.BukkitPlatform;
import dev.deepslate.sniffrtp.SniffRTPPlugin;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.plugin.Plugin;

public final class LegacyScheduling
implements BukkitPlatform.Scheduling {
    final SniffRTPPlugin plugin;
    private final Method handle;
    private final Method source;
    private final Method load;
    private final Method remove;
    private final Method loaded;
    private final Constructor<?> position;
    private final Object ticket;
    private final Map<String, Pending> pending = new HashMap<String, Pending>();
    private boolean closed;

    public LegacyScheduling(SniffRTPPlugin plugin) {
        this.plugin = plugin;
        try {
            Class<?> world = Class.forName("org.bukkit.craftbukkit.CraftWorld");
            Class<?> level = Class.forName("net.minecraft.server.level.ServerLevel");
            Class<?> cache = Class.forName("net.minecraft.server.level.ServerChunkCache");
            Class<?> type = Class.forName("net.minecraft.server.level.TicketType");
            Class<?> pos = Class.forName("net.minecraft.world.level.ChunkPos");
            this.handle = world.getMethod("getHandle", new Class[0]);
            this.source = level.getMethod("getChunkSource", new Class[0]);
            this.position = pos.getConstructor(Integer.TYPE, Integer.TYPE);
            this.load = cache.getMethod("addTicketAndLoadWithRadius", type, pos, Integer.TYPE);
            this.remove = cache.getMethod("removeTicketWithRadius", type, pos, Integer.TYPE);
            this.loaded = cache.getMethod("getChunkNow", Integer.TYPE, Integer.TYPE);
            this.ticket = type.getConstructor(Long.TYPE, Integer.TYPE).newInstance(118237L, type.getField("FLAG_LOADING").getInt(null));
        }
        catch (ReflectiveOperationException error) {
            throw new IllegalStateException("Minecraft 26.2 asynchronous chunk bridge unavailable", error);
        }
        plugin.getLogger().info("Using native nonblocking chunk preparation for Bukkit/Spigot.");
    }

    @Override
    public void player(Player p, int ticks, Runnable r, Runnable retired) {
        if (!this.plugin.isEnabled()) {
            retired.run();
            return;
        }
        Bukkit.getScheduler().runTaskLater((Plugin)this.plugin, () -> {
            if (p.isOnline()) {
                r.run();
            } else {
                retired.run();
            }
        }, (long)Math.max(1, ticks));
    }

    @Override
    public void region(World w, int x, int z, Runnable r) {
        if (Bukkit.isPrimaryThread()) {
            r.run();
        } else if (this.plugin.isEnabled()) {
            Bukkit.getScheduler().runTask((Plugin)this.plugin, r);
        }
    }

    @Override
    public void clock(Runnable r) {
        Bukkit.getScheduler().runTaskTimer((Plugin)this.plugin, r, 1L, 1L);
    }

    @Override
    public CompletableFuture<Chunk> chunk(World world, int x, int z, boolean generate, boolean urgent) {
        if (!Bukkit.isPrimaryThread()) {
            return CompletableFuture.failedFuture(new IllegalStateException("Chunk requests must run on server thread"));
        }
        if (this.closed) {
            return CompletableFuture.failedFuture(new CancellationException("Plugin stopped"));
        }
        String key = String.valueOf(world.getUID()) + ":" + x + ":" + z;
        Pending existing = this.pending.get(key);
        if (existing != null) {
            return existing.result;
        }
        try {
            Object cache = this.source.invoke(this.handle.invoke(world, new Object[0]), new Object[0]);
            if (!generate) {
                for (int dx = -1; dx <= 1; ++dx) {
                    for (int dz = -1; dz <= 1; ++dz) {
                        if (this.loaded.invoke(cache, x + dx, z + dz) != null) continue;
                        return CompletableFuture.completedFuture(null);
                    }
                }
            }
            Pending request = new Pending(this, cache, this.position.newInstance(x, z));
            this.pending.put(key, request);
            try {
                CompletableFuture nativeFuture = (CompletableFuture)this.load.invoke(cache, this.ticket, request.pos, 1);
                nativeFuture.whenComplete((value, error) -> this.region(world, x, z, () -> {
                    if (this.closed) {
                        return;
                    }
                    this.pending.remove(key, request);
                    try {
                        if (error != null) {
                            request.result.completeExceptionally((Throwable)error);
                        } else {
                            request.result.complete(this.loaded.invoke(cache, x, z) == null ? null : world.getChunkAt(x, z, false));
                        }
                    }
                    catch (Throwable failure) {
                        request.result.completeExceptionally(failure);
                    }
                    finally {
                        request.release();
                    }
                }));
            }
            catch (Throwable error2) {
                this.pending.remove(key, request);
                request.release();
                request.result.completeExceptionally(error2);
            }
            return request.result;
        }
        catch (Throwable error3) {
            return CompletableFuture.failedFuture(error3);
        }
    }

    @Override
    public CompletableFuture<Boolean> teleport(Player player, Location destination) {
        return this.chunk(destination.getWorld(), destination.getBlockX() >> 4, destination.getBlockZ() >> 4, true, false).thenApply(chunk -> chunk != null && player.isOnline() && player.teleport(destination, PlayerTeleportEvent.TeleportCause.PLUGIN));
    }

    @Override
    public void close() {
        this.closed = true;
        for (Pending request : List.copyOf(this.pending.values())) {
            request.release();
            request.result.completeExceptionally(new CancellationException("Plugin stopped"));
        }
        this.pending.clear();
        Bukkit.getScheduler().cancelTasks((Plugin)this.plugin);
    }

    private final class Pending {
        final Object cache;
        final Object pos;
        final CompletableFuture<Chunk> result;
        final /* synthetic */ LegacyScheduling this$0;

        Pending(LegacyScheduling legacyScheduling, Object cache, Object pos) {
            LegacyScheduling legacyScheduling2 = legacyScheduling;
            Objects.requireNonNull(legacyScheduling2);
            this.this$0 = legacyScheduling2;
            this.result = new CompletableFuture();
            this.cache = cache;
            this.pos = pos;
        }

        void release() {
            try {
                this.this$0.remove.invoke(this.cache, this.this$0.ticket, this.pos, 1);
            }
            catch (ReflectiveOperationException error) {
                this.this$0.plugin.getLogger().warning("Chunk ticket cleanup failed: " + String.valueOf(error));
            }
        }
    }
}

