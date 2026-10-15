package com.fanya.enchantmentcalculator.client.mixin;

import com.fanya.enchantmentcalculator.client.automation.AuthoritativeAnvilState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.protocol.game.ClientboundSetExperiencePacket;
import net.minecraft.world.inventory.AnvilMenu;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** обновление уровня после работы наковальни */
@Mixin(ClientPacketListener.class)
public abstract class AnvilExperienceMixin {
    @Inject(method = "handleSetExperience", at = @At("TAIL"))
    private void enchantmentCalculator$onServerExperience(
            ClientboundSetExperiencePacket packet,
            CallbackInfo callback
    ) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player != null && minecraft.player.containerMenu instanceof AnvilMenu menu) {
            ((AuthoritativeAnvilState) menu).enchantmentCalculator$recordServerExperience(
                    packet.getExperienceLevel()
            );
        }
    }
}
