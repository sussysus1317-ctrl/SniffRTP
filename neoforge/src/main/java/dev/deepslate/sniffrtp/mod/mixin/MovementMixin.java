/*
 * Decompiled with CFR 0.152.
 */
package dev.deepslate.sniffrtp.mod.mixin;

import dev.deepslate.sniffrtp.mod.VanillaRtp;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value={ServerGamePacketListenerImpl.class})
public abstract class MovementMixin {
    @Shadow
    public ServerPlayer player;
    @Unique
    private double sniffrtp$x;
    @Unique
    private double sniffrtp$y;
    @Unique
    private double sniffrtp$z;

    @Inject(method={"handleMovePlayer"}, at={@At(value="HEAD")})
    private void before(ServerboundMovePlayerPacket packet, CallbackInfo ci) {
        if (this.player.level().getServer().isSameThread()) {
            this.sniffrtp$x = this.player.getX();
            this.sniffrtp$y = this.player.getY();
            this.sniffrtp$z = this.player.getZ();
        }
    }

    @Inject(method={"handleMovePlayer"}, at={@At(value="RETURN")})
    private void after(ServerboundMovePlayerPacket packet, CallbackInfo ci) {
        double z;
        double y;
        if (VanillaRtp.INSTANCE == null || !this.player.level().getServer().isSameThread()) {
            return;
        }
        double x = this.player.getX() - this.sniffrtp$x;
        if (x * x + (y = this.player.getY() - this.sniffrtp$y) * y + (z = this.player.getZ() - this.sniffrtp$z) * z > 1.0E-4) {
            VanillaRtp.INSTANCE.moved(this.player);
        }
    }
}

