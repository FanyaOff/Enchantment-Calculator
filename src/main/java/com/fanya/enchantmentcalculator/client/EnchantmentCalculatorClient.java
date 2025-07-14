package com.fanya.enchantmentcalculator.client;

import com.fanya.enchantmentcalculator.EnchantmentCalculatorMod;
import com.fanya.enchantmentcalculator.data.EnchantmentData;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;

public class EnchantmentCalculatorClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> {
            EnchantmentCalculatorMod.LOGGER.info("Initializing enchantment data on world join");
            EnchantmentData.initialize();
        });

        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> {
            EnchantmentCalculatorMod.LOGGER.info("Reinitializing enchantment data on disconnect");
            EnchantmentData.reinitialize();
        });

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (client.world != null && !EnchantmentData.isInitialized()) {
                EnchantmentCalculatorMod.LOGGER.info("Late initialization of enchantment data");
                EnchantmentData.initialize();
            }
        });
    }
}
