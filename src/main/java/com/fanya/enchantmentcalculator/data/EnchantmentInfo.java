package com.fanya.enchantmentcalculator.data;

import java.util.List;
import net.minecraft.world.item.enchantment.Enchantment;

public record EnchantmentInfo(
        Enchantment enchantment,
        int maxLevel,
        List<Enchantment> incompatibleEnchantments
) {
}
