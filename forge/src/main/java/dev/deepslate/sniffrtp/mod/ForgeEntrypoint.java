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
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.server.ServerStoppingEvent;
import net.minecraftforge.fml.common.Mod;

@Mod(value="sniffrtp")
public final class ForgeEntrypoint {
    public ForgeEntrypoint() {
        VanillaRtp rtp = new VanillaRtp();
        RegisterCommandsEvent.BUS.addListener(e -> rtp.register((CommandDispatcher<CommandSourceStack>)e.getDispatcher()));
        TickEvent.ServerTickEvent.Post.BUS.addListener(e -> rtp.tick(e.server()));
        ServerStoppingEvent.BUS.addListener(e -> rtp.stop());
        LivingDamageEvent.BUS.addListener(e -> {
            if (e.getAmount() > 0.0f) {
                rtp.damage(e.getEntity(), e.getSource());
            }
        });
        LivingDeathEvent.BUS.addListener(e -> {
            LivingEntity patt0$temp = e.getEntity();
            if (patt0$temp instanceof ServerPlayer) {
                ServerPlayer p = (ServerPlayer)patt0$temp;
                rtp.engine.cancel(p, "died", false, false);
            }
        });
        PlayerEvent.PlayerLoggedOutEvent.BUS.addListener(e -> {
            Player patt0$temp = e.getEntity();
            if (patt0$temp instanceof ServerPlayer) {
                ServerPlayer p = (ServerPlayer)patt0$temp;
                rtp.quit(p);
            }
        });
        PlayerInteractEvent.RightClickBlock.BUS.addListener(e -> {
            Player patt0$temp = e.getEntity();
            if (patt0$temp instanceof ServerPlayer) {
                ServerPlayer p = (ServerPlayer)patt0$temp;
                rtp.interact(p);
            }
        });
        PlayerInteractEvent.EntityInteractSpecific.BUS.addListener(e -> {
            Player patt0$temp = e.getEntity();
            if (patt0$temp instanceof ServerPlayer) {
                ServerPlayer p = (ServerPlayer)patt0$temp;
                rtp.interact(p);
            }
        });
        PlayerInteractEvent.LeftClickBlock.BUS.addListener(e -> {
            Player patt0$temp = e.getEntity();
            if (patt0$temp instanceof ServerPlayer) {
                ServerPlayer p = (ServerPlayer)patt0$temp;
                rtp.interact(p);
            }
        });
    }
}

