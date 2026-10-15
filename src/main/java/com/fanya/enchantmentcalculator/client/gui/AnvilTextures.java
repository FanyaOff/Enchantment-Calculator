package com.fanya.enchantmentcalculator.client.gui;

import net.minecraft.resources.ResourceLocation;

/** текстуры управления наковальней */
public final class AnvilTextures {
    private AnvilTextures() {
    }

    public static final ResourceLocation PANEL_TEXTURE =
            texture("container.png");

    public static final ResourceLocation OPEN_TEXTURE =
            texture("calculate_button.png");

    public static final ResourceLocation OPEN_HOVER_TEXTURE =
            texture("calculate_button_highlited.png");

    public static final ResourceLocation UP_TEXTURE =
            texture("up.png");

    public static final ResourceLocation UP_HOVER_TEXTURE =
            texture("up_highlited.png");

    public static final ResourceLocation UP_DISABLED_TEXTURE =
            texture("up_disabled.png");

    public static final ResourceLocation DOWN_TEXTURE =
            texture("down.png");

    public static final ResourceLocation DOWN_HOVER_TEXTURE =
            texture("down_highlited.png");

    public static final ResourceLocation DOWN_DISABLED_TEXTURE =
            texture("down_disabled.png");

    public static final ResourceLocation CALCULATE_DISABLED_TEXTURE =
            texture("calculate_button_disabled.png");

    public static final ResourceLocation RESET_TEXTURE =
            texture("reset_button.png");

    public static final ResourceLocation RESET_HOVER_TEXTURE =
            texture("reset_button_highlited.png");

    public static final ResourceLocation RESET_DISABLED_TEXTURE =
            texture("reset_button_disabled.png");

    public static final ResourceLocation BACK_TEXTURE =
            texture("backward.png");

    public static final ResourceLocation BACK_HOVER_TEXTURE =
            texture("backward_highlited.png");

    public static final ResourceLocation BACK_DISABLED_TEXTURE =
            texture("backward_disabled.png");

    public static final ResourceLocation FORWARD_TEXTURE =
            texture("forward.png");

    public static final ResourceLocation FORWARD_HOVER_TEXTURE =
            texture("forward_highlited.png");

    public static final ResourceLocation FORWARD_DISABLED_TEXTURE =
            texture("forward_disabled.png");

    private static ResourceLocation texture(String fileName) {
        return ResourceLocation.fromNamespaceAndPath(
                "enchantmentcalculator",
                "textures/gui/" + fileName
        );
    }

}
