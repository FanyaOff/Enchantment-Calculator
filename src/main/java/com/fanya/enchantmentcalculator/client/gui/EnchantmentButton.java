package com.fanya.enchantmentcalculator.client.gui;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.client.MinecraftClient;

import java.util.function.BiConsumer;

public class EnchantmentButton extends ButtonWidget {
    // Приятные цвета
    private static final int BUTTON_NORMAL = 0xFFC6C6C6;
    private static final int BUTTON_HOVER = 0xFFE0E0E0;
    private static final int BUTTON_SELECTED = 0xFF9ACD32;
    private static final int BUTTON_DISABLED = 0xFF888888;
    private static final int BORDER_LIGHT = 0xFFFFFFFF;
    private static final int BORDER_DARK = 0xFF555555;
    private static final int TEXT_COLOR = 0xFF000000;
    private static final int TEXT_DISABLED = 0xFF666666;

    private final Enchantment enchantment;
    private final int maxLevel;
    private final BiConsumer<Enchantment, Integer> changeListener;
    private int currentLevel = 0;
    private boolean enabled = true;

    public EnchantmentButton(int x, int y, int width, int height,
                             Enchantment enchantment, int maxLevel,
                             BiConsumer<Enchantment, Integer> changeListener) {
        super(x, y, width, height, getButtonText(enchantment, 0),
                button -> {}, DEFAULT_NARRATION_SUPPLIER);
        this.enchantment = enchantment;
        this.maxLevel = maxLevel;
        this.changeListener = changeListener;
        updateMessage();
    }

    @Override
    public void onPress() {
        if (!enabled) return;

        currentLevel++;
        if (currentLevel > maxLevel) {
            currentLevel = 0;
        }

        updateMessage();
        changeListener.accept(enchantment, currentLevel);
    }

    private void updateMessage() {
        this.setMessage(getButtonText(enchantment, currentLevel));
    }

    private static Text getButtonText(Enchantment ench, int level) {
        String raw = getEnchantmentDisplayName(ench).getString();
        if (raw.endsWith(" I")) raw = raw.substring(0, raw.length() - 2);
        if (level == 0) {
            return Text.literal(raw).formatted(Formatting.GRAY);
        } else {
            return Text.literal(raw + " " + level).formatted(Formatting.WHITE);
        }
    }


    private static Text getEnchantmentDisplayName(Enchantment enchantment) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.world != null) {
            try {
                RegistryEntry<Enchantment> entry = client.world.getRegistryManager()
                        .getWrapperOrThrow(RegistryKeys.ENCHANTMENT)
                        .streamEntries()
                        .filter(e -> e.value() == enchantment)
                        .findFirst()
                        .orElse(null);

                if (entry != null) {
                    return Enchantment.getName(entry, 1);
                }
            } catch (Exception e) {
                // Fallback
            }
        }

        String className = enchantment.getClass().getSimpleName();
        return Text.literal(className.replace("Enchantment", ""));
    }

    @Override
    public void renderWidget(DrawContext context, int mouseX, int mouseY, float delta) {
        drawMinecraftButton(context, this.getX(), this.getY(), this.width, this.height,
                this.isHovered(), enabled, currentLevel > 0);

        int textColor = enabled ? TEXT_COLOR : TEXT_DISABLED;
        context.drawCenteredTextWithShadow(MinecraftClient.getInstance().textRenderer,
                this.getMessage(), this.getX() + this.width / 2,
                this.getY() + (this.height - 8) / 2, textColor);
    }

    private void drawMinecraftButton(DrawContext context, int x, int y, int width, int height,
                                     boolean hovered, boolean enabled, boolean selected) {
        int baseColor;
        if (!enabled) {
            baseColor = BUTTON_DISABLED;
        } else if (selected) {
            baseColor = BUTTON_SELECTED;
        } else if (hovered) {
            baseColor = BUTTON_HOVER;
        } else {
            baseColor = BUTTON_NORMAL;
        }

        context.fill(x, y, x + width, y + height, baseColor);

        if (enabled && !selected) {
            context.fill(x, y, x + width - 1, y + 1, BORDER_LIGHT);
            context.fill(x, y, x + 1, y + height - 1, BORDER_LIGHT);

            context.fill(x + 1, y + height - 1, x + width, y + height, BORDER_DARK);
            context.fill(x + width - 1, y + 1, x + width, y + height - 1, BORDER_DARK);
        } else {
            context.fill(x, y, x + width - 1, y + 1, BORDER_DARK);
            context.fill(x, y, x + 1, y + height - 1, BORDER_DARK);

            context.fill(x + 1, y + height - 1, x + width, y + height, BORDER_LIGHT);
            context.fill(x + width - 1, y + 1, x + width, y + height - 1, BORDER_LIGHT);
        }
    }

    public Enchantment getEnchantment() {
        return enchantment;
    }

    public void setLevel(int level) {
        this.currentLevel = Math.max(0, Math.min(maxLevel, level));
        updateMessage();
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
        this.active = enabled;
        if (!enabled && currentLevel > 0) {
            currentLevel = 0;
            updateMessage();
            changeListener.accept(enchantment, 0);
        }
    }
}
