package com.fanya.enchantmentcalculator.client.gui;

import com.fanya.enchantmentcalculator.client.mixin.ButtonWidgetAccessor;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.tooltip.Tooltip;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

public class CustomButtonHelper {

    public static final Identifier PANEL_TEXTURE = id("container");

    public enum ButtonType {
        UP("up", "enchantmentcalculator.ui.scroll_up"),
        DOWN("down", "enchantmentcalculator.ui.scroll_down"),
        CALCULATE("calculate_button", "enchantmentcalculator.ui.calculate"),
        RESET("reset_button", "enchantmentcalculator.ui.reset"),
        FORWARD("forward", "enchantmentcalculator.ui.next_step"),
        BACKWARD("backward", "enchantmentcalculator.ui.previous_step");

        public final Identifier normal;
        public final Identifier highlighted;
        public final Identifier disabled;
        public final String tooltipKey;

        ButtonType(String baseName, String tooltipKey) {
            this.normal = id(baseName);
            this.highlighted = id(baseName + "_highlited");
            this.disabled = id(baseName + "_disabled");
            this.tooltipKey = tooltipKey;
        }
    }

    private static Identifier id(String path) {
        return Identifier.of("enchantmentcalculator", "textures/gui/" + path + ".png");
    }

    public static ButtonWidget createButton(int x, int y, int width, int height, ButtonType type, ButtonWidget.PressAction action) {
        ButtonWidget button = new ButtonWidget(x, y, width, height, Text.empty(), action, ButtonWidgetAccessor.getDefaultNarrationSupplier()) {
            @Override
            public void renderWidget(DrawContext context, int mouseX, int mouseY, float delta) {
                Identifier texture = !this.active ? type.disabled : (this.isHovered() ? type.highlighted : type.normal);

                context.drawTexture(RenderLayer::getGuiTextured, texture, this.getX(), this.getY(), 0, 0, this.width, this.height, this.width, this.height
                );
            }
        };

        button.setTooltip(Tooltip.of(Text.translatable(type.tooltipKey)));

        return button;
    }

    public static void drawPanelTexture(DrawContext context, int x, int y, int width, int height) {
        context.drawTexture(RenderLayer::getGuiTextured, PANEL_TEXTURE, x, y, 0, 0, width, height, 256, 256);
    }
}
