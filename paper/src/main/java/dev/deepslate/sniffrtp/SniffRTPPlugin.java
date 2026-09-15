/*
 * Decompiled with CFR 0.152.
 */
package dev.deepslate.sniffrtp;

import dev.deepslate.sniffrtp.BukkitPlatform;
import dev.deepslate.sniffrtp.RtpManager;
import dev.deepslate.sniffrtp.core.ConfigStore;
import dev.deepslate.sniffrtp.internal.v7.C;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

public class SniffRTPPlugin
extends JavaPlugin
implements Listener,
CommandExecutor,
TabCompleter {
    public ConfigStore configStore;
    public RtpManager manager;
    public BukkitPlatform platform;
    private long ticks;
    private long previous;
    private double tickMs = 50.0;

    public void onEnable() {
        C.c(this.getLogger());
        this.configStore = new ConfigStore(this.getDataFolder().toPath().resolve("config.yml"), s -> this.getLogger().info((String)s));
        this.platform = new BukkitPlatform(this);
        this.manager = new RtpManager(this.platform);
        Objects.requireNonNull(this.getCommand("rtp")).setExecutor((CommandExecutor)this);
        this.getCommand("rtp").setTabCompleter((TabCompleter)this);
        Objects.requireNonNull(this.getCommand("sniffrtp")).setExecutor((CommandExecutor)this);
        this.getServer().getPluginManager().registerEvents((Listener)this, (Plugin)this);
        this.platform.clock(() -> {
            long now = System.nanoTime();
            if (this.previous != 0L) {
                this.tickMs = 0.95 * this.tickMs + 0.05 * ((double)(now - this.previous) / 1000000.0);
            }
            this.previous = now;
            this.configStore.tick(++this.ticks, Math.min(20.0, 1000.0 / this.tickMs));
            C.d();
        });
        this.getLogger().info("SniffRTP 0.0.1 enabled for Minecraft 26.2.");
    }

    public void onDisable() {
        if (this.manager != null) {
            this.manager.engine.close();
        }
        if (this.platform != null) {
            this.platform.close();
        }
        if (this.configStore != null) {
            this.configStore.close();
        }
        HandlerList.unregisterAll((Plugin)this);
    }

    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        World.Environment env;
        if (command.getName().equals("sniffrtp")) {
            if (!(sender instanceof ConsoleCommandSender) && !sender.hasPermission("rtp.admin")) {
                sender.sendMessage("No permission.");
                return true;
            }
            if (args.length != 1 || !args[0].equalsIgnoreCase("yes") && !args[0].equalsIgnoreCase("nah")) {
                sender.sendMessage("sniffrtp <yes|nah>");
                return true;
            }
            this.configStore.restore(args[0].equalsIgnoreCase("yes"));
            return true;
        }
        if (args.length > 0 && args[0].equalsIgnoreCase("reload")) {
            if (!(sender instanceof ConsoleCommandSender) && !sender.hasPermission("rtp.admin")) {
                return true;
            }
            this.configStore.reload().thenAccept(ok -> {
                if (sender instanceof Player) {
                    Player p = (Player)sender;
                    this.platform.execute(p, () -> this.platform.chat(p, ok != false ? this.configStore.get().text("messages.reloaded", "SniffRTP config reloaded.") : "Invalid config; keeping previous settings.", ok == false, this.configStore.get()), () -> {});
                } else {
                    this.getLogger().info(ok != false ? "SniffRTP config reloaded." : "Config reload failed.");
                }
            });
            return true;
        }
        if (!(sender instanceof Player)) {
            sender.sendMessage("Only players can use /rtp.");
            return true;
        }
        Player p = (Player)sender;
        if (args.length > 0 && (args[0].equalsIgnoreCase("cancel") || args[0].equalsIgnoreCase("c"))) {
            this.manager.engine.cancel(p, "canceled your RTP", true, false);
            return true;
        }
        ConfigStore.Settings c = this.configStore.get();
        if (!this.platform.admin(p) && !p.hasPermission("rtp.use")) {
            this.manager.engine.message(p, c, "no-permission", true, Map.of());
            return true;
        }
        String target = args.length == 0 ? "" : args[0].toLowerCase(Locale.ROOT);
        World world = p.getWorld();
        String biome = null;
        switch (target) {
            case "overworld": 
            case "o": {
                World.Environment environment = World.Environment.NORMAL;
                break;
            }
            case "nether": 
            case "n": {
                World.Environment environment = World.Environment.NETHER;
                break;
            }
            case "end": 
            case "e": {
                World.Environment environment = World.Environment.THE_END;
                break;
            }
            case "custom": {
                World.Environment environment = World.Environment.CUSTOM;
                break;
            }
            default: {
                World.Environment environment = env = null;
            }
        }
        if (env != null) {
            World.Environment wanted = env;
            world = this.getServer().getWorlds().stream().filter(w -> w.getEnvironment() == wanted).findFirst().orElse(null);
        } else if (!target.isEmpty()) {
            World byName = this.getServer().getWorld(target);
            if (byName != null) {
                world = byName;
            } else {
                NamespacedKey key = NamespacedKey.fromString((String)target);
                if (key != null && Registry.BIOME.get(key) != null) {
                    biome = key.toString();
                } else {
                    this.manager.engine.message(p, c, "unknown-target", true, Map.of("target", target));
                    return true;
                }
            }
        }
        if (world == null) {
            this.manager.engine.message(p, c, "no-target-world", true, Map.of("target", target));
            return true;
        }
        String dimension = switch (world.getEnvironment()) {
            case World.Environment.NORMAL -> "overworld";
            case World.Environment.NETHER -> "nether";
            case World.Environment.THE_END -> "end";
            default -> "custom";
        };
        if (c.list("rtp-disabled-worlds").stream().anyMatch(world.getName()::equalsIgnoreCase)) {
            this.manager.engine.message(p, c, "world-disabled", true, Map.of());
            return true;
        }
        if (!(this.platform.admin(p) || (c.bool("rtp-" + dimension, !dimension.equals("custom")) || p.hasPermission("rtp." + dimension)) && (biome == null || c.bool("rtp-biomes", false) || p.hasPermission("rtp.biomes")))) {
            this.manager.engine.message(p, c, "no-permission", true, Map.of());
            return true;
        }
        this.manager.engine.start(p, world, biome, c);
        return true;
    }

    public List<String> onTabComplete(CommandSender s, Command c, String a, String[] args) {
        if (args.length != 1) {
            return List.of();
        }
        ArrayList<String> targets = new ArrayList<String>(List.of("overworld", "nether", "end", "cancel"));
        if (s.hasPermission("rtp.admin")) {
            targets.add("reload");
        }
        for (World w : this.getServer().getWorlds()) {
            targets.add(w.getName());
        }
        if (this.configStore.get().bool("rtp-biomes", false) || s.hasPermission("rtp.biomes")) {
            Registry.BIOME.forEach(b -> targets.add(b.getKey().toString()));
        }
        return targets.stream().filter(x -> x.startsWith(args[0].toLowerCase(Locale.ROOT))).toList();
    }

    @EventHandler(priority=EventPriority.MONITOR, ignoreCancelled=true)
    public void move(PlayerMoveEvent e) {
        if (e.getTo() != null && e.getFrom().distanceSquared(e.getTo()) > 1.0E-4) {
            this.manager.engine.trigger(e.getPlayer(), "midrtp-cancel-onmove", "moved");
        }
    }

    @EventHandler(priority=EventPriority.MONITOR, ignoreCancelled=true)
    public void damage(EntityDamageEvent e) {
        Entity entity = e.getEntity();
        if (entity instanceof Player) {
            Player p = (Player)entity;
            this.manager.engine.trigger(p, "midrtp-cancel-ondamage", "taken damage");
        }
    }

    @EventHandler(priority=EventPriority.MONITOR, ignoreCancelled=true)
    public void hit(EntityDamageByEntityEvent e) {
        Entity entity;
        Player attacker = null;
        if (e.getDamager() instanceof Player) {
            attacker = (Player)e.getDamager();
        } else {
            Projectile projectile;
            entity = e.getDamager();
            if (entity instanceof Projectile && (projectile = (Projectile)entity).getShooter() instanceof Player) {
                attacker = (Player)projectile.getShooter();
            }
        }
        if (attacker != null) {
            this.manager.engine.trigger(attacker, "midrtp-cancel-ifhit", "hit an entity");
            entity = e.getEntity();
            if (entity instanceof Player) {
                Player victim = (Player)entity;
                this.manager.engine.trigger(attacker, "midrtp-cancel-onpvp", "entered PvP");
                this.manager.engine.trigger(victim, "midrtp-cancel-onpvp", "entered PvP");
            }
        }
    }

    @EventHandler(priority=EventPriority.MONITOR, ignoreCancelled=true)
    public void interact(PlayerInteractEvent e) {
        this.manager.engine.trigger(e.getPlayer(), "midrtp-cancel-oninteract", "interacted");
    }

    @EventHandler(priority=EventPriority.MONITOR, ignoreCancelled=true)
    public void interactEntity(PlayerInteractEntityEvent e) {
        this.manager.engine.trigger(e.getPlayer(), "midrtp-cancel-oninteract", "interacted with an entity");
    }

    @EventHandler(priority=EventPriority.MONITOR, ignoreCancelled=true)
    public void place(BlockPlaceEvent e) {
        this.manager.engine.trigger(e.getPlayer(), "midrtp-cancel-oninteract", "placed a block");
    }

    @EventHandler(priority=EventPriority.MONITOR, ignoreCancelled=true)
    public void breakBlock(BlockBreakEvent e) {
        this.manager.engine.trigger(e.getPlayer(), "midrtp-cancel-oninteract", "broke a block");
    }

    @EventHandler(priority=EventPriority.MONITOR, ignoreCancelled=true)
    public void teleport(PlayerTeleportEvent e) {
        if (!this.manager.engine.committing(e.getPlayer())) {
            this.manager.engine.cancel(e.getPlayer(), "teleported", false, true);
        }
    }

    @EventHandler
    public void death(PlayerDeathEvent e) {
        this.manager.engine.cancel(e.getEntity(), "died", false, false);
    }

    @EventHandler
    public void quit(PlayerQuitEvent e) {
        this.manager.engine.cancel(e.getPlayer(), "quit", false, false);
        this.platform.forget(e.getPlayer());
    }
}

