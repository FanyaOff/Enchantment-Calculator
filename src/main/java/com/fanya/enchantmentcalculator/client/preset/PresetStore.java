package com.fanya.enchantmentcalculator.client.preset;

import com.fanya.enchantmentcalculator.EnchantmentCalculatorMod;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParseException;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** локальные пресеты зачарований */
public final class PresetStore {
    public record Preset(String id, String name, String item, Map<String, Integer> enchantments,
                         String mode) {
        public Preset {
            enchantments = Collections.unmodifiableMap(new LinkedHashMap<>(
                    enchantments == null ? Map.of() : enchantments));
        }
    }

    private record FileData(int version, List<Preset> presets) {
    }

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path FILE = FabricLoader.getInstance().getConfigDir()
            .resolve("enchantmentcalculator-presets.json");
    private static final List<Preset> PRESETS = new ArrayList<>();
    private static boolean loaded;

    private PresetStore() {
    }

    public static List<Preset> forItem(String item) {
        load();
        return PRESETS.stream().filter(preset -> preset.item().equals(item))
                .sorted(Comparator.comparing(Preset::name, String.CASE_INSENSITIVE_ORDER))
                .toList();
    }

    public static Preset save(String name, String item, Map<String, Integer> enchantments, String mode) {
        load();
        Preset existing = PRESETS.stream()
                .filter(preset -> preset.item().equals(item) && preset.name().equalsIgnoreCase(name))
                .findFirst().orElse(null);
        Preset preset = new Preset(existing == null ? UUID.randomUUID().toString() : existing.id(),
                name, item, new LinkedHashMap<>(enchantments), mode);
        if (existing != null) {
            PRESETS.remove(existing);
        }
        PRESETS.add(preset);
        if (!write()) {
            PRESETS.remove(preset);
            if (existing != null) {
                PRESETS.add(existing);
            }
            return null;
        }
        return preset;
    }

    public static boolean delete(String id) {
        load();
        Preset preset = PRESETS.stream().filter(entry -> entry.id().equals(id))
                .findFirst().orElse(null);
        if (preset == null) {
            return false;
        }
        PRESETS.remove(preset);
        if (write()) {
            return true;
        }
        PRESETS.add(preset);
        return false;
    }

    private static void load() {
        if (loaded) {
            return;
        }
        loaded = true;
        if (!Files.exists(FILE)) {
            return;
        }
        try (Reader reader = Files.newBufferedReader(FILE)) {
            FileData data = GSON.fromJson(reader, FileData.class);
            if (data == null || data.version() != 1 || data.presets() == null) {
                return;
            }
            for (Preset preset : data.presets()) {
                if (preset != null && preset.id() != null && preset.name() != null
                        && preset.item() != null && preset.mode() != null
                        && preset.enchantments() != null && !preset.enchantments().isEmpty()) {
                    PRESETS.add(preset);
                }
            }
        } catch (IOException | JsonParseException exception) {
            EnchantmentCalculatorMod.LOGGER.warn("Cannot load enchantment presets", exception);
        }
    }

    private static boolean write() {
        Path temporary = FILE.resolveSibling(FILE.getFileName() + ".tmp");
        try {
            Files.createDirectories(FILE.getParent());
            try (Writer writer = Files.newBufferedWriter(temporary)) {
                GSON.toJson(new FileData(1, PRESETS), writer);
            }
            try {
                Files.move(temporary, FILE, StandardCopyOption.ATOMIC_MOVE,
                        StandardCopyOption.REPLACE_EXISTING);
            } catch (java.nio.file.AtomicMoveNotSupportedException exception) {
                Files.move(temporary, FILE, StandardCopyOption.REPLACE_EXISTING);
            }
            return true;
        } catch (IOException exception) {
            EnchantmentCalculatorMod.LOGGER.error("Cannot save enchantment presets", exception);
            return false;
        }
    }
}
