package com.fanya.enchantmentcalculator.client.automation;

import com.fanya.enchantmentcalculator.calculator.CalculationResult;
import com.fanya.enchantmentcalculator.calculator.EnchantmentCombination;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AnvilMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.ItemEnchantments;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

/** проверка книг и уровней */
public final class InventoryReadiness {
    public record Report(List<String> missingBooks, int levelsNeededNow, int levelsNeededForPlan) {
        public boolean ready() {
            return missingBooks.isEmpty() && levelsNeededForPlan == 0;
        }
    }

    private InventoryReadiness() {
    }

    public static Report check(
            Player player,
            AnvilMenu handler,
            List<EnchantmentCombination> selections,
            CalculationResult result,
            int stepIndex
    ) {
        HolderLookup<Enchantment> registry = player.level()
                .registryAccess().lookupOrThrow(Registries.ENCHANTMENT);
        List<String> missingBooks = new ArrayList<>();
        Set<Integer> neededLeaves = new TreeSet<>();

        for (int index = stepIndex; index < result.getSteps().size(); index++) {
            CalculationResult.Step step = result.getSteps().get(index);
            addLeafIfNeeded(neededLeaves, step.getLeftMask());
            addLeafIfNeeded(neededLeaves, step.getRightMask());
        }

        for (int index : neededLeaves) {
            EnchantmentCombination selection = selections.get(index);
            Holder.Reference<Enchantment> entry = registry.listElements()
                    .filter(candidate -> candidate.value() == selection.enchantment())
                    .findFirst()
                    .orElse(null);

            if (entry == null || !hasBook(handler, entry, selection.level())) {
                String name = entry == null
                        ? selection.enchantment().description().getString()
                                + " " + selection.level()
                        : Enchantment.getFullname(entry, selection.level()).getString();
                missingBooks.add(name);
            }
        }

        int stepCost = result.getSteps().isEmpty()
                ? 0
                : result.getSteps().get(stepIndex).getLevels();
        int levelsNeededNow = Math.max(0, stepCost - player.experienceLevel);
        int remainingLevels = 0;
        for (int index = stepIndex; index < result.getSteps().size(); index++) {
            remainingLevels += result.getSteps().get(index).getLevels();
        }
        int levelsNeededForPlan = Math.max(0, remainingLevels - player.experienceLevel);

        return new Report(List.copyOf(missingBooks), levelsNeededNow, levelsNeededForPlan);
    }

    private static void addLeafIfNeeded(Set<Integer> neededLeaves, int mask) {
        if (mask != 1 && Integer.bitCount(mask) == 1) {
            neededLeaves.add(Integer.numberOfTrailingZeros(mask) - 1);
        }
    }

    private static boolean hasBook(
            AnvilMenu handler,
            Holder<Enchantment> enchantment,
            int level
    ) {
        for (int index = 0; index < handler.slots.size(); index++) {
            if (index == 2) {
                continue;
            }
            if (containsEnchantment(handler.getSlot(index).getItem(), enchantment, level)) {
                return true;
            }
        }

        return false;
    }

    private static boolean containsEnchantment(
            ItemStack stack,
            Holder<Enchantment> enchantment,
            int level
    ) {
        if (!stack.is(Items.ENCHANTED_BOOK)) {
            return false;
        }
        if (stack.getOrDefault(DataComponents.REPAIR_COST, 0) != 0) {
            return false;
        }

        ItemEnchantments stored = stack.getOrDefault(
                DataComponents.STORED_ENCHANTMENTS,
                ItemEnchantments.EMPTY
        );
        return stored.size() == 1 && stored.getLevel(enchantment) == level;
    }
}
