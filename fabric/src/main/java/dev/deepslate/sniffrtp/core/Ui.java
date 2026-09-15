/*
 * Decompiled with CFR 0.152.
 */
package dev.deepslate.sniffrtp.core;

import dev.deepslate.sniffrtp.core.Chat;
import dev.deepslate.sniffrtp.core.ConfigStore;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public abstract class Ui {
    private long generation;

    protected abstract void title(String var1, String var2, int var3);

    protected abstract void action(String var1);

    protected abstract void boss(String var1, String var2, double var3);

    protected abstract void removeBoss();

    protected abstract void chat(String var1, boolean var2, ConfigStore.Settings var3);

    protected abstract void later(int var1, Runnable var2);

    protected abstract void sound(String var1, double var2, double var4);

    protected abstract void effect(String var1, int var2, int var3);

    protected abstract void particles(String var1, int var2, double var3, double var5, double var7, double var9);

    public void clear() {
        ++this.generation;
        this.title("", "", 0);
        this.action("");
        this.removeBoss();
    }

    private String text(ConfigStore.Settings c, String key, String fallback, Map<String, String> vars) {
        return Chat.legacy(Chat.fill(c.text(key, fallback), vars));
    }

    public void show(String phase, Map<String, String> vars, ConfigStore.Settings c) {
        if (phase.equals("start")) {
            if (c.bool("rtp-message", true) && !vars.getOrDefault("total_time", "0").equals("0")) {
                this.chat(Chat.fill(c.text("midrtp-message-customize.during", "Teleporting in <total_time> seconds"), vars), false, c);
            }
            if (c.bool("effects-mid-rtp-enabled", false)) {
                this.effects(c.list("effects-mid-rtp"), Math.max(20, Integer.parseInt(vars.getOrDefault("total_time", "0")) * 20 + 20));
            }
            return;
        }
        if (phase.equals("during")) {
            String seconds = vars.getOrDefault("time", "0");
            if (c.bool("midrtp-title", true)) {
                this.title(this.text(c, "midrtp-title-customize.during-title", "<blue>RTP</blue>", vars), this.text(c, "midrtp-title-customize.during-subtitle", "Teleporting in <time> seconds", vars), 30);
            }
            if (c.bool("midrtp-actionbar", false)) {
                this.action(this.text(c, "midrtp-actionbar-customize.during", "RTP in <time> seconds", vars));
            }
            if (c.bool("midrtp-bossbar", false)) {
                this.boss(this.text(c, "midrtp-bossbar-customize.during", "RTP in <time> seconds", vars), c.text("midrtp-bossbar-duringrtp-color", "blue"), Double.parseDouble(vars.getOrDefault("progress", "0")));
            }
            if (!seconds.equals("0") && Boolean.parseBoolean(vars.getOrDefault("play_countdown_sound", "true")) && c.bool("countdown-sfx-enabled", true)) {
                String prefix = "countdown-sfx.overrides." + seconds;
                String key = c.text(prefix + ".sound", c.text("countdown-sfx.default.sound", "block.note_block.pling"));
                this.sound(key, c.number(prefix + ".volume", c.number("countdown-sfx.default.volume", 1.0)), c.number(prefix + ".pitch", c.number("countdown-sfx.default.pitch", 1.0)));
            }
            return;
        }
        long token = this.generation;
        if (phase.equals("after")) {
            if (c.bool("rtp-message", true)) {
                this.chat(Chat.fill(c.text("midrtp-message-customize.after", "You have RTP'ed to <coord>"), vars), false, c);
            }
            if (c.bool("midrtp-title", true) && c.bool("afterrtp-title", true)) {
                this.title(this.text(c, "afterrtp-title-customize.title", "<coord>", vars), this.text(c, "afterrtp-title-customize.subtitle", "You have RTP'ed", vars), Ui.ticks(c.duration("afterrtp-title-duration", 5000L)));
            }
            if (c.bool("midrtp-actionbar", false) && c.bool("afterrtp-actionbar", true)) {
                this.repeatAction(this.text(c, "afterrtp-actionbar-customize.message", "You have RTP'ed to <coord>", vars), Ui.ticks(c.duration("afterrtp-actionbar-duration", 3000L)), token);
            }
            if (c.bool("midrtp-bossbar", false) && c.bool("afterrtp-bossbar", true)) {
                this.boss(this.text(c, "afterrtp-bossbar-customize.message", "You have RTP'ed to <coord>", vars), c.text("afterrtp-bossbar-customize.color", "blue"), 1.0);
                this.later(Ui.ticks(c.duration("afterrtp-bossbar-duration", 3000L)), () -> {
                    if (token == this.generation) {
                        this.removeBoss();
                    }
                });
            }
            if (c.bool("effects-after-rtp-enabled", true)) {
                this.effects(c.list("effects-after-rtp"), 100);
            }
            if (c.bool("after-rtp-sfx-enabled", true)) {
                this.sound(c.text("after-rtp-sfx.sound", "entity.enderman.teleport"), c.number("after-rtp-sfx.volume", 1.0), c.number("after-rtp-sfx.pitch", 1.0));
            }
            if (c.bool("after-rtp-particles-enabled", true)) {
                this.particles(c.text("after-rtp-particles.particle", "PORTAL"), c.integer("after-rtp-particles.count", 60, 0, 1000), c.number("after-rtp-particles.offset-x", 0.6), c.number("after-rtp-particles.offset-y", 0.9), c.number("after-rtp-particles.offset-z", 0.6), c.number("after-rtp-particles.extra", 0.1));
            }
            return;
        }
        if (!c.bool("event-feedback.enabled", true)) {
            return;
        }
        String prefix = "event-feedback." + phase;
        boolean cancel = phase.startsWith("canceled");
        if (phase.equals("admin-cooldown-bypass") && !c.bool("adm-bypass-message-enabled", true)) {
            return;
        }
        Object fallback = cancel ? (phase.endsWith("manual") ? "You canceled your RTP." : "RTP canceled because you <action>.") : phase.replace('-', ' ') + ".";
        String message = Chat.fill(c.text(prefix + ".text", (String)fallback), vars);
        if (c.bool(prefix + ".chat", cancel)) {
            this.chat(message, cancel, c);
        }
        int duration = Ui.ticks(c.duration(prefix + ".duration", c.duration("event-feedback.default-duration", 2000L)));
        if (c.bool(prefix + ".title", false)) {
            this.title(this.text(c, prefix + ".title-text", "RTP", vars), this.text(c, prefix + ".subtitle", message, vars), duration);
        }
        if (c.bool(prefix + ".actionbar", false)) {
            this.repeatAction(Chat.legacy(message), duration, token);
        }
        if (c.bool(prefix + ".bossbar", false)) {
            this.boss(Chat.legacy(message), c.text(prefix + ".bossbar-color", cancel ? "red" : "blue"), 1.0);
            this.later(duration, () -> {
                if (token == this.generation) {
                    this.removeBoss();
                }
            });
        }
    }

    private void repeatAction(String text, int remaining, long token) {
        if (token != this.generation) {
            return;
        }
        this.action(text);
        this.later(Math.min(20, Math.max(1, remaining)), () -> {
            if (token != this.generation) {
                return;
            }
            if (remaining > 20) {
                this.repeatAction(text, remaining - 20, token);
            } else {
                this.action("");
            }
        });
    }

    private void effects(List<String> entries, int fallback) {
        for (String entry : entries) {
            try {
                String[] p = entry.split(":");
                if (p.length < 2) continue;
                int amp = Math.max(0, Math.min(255, Integer.parseInt(p[1]) - 1));
                int duration = p.length > 2 ? Ui.ticks(ConfigStore.Settings.durationValue(p[2], (long)fallback * 50L)) : fallback;
                this.effect(p[0], amp, duration);
            }
            catch (RuntimeException runtimeException) {}
        }
    }

    private static int ticks(long ms) {
        return (int)Math.max(1L, Math.min(120000L, (ms + 49L) / 50L));
    }

    public static String color(String raw) {
        String clean = raw.toLowerCase(Locale.ROOT);
        for (String c : List.of("red", "pink", "yellow", "green", "blue", "purple", "white")) {
            if (!clean.contains(c)) continue;
            return c;
        }
        return "blue";
    }
}

