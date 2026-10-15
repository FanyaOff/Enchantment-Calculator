package com.fanya.enchantmentcalculator.client.mixin;

import com.fanya.enchantmentcalculator.EnchantmentCalculatorMod;
import com.fanya.enchantmentcalculator.calculator.CalculationResult;
import com.fanya.enchantmentcalculator.calculator.EnchantmentCalculator;
import com.fanya.enchantmentcalculator.calculator.EnchantmentCombination;
import com.fanya.enchantmentcalculator.calculator.OptimizationMode;
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
import net.minecraft.util.FormattedCharSequence;
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
        context.nextStratum();
        if (page != Page.CLOSED) {
            if (enchantmentCalculator$isCompact()) {
                context.pose().pushMatrix();
                context.pose().translate(leftPos, topPos);
                enchantmentCalculator$drawPanel(context, 0, 0, imageWidth, true);
                context.nextStratum();
                if (page == Page.SELECTION) {
                    enchantmentCalculator$drawSelection(context, 0, true);
                } else {
                    enchantmentCalculator$drawResult(context, 0, true, mouseX, mouseY);
                }
                context.pose().popMatrix();
            } else {
                int panelX = enchantmentCalculator$panelX();
                enchantmentCalculator$drawPanel(context, panelX, topPos, PANEL_WIDTH, false);
                context.nextStratum();
                if (page == Page.SELECTION) {
                    enchantmentCalculator$drawSelection(context, panelX, false);
                } else {
                    enchantmentCalculator$drawResult(context, panelX, false, mouseX, mouseY);
                }
            }
            context.nextStratum();
            for (AbstractWidget widget : calculatorWidgets) {
                widget.render(context, mouseX, mouseY, delta);
            }
        }
        context.nextStratum();
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

    @Unique
    private void enchantmentCalculator$drawSelection(GuiGraphics context, int panelX, boolean compact) {
        int centerX = compact ? 88 : panelX + PANEL_WIDTH / 2;
        int titleY = compact ? 9 : topPos + 9;
        Component title = Component.translatable("enchantmentcalculator.ui.title");
        context.drawString(font, title, centerX - font.width(title) / 2, titleY, 0xFF404040, false);
        int maxRows = compact ? 4 : 6;
        Component line = errorMessage == null
                ? Component.literal(availableEnchantments.isEmpty() ? "0/0"
                        : (scrollOffset + 1) + "-" + Math.min(scrollOffset + maxRows,
                                availableEnchantments.size()) + "/" + availableEnchantments.size())
                : Component.translatable(errorMessage);
        int lineY = compact ? 30 : topPos + 127;
        for (FormattedCharSequence wrapped : font.split(line, PANEL_WIDTH - 24)) {
            context.drawString(font, wrapped, centerX - font.width(wrapped) / 2,
                    lineY, errorMessage == null ? 0xFF404040 : 0xFFAA2222, false);
            break;
        }
    }

    @Unique
    private void enchantmentCalculator$drawResult(
            GuiGraphics context, int panelX, boolean compact, int mouseX, int mouseY) {
        int originY = compact ? 0 : topPos;
        int centerX = compact ? 88 : panelX + PANEL_WIDTH / 2;
        Component title = Component.translatable("enchantmentcalculator.ui.plan");
        context.drawString(font, title, centerX - font.width(title) / 2,
                originY + 9, 0xFF404040, false);
        if (result == null || result.getSteps().isEmpty()) return;
        Component totals = Component.translatable("enchantmentcalculator.ui.summary",
                result.getTotalLevels(), result.getTotalExperience());
        context.drawString(font, totals, centerX - font.width(totals) / 2,
                originY + 26, 0xFF404040, false);
        CalculationResult.Step step = result.getSteps().get(stepIndex);
        Component label = Component.translatable("enchantmentcalculator.ui.step_cost",
                stepIndex + 1, result.getSteps().size(), step.getLevels());
        context.drawString(font, label, centerX - font.width(label) / 2,
                originY + 42, 0xFF404040, false);
        int lineY = originY + 57;
        for (FormattedCharSequence line : font.split(Component.literal(step.getDescription()), 154)) {
            if (lineY > originY + 91) break;
            context.drawString(font, line, panelX + 11, lineY, 0xFF303030, false);
            lineY += 11;
        }
        int absoluteX = compact ? leftPos : panelX;
        int absoluteY = compact ? topPos : originY;
        if (mouseX >= absoluteX + 8 && mouseX < absoluteX + (compact ? 168 : PANEL_WIDTH - 8)
                && mouseY >= absoluteY + 54 && mouseY < absoluteY + 100) {
            context.setTooltipForNextFrame(Component.literal(step.getDescription()), mouseX, mouseY);
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
        int x = enchantmentCalculator$isCompact() ? leftPos : enchantmentCalculator$panelX();
        int width = enchantmentCalculator$isCompact() ? imageWidth : PANEL_WIDTH;
        if (mouseX >= x && mouseX < x + width && mouseY >= topPos && mouseY < topPos + PANEL_HEIGHT) {
            if (page == Page.SELECTION) {
                int rows = enchantmentCalculator$isCompact() ? 4 : 6;
                scrollOffset = Math.clamp(scrollOffset + (scrollY > 0 ? -1 : 1), 0,
                        Math.max(0, availableEnchantments.size() - rows));
            } else if (result != null) {
                stepIndex = Math.clamp(stepIndex + (scrollY > 0 ? -1 : 1),
                        0, result.getSteps().size() - 1);
            }
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
        return width < imageWidth + PANEL_WIDTH + PANEL_GAP + EDGE_MARGIN * 2;
    }

    @Unique
    private int enchantmentCalculator$panelX() {
        return leftPos + imageWidth + PANEL_GAP;
    }

    @Unique
    private void enchantmentCalculator$reflowAnvil() {
        int centered = (width - imageWidth) / 2;
        int desired = centered;
        if (!enchantmentCalculator$isCompact() && page != Page.CLOSED) {
            desired = (width - imageWidth - PANEL_GAP - PANEL_WIDTH) / 2;
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
        if (page == Page.SELECTION) enchantmentCalculator$buildSelection();
        else enchantmentCalculator$buildResult();
    }

    @Unique
    private void enchantmentCalculator$buildSelection() {
        boolean compact = enchantmentCalculator$isCompact();
        int x = compact ? leftPos : enchantmentCalculator$panelX();
        int y = topPos;
        int rows = compact ? 4 : 6;
        scrollOffset = Math.min(scrollOffset, Math.max(0, availableEnchantments.size() - rows));
        if (compact) {
            Button up = enchantmentCalculator$addIcon(x + 8, y + 25, 20, 20,
                    "enchantmentcalculator.ui.scroll_up", AnvilTextures.UP_TEXTURE,
                    AnvilTextures.UP_HOVER_TEXTURE, AnvilTextures.UP_DISABLED_TEXTURE,
                    button -> { scrollOffset--; enchantmentCalculator$rebuildWidgets(); });
            up.active = scrollOffset > 0;
            Button down = enchantmentCalculator$addIcon(x + 148, y + 25, 20, 20,
                    "enchantmentcalculator.ui.scroll_down", AnvilTextures.DOWN_TEXTURE,
                    AnvilTextures.DOWN_HOVER_TEXTURE, AnvilTextures.DOWN_DISABLED_TEXTURE,
                    button -> { scrollOffset++; enchantmentCalculator$rebuildWidgets(); });
            down.active = scrollOffset + rows < availableEnchantments.size();
        } else {
            Button up = enchantmentCalculator$addIcon(x + 169, y + 10, 20, 20,
                    "enchantmentcalculator.ui.scroll_up", AnvilTextures.UP_TEXTURE,
                    AnvilTextures.UP_HOVER_TEXTURE, AnvilTextures.UP_DISABLED_TEXTURE,
                    button -> { scrollOffset--; enchantmentCalculator$rebuildWidgets(); });
            up.active = scrollOffset > 0;
            Button down = enchantmentCalculator$addIcon(x + 169, y + 32, 20, 20,
                    "enchantmentcalculator.ui.scroll_down", AnvilTextures.DOWN_TEXTURE,
                    AnvilTextures.DOWN_HOVER_TEXTURE, AnvilTextures.DOWN_DISABLED_TEXTURE,
                    button -> { scrollOffset++; enchantmentCalculator$rebuildWidgets(); });
            down.active = scrollOffset + rows < availableEnchantments.size();
            calculateButton = enchantmentCalculator$addIcon(x + 169, y + 54, 20, 20,
                    "enchantmentcalculator.ui.calculate", AnvilTextures.OPEN_TEXTURE,
                    AnvilTextures.OPEN_HOVER_TEXTURE, AnvilTextures.CALCULATE_DISABLED_TEXTURE,
                    button -> enchantmentCalculator$calculate());
            resetButton = enchantmentCalculator$addIcon(x + 169, y + 76, 20, 20,
                    "enchantmentcalculator.ui.reset", AnvilTextures.RESET_TEXTURE,
                    AnvilTextures.RESET_HOVER_TEXTURE, AnvilTextures.RESET_DISABLED_TEXTURE,
                    button -> enchantmentCalculator$reset());
        }
        for (int row = 0; row < rows && scrollOffset + row < availableEnchantments.size(); row++) {
            Enchantment enchantment = availableEnchantments.get(scrollOffset + row);
            EnchantmentLevelSlider slider = new EnchantmentLevelSlider(
                    x + 8, y + (compact ? 47 + row * 20 : 10 + row * 22),
                    compact ? 160 : 153, compact ? 18 : 20, enchantment,
                    selectedEnchantments.getOrDefault(enchantment, 0),
                    level -> enchantmentCalculator$setLevel(enchantment, level));
            slider.active = enchantmentCalculator$isCompatible(enchantment);
            sliders.add(slider);
            calculatorWidgets.add(slider);
            addRenderableWidget(slider);
        }
        int footerY = compact ? y + 126 : y + 144;
        enchantmentCalculator$addButton(x + 8, footerY, 78, 18,
                optimizationMode.getDisplayName(), button -> {
                    optimizationMode = optimizationMode == OptimizationMode.LEVELS
                            ? OptimizationMode.EXPERIENCE : OptimizationMode.LEVELS;
                    enchantmentCalculator$rebuildWidgets();
                });
        if (compact) {
            calculateButton = enchantmentCalculator$addButton(x + 8, y + 146, 78, 18,
                    Component.translatable("enchantmentcalculator.ui.calculate_short"),
                    button -> enchantmentCalculator$calculate());
            resetButton = enchantmentCalculator$addButton(x + 90, y + 146, 78, 18,
                    Component.translatable("enchantmentcalculator.ui.reset"),
                    button -> enchantmentCalculator$reset());
        }
        enchantmentCalculator$updateSelectionControls();
    }

    @Unique
    private void enchantmentCalculator$buildResult() {
        boolean compact = enchantmentCalculator$isCompact();
        int x = compact ? leftPos : enchantmentCalculator$panelX();
        int y = topPos;
        int navigationY = compact ? y + 104 : y + 125;
        Button previous = enchantmentCalculator$addButton(x + 8, navigationY, 76, 18,
                Component.translatable("enchantmentcalculator.ui.previous_step"),
                button -> { stepIndex--; enchantmentCalculator$rebuildWidgets(); });
        previous.active = stepIndex > 0;
        Button next = enchantmentCalculator$addButton(x + 92, navigationY, 76, 18,
                Component.translatable("enchantmentcalculator.ui.next_step"),
                button -> { stepIndex++; enchantmentCalculator$rebuildWidgets(); });
        next.active = result != null && stepIndex < result.getSteps().size() - 1;
        enchantmentCalculator$addButton(x + 8, y + 145, 160, 18,
                Component.translatable("enchantmentcalculator.ui.back_selection"),
                button -> { page = Page.SELECTION; enchantmentCalculator$rebuildWidgets(); });
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
