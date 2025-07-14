package com.fanya.enchantmentcalculator.client.mixin;

import net.minecraft.client.gui.widget.ButtonWidget;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(ButtonWidget.class)
public interface ButtonWidgetAccessor {
    @Accessor("DEFAULT_NARRATION_SUPPLIER")
    static ButtonWidget.NarrationSupplier getDefaultNarrationSupplier() {
        throw new AssertionError();
    }
}
