package com.fanya.enchantmentcalculator.client.gui;

import net.minecraft.client.gui.components.Button;
import net.minecraft.resources.ResourceLocation;

/** иконка поверх обычной кнопки */
public record CalculatorIconWidget(
        Button button,
        ResourceLocation normal,
        ResourceLocation highlighted,
        ResourceLocation disabled
) {
}
