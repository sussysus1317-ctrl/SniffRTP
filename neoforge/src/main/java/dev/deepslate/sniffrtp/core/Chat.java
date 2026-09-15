/*
 * Decompiled with CFR 0.152.
 */
package dev.deepslate.sniffrtp.core;

import dev.deepslate.sniffrtp.core.ConfigStore;
import java.util.Map;
import java.util.regex.Pattern;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;

public final class Chat {
    private static final MiniMessage MINI = MiniMessage.miniMessage();
    private static final LegacyComponentSerializer LEGACY = LegacyComponentSerializer.builder().character('\u00a7').hexColors().useUnusualXRepeatedCharacterHexFormat().build();
    private static final Pattern PREFIX = Pattern.compile("(?i)^(?:\\s*\\[(?:sniff)?rtp\\]\\s*)+");

    public static String legacy(String text) {
        if (text == null) {
            return "";
        }
        try {
            return text.contains("<") ? LEGACY.serialize(MINI.deserialize((Object)text)) : text.replaceAll("&([0-9a-fk-orA-FK-OR])", "\u00a7$1");
        }
        catch (Exception e) {
            return text;
        }
    }

    public static String plain(String text) {
        return PlainTextComponentSerializer.plainText().serialize((Component)LEGACY.deserialize(Chat.legacy(text)));
    }

    public static String format(String text, boolean error, ConfigStore.Settings c) {
        String body;
        String string = body = c.bool("chat-style.strip-existing-chat-colors", true) ? Chat.plain(text) : Chat.legacy(text);
        if (c.bool("chat-style.remove-old-prefix-from-message", true)) {
            body = PREFIX.matcher(body).replaceFirst("");
        }
        if (c.bool("chat-style.trim-message", true)) {
            body = body.trim();
        }
        if (c.bool("chat-style.collapse-extra-spaces", false)) {
            body = body.replaceAll("[ \\t]+", " ");
        }
        if (!c.bool("chat-style.preserve-line-breaks", true)) {
            body = body.replace('\n', ' ');
        }
        if (error) {
            String clean = Chat.plain(body);
            String color = c.text("chat-style.error-color", "red");
            return Chat.legacy("<" + color + ">" + (c.bool("chat-style.error-bold", false) ? "<bold>" : "") + "[<bold>RTP</bold>] " + MINI.escapeTags(clean));
        }
        return "\u00a71[\u00a79" + (c.bool("chat-style.normal-prefix-bold", true) ? "\u00a7l" : "") + "RTP\u00a71]" + c.text("chat-style.separator", " ") + "\u00a7f" + (c.bool("chat-style.normal-text-bold", false) ? "\u00a7l" : "") + body;
    }

    public static String fill(String text, Map<String, String> values) {
        for (Map.Entry<String, String> e : values.entrySet()) {
            text = text.replace("<" + e.getKey() + ">", e.getValue());
        }
        return text;
    }
}

