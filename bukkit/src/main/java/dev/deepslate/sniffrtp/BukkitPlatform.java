/*
 * Decompiled with CFR 0.152.
 */
package dev.deepslate.sniffrtp;

import dev.deepslate.sniffrtp.LegacyScheduling;
import dev.deepslate.sniffrtp.SniffRTPPlugin;
import dev.deepslate.sniffrtp.core.Chat;
import dev.deepslate.sniffrtp.core.ConfigStore;
import dev.deepslate.sniffrtp.core.Geometry;
import dev.deepslate.sniffrtp.core.PreparationFuture;
import dev.deepslate.sniffrtp.core.RtpEngine;
import dev.deepslate.sniffrtp.core.Ui;
import java.util.Collection;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.HeightMap;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.Registry;
import org.bukkit.World;
import org.bukkit.WorldBorder;
import org.bukkit.block.Block;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarFlag;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.BoundingBox;

public class BukkitPlatform
implements RtpEngine.Platform<Player, World> {
    final SniffRTPPlugin plugin;
    final Scheduling scheduler;
    final Map<UUID, BukkitUi> ui = new ConcurrentHashMap<UUID, BukkitUi>();
    final Map<String, Integer> tickets = new HashMap<String, Integer>();

    public BukkitPlatform(SniffRTPPlugin plugin) {
        Scheduling s;
        this.plugin = plugin;
        try {
            Class.forName("io.papermc.paper.threadedregions.scheduler.RegionScheduler");
            s = (Scheduling)Class.forName("dev.deepslate.sniffrtp.PaperScheduling").getConstructor(SniffRTPPlugin.class).newInstance(new Object[]{plugin});
        }
        catch (ClassNotFoundException e) {
            s = new LegacyScheduling(plugin);
        }
        catch (ReflectiveOperationException e) {
            throw new IllegalStateException("Native scheduler unavailable", e);
        }
        this.scheduler = s;
    }

    public void clock(Runnable r) {
        this.scheduler.clock(r);
    }

    @Override
    public UUID id(Player p) {
        return p.getUniqueId();
    }

    @Override
    public boolean online(Player p) {
        return p.isOnline();
    }

    @Override
    public boolean admin(Player p) {
        return p.isOp() || p.hasPermission("rtp.admin") || p.hasPermission("rtp.*") || p.hasPermission("*");
    }

    @Override
    public void execute(Player p, Runnable r, Runnable retired) {
        this.scheduler.player(p, 1, r, retired);
    }

    @Override
    public void later(Player p, int ticks, Runnable r, Runnable retired) {
        this.scheduler.player(p, ticks, r, retired);
    }

    @Override
    public String worldId(World w) {
        return w.getUID().toString();
    }

    @Override
    public Geometry.Area area(Player p, World w, ConfigStore.Settings c) {
        WorldBorder b = w.getWorldBorder();
        Location center = b.getCenter();
        double half = b.getSize() / 2.0;
        Location origin = switch (c.text("rtp-center-mode", "world-spawn")) {
            case "player" -> p.getLocation();
            case "fixed", "configured", "custom" -> new Location(w, c.number("rtp-center-x", 0.0), 0.0, c.number("rtp-center-z", 0.0));
            default -> w.getSpawnLocation();
        };
        return new Geometry.Area(origin.getX(), origin.getZ(), c.number("rtp-min-radius", 250.0), c.number("rtp-radius", 10000.0), new Geometry.Border(center.getX() - half, center.getZ() - half, center.getX() + half, center.getZ() + half));
    }

    @Override
    public CompletableFuture<RtpEngine.Lease> prepare(Player p, World w, Geometry.Point point, String biome, ConfigStore.Settings c) {
        PreparationFuture result = new PreparationFuture(() -> {});
        Geometry.Area bounds = this.area(p, w, c);
        this.scheduler.chunk(w, point.x() >> 4, point.z() >> 4, c.bool("rtp-generate-new-chunks", true), c.bool("rtp-urgent-chunk-loads", false)).whenComplete((chunk, error) -> {
            if (error != null) {
                result.completeExceptionally((Throwable)error);
                return;
            }
            if (chunk == null || result.stopped()) {
                result.complete(null);
                return;
            }
            this.scheduler.region(w, point.x() >> 4, point.z() >> 4, () -> {
                try {
                    if (result.stopped()) {
                        result.complete(null);
                        return;
                    }
                    for (int i = 0; i < 16; ++i) {
                        double y;
                        Geometry.Point probe = Geometry.column(point, i);
                        if (!bounds.contains(probe) || (y = BukkitPlatform.safeY(w, probe, c)) < (double)w.getMinHeight() || biome != null && !w.getBiome(probe.x(), (int)y, probe.z()).getKey().toString().equals(biome)) continue;
                        BukkitLease lease = new BukkitLease(this, w, probe, y);
                        lease.retain();
                        result.complete(lease);
                        return;
                    }
                    result.complete(null);
                }
                catch (Throwable e) {
                    result.completeExceptionally(e);
                }
            });
        });
        return result;
    }

    static boolean unsafe(Material m, ConfigStore.Settings c) {
        String n = m.name();
        return c.list("rtp-avoid-blocks").stream().anyMatch(n::equalsIgnoreCase) || n.contains("LEAVES") || n.contains("SLAB") || n.contains("STAIRS") || n.contains("FENCE") || n.contains("WALL") || n.contains("PANE") || n.contains("CARPET") || n.contains("RAIL") || n.contains("PRESSURE_PLATE") || n.contains("DOOR") || n.contains("CHEST") || n.contains("HOPPER") || n.contains("FARMLAND") || n.equals("DIRT_PATH") || n.equals("SOUL_SAND") || Set.of("MAGMA_BLOCK", "CACTUS", "CAMPFIRE", "SOUL_CAMPFIRE", "FIRE", "SOUL_FIRE", "LAVA", "WATER", "POWDER_SNOW", "SWEET_BERRY_BUSH", "WITHER_ROSE", "POINTED_DRIPSTONE").contains(n);
    }

    static boolean full(Block b, ConfigStore.Settings c) {
        if (BukkitPlatform.unsafe(b.getType(), c) || b.isLiquid() || !b.getType().isSolid()) {
            return false;
        }
        Collection boxes = b.getCollisionShape().getBoundingBoxes();
        if (boxes.size() != 1) {
            return false;
        }
        BoundingBox box = (BoundingBox)boxes.iterator().next();
        return box.getWidthX() >= 0.999999 && box.getHeight() >= 0.999999 && box.getWidthZ() >= 0.999999;
    }

    static boolean safe(World w, Geometry.Point p, int y, ConfigStore.Settings c) {
        return y > w.getMinHeight() && y + 1 < w.getMaxHeight() && BukkitPlatform.full(w.getBlockAt(p.x(), y - 1, p.z()), c) && w.getBlockAt(p.x(), y, p.z()).getType().isAir() && w.getBlockAt(p.x(), y + 1, p.z()).getType().isAir();
    }

    static double safeY(World w, Geometry.Point p, ConfigStore.Settings c) {
        if (w.getEnvironment() == World.Environment.NETHER) {
            for (int y = Math.min(w.getMaxHeight() - 2, c.integer("safety.nether-max-floor-y", 120, 1, 120) + 1); y > w.getMinHeight(); --y) {
                if (!BukkitPlatform.safe(w, p, y, c)) continue;
                return y;
            }
        } else {
            int y = w.getHighestBlockYAt(p.x(), p.z(), HeightMap.MOTION_BLOCKING_NO_LEAVES) + 1;
            if (BukkitPlatform.safe(w, p, y, c)) {
                return y;
            }
        }
        return Double.NEGATIVE_INFINITY;
    }

    @Override
    public CompletableFuture<Boolean> teleport(Player p, World w, RtpEngine.Lease target, ConfigStore.Settings c) {
        CompletableFuture<Boolean> result = new CompletableFuture<Boolean>();
        Geometry.Point point = target.point();
        this.scheduler.region(w, point.x() >> 4, point.z() >> 4, () -> {
            boolean safe = BukkitPlatform.safe(w, point, (int)target.y(), c);
            this.execute(p, () -> {
                if (!safe || !this.area(p, w, c).contains(point)) {
                    result.complete(false);
                    return;
                }
                Location dest = new Location(w, (double)point.x() + 0.5, target.y(), (double)point.z() + 0.5, p.getLocation().getYaw(), p.getLocation().getPitch());
                this.scheduler.teleport(p, dest).whenComplete((ok, error) -> {
                    if (error != null) {
                        result.completeExceptionally((Throwable)error);
                    } else {
                        result.complete((Boolean)ok);
                    }
                });
            }, () -> result.complete(false));
        });
        return result;
    }

    @Override
    public void chat(Player p, String text, boolean error, ConfigStore.Settings c) {
        p.sendMessage(Chat.format(text, error, c));
    }

    @Override
    public void ui(Player p, String phase, Map<String, String> values, ConfigStore.Settings c) {
        this.ui.computeIfAbsent(this.id(p), i -> new BukkitUi(this, p)).show(phase, values, c);
    }

    @Override
    public void clear(Player p) {
        BukkitUi u = this.ui.get(this.id(p));
        if (u != null) {
            u.clear();
        }
    }

    public void forget(Player p) {
        this.clear(p);
        this.ui.remove(this.id(p));
    }

    @Override
    public void log(String text) {
        this.plugin.getLogger().info(text);
    }

    public void close() {
        this.scheduler.close();
        this.ui.clear();
    }

    public static interface Scheduling {
        public void player(Player var1, int var2, Runnable var3, Runnable var4);

        public void region(World var1, int var2, int var3, Runnable var4);

        public void clock(Runnable var1);

        public void close();

        public CompletableFuture<Chunk> chunk(World var1, int var2, int var3, boolean var4, boolean var5);

        public CompletableFuture<Boolean> teleport(Player var1, Location var2);
    }

    final class BukkitUi
    extends Ui {
        final Player p;
        BossBar bar;
        final /* synthetic */ BukkitPlatform this$0;

        BukkitUi(BukkitPlatform this$0, Player p) {
            BukkitPlatform bukkitPlatform = this$0;
            Objects.requireNonNull(bukkitPlatform);
            this.this$0 = bukkitPlatform;
            this.p = p;
        }

        @Override
        protected void title(String t, String s, int ticks) {
            this.p.sendTitle(t, s, 0, ticks, 0);
        }

        @Override
        protected void action(String t) {
            this.p.spigot().sendMessage(ChatMessageType.ACTION_BAR, TextComponent.fromLegacyText((String)t));
        }

        @Override
        protected void boss(String t, String color, double progress) {
            if (this.bar == null) {
                this.bar = Bukkit.createBossBar((String)t, (BarColor)BarColor.valueOf((String)Ui.color(color).toUpperCase(Locale.ROOT)), (BarStyle)BarStyle.SOLID, (BarFlag[])new BarFlag[0]);
                this.bar.addPlayer(this.p);
            } else {
                this.bar.setTitle(t);
                this.bar.setColor(BarColor.valueOf((String)Ui.color(color).toUpperCase(Locale.ROOT)));
            }
            this.bar.setProgress(Math.max(0.0, Math.min(1.0, progress)));
        }

        @Override
        protected void removeBoss() {
            if (this.bar != null) {
                this.bar.removeAll();
                this.bar = null;
            }
        }

        @Override
        protected void chat(String t, boolean error, ConfigStore.Settings c) {
            this.this$0.chat(this.p, t, error, c);
        }

        @Override
        protected void later(int ticks, Runnable r) {
            this.this$0.later(this.p, ticks, r, () -> {});
        }

        @Override
        protected void sound(String key, double volume, double pitch) {
            this.p.playSound(this.p.getLocation(), key, (float)volume, (float)pitch);
        }

        @Override
        protected void effect(String key, int amplifier, int ticks) {
            PotionEffectType type;
            NamespacedKey id = NamespacedKey.fromString((String)key.toLowerCase(Locale.ROOT));
            PotionEffectType potionEffectType = type = id == null ? null : (PotionEffectType)Registry.EFFECT.get(id);
            if (type != null) {
                this.p.addPotionEffect(new PotionEffect(type, ticks, amplifier, false, false, true));
            }
        }

        @Override
        protected void particles(String key, int count, double x, double y, double z, double extra) {
            try {
                this.p.spawnParticle(Particle.valueOf((String)key.toUpperCase(Locale.ROOT)), this.p.getLocation().add(0.0, 1.0, 0.0), count, x, y, z, extra);
            }
            catch (IllegalArgumentException illegalArgumentException) {
                // empty catch block
            }
        }
    }

    final class BukkitLease
    implements RtpEngine.Lease {
        final World world;
        final Geometry.Point point;
        final double y;
        final AtomicBoolean closed;
        final /* synthetic */ BukkitPlatform this$0;

        BukkitLease(BukkitPlatform this$0, World w, Geometry.Point p, double y) {
            BukkitPlatform bukkitPlatform = this$0;
            Objects.requireNonNull(bukkitPlatform);
            this.this$0 = bukkitPlatform;
            this.closed = new AtomicBoolean();
            this.world = w;
            this.point = p;
            this.y = y;
        }

        String key() {
            return String.valueOf(this.world.getUID()) + ":" + this.point.chunk();
        }

        /*
         * WARNING - Removed try catching itself - possible behaviour change.
         */
        void retain() {
            Map<String, Integer> map = this.this$0.tickets;
            synchronized (map) {
                int n = this.this$0.tickets.getOrDefault(this.key(), 0);
                if (n == 0) {
                    this.world.addPluginChunkTicket(this.point.x() >> 4, this.point.z() >> 4, (Plugin)this.this$0.plugin);
                }
                this.this$0.tickets.put(this.key(), n + 1);
            }
        }

        @Override
        public Geometry.Point point() {
            return this.point;
        }

        @Override
        public double y() {
            return this.y;
        }

        @Override
        public void close() {
            if (!this.closed.compareAndSet(false, true)) {
                return;
            }
            this.this$0.scheduler.region(this.world, this.point.x() >> 4, this.point.z() >> 4, () -> {
                Map<String, Integer> map = this.this$0.tickets;
                synchronized (map) {
                    int n = this.this$0.tickets.getOrDefault(this.key(), 0);
                    if (n <= 1) {
                        this.this$0.tickets.remove(this.key());
                        this.world.removePluginChunkTicket(this.point.x() >> 4, this.point.z() >> 4, (Plugin)this.this$0.plugin);
                    } else {
                        this.this$0.tickets.put(this.key(), n - 1);
                    }
                }
            });
        }
    }
}

