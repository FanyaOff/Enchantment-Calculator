package com.fanya.enchantmentcalculator.client.automation;

import com.fanya.enchantmentcalculator.calculator.CalculationResult;
import com.fanya.enchantmentcalculator.calculator.EnchantmentCombination;
import net.minecraft.client.Minecraft;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.inventory.AnvilMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.ItemEnchantments;

import java.util.ArrayList;
import java.util.List;

/** подготовка предметов обычными кликами */
public final class AutomaticAnvil {
    public record Click(int slot, ClickType type) {
    }

    public record Preparation(boolean started, String problem, List<Click> clicks) {
        public static Preparation success() {
            return new Preparation(true, null, List.of());
        }

        public static Preparation success(List<Click> clicks) {
            return new Preparation(true, null, List.copyOf(clicks));
        }

        public static Preparation failed(String problem) {
            return new Preparation(false, problem, List.of());
        }
    }

    private AutomaticAnvil() {
    }

    public static Preparation prepare(
            Minecraft client,
            AnvilMenu menu,
            ItemStack originalItem,
            List<EnchantmentCombination> selections,
            CalculationResult.Step step
    ) {
        Preparation preflight = check(client, menu, originalItem, selections, step);
        if (!preflight.started()) {
            return preflight;
        }

        int leftMask = step.getLeftMask();
        int rightMask = step.getRightMask();
        ItemStack leftInput = menu.getSlot(0).getItem();
        ItemStack rightInput = menu.getSlot(1).getItem();
        boolean relocateOriginal = canRelocateOriginal(leftInput, leftMask, originalItem);
        int leftSource = leftInput.isEmpty() || relocateOriginal
                ? findInventorySlot(menu, leftMask, originalItem, selections)
                : 0;
        int rightSource = rightInput.isEmpty()
                ? findInventorySlot(menu, rightMask, originalItem, selections)
                : 1;

        if (leftSource < 0) {
            return Preparation.failed("enchantmentcalculator.auto.missing_left");
        }
        if (rightSource < 0) {
            return Preparation.failed("enchantmentcalculator.auto.missing_right");
        }

        List<Click> clicks = new ArrayList<>();
        if (relocateOriginal) {
            int freeSlot = findEmptyInventorySlot(menu);
            if (freeSlot < 0) {
                return Preparation.failed("enchantmentcalculator.auto.free_slot");
            }
            clicks.add(new Click(0, ClickType.PICKUP));
            clicks.add(new Click(freeSlot, ClickType.PICKUP));
        }
        if (leftSource != 0) {
            clicks.add(new Click(leftSource, ClickType.PICKUP));
            clicks.add(new Click(0, ClickType.PICKUP));
        }
        if (rightSource != 1) {
            clicks.add(new Click(rightSource, ClickType.PICKUP));
            clicks.add(new Click(1, ClickType.PICKUP));
        }

        return Preparation.success(clicks);
    }

    /** проверка перед запуском */
    public static Preparation check(
            Minecraft client,
            AnvilMenu menu,
            ItemStack originalItem,
            List<EnchantmentCombination> selections,
            CalculationResult.Step step
    ) {
        if (client.player == null || client.gameMode == null) {
            return Preparation.failed("enchantmentcalculator.auto.player_unavailable");
        }
        if (!hasInventorySpace(client)) {
            return Preparation.failed("enchantmentcalculator.auto.free_slot");
        }
        if (!menu.getCarried().isEmpty()) {
            return Preparation.failed("enchantmentcalculator.auto.empty_cursor");
        }
        if (!menu.getSlot(2).getItem().isEmpty()) {
            return Preparation.failed("enchantmentcalculator.auto.take_output");
        }
        int leftMask = step.getLeftMask();
        int rightMask = step.getRightMask();
        ItemStack leftInput = menu.getSlot(0).getItem();
        ItemStack rightInput = menu.getSlot(1).getItem();

        if (!leftInput.isEmpty()
                && !matches(leftInput, leftMask, originalItem, selections)
                && !canRelocateOriginal(leftInput, leftMask, originalItem)) {
            return Preparation.failed("enchantmentcalculator.auto.clear_left");
        }
        if (!rightInput.isEmpty() && !matches(rightInput, rightMask, originalItem, selections)) {
            return Preparation.failed("enchantmentcalculator.auto.clear_right");
        }

        if ((leftInput.isEmpty() || canRelocateOriginal(leftInput, leftMask, originalItem))
                && findInventorySlot(menu, leftMask, originalItem, selections) < 0) {
            return Preparation.failed("enchantmentcalculator.auto.missing_left");
        }
        if (rightInput.isEmpty()
                && findInventorySlot(menu, rightMask, originalItem, selections) < 0) {
            return Preparation.failed("enchantmentcalculator.auto.missing_right");
        }

        return Preparation.success();
    }

