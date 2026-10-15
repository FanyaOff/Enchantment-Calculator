package com.fanya.enchantmentcalculator.client.mixin;

import com.fanya.enchantmentcalculator.client.automation.AuthoritativeAnvilState;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.AnvilMenu;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

/** отслеживание обновлений наковальни */
@Mixin(AbstractContainerMenu.class)
public abstract class AnvilSyncMixin implements AuthoritativeAnvilState {
    @Unique
    private int enchantmentCalculator$outputRevision;

    @Unique
    private ItemStack enchantmentCalculator$serverOutput = ItemStack.EMPTY;

    @Unique
    private int enchantmentCalculator$costRevision;

    @Unique
    private int enchantmentCalculator$serverCost;

    @Unique
    private int enchantmentCalculator$experienceRevision;

    @Unique
    private int enchantmentCalculator$serverExperienceLevel;

    @Inject(method = "setItem", at = @At("TAIL"))
    private void enchantmentCalculator$onServerSlot(
            int slot,
            int stateId,
            ItemStack stack,
            CallbackInfo callback
    ) {
        if ((Object) this instanceof AnvilMenu && slot == 2) {
            enchantmentCalculator$recordOutput(stack);
        }
    }

    @Inject(method = "initializeContents", at = @At("TAIL"))
    private void enchantmentCalculator$onServerContents(
            int stateId,
            List<ItemStack> stacks,
            ItemStack cursor,
            CallbackInfo callback
    ) {
        if (!((Object) this instanceof AnvilMenu)) {
            return;
        }
        if (stacks.size() > 2) {
            enchantmentCalculator$recordOutput(stacks.get(2));
        }
    }

    @Inject(method = "setData", at = @At("TAIL"))
    private void enchantmentCalculator$onServerCost(int id, int value, CallbackInfo callback) {
        if ((Object) this instanceof AnvilMenu && id == 0) {
            enchantmentCalculator$serverCost = value;
            enchantmentCalculator$costRevision++;
        }
    }

    @Unique
    private void enchantmentCalculator$recordOutput(ItemStack stack) {
        enchantmentCalculator$outputRevision++;
        enchantmentCalculator$serverOutput = stack.copy();
    }

    @Override
    public int enchantmentCalculator$outputRevision() {
        return enchantmentCalculator$outputRevision;
    }

    @Override
    public ItemStack enchantmentCalculator$serverOutput() {
        return enchantmentCalculator$serverOutput;
    }

    @Override
    public int enchantmentCalculator$costRevision() {
        return enchantmentCalculator$costRevision;
    }

    @Override
    public int enchantmentCalculator$serverCost() {
        return enchantmentCalculator$serverCost;
    }

    @Override
    public int enchantmentCalculator$experienceRevision() {
        return enchantmentCalculator$experienceRevision;
    }

    @Override
    public int enchantmentCalculator$serverExperienceLevel() {
        return enchantmentCalculator$serverExperienceLevel;
    }

    @Override
    public void enchantmentCalculator$recordServerExperience(int level) {
        enchantmentCalculator$serverExperienceLevel = level;
        enchantmentCalculator$experienceRevision++;
    }
}
