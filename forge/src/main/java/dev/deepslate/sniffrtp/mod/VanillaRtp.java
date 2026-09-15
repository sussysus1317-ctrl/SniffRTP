/*
 * Decompiled with CFR 0.152.
 */
package dev.deepslate.sniffrtp.mod;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import dev.deepslate.sniffrtp.core.Chat;
import dev.deepslate.sniffrtp.core.ConfigStore;
import dev.deepslate.sniffrtp.core.Geometry;
import dev.deepslate.sniffrtp.core.PreparationFuture;
import dev.deepslate.sniffrtp.core.RtpEngine;
import dev.deepslate.sniffrtp.core.Ui;
import dev.deepslate.sniffrtp.internal.v7.C;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.PriorityQueue;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicBoolean;
import net.luckperms.api.LuckPermsProvider;
import net.luckperms.api.cacheddata.CachedPermissionData;
import net.luckperms.api.model.user.User;
import net.luckperms.api.util.Tristate;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientboundSetSubtitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitlesAnimationPacket;
import net.minecraft.network.protocol.game.ClientboundSoundPacket;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ChunkResult;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerChunkCache;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.TicketType;
import net.minecraft.server.permissions.Permissions;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.BossEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.border.WorldBorder;
import net.minecraft.world.level.levelgen.Heightmap;

