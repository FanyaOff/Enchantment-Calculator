package com.fanya.enchantmentcalculator.client.mixin;

import com.fanya.enchantmentcalculator.EnchantmentCalculatorMod;
import com.fanya.enchantmentcalculator.calculator.CalculationResult;
import com.fanya.enchantmentcalculator.calculator.EnchantmentCalculator;
import com.fanya.enchantmentcalculator.calculator.EnchantmentCombination;
import com.fanya.enchantmentcalculator.calculator.OptimizationMode;
import com.fanya.enchantmentcalculator.client.gui.AnvilPanelRenderer;
import com.fanya.enchantmentcalculator.client.gui.AnvilTextures;
import com.fanya.enchantmentcalculator.client.gui.CalculatorIconWidget;
import com.fanya.enchantmentcalculator.client.gui.EnchantmentLevelSlider;
import com.fanya.enchantmentcalculator.data.EnchantmentData;
import com.fanya.enchantmentcalculator.data.EnchantmentInfo;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.inventory.AnvilScreen;
import net.minecraft.client.gui.screens.inventory.ItemCombinerScreen;
//? if >1.21.8
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.AnvilMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** The Fair Play screen only reads item data and displays a calculated plan. */
@Mixin(AnvilScreen.class)
public abstract class FairPlayAnvilScreenMixin extends ItemCombinerScreen<AnvilMenu> {
    @Unique private static final int PANEL_WIDTH = 197;
    @Unique private static final int PANEL_HEIGHT = 166;
    @Unique private static final int PANEL_GAP = 4;
    @Unique private static final int EDGE_MARGIN = 4;
    @Unique private static final int MIN_SIDE_WIDTH = 140;

    @Shadow private EditBox name;

    @Unique private final List<AbstractWidget> calculatorWidgets = new ArrayList<>();
    @Unique private final List<CalculatorIconWidget> iconWidgets = new ArrayList<>();
    @Unique private final List<EnchantmentLevelSlider> sliders = new ArrayList<>();
    @Unique private final Map<Enchantment, Integer> selectedEnchantments = new LinkedHashMap<>();
    @Unique private List<Enchantment> availableEnchantments = List.of();
    @Unique private ItemStack previousItem = ItemStack.EMPTY;
    @Unique private CalculationResult result;
    @Unique private OptimizationMode optimizationMode = OptimizationMode.LEVELS;
    @Unique private Page page = Page.CLOSED;
    @Unique private int scrollOffset;
    @Unique private int stepIndex;
    @Unique private String errorMessage;
    @Unique private Button calculateButton;
    @Unique private Button resetButton;

    private enum Page { CLOSED, SELECTION, RESULT }

