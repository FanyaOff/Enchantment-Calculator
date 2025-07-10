package com.fanya.enchantmentcalculator.data;

import net.minecraft.enchantment.Enchantment;
import java.util.List;

public record EnchantmentInfo(
        Enchantment enchantment,
        int maxLevel,
        int weight,
        List<Enchantment> incompatibleEnchantments
) {}