public final class VanillaRtp
implements RtpEngine.Platform<ServerPlayer, ServerLevel> {
    public static VanillaRtp INSTANCE;
    public final ConfigStore config;
    public final RtpEngine<ServerPlayer, ServerLevel> engine;
    private MinecraftServer server;
    private long tick;
    private long previous;
    private double tickMs = 50.0;
    private boolean stopping;
    private final PriorityQueue<Job> jobs = new PriorityQueue<Job>(Comparator.comparingLong(Job::tick));
    private final Map<UUID, VanillaUi> uis = new HashMap<UUID, VanillaUi>();
    private final Map<String, Integer> tickets = new HashMap<String, Integer>();
    private final TicketType ticketType = new TicketType(120001L, 2);
    private final boolean luckPerms;

    public VanillaRtp() {
        boolean lp;
        C.c();
        this.config = new ConfigStore(Path.of("config", "sniffrtp", "config.yml"), System.out::println);
        this.engine = new RtpEngine<ServerPlayer, ServerLevel>(this);
        try {
            Class.forName("net.luckperms.api.LuckPermsProvider", false, this.getClass().getClassLoader());
            lp = true;
        }
        catch (ClassNotFoundException e) {
            lp = false;
        }
        this.luckPerms = lp;
        INSTANCE = this;
    }

    public void tick(MinecraftServer server) {
        this.server = server;
        long now = System.nanoTime();
        if (this.previous != 0L) {
            this.tickMs = 0.95 * this.tickMs + 0.05 * ((double)(now - this.previous) / 1000000.0);
        }
        this.previous = now;
        this.config.tick(++this.tick, Math.min(20.0, 1000.0 / this.tickMs));
        C.d();
        int size = this.jobs.size();
        for (int i = 0; i < size && !this.jobs.isEmpty() && this.jobs.peek().tick <= this.tick; ++i) {
            Job j = (Job)this.jobs.remove();
            if (this.online(j.player)) {
                j.run.run();
                continue;
            }
            j.retired.run();
        }
    }

    public void stop() {
        this.stopping = true;
        this.engine.close();
        for (VanillaUi ui : this.uis.values()) {
            ui.clear();
        }
        this.uis.clear();
        while (!this.jobs.isEmpty()) {
            ((Job)this.jobs.remove()).retired.run();
        }
        this.config.close();
    }

    public void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register((LiteralArgumentBuilder)((LiteralArgumentBuilder)Commands.literal((String)"rtp").executes(c -> this.command((CommandSourceStack)c.getSource(), ""))).then(Commands.argument((String)"target", (ArgumentType)StringArgumentType.word()).suggests((c, b) -> {
            for (String s : List.of("overworld", "nether", "end", "cancel", "reload")) {
                b.suggest(s);
            }
            return b.buildFuture();
        }).executes(c -> this.command((CommandSourceStack)c.getSource(), StringArgumentType.getString((CommandContext)c, (String)"target")))));
        dispatcher.register((LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)Commands.literal((String)"sniffrtp").requires(s -> {
            ServerPlayer p;
            Entity patt0$temp;
            return s.permissions().hasPermission(Permissions.COMMANDS_ADMIN) || (patt0$temp = s.getEntity()) instanceof ServerPlayer && this.admin(p = (ServerPlayer)patt0$temp);
        })).then(Commands.literal((String)"yes").executes(c -> {
            this.config.restore(true);
            return 1;
        }))).then(Commands.literal((String)"nah").executes(c -> {
            this.config.restore(false);
            return 1;
        })));
    }

    private int command(CommandSourceStack source, String target) {
        String kind;
        ResourceKey dimension;
        ServerPlayer sp;
        ServerPlayer p;
        this.server = source.getServer();
        ConfigStore.Settings c = this.config.get();
        Entity entity = source.getEntity();
        ServerPlayer serverPlayer = p = entity instanceof ServerPlayer ? (sp = (ServerPlayer)entity) : null;
        if (target.equalsIgnoreCase("reload")) {
            if (source.permissions().hasPermission(Permissions.COMMANDS_ADMIN) || p != null && this.admin(p)) {
                this.config.reload();
            }
            return 1;
        }
        if (p == null) {
            source.sendFailure((Component)Component.literal((String)"Only players can use /rtp."));
            return 0;
        }
        if ((target = target.toLowerCase(Locale.ROOT)).equals("cancel") || target.equals("c")) {
            this.engine.cancel(p, "canceled your RTP", true, false);
            return 1;
        }
        if (!this.permission(p, "rtp.use", true)) {
            this.engine.message(p, c, "no-permission", true, Map.of());
            return 0;
        }
        ServerLevel world = p.level();
        String biome = null;
        switch (target) {
            case "overworld": 
            case "o": {
                ResourceKey resourceKey = Level.OVERWORLD;
                break;
            }
            case "nether": 
            case "n": {
                ResourceKey resourceKey = Level.NETHER;
                break;
            }
            case "end": 
            case "e": {
                ResourceKey resourceKey = Level.END;
                break;
            }
            default: {
                ResourceKey resourceKey = dimension = null;
            }
        }
        if (dimension != null) {
            world = this.server.getLevel(dimension);
        } else if (!target.isEmpty()) {
            Identifier id = Identifier.tryParse((String)target);
            ServerLevel chosen = null;
            for (ServerLevel w : this.server.getAllLevels()) {
                if ((!target.equals("custom") || Set.of(Level.OVERWORLD, Level.NETHER, Level.END).contains(w.dimension())) && (id == null || !w.dimension().identifier().equals((Object)id))) continue;
                chosen = w;
                break;
            }
            if (chosen != null) {
                world = chosen;
            } else if (id != null && world.registryAccess().lookupOrThrow(Registries.BIOME).get(ResourceKey.create((ResourceKey)Registries.BIOME, (Identifier)id)).isPresent()) {
                biome = id.toString();
            } else {
                this.engine.message(p, c, "unknown-target", true, Map.of("target", target));
                return 0;
            }
        }
        if (world == null) {
            this.engine.message(p, c, "no-target-world", true, Map.of("target", target));
            return 0;
        }
        String string = world.dimension().equals(Level.OVERWORLD) ? "overworld" : (world.dimension().equals(Level.NETHER) ? "nether" : (kind = world.dimension().equals(Level.END) ? "end" : "custom"));
        if (c.list("rtp-disabled-worlds").contains(this.worldId(world)) || c.list("rtp-disabled-worlds").contains(world.dimension().identifier().getPath())) {
            this.engine.message(p, c, "world-disabled", true, Map.of());
            return 0;
        }
        if (!this.permission(p, "rtp." + kind, c.bool("rtp-" + kind, !kind.equals("custom"))) || biome != null && !this.permission(p, "rtp.biomes", c.bool("rtp-biomes", false))) {
            this.engine.message(p, c, "no-permission", true, Map.of());
            return 0;
        }
        this.engine.start(p, world, biome, c);
        return 1;
    }

    public boolean permission(ServerPlayer p, String node, boolean fallback) {
        if (this.admin(p)) {
            return true;
        }
        if (this.luckPerms) {
            try {
                Tristate value;
                User user = LuckPermsProvider.get().getUserManager().getUser(p.getUUID());
                if (user != null && (value = user.getCachedData().getPermissionData().checkPermission(node)) != Tristate.UNDEFINED) {
                    return value.asBoolean();
                }
            }
            catch (IllegalStateException illegalStateException) {
                // empty catch block
            }
        }
        return fallback;
    }

    @Override
    public boolean admin(ServerPlayer p) {
        if (p.permissions().hasPermission(Permissions.COMMANDS_ADMIN)) {
            return true;
        }
        if (this.luckPerms) {
            try {
                User user = LuckPermsProvider.get().getUserManager().getUser(p.getUUID());
                if (user != null) {
                    CachedPermissionData data = user.getCachedData().getPermissionData();
                    return data.checkPermission("rtp.admin").asBoolean() || data.checkPermission("rtp.*").asBoolean();
                }
            }
            catch (IllegalStateException illegalStateException) {
                // empty catch block
            }
        }
        return false;
    }

    @Override
    public UUID id(ServerPlayer p) {
        return p.getUUID();
    }

    @Override
    public boolean online(ServerPlayer p) {
        return !p.isRemoved() && p.connection != null && p.connection.isAcceptingMessages();
    }

    @Override
    public void execute(ServerPlayer p, Runnable work, Runnable retired) {
        MinecraftServer s = p.level().getServer();
        if (this.stopping) {
            retired.run();
            return;
        }
        if (s.isSameThread()) {
            if (this.online(p)) {
                work.run();
            } else {
                retired.run();
            }
        } else {
            s.execute(() -> {
                if (this.online(p) && !this.stopping) {
                    work.run();
                } else {
                    retired.run();
                }
            });
        }
    }

    @Override
    public void later(ServerPlayer p, int ticks, Runnable work, Runnable retired) {
        this.jobs.add(new Job(this.tick + (long)Math.max(1, ticks), p, work, retired));
    }

    @Override
    public String worldId(ServerLevel w) {
        return w.dimension().identifier().toString();
    }

    @Override
    public Geometry.Area area(ServerPlayer p, ServerLevel w, ConfigStore.Settings c) {
        WorldBorder b = w.getWorldBorder();
        BlockPos spawn = w.getRespawnData().pos();
        double x = spawn.getX();
        double z = spawn.getZ();
        String mode = c.text("rtp-center-mode", "world-spawn");
        if (mode.equals("player")) {
            x = p.getX();
            z = p.getZ();
        } else if (Set.of("fixed", "configured", "custom").contains(mode)) {
            x = c.number("rtp-center-x", 0.0);
            z = c.number("rtp-center-z", 0.0);
        }
        return new Geometry.Area(x, z, c.number("rtp-min-radius", 250.0), c.number("rtp-radius", 10000.0), new Geometry.Border(b.getMinX(), b.getMinZ(), b.getMaxX(), b.getMaxZ()));
    }

    @Override
    public CompletableFuture<RtpEngine.Lease> prepare(ServerPlayer p, ServerLevel w, Geometry.Point point, String biome, ConfigStore.Settings c) {
        ServerChunkCache cache = w.getChunkSource();
        VanillaLease lease = new VanillaLease(this, w, point);
        PreparationFuture result = new PreparationFuture(lease::close);
        if (!c.bool("rtp-generate-new-chunks", true) && cache.getChunkNow(point.x() >> 4, point.z() >> 4) == null) {
            result.complete(null);
            return result;
        }
        lease.retain();
        cache.addTicketAndLoadWithRadius(this.ticketType, new ChunkPos(point.x() >> 4, point.z() >> 4), 0).whenComplete((chunk, error) -> w.getServer().execute(() -> {
            try {
                ChunkResult status;
                if (this.stopping || result.stopped() || error != null || chunk instanceof ChunkResult && !(status = (ChunkResult)chunk).isSuccess()) {
                    lease.close();
                    if (error != null) {
                        result.completeExceptionally((Throwable)error);
                    } else {
                        result.complete(null);
                    }
                    return;
                }
                if (cache.getChunkNow(point.x() >> 4, point.z() >> 4) == null) {
                    lease.close();
                    result.complete(null);
                    return;
                }
                Geometry.Area bounds = this.area(p, w, c);
                for (int i = 0; i < 16; ++i) {
                    double y;
                    Geometry.Point probe = Geometry.column(point, i);
                    if (!bounds.contains(probe) || !Double.isFinite(y = VanillaRtp.safeY(w, probe, c)) || biome != null && !w.getBiome(new BlockPos(probe.x(), (int)y, probe.z())).unwrapKey().map(k -> k.identifier().toString().equals(biome)).orElse(false).booleanValue()) continue;
                    lease.point = probe;
                    lease.y = y;
                    result.complete(lease);
                    return;
                }
                lease.close();
                result.complete(null);
            }
            catch (Throwable e) {
                lease.close();
                result.completeExceptionally(e);
            }
        }));
        return result;
    }

    static boolean unsafe(BlockState state, ConfigStore.Settings c) {
        String n = BuiltInRegistries.BLOCK.getKey((Object)state.getBlock()).getPath().toUpperCase(Locale.ROOT);
        return c.list("rtp-avoid-blocks").stream().anyMatch(n::equalsIgnoreCase) || n.contains("LEAVES") || n.contains("SLAB") || n.contains("STAIRS") || n.contains("FENCE") || n.contains("WALL") || n.contains("PANE") || n.contains("CARPET") || n.contains("RAIL") || n.contains("PRESSURE_PLATE") || n.contains("DOOR") || n.contains("CHEST") || n.contains("HOPPER") || Set.of("FARMLAND", "DIRT_PATH", "SOUL_SAND", "MAGMA_BLOCK", "CACTUS", "CAMPFIRE", "SOUL_CAMPFIRE", "FIRE", "SOUL_FIRE", "LAVA", "WATER", "POWDER_SNOW", "SWEET_BERRY_BUSH", "WITHER_ROSE", "POINTED_DRIPSTONE").contains(n);
    }

    static boolean safe(ServerLevel w, Geometry.Point p, int y, ConfigStore.Settings c) {
        if (y <= w.getMinY() || y + 1 > w.getMaxY()) {
            return false;
        }
        BlockPos ground = new BlockPos(p.x(), y - 1, p.z());
        BlockState block = w.getBlockState(ground);
        return !VanillaRtp.unsafe(block, c) && block.getFluidState().isEmpty() && block.isCollisionShapeFullBlock((BlockGetter)w, ground) && w.getBlockState(ground.above()).isAir() && w.getBlockState(ground.above(2)).isAir();
    }

    static double safeY(ServerLevel w, Geometry.Point p, ConfigStore.Settings c) {
        if (w.dimensionType().hasCeiling()) {
            for (int y = Math.min(w.getMaxY() - 1, c.integer("safety.nether-max-floor-y", 120, 1, 120) + 1); y > w.getMinY(); --y) {
                if (!VanillaRtp.safe(w, p, y, c)) continue;
                return y;
            }
        } else {
            int y = w.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, p.x(), p.z());
            if (VanillaRtp.safe(w, p, y, c)) {
                return y;
            }
        }
        return Double.NEGATIVE_INFINITY;
    }

    @Override
    public CompletableFuture<Boolean> teleport(ServerPlayer p, ServerLevel w, RtpEngine.Lease lease, ConfigStore.Settings c) {
        Geometry.Point point = lease.point();
        if (!this.area(p, w, c).contains(point) || !VanillaRtp.safe(w, point, (int)lease.y(), c)) {
            return CompletableFuture.completedFuture(false);
        }
        return CompletableFuture.completedFuture(p.teleportTo(w, (double)point.x() + 0.5, lease.y(), (double)point.z() + 0.5, Set.of(), p.getYRot(), p.getXRot(), true));
    }

    @Override
    public void chat(ServerPlayer p, String text, boolean error, ConfigStore.Settings c) {
        p.sendSystemMessage(VanillaRtp.component(Chat.format(text, error, c)));
    }

    @Override
    public void ui(ServerPlayer p, String phase, Map<String, String> vars, ConfigStore.Settings c) {
        this.uis.computeIfAbsent(p.getUUID(), id -> new VanillaUi(this, p)).show(phase, vars, c);
    }

    @Override
    public void clear(ServerPlayer p) {
        VanillaUi ui = this.uis.get(p.getUUID());
        if (ui != null) {
            ui.clear();
        }
    }

    public void quit(ServerPlayer p) {
        this.engine.cancel(p, "quit", false, false);
        this.clear(p);
        this.uis.remove(p.getUUID());
    }

    @Override
    public void log(String text) {
        System.out.println(text);
    }

    public void moved(ServerPlayer p) {
        this.engine.trigger(p, "midrtp-cancel-onmove", "moved");
    }

    public void interact(ServerPlayer p) {
        this.engine.trigger(p, "midrtp-cancel-oninteract", "interacted");
    }

    public void damage(LivingEntity victim, DamageSource source) {
        Entity entity;
        if (victim instanceof ServerPlayer) {
            ServerPlayer p = (ServerPlayer)victim;
            this.engine.trigger(p, "midrtp-cancel-ondamage", "taken damage");
        }
        if ((entity = source.getEntity()) instanceof ServerPlayer) {
            ServerPlayer attacker = (ServerPlayer)entity;
            this.engine.trigger(attacker, "midrtp-cancel-ifhit", "hit an entity");
            if (victim instanceof ServerPlayer) {
                ServerPlayer p = (ServerPlayer)victim;
                this.engine.trigger(attacker, "midrtp-cancel-onpvp", "entered PvP");
                this.engine.trigger(p, "midrtp-cancel-onpvp", "entered PvP");
            }
        }
    }

    public static Component component(String legacy) {
        MutableComponent out = Component.empty();
        Style style = Style.EMPTY;
        StringBuilder text = new StringBuilder();
        int[] colors = new int[]{0, 170, 43520, 43690, 0xAA0000, 0xAA00AA, 0xFFAA00, 0xAAAAAA, 0x555555, 0x5555FF, 0x55FF55, 0x55FFFF, 0xFF5555, 0xFF55FF, 0xFFFF55, 0xFFFFFF};
        for (int i = 0; i < legacy.length(); ++i) {
            char code;
            int color;
            char ch = legacy.charAt(i);
            if (ch != '\u00a7' || i + 1 >= legacy.length()) {
                text.append(ch);
                continue;
            }
            if (!text.isEmpty()) {
                out.append((Component)Component.literal((String)text.toString()).setStyle(style));
                text.setLength(0);
            }
            if ((color = "0123456789abcdef".indexOf(code = Character.toLowerCase(legacy.charAt(++i)))) >= 0) {
                style = Style.EMPTY.withColor(colors[color]);
                continue;
            }
            if (code == 'x' && i + 12 < legacy.length()) {
                StringBuilder hex = new StringBuilder();
                for (int j = 0; j < 6; ++j) {
                    hex.append(legacy.charAt(i += 2));
                }
                try {
                    style = Style.EMPTY.withColor(Integer.parseInt(hex.toString(), 16));
                }
                catch (NumberFormatException numberFormatException) {}
                continue;
            }
            style = switch (code) {
                case 'l' -> style.withBold(Boolean.valueOf(true));
                case 'o' -> style.withItalic(Boolean.valueOf(true));
                case 'n' -> style.withUnderlined(Boolean.valueOf(true));
                case 'm' -> style.withStrikethrough(Boolean.valueOf(true));
                case 'k' -> style.withObfuscated(Boolean.valueOf(true));
                case 'r' -> Style.EMPTY;
                default -> style;
            };
        }
        if (!text.isEmpty()) {
            out.append((Component)Component.literal((String)text.toString()).setStyle(style));
        }
        return out;
    }

    private record Job(long tick, ServerPlayer player, Runnable run, Runnable retired) {
    }

    private final class VanillaUi
    extends Ui {
        final ServerPlayer p;
        ServerBossEvent bar;
        final /* synthetic */ VanillaRtp this$0;

        VanillaUi(VanillaRtp vanillaRtp, ServerPlayer p) {
            VanillaRtp vanillaRtp2 = vanillaRtp;
            Objects.requireNonNull(vanillaRtp2);
            this.this$0 = vanillaRtp2;
            this.p = p;
        }

        @Override
        protected void title(String t, String s, int ticks) {
            this.p.connection.send((Packet)new ClientboundSetTitlesAnimationPacket(0, ticks, 0));
            this.p.connection.send((Packet)new ClientboundSetTitleTextPacket(VanillaRtp.component(t)));
            this.p.connection.send((Packet)new ClientboundSetSubtitleTextPacket(VanillaRtp.component(s)));
        }

        @Override
        protected void action(String text) {
            this.p.sendOverlayMessage(VanillaRtp.component(text));
        }

        @Override
        protected void boss(String text, String color, double progress) {
            BossEvent.BossBarColor col = BossEvent.BossBarColor.valueOf((String)Ui.color(color).toUpperCase(Locale.ROOT));
            if (this.bar == null) {
                this.bar = new ServerBossEvent(UUID.randomUUID(), VanillaRtp.component(text), col, BossEvent.BossBarOverlay.PROGRESS);
                this.bar.addPlayer(this.p);
            }
            this.bar.setName(VanillaRtp.component(text));
            this.bar.setColor(col);
            this.bar.setProgress((float)Math.max(0.0, Math.min(1.0, progress)));
        }

        @Override
        protected void removeBoss() {
            if (this.bar != null) {
                this.bar.removeAllPlayers();
                this.bar = null;
            }
        }

        @Override
        protected void chat(String text, boolean error, ConfigStore.Settings c) {
            this.this$0.chat(this.p, text, error, c);
        }

        @Override
        protected void later(int ticks, Runnable r) {
            this.this$0.later(this.p, ticks, r, () -> {});
        }

        @Override
        protected void sound(String key, double volume, double pitch) {
            Identifier id = Identifier.tryParse((String)key.toLowerCase(Locale.ROOT));
            if (id == null) {
                return;
            }
            Optional sound = BuiltInRegistries.SOUND_EVENT.get(id);
            sound.ifPresent(s -> this.p.connection.send((Packet)new ClientboundSoundPacket((Holder)s, SoundSource.PLAYERS, this.p.getX(), this.p.getY(), this.p.getZ(), (float)volume, (float)pitch, ThreadLocalRandom.current().nextLong())));
        }

        @Override
        protected void effect(String key, int amp, int ticks) {
            Identifier id = Identifier.tryParse((String)key.toLowerCase(Locale.ROOT));
            if (id != null) {
                BuiltInRegistries.MOB_EFFECT.get(id).ifPresent(e -> this.p.addEffect(new MobEffectInstance((Holder)e, ticks, amp, false, false, true)));
            }
        }

        @Override
        protected void particles(String key, int count, double x, double y, double z, double extra) {
            Object object;
            Identifier id = Identifier.tryParse((String)key.toLowerCase(Locale.ROOT));
            if (id != null && (object = BuiltInRegistries.PARTICLE_TYPE.getValue(id)) instanceof ParticleOptions) {
                ParticleOptions options = (ParticleOptions)object;
                this.p.level().sendParticles(this.p, options, false, false, this.p.getX(), this.p.getY() + 1.0, this.p.getZ(), count, x, y, z, extra);
            }
        }
    }

    private final class VanillaLease
    implements RtpEngine.Lease {
        final ServerLevel world;
        Geometry.Point point;
        double y;
        final AtomicBoolean closed;
        final /* synthetic */ VanillaRtp this$0;

        VanillaLease(VanillaRtp vanillaRtp, ServerLevel w, Geometry.Point point) {
            VanillaRtp vanillaRtp2 = vanillaRtp;
            Objects.requireNonNull(vanillaRtp2);
            this.this$0 = vanillaRtp2;
            this.closed = new AtomicBoolean();
            this.world = w;
            this.point = point;
        }

        String key() {
            return this.this$0.worldId(this.world) + ":" + this.point.chunk();
        }

        void retain() {
            int n = this.this$0.tickets.getOrDefault(this.key(), 0);
            if (n == 0) {
                this.world.getChunkSource().addTicketWithRadius(this.this$0.ticketType, new ChunkPos(this.point.x() >> 4, this.point.z() >> 4), 0);
            }
            this.this$0.tickets.put(this.key(), n + 1);
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
            Runnable r = () -> {
                int n = this.this$0.tickets.getOrDefault(this.key(), 0);
                if (n <= 1) {
                    this.this$0.tickets.remove(this.key());
                    this.world.getChunkSource().removeTicketWithRadius(this.this$0.ticketType, new ChunkPos(this.point.x() >> 4, this.point.z() >> 4), 0);
                } else {
                    this.this$0.tickets.put(this.key(), n - 1);
                }
            };
            if (this.world.getServer().isSameThread()) {
                r.run();
            } else {
                this.world.getServer().execute(r);
            }
        }
    }
}

