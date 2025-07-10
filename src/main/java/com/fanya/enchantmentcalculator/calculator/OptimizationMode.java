package com.fanya.enchantmentcalculator.calculator;

import net.minecraft.text.Text;

public enum OptimizationMode {
    LEVELS("Levels"),
    EXPERIENCE("Experience");

    private final String name;

    OptimizationMode(String name) {
        this.name = name;
    }

    public Text getDisplayName() {
        return Text.literal(name);
    }
}
