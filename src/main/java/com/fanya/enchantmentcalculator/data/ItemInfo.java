package com.fanya.enchantmentcalculator.data;

import net.minecraft.enchantment.Enchantment;
import net.minecraft.item.Item;
import java.util.List;

public record ItemInfo(
        Item item,
        List<Enchantment> applicableEnchantments
) {}
