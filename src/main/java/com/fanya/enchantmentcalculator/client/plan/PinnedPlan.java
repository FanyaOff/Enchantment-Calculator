package com.fanya.enchantmentcalculator.client.plan;

import com.fanya.enchantmentcalculator.calculator.CalculationResult;
import com.fanya.enchantmentcalculator.calculator.EnchantmentCombination;
import com.fanya.enchantmentcalculator.calculator.OptimizationMode;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.item.ItemStack;

import java.lang.ref.WeakReference;
import java.util.List;

/** план между открытиями наковальни */
public record PinnedPlan(
        WeakReference<ClientLevel> world,
        CalculationResult result,
        List<EnchantmentCombination> selections,
        ItemStack item,
        OptimizationMode mode,
        int completedSteps,
        int shownStep
) {
    public PinnedPlan {
        selections = List.copyOf(selections);
        item = item.copy();
    }
}
