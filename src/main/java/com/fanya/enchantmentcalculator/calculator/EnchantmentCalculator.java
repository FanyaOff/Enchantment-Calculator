package com.fanya.enchantmentcalculator.calculator;

import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;

import java.util.ArrayList;
import java.util.List;

public final class EnchantmentCalculator {
    private static final int MAX_ANVIL_COST = 39;
    private static final int MAX_ENCHANTMENTS = 15;

    private EnchantmentCalculator() {
    }

    public static CalculationResult calculate(
            ItemStack target,
            List<EnchantmentCombination> enchantments,
            OptimizationMode mode
    ) {
        if (target.isEmpty() || enchantments.isEmpty()) {
            throw new IllegalArgumentException("enchantmentcalculator.error.no_selection");
        }
        if (enchantments.size() > MAX_ENCHANTMENTS) {
            throw new IllegalArgumentException("enchantmentcalculator.error.too_many");
        }

        List<AnvilOptimizer.Leaf> leaves = new ArrayList<>();
        List<String> bookNames = new ArrayList<>();
        int repairCost = target.getOrDefault(DataComponents.REPAIR_COST, 0);
        int priorWork = workFromRepairCost(repairCost);
        String itemName = target.getHoverName().getString();
        leaves.add(new AnvilOptimizer.Leaf(
                itemName,
                0,
                priorWork,
                true
        ));

        for (EnchantmentCombination selection : enchantments) {
            Enchantment enchantment = selection.enchantment();
            if (selection.level() < 1 || selection.level() > enchantment.getMaxLevel()) {
                throw new IllegalArgumentException("enchantmentcalculator.error.invalid_level");
            }

            int cost = Math.max(1, enchantment.getAnvilCost() / 2) * selection.level();
            String name = enchantment.description().getString();
            if (selection.level() > 1) {
                name += " " + selection.level();
            }
            bookNames.add(name);
            String bookName = Component.translatable("enchantmentcalculator.book", name).getString();
            leaves.add(new AnvilOptimizer.Leaf(bookName, cost, 0, false));
        }

        AnvilOptimizer.Result result = AnvilOptimizer.solve(leaves, mode, MAX_ANVIL_COST);
        if (result == null) {
            throw new IllegalArgumentException("enchantmentcalculator.error.no_path");
        }

        List<CalculationResult.Step> steps = new ArrayList<>();
        for (AnvilOptimizer.Merge merge : result.steps()) {
            String left = operandName(merge.left().mask(), itemName, bookNames);
            String right = operandName(merge.right().mask(), itemName, bookNames);
            String description = Component.translatable(
                    "enchantmentcalculator.combine_ordered", left, right
            ).getString();
            steps.add(new CalculationResult.Step(
                    description,
                    left,
                    right,
                    merge.levels(),
                    merge.experience(),
                    merge.priorPenalty(),
                    merge.left().mask(),
                    merge.right().mask()
            ));
        }

        return new CalculationResult(steps, result.totalLevels(), result.totalExperience());
    }

    static String operandName(int mask, String itemName, List<String> bookNames) {
        List<String> includedBooks = new ArrayList<>();
        for (int index = 0; index < bookNames.size(); index++) {
            if ((mask & (1 << (index + 1))) != 0) {
                includedBooks.add(bookNames.get(index));
            }
        }
        if ((mask & 1) != 0) {
            return includedBooks.isEmpty() ? itemName
                    : itemName + " (" + String.join(", ", includedBooks) + ")";
        }
        return Component.translatable(
                "enchantmentcalculator.book", String.join(", ", includedBooks)
        ).getString();
    }

    static int workFromRepairCost(int repairCost) {
        if (repairCost <= 0) {
            return 0;
        }

        int work = 0;
        while (repairCost > 0 && work < 30) {
            work++;
            repairCost >>>= 1;
        }

        return work;
    }
}
