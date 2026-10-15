package com.fanya.enchantmentcalculator.client.gui;

import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.enchantment.Enchantment;

import java.util.function.IntConsumer;

/** ползунок уровня чар */
public final class EnchantmentLevelSlider extends AbstractSliderButton {
    private final Enchantment enchantment;
    private final int maxLevel;
    private final IntConsumer onLevelChanged;
    private int selectedLevel;

    public EnchantmentLevelSlider(
            int x,
            int y,
            int width,
            int height,
            Enchantment enchantment,
            int initialLevel,
            IntConsumer onLevelChanged
    ) {
        super(x, y, width, height, Component.empty(),
                (double) initialLevel / enchantment.getMaxLevel());
        this.enchantment = enchantment;
        this.maxLevel = enchantment.getMaxLevel();
        this.onLevelChanged = onLevelChanged;
        this.selectedLevel = initialLevel;

        updateMessage();
        setTooltip(Tooltip.create(Component.translatable(
                "enchantmentcalculator.ui.slider_hint",
                enchantment.description(),
                maxLevel
        )));
    }

    public Enchantment enchantment() {
        return enchantment;
    }

    @Override
    protected void updateMessage() {
        if (enchantment == null) {
            return;
        }
        setMessage(Component.translatable(
                "enchantmentcalculator.ui.slider_level",
                enchantment.description(),
                currentLevel(),
                maxLevel
        ));
    }

    @Override
    protected void applyValue() {
        int level = currentLevel();
        if (level != selectedLevel) {
            selectedLevel = level;
            onLevelChanged.accept(level);
        }
    }

    private int currentLevel() {
        return (int) Math.round(value * maxLevel);
    }
}