    public FairPlayAnvilScreenMixin(AnvilMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, null);
    }

    @Inject(method = "subInit", at = @At("TAIL"))
    private void enchantmentCalculator$onSetup(CallbackInfo callback) {
        enchantmentCalculator$refreshItem();
        enchantmentCalculator$rebuildWidgets();
    }

    @Inject(method = "slotChanged", at = @At("TAIL"))
    private void enchantmentCalculator$onSlotUpdate(
            AbstractContainerMenu menu, int slotId, ItemStack stack, CallbackInfo callback) {
        if (slotId == 0 && enchantmentCalculator$refreshItem()) {
            enchantmentCalculator$rebuildWidgets();
        }
    }

    @Override
    public void render(GuiGraphics context, int mouseX, int mouseY, float delta) {
        super.render(context, mouseX, mouseY, delta);
        if (page == Page.CLOSED) {
            context.nextStratum();
            enchantmentCalculator$drawIcons(context);
            return;
        }
        context.nextStratum();
        AnvilPanelRenderer painter = new AnvilPanelRenderer(
                font, leftPos, topPos, availableEnchantments, scrollOffset, errorMessage,
                result, stepIndex, null, null, 0, null, enchantmentCalculator$pinFitsBelow());
        if (enchantmentCalculator$isCompact()) {
            context.pose().pushMatrix();
            context.pose().translate(leftPos, topPos);
            enchantmentCalculator$drawPanel(context, 0, 0, imageWidth, true);
            context.nextStratum();
            if (page == Page.SELECTION) painter.drawSelection(context);
            else painter.drawResult(context, mouseX, mouseY);
            context.pose().popMatrix();
        } else {
            int panelWidth = enchantmentCalculator$sidePanelWidth();
            if (page == Page.SELECTION || enchantmentCalculator$hasTwoSides()) {
                enchantmentCalculator$drawPanel(context,
                        enchantmentCalculator$leftPanelX(), topPos, panelWidth, false);
                if (errorMessage != null && calculateButton != null) {
                    calculateButton.setTooltip(Tooltip.create(Component.translatable(errorMessage)));
                }
            }
            if (page == Page.RESULT) {
                int panelX = enchantmentCalculator$rightPanelX();
                enchantmentCalculator$drawPanel(context, panelX, topPos, panelWidth, false);
                painter.drawSideResult(context, panelX, panelWidth, mouseX, mouseY);
            }
        }
        context.nextStratum();
        for (AbstractWidget widget : calculatorWidgets) widget.render(context, mouseX, mouseY, delta);
        context.nextStratum();
        enchantmentCalculator$drawIcons(context);
    }

    @Unique
    private void enchantmentCalculator$drawIcons(GuiGraphics context) {
        for (CalculatorIconWidget icon : iconWidgets) {
            Button widget = icon.button();
            ResourceLocation texture = !widget.active ? icon.disabled()
                    : widget.isHovered() ? icon.highlighted() : icon.normal();
            context.blit(RenderPipelines.GUI_TEXTURED, texture, widget.getX(), widget.getY(),
                    0, 0, widget.getWidth(), widget.getHeight(),
                    widget.getWidth(), widget.getHeight());
        }
    }

    @Unique
    private void enchantmentCalculator$drawPanel(
            GuiGraphics context, int x, int y, int width, boolean separator) {
        context.blit(RenderPipelines.GUI_TEXTURED, AnvilTextures.PANEL_TEXTURE,
                x, y, 0, 0, 4, PANEL_HEIGHT, 256, 256);
        context.blit(RenderPipelines.GUI_TEXTURED, AnvilTextures.PANEL_TEXTURE,
                x + 4, y, 4, 0, width - 8, PANEL_HEIGHT, 256, 256);
        context.blit(RenderPipelines.GUI_TEXTURED, AnvilTextures.PANEL_TEXTURE,
                x + width - 4, y, 193, 0, 4, PANEL_HEIGHT, 256, 256);
        if (separator) {
            context.fill(x + 7, y + 23, x + width - 7, y + 24, 0xFF777777);
            context.fill(x + 7, y + 24, x + width - 7, y + 25, 0xFFFFFFFF);
        }
    }

    @Override
    protected void renderTooltip(GuiGraphics context, int mouseX, int mouseY) {
        if (page != Page.CLOSED && enchantmentCalculator$isCompact()) hoveredSlot = null;
        super.renderTooltip(context, mouseX, mouseY);
    }

    //? if <=1.21.8 {
    /*@Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (page == Page.CLOSED || !enchantmentCalculator$isCompact())
            return super.mouseClicked(mouseX, mouseY, button);
        for (int i = calculatorWidgets.size() - 1; i >= 0; i--) {
            AbstractWidget widget = calculatorWidgets.get(i);
            if (widget.mouseClicked(mouseX, mouseY, button)) {
                setFocused(widget);
                setDragging(widget instanceof EnchantmentLevelSlider && button == 0);
                return true;
            }
        }
        return true;
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (getFocused() instanceof EnchantmentLevelSlider slider && isDragging())
            return slider.mouseDragged(mouseX, mouseY, button, dragX, dragY);
        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (getFocused() instanceof EnchantmentLevelSlider slider && isDragging()) {
            setDragging(false);
            return slider.mouseReleased(mouseX, mouseY, button);
        }
        return super.mouseReleased(mouseX, mouseY, button);
    }*/
    //?} else {
    @Override
    public boolean mouseClicked(MouseButtonEvent click, boolean doubled) {
        if (page == Page.CLOSED || !enchantmentCalculator$isCompact())
            return super.mouseClicked(click, doubled);
        for (int i = calculatorWidgets.size() - 1; i >= 0; i--) {
            AbstractWidget widget = calculatorWidgets.get(i);
            if (widget.mouseClicked(click, doubled)) {
                setFocused(widget);
                setDragging(widget instanceof EnchantmentLevelSlider && click.button() == 0);
                return true;
            }
        }
        return true;
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent click, double dragX, double dragY) {
        if (getFocused() instanceof EnchantmentLevelSlider slider && isDragging())
            return slider.mouseDragged(click, dragX, dragY);
        return super.mouseDragged(click, dragX, dragY);
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent click) {
        if (getFocused() instanceof EnchantmentLevelSlider slider && isDragging()) {
            setDragging(false);
            return slider.mouseReleased(click);
        }
        return super.mouseReleased(click);
    }
    //?}

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (page == Page.CLOSED || scrollY == 0) return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
        int selectionX = enchantmentCalculator$isCompact() ? leftPos : enchantmentCalculator$leftPanelX();
        int resultX = enchantmentCalculator$isCompact() ? leftPos : enchantmentCalculator$rightPanelX();
        int panelWidth = enchantmentCalculator$isCompact() ? imageWidth : enchantmentCalculator$sidePanelWidth();
        if (mouseY >= topPos && mouseY < topPos + PANEL_HEIGHT
                && mouseX >= selectionX && mouseX < selectionX + panelWidth
                && (page == Page.SELECTION || enchantmentCalculator$hasTwoSides())) {
            int rows = enchantmentCalculator$isCompact() ? 4 : 6;
            scrollOffset = Math.clamp(scrollOffset + (scrollY > 0 ? -1 : 1), 0,
                    Math.max(0, availableEnchantments.size() - rows));
            enchantmentCalculator$rebuildWidgets();
            return true;
        }
        if (page == Page.RESULT && mouseY >= topPos && mouseY < topPos + PANEL_HEIGHT
                && mouseX >= resultX && mouseX < resultX + panelWidth && result != null) {
            stepIndex = Math.clamp(stepIndex + (scrollY > 0 ? -1 : 1),
                    0, result.getSteps().size() - 1);
            enchantmentCalculator$rebuildWidgets();
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    @Unique
    private boolean enchantmentCalculator$refreshItem() {
        ItemStack item = menu.getSlot(0).getItem();
        if (ItemStack.matches(item, previousItem)) return false;
        previousItem = item.copy();
        selectedEnchantments.clear();
        availableEnchantments = EnchantmentData.getApplicableEnchantments(item);
        result = null;
        errorMessage = null;
        scrollOffset = 0;
        stepIndex = 0;
        page = item.isEmpty() ? Page.CLOSED : Page.SELECTION;
        return true;
    }

    @Unique
    private boolean enchantmentCalculator$isCompact() {
        return !enchantmentCalculator$hasTwoSides() && !enchantmentCalculator$hasOneSide();
    }

    @Unique
    private boolean enchantmentCalculator$hasTwoSides() {
        int centeredSide = (width - imageWidth) / 2;
        return centeredSide - PANEL_GAP - EDGE_MARGIN >= MIN_SIDE_WIDTH;
    }

    @Unique
    private boolean enchantmentCalculator$hasOneSide() {
        return width >= PANEL_WIDTH + PANEL_GAP + imageWidth + EDGE_MARGIN * 2;
    }

    @Unique
    private int enchantmentCalculator$sidePanelWidth() {
        if (enchantmentCalculator$hasTwoSides()) {
            int centeredSide = (width - imageWidth) / 2;
            return Math.min(PANEL_WIDTH, centeredSide - PANEL_GAP - EDGE_MARGIN);
        }
        return PANEL_WIDTH;
    }

    @Unique
    private int enchantmentCalculator$leftPanelX() {
        return leftPos - PANEL_GAP - enchantmentCalculator$sidePanelWidth();
    }

    @Unique
    private int enchantmentCalculator$rightPanelX() {
        return leftPos + imageWidth + PANEL_GAP;
    }

    @Unique
    private boolean enchantmentCalculator$pinFitsBelow() {
        return topPos + imageHeight + 24 <= height;
    }

    @Unique
    private void enchantmentCalculator$reflowAnvil() {
        int centered = (width - imageWidth) / 2;
        int desired = centered;
        if (!enchantmentCalculator$hasTwoSides() && enchantmentCalculator$hasOneSide()
                && page != Page.CLOSED) {
            int groupX = (width - PANEL_WIDTH - PANEL_GAP - imageWidth) / 2;
            desired = page == Page.SELECTION ? groupX + PANEL_WIDTH + PANEL_GAP : groupX;
        }
        if (leftPos != desired) {
            leftPos = desired;
            name.setX(leftPos + 62);
        }
    }

    @Unique
    private void enchantmentCalculator$rebuildWidgets() {
        for (AbstractWidget widget : calculatorWidgets) removeWidget(widget);
        calculatorWidgets.clear();
        iconWidgets.clear();
        sliders.clear();
        calculateButton = null;
        resetButton = null;
        enchantmentCalculator$reflowAnvil();
        if (menu.getSlot(0).getItem().isEmpty()) return;
        if (page == Page.CLOSED) {
            enchantmentCalculator$addIcon(leftPos + imageWidth - 24, topPos + 3, 20, 20,
                    "enchantmentcalculator.ui.open", AnvilTextures.OPEN_TEXTURE,
                    AnvilTextures.OPEN_HOVER_TEXTURE, AnvilTextures.OPEN_TEXTURE,
                    button -> { page = Page.SELECTION; enchantmentCalculator$rebuildWidgets(); });
            return;
        }
        if (enchantmentCalculator$isCompact()) {
            enchantmentCalculator$addButton(leftPos + 151, topPos + 4, 18, 18,
                    Component.literal("×"), button -> {
                        page = Page.CLOSED; enchantmentCalculator$rebuildWidgets();
                    });
        }
        if (enchantmentCalculator$isCompact()) {
            if (page == Page.SELECTION) enchantmentCalculator$buildSelection();
            else enchantmentCalculator$buildResult();
        } else {
            if (page == Page.SELECTION || enchantmentCalculator$hasTwoSides()) {
                enchantmentCalculator$buildSideSelection();
            }
            if (page == Page.RESULT) enchantmentCalculator$buildSideResult();
        }
    }

    @Unique
    private void enchantmentCalculator$buildSelection() {
        Button up = enchantmentCalculator$addIcon(leftPos + 8, topPos + 25, 20, 20,
                "enchantmentcalculator.ui.scroll_up", AnvilTextures.UP_TEXTURE,
                AnvilTextures.UP_HOVER_TEXTURE, AnvilTextures.UP_DISABLED_TEXTURE,
                button -> enchantmentCalculator$scroll(-1));
        up.active = scrollOffset > 0;
        Button down = enchantmentCalculator$addIcon(leftPos + 148, topPos + 25, 20, 20,
                "enchantmentcalculator.ui.scroll_down", AnvilTextures.DOWN_TEXTURE,
                AnvilTextures.DOWN_HOVER_TEXTURE, AnvilTextures.DOWN_DISABLED_TEXTURE,
                button -> enchantmentCalculator$scroll(1));
        down.active = scrollOffset + 4 < availableEnchantments.size();

        for (int row = 0; row < 4 && scrollOffset + row < availableEnchantments.size(); row++) {
            enchantmentCalculator$addSlider(leftPos + 8, topPos + 47 + row * 20,
                    160, 18, availableEnchantments.get(scrollOffset + row));
        }
        enchantmentCalculator$addButton(leftPos + 8, topPos + 126, 78, 18,
                Component.translatable("enchantmentcalculator.ui.import_books"),
                button -> enchantmentCalculator$importBooks());
        enchantmentCalculator$addButton(leftPos + 90, topPos + 126, 78, 18,
                optimizationMode.getDisplayName(), button -> enchantmentCalculator$toggleMode());
        calculateButton = enchantmentCalculator$addButton(leftPos + 8, topPos + 146, 78, 18,
                Component.translatable("enchantmentcalculator.ui.calculate_short"),
                button -> enchantmentCalculator$calculate());
        resetButton = enchantmentCalculator$addButton(leftPos + 90, topPos + 146, 78, 18,
                Component.translatable("enchantmentcalculator.ui.reset"),
                button -> enchantmentCalculator$reset());
        enchantmentCalculator$updateSelectionControls();
    }

    @Unique
    private void enchantmentCalculator$buildResult() {
        Button previous = enchantmentCalculator$addButton(leftPos + 8, topPos + 104, 76, 18,
                Component.translatable("enchantmentcalculator.ui.previous_step"),
                button -> { stepIndex--; enchantmentCalculator$rebuildWidgets(); });
        previous.active = stepIndex > 0;
        Button next = enchantmentCalculator$addButton(leftPos + 92, topPos + 104, 76, 18,
                Component.translatable("enchantmentcalculator.ui.next_step"),
                button -> { stepIndex++; enchantmentCalculator$rebuildWidgets(); });
        next.active = result != null && stepIndex < result.getSteps().size() - 1;
        Button back = enchantmentCalculator$addButton(leftPos + 8, topPos + 145, 78, 18,
                Component.translatable("enchantmentcalculator.ui.edit_short"),
                button -> { page = Page.SELECTION; enchantmentCalculator$rebuildWidgets(); });
        back.setTooltip(Tooltip.create(Component.translatable("enchantmentcalculator.ui.back_selection")));
    }

    @Unique
    private void enchantmentCalculator$buildSideSelection() {
        int panelX = enchantmentCalculator$leftPanelX();
        int panelWidth = enchantmentCalculator$sidePanelWidth();
        int railX = panelX + panelWidth - 28;
        int firstY = topPos + 10;
        Button up = enchantmentCalculator$addIcon(railX, firstY, 20, 20,
                "enchantmentcalculator.ui.scroll_up", AnvilTextures.UP_TEXTURE,
                AnvilTextures.UP_HOVER_TEXTURE, AnvilTextures.UP_DISABLED_TEXTURE,
                button -> enchantmentCalculator$scroll(-1));
        up.active = scrollOffset > 0;
        Button down = enchantmentCalculator$addIcon(railX, firstY + 22, 20, 20,
                "enchantmentcalculator.ui.scroll_down", AnvilTextures.DOWN_TEXTURE,
                AnvilTextures.DOWN_HOVER_TEXTURE, AnvilTextures.DOWN_DISABLED_TEXTURE,
                button -> enchantmentCalculator$scroll(1));
        down.active = scrollOffset + 6 < availableEnchantments.size();
        calculateButton = enchantmentCalculator$addIcon(railX, firstY + 44, 20, 20,
                "enchantmentcalculator.ui.calculate", AnvilTextures.OPEN_TEXTURE,
                AnvilTextures.OPEN_HOVER_TEXTURE, AnvilTextures.CALCULATE_DISABLED_TEXTURE,
                button -> enchantmentCalculator$calculate());
        resetButton = enchantmentCalculator$addIcon(railX, firstY + 66, 20, 20,
                "enchantmentcalculator.ui.reset", AnvilTextures.RESET_TEXTURE,
                AnvilTextures.RESET_HOVER_TEXTURE, AnvilTextures.RESET_DISABLED_TEXTURE,
                button -> enchantmentCalculator$reset());
        for (int row = 0; row < 6 && scrollOffset + row < availableEnchantments.size(); row++) {
            enchantmentCalculator$addSlider(panelX + 8, firstY + row * 22,
                    panelWidth - 44, 20, availableEnchantments.get(scrollOffset + row));
        }
        int footerWidth = (panelWidth - 20) / 2;
        Button importBooks = enchantmentCalculator$addButton(
                panelX + 8, topPos + 144, footerWidth, 18,
                Component.translatable("enchantmentcalculator.ui.import_books"),
                button -> enchantmentCalculator$importBooks());
        importBooks.setTooltip(Tooltip.create(importBooks.getMessage()));
        Button mode = enchantmentCalculator$addButton(
                panelX + 12 + footerWidth, topPos + 144, footerWidth, 18,
                optimizationMode.getDisplayName(), button -> enchantmentCalculator$toggleMode());
        mode.setTooltip(Tooltip.create(mode.getMessage()));
        enchantmentCalculator$updateSelectionControls();
    }

    @Unique
    private void enchantmentCalculator$buildSideResult() {
        int panelX = enchantmentCalculator$rightPanelX();
        int panelWidth = enchantmentCalculator$sidePanelWidth();
        Button previous = enchantmentCalculator$addIcon(panelX + 9, topPos + 145, 12, 17,
                "enchantmentcalculator.ui.previous_step", AnvilTextures.BACK_TEXTURE,
                AnvilTextures.BACK_HOVER_TEXTURE, AnvilTextures.BACK_DISABLED_TEXTURE,
                button -> { stepIndex--; enchantmentCalculator$rebuildWidgets(); });
        previous.active = stepIndex > 0;
        Button next = enchantmentCalculator$addIcon(panelX + panelWidth - 21, topPos + 145, 12, 17,
                "enchantmentcalculator.ui.next_step", AnvilTextures.FORWARD_TEXTURE,
                AnvilTextures.FORWARD_HOVER_TEXTURE, AnvilTextures.FORWARD_DISABLED_TEXTURE,
                button -> { stepIndex++; enchantmentCalculator$rebuildWidgets(); });
        next.active = result != null && stepIndex < result.getSteps().size() - 1;
        if (!enchantmentCalculator$hasTwoSides()) {
            Button edit = enchantmentCalculator$addButton(panelX + 8, topPos + 125, 20, 18,
                    Component.literal("←"), button -> {
                        page = Page.SELECTION; enchantmentCalculator$rebuildWidgets();
                    });
            edit.setTooltip(Tooltip.create(Component.translatable(
                    "enchantmentcalculator.ui.back_selection")));
        }
    }

    @Unique
    private void enchantmentCalculator$addSlider(
            int x, int y, int width, int height, Enchantment enchantment) {
        EnchantmentLevelSlider slider = new EnchantmentLevelSlider(x, y, width, height,
                enchantment, selectedEnchantments.getOrDefault(enchantment, 0),
                level -> enchantmentCalculator$setLevel(enchantment, level));
        slider.active = enchantmentCalculator$isCompatible(enchantment);
        sliders.add(slider);
        calculatorWidgets.add(slider);
        addRenderableWidget(slider);
    }

    @Unique
    private void enchantmentCalculator$scroll(int direction) {
        int rows = enchantmentCalculator$isCompact() ? 4 : 6;
        scrollOffset = Math.clamp(scrollOffset + direction, 0,
                Math.max(0, availableEnchantments.size() - rows));
        enchantmentCalculator$rebuildWidgets();
    }

    @Unique
    private void enchantmentCalculator$toggleMode() {
        optimizationMode = optimizationMode == OptimizationMode.LEVELS
                ? OptimizationMode.EXPERIENCE : OptimizationMode.LEVELS;
        result = null;
        enchantmentCalculator$rebuildWidgets();
    }

    @Unique
    private Button enchantmentCalculator$addButton(
            int x, int y, int width, int height, Component label, Button.OnPress action) {
        Button button = Button.builder(label, action).bounds(x, y, width, height).build();
        calculatorWidgets.add(button);
        addRenderableWidget(button);
        return button;
    }

    @Unique
    private Button enchantmentCalculator$addIcon(
            int x, int y, int width, int height, String tooltip,
            ResourceLocation normal, ResourceLocation highlighted,
            ResourceLocation disabled, Button.OnPress action) {
        Button button = enchantmentCalculator$addButton(x, y, width, height,
                Component.empty(), action);
        button.setTooltip(Tooltip.create(Component.translatable(tooltip)));
        button.setAlpha(0.0F);
        iconWidgets.add(new CalculatorIconWidget(button, normal, highlighted, disabled));
        return button;
    }

    @Unique
    private boolean enchantmentCalculator$isCompatible(Enchantment candidate) {
        if (selectedEnchantments.containsKey(candidate)) return true;
        EnchantmentInfo info = EnchantmentData.getEnchantmentInfo(candidate);
        if (info == null) return false;
        for (Enchantment selected : selectedEnchantments.keySet()) {
            if (info.incompatibleEnchantments().contains(selected)) return false;
        }
        return true;
    }

    @Unique
    private void enchantmentCalculator$setLevel(Enchantment enchantment, int level) {
        if (level == 0) selectedEnchantments.remove(enchantment);
        else selectedEnchantments.put(enchantment, level);
        result = null;
        errorMessage = null;
        for (EnchantmentLevelSlider slider : sliders)
            slider.active = enchantmentCalculator$isCompatible(slider.enchantment());
        enchantmentCalculator$updateSelectionControls();
    }

    @Unique
    private void enchantmentCalculator$updateSelectionControls() {
        boolean active = !selectedEnchantments.isEmpty();
        if (calculateButton != null) calculateButton.active = active;
        if (resetButton != null) resetButton.active = active;
    }

    @Unique
    private void enchantmentCalculator$reset() {
        selectedEnchantments.clear();
        result = null;
        errorMessage = null;
        enchantmentCalculator$rebuildWidgets();
    }

    @Unique
    private void enchantmentCalculator$importBooks() {
        net.minecraft.client.Minecraft client = net.minecraft.client.Minecraft.getInstance();
        if (client.player == null || client.level == null) return;
        for (int slot = 0; slot < client.player.getInventory().getContainerSize(); slot++) {
            enchantmentCalculator$importBook(client.player.getInventory().getItem(slot));
        }
        enchantmentCalculator$importBook(menu.getSlot(1).getItem());
        result = null;
        errorMessage = null;
        enchantmentCalculator$rebuildWidgets();
    }

    @Unique
    private void enchantmentCalculator$importBook(ItemStack stack) {
        if (!stack.is(Items.ENCHANTED_BOOK)) return;
        ItemEnchantments enchantments = stack.getOrDefault(
                DataComponents.STORED_ENCHANTMENTS, ItemEnchantments.EMPTY);
        if (enchantments.size() != 1) return;
        for (Holder<Enchantment> entry : enchantments.keySet()) {
            Enchantment enchantment = entry.value();
            if (!availableEnchantments.contains(enchantment)
                    || !enchantmentCalculator$isCompatible(enchantment)) continue;
            int level = enchantments.getLevel(entry);
            selectedEnchantments.put(enchantment,
                    Math.max(level, selectedEnchantments.getOrDefault(enchantment, 0)));
        }
    }

    @Unique
    private void enchantmentCalculator$calculate() {
        List<EnchantmentCombination> selections = new ArrayList<>();
        for (Map.Entry<Enchantment, Integer> entry : selectedEnchantments.entrySet())
            selections.add(new EnchantmentCombination(entry.getKey(), entry.getValue()));
        try {
            result = EnchantmentCalculator.calculate(menu.getSlot(0).getItem(),
                    selections, optimizationMode);
            errorMessage = null;
            stepIndex = 0;
            page = Page.RESULT;
            enchantmentCalculator$rebuildWidgets();
        } catch (IllegalArgumentException exception) {
            errorMessage = exception.getMessage();
            EnchantmentCalculatorMod.LOGGER.warn("Cannot calculate anvil path: {}", errorMessage);
        }
    }
}
