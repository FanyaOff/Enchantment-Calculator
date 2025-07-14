package com.fanya.enchantmentcalculator.data;

import net.minecraft.enchantment.Enchantment;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.util.Identifier;
import net.minecraft.client.MinecraftClient;

import java.util.*;

public class EnchantmentData {
    private static final Map<Enchantment, EnchantmentInfo> ENCHANTMENT_INFO = new HashMap<>();
    private static final Map<Item, ItemInfo> ITEM_INFO = new HashMap<>();
    private static boolean initialized = false;

    public static void initialize() {
        if (initialized) return;

        MinecraftClient client = MinecraftClient.getInstance();
        if (client.world != null) {
            loadEnchantmentData(client);
            loadItemData(client);
            initialized = true;
        }
    }

    private static void loadEnchantmentData(MinecraftClient client) {
        assert client.world != null;
        RegistryWrapper<Enchantment> enchantmentRegistry = client.world.getRegistryManager().getWrapperOrThrow(RegistryKeys.ENCHANTMENT);


        for (RegistryEntry.Reference<Enchantment> entry : enchantmentRegistry.streamEntries().toList()) {
            Enchantment enchantment = entry.value();
            entry.getKey().map(RegistryKey::getValue);
            int weight = getEnchantmentWeight(enchantment, entry);

            EnchantmentInfo info = new EnchantmentInfo(
                    enchantment,
                    enchantment.getMaxLevel(),
                    weight,
                    getIncompatibleEnchantments(enchantment, enchantmentRegistry)
            );
            ENCHANTMENT_INFO.put(enchantment, info);
        }

    }


    private static void loadItemData(MinecraftClient client) {
        for (Item item : net.minecraft.registry.Registries.ITEM) {
            List<Enchantment> applicableEnchantments = getApplicableEnchantments(item, client);
            if (!applicableEnchantments.isEmpty()) {
                ITEM_INFO.put(item, new ItemInfo(item, applicableEnchantments));
            }
        }
    }

    private static int getEnchantmentWeight(Enchantment enchantment, RegistryEntry.Reference<Enchantment> entry) {
        // Пытаемся получить реальный вес из игры
        try {
            // В Minecraft 1.21 может быть доступен метод getWeight() или аналогичный
            // Проверьте документацию API для вашей версии
            return enchantment.getWeight(); // Если такой метод существует
        } catch (Exception e) {
            // Fallback на ручные значения
            Identifier id = entry.getKey().map(RegistryKey::getValue).orElse(null);
            if (id == null) return 1;

            String name = id.getPath();
            return switch (name) {
                // Используем значения из приложенного кода сайта
                case "protection", "sharpness", "efficiency", "power", "unbreaking",
                     "feather_falling", "fire_protection", "knockback", "loyalty",
                     "piercing", "projectile_protection", "quick_charge", "smite",
                     "bane_of_arthropods", "density" -> 1;

                case "aqua_affinity", "blast_protection", "depth_strider", "fire_aspect",
                     "flame", "frost_walker", "impaling", "luck_of_the_sea", "lure",
                     "mending", "multishot", "punch", "respiration", "riptide",
                     "breach", "wind_burst", "looting", "fortune", "sweeping_edge" -> 2;

                case "soul_speed", "swift_sneak", "thorns", "binding_curse",
                     "vanishing_curse", "silk_touch", "infinity", "channeling" -> 4;

                default -> 1;
            };
        }
    }


