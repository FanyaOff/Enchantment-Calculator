package com.fanya.enchantmentcalculator.client.automation;

import net.minecraft.world.item.ItemStack;

/** данные наковальни от сервера */
public interface AuthoritativeAnvilState {
    int enchantmentCalculator$outputRevision();

    ItemStack enchantmentCalculator$serverOutput();

    int enchantmentCalculator$costRevision();

    int enchantmentCalculator$serverCost();

    int enchantmentCalculator$experienceRevision();

    int enchantmentCalculator$serverExperienceLevel();

    void enchantmentCalculator$recordServerExperience(int level);
}
