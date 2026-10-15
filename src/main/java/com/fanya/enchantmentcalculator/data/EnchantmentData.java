package com.fanya.enchantmentcalculator.data;

import net.minecraft.client.Minecraft;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.ItemEnchantments;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/** чары из реестра мира */
public final class EnchantmentData {
    private EnchantmentData() {
    }

    private static HolderLookup<Enchantment> registry() {
        Minecraft client = Minecraft.getInstance();
        if (client.level == null) {
            return null;
        }
        return client.level.registryAccess().lookupOrThrow(Registries.ENCHANTMENT);
    }

    public static EnchantmentInfo getEnchantmentInfo(Enchantment enchantment) {
        HolderLookup<Enchantment> registry = registry();
        if (registry == null) {
            return null;
        }

        Holder.Reference<Enchantment> current = registry.listElements()
                .filter(entry -> entry.value() == enchantment)
                .findFirst()
                .orElse(null);
        if (current == null) {
            return null;
        }

        List<Enchantment> incompatible = registry.listElements()
                .filter(other -> other.value() != enchantment && !Enchantment.areCompatible(current, other))
                .map(Holder::value)
                .toList();
        return new EnchantmentInfo(
                enchantment,
                enchantment.getMaxLevel(),
                incompatible
        );
    }

    public static List<Enchantment> getApplicableEnchantments(ItemStack stack) {
        HolderLookup<Enchantment> registry = registry();
        if (registry == null || stack.isEmpty()) {
            return List.of();
        }

        ItemEnchantments existing = stack.getOrDefault(
                DataComponents.ENCHANTMENTS,
                ItemEnchantments.EMPTY
        );
        List<Enchantment> result = new ArrayList<>();
        registry.listElements()
                .filter(entry -> entry.value().canEnchant(stack))
                .filter(entry -> existing.getLevel(entry) == 0)
                .filter(entry -> existing.keySet().stream()
                        .allMatch(current -> Enchantment.areCompatible(entry, current)))
                .forEach(entry -> result.add(entry.value()));
        result.sort(Comparator.comparing(enchantment -> enchantment.description().getString()));
        return result;
    }

    public static String getId(Enchantment enchantment) {
        HolderLookup<Enchantment> registry = registry();
        if (registry == null) {
            return null;
        }
        //? if >=1.21.11 {
        /*return registry.listElements().filter(entry -> entry.value() == enchantment)
                .map(entry -> entry.key().identifier().toString()).findFirst().orElse(null);*/
        //?} else {
        return registry.listElements().filter(entry -> entry.value() == enchantment)
                .map(entry -> entry.key().location().toString()).findFirst().orElse(null);
        //?}
    }

    public static Enchantment byId(String id) {
        HolderLookup<Enchantment> registry = registry();
        if (registry == null || ResourceLocation.tryParse(id) == null) {
            return null;
        }
        //? if >=1.21.11 {
        /*return registry.listElements()
                .filter(entry -> entry.key().identifier().toString().equals(id))
                .map(Holder::value).findFirst().orElse(null);*/
        //?} else {
        return registry.listElements()
                .filter(entry -> entry.key().location().toString().equals(id))
                .map(Holder::value).findFirst().orElse(null);
        //?}
    }

}
