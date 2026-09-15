/*
 * Decompiled with CFR 0.152.
 */
package dev.deepslate.sniffrtp;

import dev.deepslate.sniffrtp.BukkitPlatform;
import dev.deepslate.sniffrtp.core.RtpEngine;
import org.bukkit.World;
import org.bukkit.entity.Player;

public final class RtpManager {
    public final RtpEngine<Player, World> engine;

    public RtpManager(BukkitPlatform platform) {
        this.engine = new RtpEngine<Player, World>(platform);
    }
}

