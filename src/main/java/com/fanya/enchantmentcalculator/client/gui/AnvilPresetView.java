package com.fanya.enchantmentcalculator.client.gui;

import com.fanya.enchantmentcalculator.client.preset.PresetStore;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

import java.util.List;
import java.util.function.Consumer;
import java.util.function.IntConsumer;
import java.util.function.Predicate;

/** вид списка пресетов и окна сохранения */
public final class AnvilPresetView {
    public interface ButtonFactory {
        Button add(int x, int y, int width, int height, Component label, Button.OnPress action);
    }

    public record Actions(Consumer<PresetStore.Preset> apply, IntConsumer scroll,
                   Runnable edit, Runnable delete, Runnable backToPlan) {
    }

    private AnvilPresetView() {
    }

    public static void buildList(
            ButtonFactory buttons, Actions actions,
            List<PresetStore.Preset> presets, int panelX, int topPos, int panelWidth,
            int offset, String selectedId, String deleteId,
            boolean autoPending, boolean hasItem, boolean showPlan
    ) {
        for (int row = 0; row < 4 && row + offset < presets.size(); row++) {
            PresetStore.Preset preset = presets.get(row + offset);
            Button entry = buttons.add(
                    panelX + 8, topPos + 37 + row * 22, panelWidth - 40, 20,
                    Component.literal((preset.id().equals(selectedId) ? "▸ " : "")
                            + preset.name()),
                    button -> actions.apply().accept(preset));
            entry.active = !autoPending && hasItem;
            entry.setTooltip(Tooltip.create(Component.translatable(
                    "enchantmentcalculator.preset.entry_hint",
                    preset.name(), preset.enchantments().size())));
        }
        Button up = buttons.add(
                panelX + panelWidth - 29, topPos + 37, 21, 20,
                Component.literal("↑"), button -> actions.scroll().accept(-1));
        up.active = offset > 0 && !autoPending;
        Button down = buttons.add(
                panelX + panelWidth - 29, topPos + 103, 21, 20,
                Component.literal("↓"), button -> actions.scroll().accept(1));
        down.active = offset + 4 < presets.size() && !autoPending;

        if (showPlan) {
            buttons.add(panelX + 8, topPos + 124, panelWidth - 16, 18,
                    Component.translatable("enchantmentcalculator.preset.back_plan"),
                    button -> actions.backToPlan().run());
        }

        int footerWidth = (panelWidth - 20) / 2;
        Button back = buttons.add(
                panelX + 8, topPos + 145, footerWidth, 18,
                Component.translatable("enchantmentcalculator.preset.edit"),
                button -> actions.edit().run());
        back.active = !autoPending && hasItem;
        Button delete = buttons.add(
                panelX + 12 + footerWidth, topPos + 145, footerWidth, 18,
                Component.translatable(selectedId != null && selectedId.equals(deleteId)
                        ? "enchantmentcalculator.preset.confirm_delete"
                        : "enchantmentcalculator.preset.delete"),
                button -> actions.delete().run());
        delete.active = selectedId != null && !autoPending && hasItem;
    }

    public static void drawList(
            GuiGraphics context, AnvilPanelRenderer painter,
            ItemStack item, List<PresetStore.Preset> presets, String message,
            int panelX, int panelY, int panelWidth, boolean showPlan, boolean hasItem
    ) {
        int center = panelX + panelWidth / 2;
        painter.drawCenteredFit(context,
                Component.translatable("enchantmentcalculator.preset.title"),
                center, panelY + 8, panelWidth - 40, 0xFF303030);
        painter.drawCenteredFit(context,
                hasItem ? item.getHoverName()
                        : Component.translatable("enchantmentcalculator.preset.insert_item", item.getHoverName()),
                center, panelY + 22, panelWidth - 18, 0xFF66523D);
        if (presets.isEmpty()) {
            painter.drawCenteredFit(context,
                    Component.translatable("enchantmentcalculator.preset.empty"),
                    center, panelY + 73, panelWidth - 22, 0xFF666666);
        }
        if (!showPlan) {
            Component hint = message == null
                    ? Component.translatable("enchantmentcalculator.preset.count", presets.size())
                    : Component.translatable(message);
            painter.drawCenteredFit(context, hint, center, panelY + 129, panelWidth - 18,
                    message == null ? 0xFF646464 : 0xFFA33A26);
        }
    }

    public static EditBox buildDialog(
            Font font, int screenWidth, int screenHeight, String draft,
            Predicate<String> alreadyNamed, Consumer<String> onChange,
            Runnable cancel, Runnable save,
            ButtonFactory buttons, Consumer<EditBox> register,
            List<AbstractWidget> dialogWidgets
    ) {
        int modalWidth = Math.min(230, screenWidth - 16);
        int x = (screenWidth - modalWidth) / 2;
        int y = (screenHeight - 118) / 2;
        EditBox input = new EditBox(font, x + 12, y + 51, modalWidth - 24, 18,
                Component.translatable("enchantmentcalculator.preset.name"));
        input.setMaxLength(40);
        input.setValue(draft);
        input.setHint(Component.translatable("enchantmentcalculator.preset.name_hint"));
        register.accept(input);
        dialogWidgets.add(input);

        int buttonWidth = (modalWidth - 28) / 2;
        Button cancelButton = buttons.add(x + 12, y + 83, buttonWidth, 20,
                Component.translatable("enchantmentcalculator.preset.cancel"),
                button -> cancel.run());
        dialogWidgets.add(cancelButton);
        Button saveButton = buttons.add(x + 16 + buttonWidth, y + 83, buttonWidth, 20,
                Component.translatable("enchantmentcalculator.preset.save"),
                button -> save.run());
        dialogWidgets.add(saveButton);
        input.setResponder(value -> {
            onChange.accept(value);
            saveButton.active = !value.trim().isEmpty();
            saveButton.setMessage(Component.translatable(alreadyNamed.test(value.trim())
                    ? "enchantmentcalculator.preset.update"
                    : "enchantmentcalculator.preset.save"));
        });
        saveButton.active = !draft.trim().isEmpty();
        return input;
    }

    public static void drawDialog(
            GuiGraphics context, AnvilPanelRenderer painter,
            int screenWidth, int screenHeight, ItemStack item,
            int enchantmentCount, Component mode, boolean saveError
    ) {
        int modalWidth = Math.min(230, screenWidth - 16);
        int x = (screenWidth - modalWidth) / 2;
        int y = (screenHeight - 118) / 2;
        context.fill(0, 0, screenWidth, screenHeight, 0x88000000);
        context.fill(x, y, x + modalWidth, y + 118, 0xFF3D3831);
        context.fill(x + 2, y + 2, x + modalWidth - 2, y + 116, 0xFFC6BBA3);
        context.fill(x + 2, y + 2, x + modalWidth - 2, y + 5, 0xFFB48B48);
        painter.drawCenteredFit(context,
                Component.translatable("enchantmentcalculator.preset.save_title"),
                x + modalWidth / 2, y + 11, modalWidth - 24, 0xFF302920);
        painter.drawCenteredFit(context, item.getHoverName(),
                x + modalWidth / 2, y + 27, modalWidth - 24, 0xFF604628);
        painter.drawCenteredFit(context,
                Component.translatable("enchantmentcalculator.preset.details", enchantmentCount, mode),
                x + modalWidth / 2, y + 39, modalWidth - 24, 0xFF514B41);
        if (saveError) {
            painter.drawCenteredFit(context,
                    Component.translatable("enchantmentcalculator.preset.save_failed"),
                    x + modalWidth / 2, y + 106, modalWidth - 24, 0xFFA33A26);
        }
    }
}