    private static boolean canRelocateOriginal(
            ItemStack stack,
            int leftMask,
            ItemStack originalItem
    ) {
        return (leftMask & 1) == 0
                && !stack.isEmpty()
                && ItemStack.isSameItemSameComponents(stack, originalItem);
    }

    public static boolean hasInventorySpace(Minecraft client) {
        if (client.player == null) {
            return false;
        }
        for (int slot = 0; slot < 36; slot++) {
            if (client.player.getInventory().getItem(slot).isEmpty()) {
                return true;
            }
        }
        return false;
    }

    public static boolean matchesPlannedResult(
            ItemStack output,
            CalculationResult.Step step,
            ItemStack originalItem,
            List<EnchantmentCombination> selections
    ) {
        int resultMask = step.getLeftMask() | step.getRightMask();
        return matches(output, resultMask, originalItem, selections);
    }

    /** проверка промежуточных предметов */
    public static boolean matchesPlannedOperand(
            ItemStack stack,
            CalculationResult result,
            ItemStack originalItem,
            List<EnchantmentCombination> selections
    ) {
        for (CalculationResult.Step step : result.getSteps()) {
            if (matches(stack, step.getLeftMask(), originalItem, selections)
                    || matches(stack, step.getRightMask(), originalItem, selections)
                    || matchesPlannedResult(stack, step, originalItem, selections)) {
                return true;
            }
        }
        return false;
    }

    private static int findEmptyInventorySlot(AnvilMenu menu) {
        for (int slot = 3; slot < menu.slots.size(); slot++) {
            if (menu.getSlot(slot).getItem().isEmpty()) {
                return slot;
            }
        }
        return -1;
    }

    private static int findInventorySlot(
            AnvilMenu menu,
            int mask,
            ItemStack originalItem,
            List<EnchantmentCombination> selections
    ) {
        for (int slot = 3; slot < menu.slots.size(); slot++) {
            ItemStack candidate = menu.getSlot(slot).getItem();
            if (matches(candidate, mask, originalItem, selections)) {
                return slot;
            }
        }
        return -1;
    }

    private static boolean matches(
            ItemStack stack,
            int mask,
            ItemStack originalItem,
            List<EnchantmentCombination> selections
    ) {
        if (stack.isEmpty()) {
            return false;
        }
        if ((mask & 1) != 0) {
            return matchesItem(stack, mask, originalItem, selections);
        }
        return matchesBook(stack, mask, selections);
    }

    private static boolean matchesItem(
            ItemStack stack,
            int mask,
            ItemStack originalItem,
            List<EnchantmentCombination> selections
    ) {
        if (mask == 1) {
            return ItemStack.isSameItemSameComponents(stack, originalItem);
        }
        if (!ItemStack.isSameItem(stack, originalItem)) {
            return false;
        }

        ItemEnchantments enchantments = stack.getOrDefault(
                DataComponents.ENCHANTMENTS,
                ItemEnchantments.EMPTY
        );
        return matchesSelectedEnchantments(enchantments, mask, selections);
    }

    private static boolean matchesBook(
            ItemStack stack,
            int mask,
            List<EnchantmentCombination> selections
    ) {
        if (!stack.is(Items.ENCHANTED_BOOK)) {
            return false;
        }
        if (Integer.bitCount(mask) == 1
                && stack.getOrDefault(DataComponents.REPAIR_COST, 0) != 0) {
            return false;
        }
        ItemEnchantments enchantments = stack.getOrDefault(
                DataComponents.STORED_ENCHANTMENTS,
                ItemEnchantments.EMPTY
        );
        if (enchantments.size() != Integer.bitCount(mask)) {
            return false;
        }
        return matchesSelectedEnchantments(enchantments, mask, selections);
    }

    private static boolean matchesSelectedEnchantments(
            ItemEnchantments enchantments,
            int mask,
            List<EnchantmentCombination> selections
    ) {
        for (int index = 0; index < selections.size(); index++) {
            EnchantmentCombination selection = selections.get(index);
            int expectedLevel = (mask & (1 << (index + 1))) == 0
                    ? 0
                    : selection.level();
            int actualLevel = levelOf(enchantments, selection.enchantment());
            if (actualLevel != expectedLevel) {
                return false;
            }
        }
        return true;
    }

    private static int levelOf(ItemEnchantments enchantments, Enchantment target) {
        for (Holder<Enchantment> entry : enchantments.keySet()) {
            if (entry.value() == target) {
                return enchantments.getLevel(entry);
            }
        }
        return 0;
    }
}
