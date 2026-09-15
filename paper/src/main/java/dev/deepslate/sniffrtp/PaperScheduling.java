/*
 * Decompiled with CFR 0.152.
 */
package dev.deepslate.sniffrtp;

import dev.deepslate.sniffrtp.BukkitPlatform;
import dev.deepslate.sniffrtp.SniffRTPPlugin;
import io.papermc.paper.threadedregions.scheduler.ScheduledTask;
import java.util.concurrent.CompletableFuture;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.plugin.Plugin;

public final class PaperScheduling
implements BukkitPlatform.Scheduling {
    final SniffRTPPlugin plugin;

    public PaperScheduling(SniffRTPPlugin p) {
        this.plugin = p;
    }

    @Override
    public void player(Player p, int ticks, Runnable r, Runnable retired) {
        if (!this.plugin.isEnabled()) {
            retired.run();
            return;
        }
        ScheduledTask task = p.getScheduler().runDelayed((Plugin)this.plugin, t -> r.run(), retired, (long)Math.max(1, ticks));
        if (task == null) {
            retired.run();
        }
    }

    @Override
    public void region(World w, int x, int z, Runnable r) {
        if (Bukkit.isOwnedByCurrentRegion((World)w, (int)x, (int)z)) {
            r.run();
            return;
        }
        if (this.plugin.isEnabled()) {
            Bukkit.getRegionScheduler().execute((Plugin)this.plugin, w, x, z, r);
        }
    }

    @Override
    public void clock(Runnable r) {
        Bukkit.getGlobalRegionScheduler().runAtFixedRate((Plugin)this.plugin, t -> r.run(), 1L, 1L);
    }

    @Override
    public CompletableFuture<Chunk> chunk(World w, int x, int z, boolean generate, boolean urgent) {
        return w.getChunkAtAsync(x, z, generate, urgent);
    }

    @Override
    public CompletableFuture<Boolean> teleport(Player p, Location l) {
        return p.teleportAsync(l, PlayerTeleportEvent.TeleportCause.PLUGIN);
    }

    @Override
    public void close() {
        Bukkit.getGlobalRegionScheduler().cancelTasks((Plugin)this.plugin);
        Bukkit.getAsyncScheduler().cancelTasks((Plugin)this.plugin);
    }
}

