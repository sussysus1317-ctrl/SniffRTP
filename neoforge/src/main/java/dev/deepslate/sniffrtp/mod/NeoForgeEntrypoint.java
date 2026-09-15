/*
 * Decompiled with CFR 0.152.
 */
package dev.deepslate.sniffrtp.mod;

import com.mojang.brigadier.CommandDispatcher;
import dev.deepslate.sniffrtp.mod.VanillaRtp;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

@Mod(value="sniffrtp")
public final class NeoForgeEntrypoint {
    public NeoForgeEntrypoint() {
        VanillaRtp rtp = new VanillaRtp();
        NeoForge.EVENT_BUS.addListener(RegisterCommandsEvent.class, e -> rtp.register((CommandDispatcher<CommandSourceStack>)e.getDispatcher()));
        NeoForge.EVENT_BUS.addListener(ServerTickEvent.Post.class, e -> rtp.tick(e.getServer()));
        NeoForge.EVENT_BUS.addListener(ServerStoppingEvent.class, e -> rtp.stop());
        NeoForge.EVENT_BUS.addListener(LivingDamageEvent.Post.class, e -> {
            if (e.getHealthDamage() > 0.0f) {
                rtp.damage(e.getEntity(), e.getSource());
            }
        });
        NeoForge.EVENT_BUS.addListener(LivingDeathEvent.class, e -> {
            LivingEntity patt0$temp = e.getEntity();
            if (patt0$temp instanceof ServerPlayer) {
                ServerPlayer p = (ServerPlayer)patt0$temp;
                rtp.engine.cancel(p, "died", false, false);
            }
        });
        NeoForge.EVENT_BUS.addListener(PlayerEvent.PlayerLoggedOutEvent.class, e -> {
            Player patt0$temp = e.getEntity();
            if (patt0$temp instanceof ServerPlayer) {
                ServerPlayer p = (ServerPlayer)patt0$temp;
                rtp.quit(p);
            }
        });
        NeoForge.EVENT_BUS.addListener(PlayerInteractEvent.RightClickBlock.class, e -> {
            Player patt0$temp = e.getEntity();
            if (patt0$temp instanceof ServerPlayer) {
                ServerPlayer p = (ServerPlayer)patt0$temp;
                rtp.interact(p);
            }
        });
        NeoForge.EVENT_BUS.addListener(PlayerInteractEvent.EntityInteract.class, e -> {
            Player patt0$temp = e.getEntity();
            if (patt0$temp instanceof ServerPlayer) {
                ServerPlayer p = (ServerPlayer)patt0$temp;
                rtp.interact(p);
            }
        });
        NeoForge.EVENT_BUS.addListener(PlayerInteractEvent.LeftClickBlock.class, e -> {
            Player patt0$temp = e.getEntity();
            if (patt0$temp instanceof ServerPlayer) {
                ServerPlayer p = (ServerPlayer)patt0$temp;
                rtp.interact(p);
            }
        });
    }
}

