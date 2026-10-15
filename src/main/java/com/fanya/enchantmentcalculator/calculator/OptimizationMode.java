package com.fanya.enchantmentcalculator.calculator;

import net.minecraft.network.chat.Component;

public enum OptimizationMode {
    LEVELS("enchantmentcalculator.ui.mode_levels"),
    EXPERIENCE("enchantmentcalculator.ui.mode_experience");

    private final String translationKey;

    OptimizationMode(String translationKey) {
        this.translationKey = translationKey;
    }

    public Component getDisplayName() {
        return Component.translatable(translationKey);
    }
}