    private static List<Enchantment> getIncompatibleEnchantments(Enchantment enchantment, RegistryWrapper<Enchantment> enchantmentRegistry) {
        List<Enchantment> incompatible = new ArrayList<>();

        // Получаем ID текущего зачарования
        String currentName = null;
        for (RegistryEntry.Reference<Enchantment> entry : enchantmentRegistry.streamEntries().toList()) {
            if (entry.value() == enchantment) {
                Identifier currentId = entry.getKey().map(RegistryKey::getValue).orElse(null);
                if (currentId != null) {
                    currentName = currentId.getPath();
                    break;
                }
            }
        }

        if (currentName == null) return incompatible;

        // Проверяем все зачарования на совместимость
        for (RegistryEntry.Reference<Enchantment> entry : enchantmentRegistry.streamEntries().toList()) {
            Enchantment other = entry.value();
            if (other == enchantment) continue;

            Identifier otherId = entry.getKey().map(RegistryKey::getValue).orElse(null);
            if (otherId == null) continue;

            String otherName = otherId.getPath();

            // Проверяем несовместимость на основе предопределенных правил
            if (areIncompatible(currentName, otherName)) {
                incompatible.add(other);
            }
        }

        return incompatible;
    }



    private static boolean areIncompatible(String enchant1, String enchant2) {
        // Защита
        Set<String> protectionGroup = Set.of("protection", "blast_protection", "fire_protection", "projectile_protection");
        if (protectionGroup.contains(enchant1) && protectionGroup.contains(enchant2)) {
            return true;
        }

        // Урон
        Set<String> damageGroup = Set.of("sharpness", "smite", "bane_of_arthropods");
        if (damageGroup.contains(enchant1) && damageGroup.contains(enchant2)) {
            return true;
        }

        // Булава урон
        Set<String> maceDamageGroup = Set.of("density", "breach", "smite", "bane_of_arthropods");
        if (maceDamageGroup.contains(enchant1) && maceDamageGroup.contains(enchant2)) {
            return true;
        }

        // Особые несовместимости
        Map<String, Set<String>> incompatibilities = Map.ofEntries(
                Map.entry("depth_strider", Set.of("frost_walker")),
                Map.entry("frost_walker", Set.of("depth_strider")),
                Map.entry("fortune", Set.of("silk_touch")),
                Map.entry("silk_touch", Set.of("fortune")),
                Map.entry("infinity", Set.of("mending")),
                Map.entry("mending", Set.of("infinity")),
                Map.entry("channeling", Set.of("riptide")),
                Map.entry("riptide", Set.of("channeling", "loyalty")),
                Map.entry("loyalty", Set.of("riptide")),
                Map.entry("multishot", Set.of("piercing")),
                Map.entry("piercing", Set.of("multishot"))
        );


        return incompatibilities.getOrDefault(enchant1, Set.of()).contains(enchant2);
    }

    private static List<Enchantment> getApplicableEnchantments(Item item, MinecraftClient client) {
        List<Enchantment> applicable = new ArrayList<>();
        ItemStack stack = new ItemStack(item);

        if (client.world != null) {
            RegistryWrapper<Enchantment> enchantmentRegistry = client.world.getRegistryManager().getWrapperOrThrow(RegistryKeys.ENCHANTMENT);

            for (RegistryEntry<Enchantment> entry : enchantmentRegistry.streamEntries().toList()) {
                Enchantment enchantment = entry.value();
                if (enchantment.isAcceptableItem(stack)) {
                    applicable.add(enchantment);
                }
            }
        }

        return applicable;
    }

    public static EnchantmentInfo getEnchantmentInfo(Enchantment enchantment) {
        return ENCHANTMENT_INFO.get(enchantment);
    }

    public static List<Enchantment> getApplicableEnchantments(ItemStack stack) {
        if (stack.isEmpty()) return Collections.emptyList();

        ItemInfo itemInfo = ITEM_INFO.get(stack.getItem());
        return itemInfo != null ? itemInfo.applicableEnchantments() : Collections.emptyList();
    }

    public static boolean isEnchantable(ItemStack stack) {
        if (!initialized) {
            initialize();
        }

        return !getApplicableEnchantments(stack).isEmpty();
    }

    public static void reinitialize() {
        initialized = false;
        ENCHANTMENT_INFO.clear();
        ITEM_INFO.clear();
        initialize();
    }

    public static boolean isInitialized() {
        return initialized;
    }
}
