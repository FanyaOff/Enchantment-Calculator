package com.fanya.enchantmentcalculator;

import net.fabricmc.api.ModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class EnchantmentCalculatorMod implements ModInitializer {
    public static final String MOD_ID = "assets/enchantmentcalculator";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    @Override
    public void onInitialize() {
        LOGGER.info("Enchantments Calculator mod initialized!");
    }
}
