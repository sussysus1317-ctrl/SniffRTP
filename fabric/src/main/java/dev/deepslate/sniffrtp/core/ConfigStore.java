/*
 * Decompiled with CFR 0.152.
 */
package dev.deepslate.sniffrtp.core;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.FileAttribute;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.BaseConstructor;
import org.yaml.snakeyaml.constructor.SafeConstructor;

public final class ConfigStore
implements AutoCloseable {
    private final Path file;
    private final String defaults;
    private final Consumer<String> log;
    private final ExecutorService io = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "SniffRTP-config");
        t.setDaemon(true);
        return t;
    });
    private final AtomicBoolean checking = new AtomicBoolean();
    private volatile Settings settings;
    private volatile String lastValid;
    private volatile boolean recovery;
    private long modified = -1L;
    private long size = -1L;
    private long nextCheck;

    public ConfigStore(Path file, Consumer<String> log) {
        this.file = file;
        this.log = log;
        try (InputStream in = ConfigStore.class.getResourceAsStream("/config.yml");){
            if (in == null) {
                throw new IllegalStateException("Missing config.yml defaults");
            }
            this.lastValid = this.defaults = new String(in.readAllBytes(), StandardCharsets.UTF_8);
            this.settings = this.parse(this.defaults);
            Files.createDirectories(file.getParent(), new FileAttribute[0]);
            if (!Files.exists(file, new LinkOption[0])) {
                this.write(this.defaults);
            }
            this.read();
        }
        catch (Exception e) {
            throw new IllegalStateException("Cannot initialize SniffRTP configuration", e);
        }
    }

    public Settings get() {
        return this.settings;
    }

    public boolean needsRecovery() {
        return this.recovery;
    }

    public static int interval(double tps) {
        return tps < 10.0 ? 0 : (tps > 15.0 ? 20 : (tps > 12.0 ? 100 : 200));
    }

    public void tick(long tick, double tps) {
        if (!this.settings.bool("live-config-sync.enabled", true) || tick < this.nextCheck) {
            return;
        }
        int interval = ConfigStore.interval(tps);
        if (interval == 0) {
            return;
        }
        this.nextCheck = tick + (long)interval;
        if (this.checking.compareAndSet(false, true)) {
            this.io.execute(() -> {
                try {
                    this.check();
                }
                finally {
                    this.checking.set(false);
                }
            });
        }
    }

    private void check() {
        try {
            if (!Files.exists(this.file, new LinkOption[0])) {
                if (!this.recovery) {
                    Files.createDirectories(this.file.getParent(), new FileAttribute[0]);
                    this.recovery = true;
                    this.log.accept("[SniffRTP] config.yml was deleted.\nType \"sniffrtp yes\" to restore the previous config.\nType \"sniffrtp nah\" to restore defaults.");
                }
                return;
            }
            long m = Files.getLastModifiedTime(this.file, new LinkOption[0]).toMillis();
            long s = Files.size(this.file);
            if (m != this.modified || s != this.size) {
                this.read();
                this.log.accept("[SniffRTP] Config reloaded.");
            }
        }
        catch (Exception e) {
            this.log.accept("[SniffRTP] Invalid config; keeping last valid settings: " + e.getMessage());
            try {
                this.modified = Files.getLastModifiedTime(this.file, new LinkOption[0]).toMillis();
                this.size = Files.size(this.file);
            }
            catch (Exception exception) {
                // empty catch block
            }
        }
    }

    public CompletableFuture<Boolean> reload() {
        return CompletableFuture.supplyAsync(() -> {
            try {
                this.read();
                return true;
            }
            catch (Exception e) {
                this.log.accept("[SniffRTP] Config reload rejected: " + e.getMessage());
                return false;
            }
        }, this.io);
    }

    public CompletableFuture<Boolean> restore(boolean previous) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                this.write(previous ? this.lastValid : this.defaults);
                this.read();
                this.log.accept("[SniffRTP] Config restored.");
                return true;
            }
            catch (Exception e) {
                this.log.accept("[SniffRTP] Config restore failed: " + e.getMessage());
                return false;
            }
        }, this.io);
    }

    private void write(String text) throws Exception {
        Files.createDirectories(this.file.getParent(), new FileAttribute[0]);
        Path temp = this.file.resolveSibling(String.valueOf(this.file.getFileName()) + ".tmp");
        Files.writeString(temp, (CharSequence)text, StandardCharsets.UTF_8, new OpenOption[0]);
        try {
            Files.move(temp, this.file, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        }
        catch (AtomicMoveNotSupportedException e) {
            Files.move(temp, this.file, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    private void read() throws Exception {
        Settings candidate;
        String text = Files.readString(this.file);
        this.settings = candidate = this.parse(text);
        this.lastValid = text;
        this.recovery = false;
        this.modified = Files.getLastModifiedTime(this.file, new LinkOption[0]).toMillis();
        this.size = Files.size(this.file);
    }

    private Settings parse(String text) {
        LoaderOptions options = new LoaderOptions();
        options.setAllowDuplicateKeys(false);
        options.setMaxAliasesForCollections(20);
        Yaml yaml = new Yaml((BaseConstructor)new SafeConstructor(options));
        Object value = yaml.load(text);
        if (!(value instanceof Map)) {
            throw new IllegalArgumentException("Expected a YAML mapping");
        }
        Map map = (Map)value;
        LinkedHashMap<String, Object> values = new LinkedHashMap<String, Object>();
        ConfigStore.flatten("", map, values);
        Object def = yaml.load(this.defaults);
        LinkedHashMap<String, Object> fallback = new LinkedHashMap<String, Object>();
        ConfigStore.flatten("", (Map)def, fallback);
        Settings out = new Settings(values, fallback);
        if (out.number("rtp-radius", 10000.0) < 1.0 || out.number("rtp-radius", 10000.0) > 2.9E7) {
            throw new IllegalArgumentException("rtp-radius must be 1..29000000");
        }
        if (out.number("rtp-min-radius", 250.0) < 0.0 || out.number("rtp-min-radius", 250.0) > out.number("rtp-radius", 10000.0)) {
            throw new IllegalArgumentException("rtp-min-radius must be between 0 and rtp-radius");
        }
        return out;
    }

    private static void flatten(String prefix, Map<?, ?> map, Map<String, Object> out) {
        map.forEach((k, v) -> {
            String key = prefix + String.valueOf(k);
            if (v instanceof Map) {
                Map m = (Map)((Object)v);
                ConfigStore.flatten(key + ".", m, out);
            } else if (v != null) {
                List list;
                if (v instanceof List) {
                    List l = v;
                    list = List.copyOf(l);
                } else {
                    list = v;
                }
                out.put(key, list);
            }
        });
    }

    @Override
    public void close() {
        this.io.shutdown();
    }

    public static final class Settings {
        private final Map<String, Object> values;
        private final Map<String, Object> defaults;
        private static final List<String> GROUPS = List.of("general", "rtp", "search", "safety", "cancel", "cooldown", "ui", "chat", "effects", "advanced");

        public Settings(Map<String, Object> values, Map<String, Object> defaults) {
            this.values = Map.copyOf(values);
            this.defaults = Map.copyOf(defaults);
        }

        public Object value(String key) {
            Object v = this.values.get(key);
            if (v != null) {
                return v;
            }
            for (String g : GROUPS) {
                v = this.values.get(g + "." + key);
                if (v == null) continue;
                return v;
            }
            v = this.defaults.get(key);
            if (v != null) {
                return v;
            }
            for (String g : GROUPS) {
                v = this.defaults.get(g + "." + key);
                if (v == null) continue;
                return v;
            }
            return null;
        }

        public String text(String key, String fallback) {
            Object v = this.value(key);
            return v == null ? fallback : String.valueOf(v);
        }

        public boolean bool(String key, boolean fallback) {
            Object v = this.value(key);
            return v == null ? fallback : Boolean.parseBoolean(String.valueOf(v));
        }

        public double number(String key, double fallback) {
            try {
                double v = Double.parseDouble(this.text(key, ""));
                return Double.isFinite(v) ? v : fallback;
            }
            catch (Exception e) {
                return fallback;
            }
        }

        public int integer(String key, int fallback, int min, int max) {
            return (int)Math.max((double)min, Math.min((double)max, this.number(key, fallback)));
        }

        public List<String> list(String key) {
            Object v = this.value(key);
            if (v instanceof List) {
                List l = (List)v;
                return l.stream().map(String::valueOf).toList();
            }
            if (v == null) {
                return List.of();
            }
            return Arrays.stream(v.toString().split(",")).map(String::trim).filter(s -> !s.isEmpty()).toList();
        }

        public long duration(String key, long fallback) {
            return Settings.durationValue(this.value(key), fallback);
        }

        public static long durationValue(Object v, long fallback) {
            if (v == null) {
                return fallback;
            }
            Matcher m = Pattern.compile("(?i)^\\s*(\\d+(?:\\.\\d+)?)\\s*(ms|s|m|h)?\\s*$").matcher(v.toString());
            if (!m.matches()) {
                return fallback;
            }
            double factor = switch (m.group(2) == null ? "s" : m.group(2).toLowerCase(Locale.ROOT)) {
                case "ms" -> 1.0;
                case "m" -> 60000.0;
                case "h" -> 3600000.0;
                default -> 1000.0;
            };
            return (long)Math.min(2.592E9, Double.parseDouble(m.group(1)) * factor);
        }
    }
}

