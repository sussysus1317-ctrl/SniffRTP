/*
 * Decompiled with CFR 0.152.
 */
package dev.deepslate.sniffrtp.mod;

import com.mojang.brigadier.CommandDispatcher;
import dev.deepslate.sniffrtp.mod.VanillaRtp;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.AttackBlockCallback;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.fabricmc.fabric.api.event.player.UseEntityCallback;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;

public final class FabricEntrypoint
implements ModInitializer {
    public void onInitialize() {
        VanillaRtp rtp = new VanillaRtp();
        CommandRegistrationCallback.EVENT.register((dispatcher, registries, environment) -> rtp.register((CommandDispatcher<CommandSourceStack>)dispatcher));
        ServerTickEvents.END_SERVER_TICK.register(rtp::tick);
        ServerLifecycleEvents.SERVER_STOPPING.register(server -> rtp.stop());
        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> rtp.quit(handler.player));
        ServerLivingEntityEvents.AFTER_DAMAGE.register((entity, source, base, taken, blocked) -> {
            if (taken > 0.0f) {
                rtp.damage(entity, source);
            }
        });
        ServerLivingEntityEvents.AFTER_DEATH.register((entity, source) -> {
            if (entity instanceof ServerPlayer) {
                ServerPlayer p = (ServerPlayer)entity;
                rtp.engine.cancel(p, "died", false, false);
            }
        });
        UseBlockCallback.EVENT.register((player, world, hand, hit) -> {
            if (player instanceof ServerPlayer) {
                ServerPlayer p = (ServerPlayer)player;
                rtp.interact(p);
            }
            return InteractionResult.PASS;
        });
        UseEntityCallback.EVENT.register((player, world, hand, entity, hit) -> {
            if (player instanceof ServerPlayer) {
                ServerPlayer p = (ServerPlayer)player;
                rtp.interact(p);
            }
            return InteractionResult.PASS;
        });
        AttackBlockCallback.EVENT.register((player, world, hand, pos, direction) -> {
            if (player instanceof ServerPlayer) {
                ServerPlayer p = (ServerPlayer)player;
                rtp.interact(p);
            }
            return InteractionResult.PASS;
        });
    }
}

