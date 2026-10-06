package com.neoncomfort;

import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

/** Сохранение настроек в config/neoncomfort.properties */
public final class Config {
    private static Path file() {
        return FabricLoader.getInstance().getConfigDir().resolve("neoncomfort.properties");
    }

    private static String key(Modules.Module m) {
        return m.name.toLowerCase().replaceAll("[^a-z0-9]+", "_");
    }

    public static void load() {
        Path f = file();
        if (!Files.exists(f)) return;
        Properties p = new Properties();
        try (InputStream in = Files.newInputStream(f)) {
            p.load(in);
        } catch (IOException e) {
            return;
        }
        for (Modules.Module m : Modules.ALL) {
            String v = p.getProperty(key(m));
            if (v == null) continue;
            try {
                if (m.isOption()) {
                    int i = Integer.parseInt(v.trim());
                    if (i >= 0 && i < m.values.length) m.index = i;
                } else {
                    m.enabled = Boolean.parseBoolean(v.trim());
                }
            } catch (NumberFormatException ignored) { }
        }
    }

    public static void save() {
        Properties p = new Properties();
        for (Modules.Module m : Modules.ALL) {
            p.setProperty(key(m), m.isOption() ? String.valueOf(m.index) : String.valueOf(m.enabled));
        }
        try (OutputStream out = Files.newOutputStream(file())) {
            p.store(out, "NeonComfort settings");
        } catch (IOException ignored) { }
    }

    private Config() {}
}
