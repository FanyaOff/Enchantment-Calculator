package com.fanya.enchantmentcalculator.client.mixin;

import com.fanya.enchantmentcalculator.EnchantmentCalculatorMod;
import com.fanya.enchantmentcalculator.calculator.CalculationResult;
import com.fanya.enchantmentcalculator.calculator.EnchantmentCalculator;
import com.fanya.enchantmentcalculator.calculator.EnchantmentCombination;
import com.fanya.enchantmentcalculator.calculator.OptimizationMode;
import com.fanya.enchantmentcalculator.client.automation.AutomaticAnvil;
import com.fanya.enchantmentcalculator.client.automation.AuthoritativeAnvilState;
import com.fanya.enchantmentcalculator.client.automation.InventoryReadiness;
import com.fanya.enchantmentcalculator.client.gui.AnvilPanelRenderer;
import com.fanya.enchantmentcalculator.client.gui.AnvilPresetView;
import com.fanya.enchantmentcalculator.client.gui.AnvilTextures;
import com.fanya.enchantmentcalculator.client.gui.CalculatorIconWidget;
import com.fanya.enchantmentcalculator.client.gui.EnchantmentLevelSlider;
import com.fanya.enchantmentcalculator.client.plan.PinnedPlan;
import com.fanya.enchantmentcalculator.client.preset.PresetStore;
import com.fanya.enchantmentcalculator.data.EnchantmentData;
import com.fanya.enchantmentcalculator.data.EnchantmentInfo;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.ArrayList;
import java.lang.ref.WeakReference;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.inventory.AnvilScreen;
import net.minecraft.client.gui.screens.inventory.ItemCombinerScreen;
//? if >1.21.8
import net.minecraft.client.input.MouseButtonEvent;
//? if >1.21.8
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.AnvilMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.ItemEnchantments;

/** панели калькулятора у наковальни */
@Mixin(AnvilScreen.class)
public abstract class AnvilScreenMixin extends ItemCombinerScreen<AnvilMenu> {
    @Unique
    private static final int PANEL_WIDTH = 197;

    @Unique
    private static final int PANEL_HEIGHT = 166;

    @Unique
    private static final int PANEL_GAP = 4;

    @Unique
    private static final int MIN_SIDE_WIDTH = 140;

    @Unique
    private static final int EDGE_MARGIN = 4;

    @Unique
    private static final int AUTO_TIMEOUT_TICKS = 400;

    @Unique
    private final List<AbstractWidget> calculatorWidgets = new ArrayList<>();

    @Unique
    private final List<AbstractWidget> presetDialogWidgets = new ArrayList<>();

    @Unique
    private final List<EnchantmentLevelSlider> levelSliders = new ArrayList<>();

    @Unique
    private Button calculateButton;

    @Unique
    private Button resetButton;

    @Unique
    private Button savePresetButton;

    @Unique
    private EditBox presetName;

    @Unique
    private String presetDraft = "";

    @Unique
    private boolean presetSaveError;

    @Unique
    private boolean presetDialog;

    @Unique
    private boolean presetWorkspace;

    @Unique
    private String selectedPresetId;

    @Unique
    private String deletePresetId;

    @Unique
    private int presetScrollOffset;

    @Unique
    private String presetMessage;

    @Unique
    private static PinnedPlan pinnedPlan;

    @Unique
    private boolean pinnedView;

    @Shadow
    private EditBox name;

    @Unique
    private final List<CalculatorIconWidget> iconWidgets = new ArrayList<>();

    @Unique
    private final Map<Enchantment, Integer> selectedEnchantments = new LinkedHashMap<>();

    @Unique
    private List<Enchantment> availableEnchantments = List.of();

    @Unique
    private ItemStack previousItem = ItemStack.EMPTY;

    @Unique
    private CalculationResult result;

    @Unique
    private InventoryReadiness.Report readiness;

    @Unique
    private String readinessProblem;

    @Unique
    private OptimizationMode optimizationMode = OptimizationMode.LEVELS;

    @Unique
    private Page page = Page.CLOSED;

    @Unique
    private int scrollOffset;

    @Unique
    private int readinessScrollOffset;

    @Unique
    private int stepIndex;

    @Unique
    private int completedSteps;

    @Unique
    private String errorMessage;

    @Unique
    private Component automaticStatus;

    @Unique
    private ItemStack planItem = ItemStack.EMPTY;

    @Unique
    private boolean preservePlan;

    @Unique
    private boolean autoPending;

    @Unique
    private AutoPhase autoPhase = AutoPhase.IDLE;

    @Unique
    private int outputRevisionBeforePreparation;

    @Unique
    private int costRevisionBeforePreparation;

    @Unique
    private int costRevisionBeforePickup;

    @Unique
    private int experienceRevisionBeforePickup;

    @Unique
    private int levelsBeforePickup;

    @Unique
    private int outputRevisionBeforePickup;

    @Unique
    private int resultInventorySlot;

    @Unique
    private int collectionStableTicks;

    @Unique
    private List<AutomaticAnvil.Click> preparationClicks = List.of();

    @Unique
    private int preparationClickIndex;

    @Unique
    private int preparationMismatchTicks;

    @Unique
    private ItemStack expectedCarriedAfterClick = ItemStack.EMPTY;

    @Unique
    private final Map<Integer, ItemStack> expectedSlotsAfterClick = new LinkedHashMap<>();

    @Unique
    private int pendingTicks;

    private enum Page {
        CLOSED,
        SELECTION,
        PRESETS,
        RESULT,
        READINESS
    }

    private enum AutoPhase {
        IDLE,
        PREPARING,
        WAIT_RESULT,
        WAIT_COLLECTION,
        WAIT_STOW
    }

    public AnvilScreenMixin(AnvilMenu handler, Inventory inventory, Component title) {
        super(handler, inventory, title, null);
    }

    @Inject(method = "subInit", at = @At("TAIL"))
    private void enchantmentCalculator$onSetup(CallbackInfo callback) {
        enchantmentCalculator$restorePinnedPlan();
        enchantmentCalculator$refreshItem();
        enchantmentCalculator$rebuildWidgets();
    }

    @Inject(method = "slotChanged", at = @At("TAIL"))
    private void enchantmentCalculator$onSlotUpdate(
            AbstractContainerMenu screenHandler,
            int slotId,
            ItemStack stack,
            CallbackInfo callback
    ) {
        if (slotId == 0) {
            if (enchantmentCalculator$refreshItem()) {
                enchantmentCalculator$rebuildWidgets();
            }
        }
    }

    @Inject(method = "containerTick", at = @At("TAIL"))
    private void enchantmentCalculator$onTick(CallbackInfo callback) {
        if (page == Page.READINESS && !autoPending) {
            enchantmentCalculator$updateReadiness();
        }
        if (!autoPending) {
            return;
        }

        pendingTicks++;
        if (autoPhase == AutoPhase.PREPARING) {
            enchantmentCalculator$advancePreparation();
        } else if (autoPhase == AutoPhase.WAIT_RESULT
                && !menu.getSlot(2).getItem().isEmpty()) {
            enchantmentCalculator$takeAutomaticResult();
        } else if (autoPhase == AutoPhase.WAIT_COLLECTION) {
            enchantmentCalculator$continueAutomaticRun();
        } else if (autoPhase == AutoPhase.WAIT_STOW) {
            enchantmentCalculator$finishStowingResult();
        }
        if (autoPending && pendingTicks > AUTO_TIMEOUT_TICKS) {
            AuthoritativeAnvilState sync = enchantmentCalculator$serverSync();
            EnchantmentCalculatorMod.LOGGER.warn(
                    "Auto anvil timed out at step {} phase {}: cost {} (revision {}), "
                            + "server level {} (revision {}), output empty {}, cursor empty {}, "
                            + "preparation clicks {}/{}",
                    stepIndex + 1, autoPhase,
                    sync.enchantmentCalculator$serverCost(), sync.enchantmentCalculator$costRevision(),
                    sync.enchantmentCalculator$serverExperienceLevel(),
                    sync.enchantmentCalculator$experienceRevision(),
                    menu.getSlot(2).getItem().isEmpty(), menu.getCarried().isEmpty(),
                    preparationClickIndex, preparationClicks.size()
            );
            String reason = autoPhase == AutoPhase.WAIT_COLLECTION || autoPhase == AutoPhase.WAIT_STOW
                    ? "enchantmentcalculator.auto.collection_timeout"
                    : "enchantmentcalculator.auto.no_result";
            enchantmentCalculator$stopAutomaticRun(Component.translatable(reason));
        }
    }

    @Override
    public void render(
            GuiGraphics context,
            int mouseX,
            int mouseY,
            float delta
    ) {
        super.render(context, mouseX, mouseY, delta);

        if (page == Page.CLOSED) {
            context.nextStratum();
            enchantmentCalculator$drawIcons(context);
            return;
        }

        // кнопки поверх панели
        context.nextStratum();
        AnvilPanelRenderer painter = enchantmentCalculator$painter();
        if (enchantmentCalculator$isCompact()) {
            context.pose().pushMatrix();
            context.pose().translate(leftPos, topPos);
            enchantmentCalculator$drawPanel(context, 0, 0, imageWidth, true);
            context.nextStratum();
            switch (page) {
                case SELECTION -> painter.drawSelection(context);
                case PRESETS -> enchantmentCalculator$drawCompactPresets(context, painter);
                case RESULT -> painter.drawResult(context, mouseX, mouseY);
                case READINESS -> painter.drawReadiness(context);
                default -> {
                }
            }
            context.pose().popMatrix();
        } else {
            int panelWidth = enchantmentCalculator$sidePanelWidth();
            if (presetWorkspace && (page == Page.PRESETS || enchantmentCalculator$hasTwoSides())) {
                int leftPanelX = enchantmentCalculator$leftPanelX();
                enchantmentCalculator$drawPanel(context, leftPanelX, topPos, panelWidth, false);
                enchantmentCalculator$drawSidePresets(context, painter, leftPanelX, panelWidth);
            } else if (page == Page.SELECTION
                    || (enchantmentCalculator$hasTwoSides() && !pinnedView && !presetWorkspace)) {
                int leftPanelX = enchantmentCalculator$leftPanelX();
                enchantmentCalculator$drawPanel(context, leftPanelX, topPos, panelWidth, false);
                enchantmentCalculator$drawSideSelection(
                        context, leftPanelX, panelWidth, mouseX, mouseY
                );
            }

            if (page == Page.RESULT || page == Page.READINESS) {
                int rightPanelX = enchantmentCalculator$rightPanelX();
                enchantmentCalculator$drawPanel(context, rightPanelX, topPos, panelWidth, false);
                if (page == Page.RESULT) {
                    painter.drawSideResult(context, rightPanelX, panelWidth, mouseX, mouseY);
                } else {
                    painter.drawSideReadiness(
                            context, rightPanelX, panelWidth, mouseX, mouseY
                    );
                }
            }
        }

        context.nextStratum();
        for (AbstractWidget widget : calculatorWidgets) {
            if (!presetDialogWidgets.contains(widget)) {
                widget.render(context, mouseX, mouseY, delta);
            }
        }
        context.nextStratum();
        enchantmentCalculator$drawIcons(context);
        if (presetDialog) {
            context.nextStratum();
            enchantmentCalculator$drawPresetDialog(context, painter);
            context.nextStratum();
            for (AbstractWidget widget : presetDialogWidgets) {
                widget.render(context, mouseX, mouseY, delta);
            }
        }
    }

    @Unique
    private void enchantmentCalculator$drawPanel(
            GuiGraphics context,
            int panelX,
            int panelY,
            int panelWidth,
            boolean separator
    ) {
        // растягиваем только середину текстуры
        context.blit(RenderPipelines.GUI_TEXTURED, AnvilTextures.PANEL_TEXTURE,
                panelX, panelY, 0, 0, 4, PANEL_HEIGHT, 256, 256);
        context.blit(RenderPipelines.GUI_TEXTURED, AnvilTextures.PANEL_TEXTURE,
                panelX + 4, panelY, 4, 0, panelWidth - 8, PANEL_HEIGHT, 256, 256);
        context.blit(RenderPipelines.GUI_TEXTURED, AnvilTextures.PANEL_TEXTURE,
                panelX + panelWidth - 4, panelY, 193, 0, 4, PANEL_HEIGHT, 256, 256);

        if (separator) {
            context.fill(panelX + 7, panelY + 23,
                    panelX + panelWidth - 7, panelY + 24, 0xFF777777);
            context.fill(panelX + 7, panelY + 24,
                    panelX + panelWidth - 7, panelY + 25, 0xFFFFFFFF);
        }
    }

    @Unique
    private AnvilPanelRenderer enchantmentCalculator$painter() {
        AnvilPanelRenderer.Readiness displayReadiness = readiness == null ? null
                : new AnvilPanelRenderer.Readiness(readiness.missingBooks(),
                        readiness.levelsNeededNow(), readiness.levelsNeededForPlan());
        return new AnvilPanelRenderer(
                font, leftPos, topPos, availableEnchantments, scrollOffset, errorMessage,
                result, stepIndex, automaticStatus, displayReadiness, readinessScrollOffset,
                readinessProblem, enchantmentCalculator$pinFitsBelow());
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
    private boolean enchantmentCalculator$hasTwoSides() {
        int centeredSide = (width - imageWidth) / 2;
        return centeredSide - PANEL_GAP - EDGE_MARGIN >= MIN_SIDE_WIDTH;
    }

    @Unique
    private boolean enchantmentCalculator$hasOneSide() {
        int groupWidth = PANEL_WIDTH + PANEL_GAP + imageWidth;
        return width >= groupWidth + 2 * EDGE_MARGIN;
    }

    @Unique
    private boolean enchantmentCalculator$isCompact() {
        return !enchantmentCalculator$hasTwoSides()
                && !enchantmentCalculator$hasOneSide();
    }

    @Unique
    private boolean enchantmentCalculator$pinFitsBelow() {
        return topPos + imageHeight + 24 <= height;
    }

    @Unique
    private void enchantmentCalculator$reflowAnvil() {
        int centered = (width - imageWidth) / 2;
        int desired = centered;
        if (!enchantmentCalculator$hasTwoSides()
                && enchantmentCalculator$hasOneSide()
                && page != Page.CLOSED) {
            int groupX = (width - PANEL_WIDTH - PANEL_GAP - imageWidth) / 2;
            desired = page == Page.SELECTION || page == Page.PRESETS
                    ? groupX + PANEL_WIDTH + PANEL_GAP
                    : groupX;
        }
        if (leftPos != desired) {
            leftPos = desired;
            name.setX(leftPos + 62);
        }
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
    private void enchantmentCalculator$drawIcons(GuiGraphics context) {
        for (CalculatorIconWidget icon : iconWidgets) {
            Button widget = icon.button();
            ResourceLocation texture = !widget.active
                    ? icon.disabled()
                    : widget.isHovered() ? icon.highlighted() : icon.normal();
            context.blit(
                    RenderPipelines.GUI_TEXTURED,
                    texture,
                    widget.getX(),
                    widget.getY(),
                    0,
                    0,
                    widget.getWidth(),
                    widget.getHeight(),
                    widget.getWidth(),
                    widget.getHeight()
            );
        }
    }

    @Override
    protected void renderTooltip(GuiGraphics context, int mouseX, int mouseY) {
        if (presetDialog || (page != Page.CLOSED && enchantmentCalculator$isCompact())) {
            hoveredSlot = null;
        }
        super.renderTooltip(context, mouseX, mouseY);
    }

    //? if <=1.21.8 {
    /*@Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (autoPending) {
            return true;
        }
        if (presetDialog) {
            for (int index = presetDialogWidgets.size() - 1; index >= 0; index--) {
                AbstractWidget widget = presetDialogWidgets.get(index);
                if (widget.mouseClicked(mouseX, mouseY, button)) {
                    setFocused(widget);
                    return true;
                }
            }
            return true;
        }
        if (page == Page.CLOSED || !enchantmentCalculator$isCompact()) {
            return super.mouseClicked(mouseX, mouseY, button);
        }

        for (int index = calculatorWidgets.size() - 1; index >= 0; index--) {
            AbstractWidget widget = calculatorWidgets.get(index);
            if (widget.mouseClicked(mouseX, mouseY, button)) {
                setFocused(widget);
                setDragging(widget instanceof EnchantmentLevelSlider && button == 0);
                return true;
            }
        }
        return true;
    }

    @Override
    public boolean mouseDragged(
            double mouseX, double mouseY, int button, double dragX, double dragY
    ) {
        if (getFocused() instanceof EnchantmentLevelSlider slider && isDragging()) {
            return slider.mouseDragged(mouseX, mouseY, button, dragX, dragY);
        }
        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (getFocused() instanceof EnchantmentLevelSlider slider && isDragging()) {
            setDragging(false);
            return slider.mouseReleased(mouseX, mouseY, button);
        }
        return super.mouseReleased(mouseX, mouseY, button);
    }
    *///?} else {
    @Override
    public boolean mouseClicked(MouseButtonEvent click, boolean doubled) {
        if (autoPending) {
            return true;
        }
        if (presetDialog) {
            for (int index = presetDialogWidgets.size() - 1; index >= 0; index--) {
                AbstractWidget widget = presetDialogWidgets.get(index);
                if (widget.mouseClicked(click, doubled)) {
                    setFocused(widget);
                    return true;
                }
            }
            return true;
        }
        if (page == Page.CLOSED || !enchantmentCalculator$isCompact()) {
            return super.mouseClicked(click, doubled);
        }

        // не даём нажать скрытые слоты
        for (int index = calculatorWidgets.size() - 1; index >= 0; index--) {
            AbstractWidget widget = calculatorWidgets.get(index);
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
        if (getFocused() instanceof EnchantmentLevelSlider slider && isDragging()) {
            return slider.mouseDragged(click, dragX, dragY);
        }
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
        if (page == Page.CLOSED || scrollY == 0) {
            return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
        }
        if (autoPending || presetDialog) {
            return true;
        }

        int direction = scrollY > 0 ? -1 : 1;
        if (enchantmentCalculator$isCompact()) {
            if (enchantmentCalculator$insidePanel(
                    mouseX, mouseY, leftPos, topPos, imageWidth)) {
                enchantmentCalculator$scrollPage(direction);
                return true;
            }
        } else {
            if ((page == Page.SELECTION || page == Page.PRESETS || enchantmentCalculator$hasTwoSides())
                    && enchantmentCalculator$insidePanel(
                            mouseX, mouseY, enchantmentCalculator$leftPanelX(),
                            topPos, enchantmentCalculator$sidePanelWidth())) {
                if (presetWorkspace) {
                    enchantmentCalculator$scrollPresets(direction);
                } else {
                    enchantmentCalculator$scroll(direction);
                }
                return true;
            }
            if (page != Page.SELECTION
                    && enchantmentCalculator$insidePanel(
                            mouseX, mouseY, enchantmentCalculator$rightPanelX(),
                            topPos, enchantmentCalculator$sidePanelWidth())) {
                enchantmentCalculator$scrollPage(direction);
                return true;
            }
        }

        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    //? if <=1.21.8 {
    /*@Inject(method = "keyPressed", at = @At("HEAD"), cancellable = true)
    private void enchantmentCalculator$onPresetKey(
            int keyCode, int scanCode, int modifiers,
            CallbackInfoReturnable<Boolean> callback
    ) {
        if (enchantmentCalculator$handlePresetKey(keyCode)) {
            callback.setReturnValue(true);
        } else if (presetDialog) {
            if (presetName != null) {
                presetName.keyPressed(keyCode, scanCode, modifiers);
            }
            callback.setReturnValue(true);
        }
    }*/
    //?} else {
    @Inject(method = "keyPressed", at = @At("HEAD"), cancellable = true)
    private void enchantmentCalculator$onPresetKey(
            KeyEvent event, CallbackInfoReturnable<Boolean> callback
    ) {
        if (enchantmentCalculator$handlePresetKey(event.key())) {
            callback.setReturnValue(true);
        } else if (presetDialog) {
            if (presetName != null) {
                presetName.keyPressed(event);
            }
            callback.setReturnValue(true);
        }
    }
    //?}

    @Unique
    private boolean enchantmentCalculator$handlePresetKey(int keyCode) {
        if (!presetDialog) {
            return false;
        }
        if (keyCode == 256) {
            enchantmentCalculator$closePresetDialog();
            return true;
        }
        if (keyCode == 257 || keyCode == 335) {
            enchantmentCalculator$savePreset();
            return true;
        }
        return false;
    }

    @Unique
    private boolean enchantmentCalculator$insidePanel(
            double mouseX,
            double mouseY,
            int panelX,
            int panelY,
            int panelWidth
    ) {
        return mouseX >= panelX && mouseX < panelX + panelWidth
                && mouseY >= panelY && mouseY < panelY + PANEL_HEIGHT;
    }

    @Unique
    private void enchantmentCalculator$scrollPage(int direction) {
        if (page == Page.SELECTION) {
            enchantmentCalculator$scroll(direction);
        } else if (page == Page.PRESETS) {
            enchantmentCalculator$scrollPresets(direction);
        } else if (page == Page.RESULT && result != null
                && !result.getSteps().isEmpty()) {
            stepIndex = Math.clamp(stepIndex + direction, 0, result.getSteps().size() - 1);
            enchantmentCalculator$savePinnedPlan();
            enchantmentCalculator$rebuildWidgets();
        } else if (page == Page.READINESS && readiness != null) {
            readinessScrollOffset = Math.clamp(
                    readinessScrollOffset + direction,
                    0,
                    Math.max(0, readiness.missingBooks().size() - 1)
            );
            enchantmentCalculator$rebuildWidgets();
        }
    }

    @Unique
    private boolean enchantmentCalculator$refreshItem() {
        ItemStack item = menu.getSlot(0).getItem();
        if (ItemStack.matches(item, previousItem)) {
            return false;
        }

        if (pinnedView && result != null) {
            previousItem = item.copy();
            return true;
        }

        boolean nextPresetItem = presetWorkspace && result != null && completedSteps > 0
                && previousItem.isEmpty() && !item.isEmpty()
                && ItemStack.isSameItemSameComponents(item, planItem);
        if (!nextPresetItem && (autoPending || preservePlan) && result != null
                && (autoPending || item.isEmpty()
                        || AutomaticAnvil.matchesPlannedOperand(
                                item, result, planItem,
                                enchantmentCalculator$selections()))) {
            previousItem = item.copy();
            return true;
        }

        previousItem = item.copy();
        presetDialog = false;
        presetName = null;
        selectedEnchantments.clear();
        availableEnchantments = EnchantmentData.getApplicableEnchantments(item);
        result = null;
        readiness = null;
        readinessProblem = null;
        errorMessage = null;
        scrollOffset = 0;
        readinessScrollOffset = 0;
        stepIndex = 0;
        completedSteps = 0;
        preservePlan = false;
        autoPending = false;
        autoPhase = AutoPhase.IDLE;
        automaticStatus = null;

        PresetStore.Preset selectedPreset = enchantmentCalculator$selectedPreset(item);
        if (selectedPreset == null && !item.isEmpty()) {
            selectedPresetId = null;
        }

        if (item.isEmpty()) {
            page = Page.CLOSED;
        } else if (presetWorkspace) {
            page = Page.PRESETS;
            if (selectedPreset != null) {
                enchantmentCalculator$applyPreset(selectedPreset);
            }
        } else if ((page == Page.CLOSED && !enchantmentCalculator$isCompact())
                || page == Page.RESULT || page == Page.READINESS) {
            page = Page.SELECTION;
        }
        return true;
    }

    @Unique
    private void enchantmentCalculator$rebuildWidgets() {
        for (AbstractWidget widget : calculatorWidgets) {
            removeWidget(widget);
        }
        calculatorWidgets.clear();
        presetDialogWidgets.clear();
        iconWidgets.clear();
        levelSliders.clear();
        calculateButton = null;
        resetButton = null;
        savePresetButton = null;

        enchantmentCalculator$reflowAnvil();
        int visibleRows = enchantmentCalculator$isCompact() ? 4 : 6;
        scrollOffset = Math.min(
                scrollOffset,
                Math.max(0, availableEnchantments.size() - visibleRows)
        );

        if (menu.getSlot(0).getItem().isEmpty() && result == null) {
            return;
        }

        if (page == Page.CLOSED) {
            if (menu.getSlot(0).getItem().isEmpty()) {
                return;
            }
            enchantmentCalculator$addIconButton(
                    leftPos + imageWidth - 24,
                    topPos + 3,
                    20,
                    20,
                    Component.translatable("enchantmentcalculator.ui.open"),
                    AnvilTextures.OPEN_TEXTURE,
                    AnvilTextures.OPEN_HOVER_TEXTURE,
                    AnvilTextures.OPEN_TEXTURE,
                    button -> enchantmentCalculator$show(presetWorkspace ? Page.PRESETS : Page.SELECTION)
            );
            return;
        }

        if (enchantmentCalculator$isCompact()) {
            enchantmentCalculator$addButton(
                    leftPos + 151,
                    topPos + 4,
                    18,
                    18,
                    Component.literal("×"),
                    button -> enchantmentCalculator$show(Page.CLOSED)
            );

            switch (page) {
                case SELECTION -> enchantmentCalculator$buildSelectionWidgets();
                case PRESETS -> enchantmentCalculator$buildCompactPresetWidgets();
                case RESULT -> enchantmentCalculator$buildResultWidgets();
                case READINESS -> enchantmentCalculator$buildReadinessWidgets();
                default -> {
                }
            }
        } else {
            if (presetWorkspace && (page == Page.PRESETS || enchantmentCalculator$hasTwoSides())) {
                enchantmentCalculator$buildSidePresetWidgets();
            } else if (page == Page.SELECTION
                    || (enchantmentCalculator$hasTwoSides() && !pinnedView && !presetWorkspace)) {
                enchantmentCalculator$buildSideSelectionWidgets();
            }
            if (page == Page.RESULT) {
                enchantmentCalculator$buildSideResultWidgets();
            } else if (page == Page.READINESS) {
                enchantmentCalculator$buildSideReadinessWidgets();
            }
        }

        if (page == Page.RESULT && !enchantmentCalculator$isCompact()
                && enchantmentCalculator$pinFitsBelow()) {
            enchantmentCalculator$addPinButton(
                    leftPos, topPos + imageHeight + 4, imageWidth, 18
            );
        }
        if (presetDialog) {
            for (AbstractWidget widget : calculatorWidgets) {
                widget.active = false;
            }
            enchantmentCalculator$buildPresetDialog();
        }
    }

    @Unique
    private Button enchantmentCalculator$addButton(
            int buttonX,
            int buttonY,
            int width,
            int height,
            Component label,
            Button.OnPress action
    ) {
        Button widget = Button.builder(label, action)
                .bounds(buttonX, buttonY, width, height)
                .build();
        calculatorWidgets.add(widget);
        addRenderableWidget(widget);
        return widget;
    }

    @Unique
    private void enchantmentCalculator$addLevelSlider(
            int x,
            int y,
            int width,
            int height,
            Enchantment enchantment
    ) {
        EnchantmentLevelSlider slider = new EnchantmentLevelSlider(
                x, y, width, height,
                enchantment,
                selectedEnchantments.getOrDefault(enchantment, 0),
                level -> enchantmentCalculator$setLevel(enchantment, level)
        );
        slider.active = !autoPending && enchantmentCalculator$isCompatible(enchantment);
        levelSliders.add(slider);
        calculatorWidgets.add(slider);
        addRenderableWidget(slider);
    }

    @Unique
    private void enchantmentCalculator$addPinButton(int x, int y, int width, int height) {
        String key = pinnedView
                ? (width < 100 ? "enchantmentcalculator.ui.unpin_short"
                        : "enchantmentcalculator.ui.unpin_result")
                : (width < 100 ? "enchantmentcalculator.ui.pin_short"
                        : "enchantmentcalculator.ui.pin_result");
        Button pin = enchantmentCalculator$addButton(
                x, y, width, height,
                Component.translatable(key),
                button -> enchantmentCalculator$togglePin()
        );
        pin.active = !autoPending;
        pin.setTooltip(Tooltip.create(Component.translatable(pinnedView
                ? "enchantmentcalculator.ui.unpin_result"
                : "enchantmentcalculator.ui.pin_result")));
    }

    @Unique
    private Button enchantmentCalculator$addIconButton(
            int buttonX,
            int buttonY,
            int width,
            int height,
            Component tooltip,
            ResourceLocation normal,
            ResourceLocation highlighted,
            ResourceLocation disabled,
            Button.OnPress action
    ) {
        Button widget = enchantmentCalculator$addButton(
                buttonX,
                buttonY,
                width,
                height,
                Component.empty(),
                action
        );
        widget.setTooltip(Tooltip.create(tooltip));
        // иконка вместо обычной кнопки
        widget.setAlpha(0.0F);
        iconWidgets.add(new CalculatorIconWidget(widget, normal, highlighted, disabled));
        return widget;
    }

    @Unique
    private void enchantmentCalculator$show(Page newPage) {
        if (menu.getSlot(0).getItem().isEmpty()) {
            if (newPage == Page.SELECTION) {
                newPage = result == null ? Page.CLOSED
                        : presetWorkspace ? Page.PRESETS : Page.RESULT;
            } else if (newPage == Page.PRESETS && result == null) {
                newPage = Page.CLOSED;
            }
        }
        page = newPage;
        enchantmentCalculator$rebuildWidgets();
    }

    @Unique
    private void enchantmentCalculator$restorePinnedPlan() {
        Minecraft client = Minecraft.getInstance();
        if (pinnedPlan == null) {
            return;
        }
        if (pinnedPlan.world().get() != client.level) {
            pinnedPlan = null;
            return;
        }

        result = pinnedPlan.result();
        selectedEnchantments.clear();
        for (EnchantmentCombination selection : pinnedPlan.selections()) {
            selectedEnchantments.put(selection.enchantment(), selection.level());
        }
        planItem = pinnedPlan.item().copy();
        optimizationMode = pinnedPlan.mode();
        completedSteps = pinnedPlan.completedSteps();
        stepIndex = Math.clamp(
                pinnedPlan.shownStep(), 0, result.getSteps().size() - 1
        );
        previousItem = menu.getSlot(0).getItem().copy();
        preservePlan = true;
        pinnedView = true;
        page = Page.RESULT;
    }

    @Unique
    private void enchantmentCalculator$savePinnedPlan() {
        Minecraft client = Minecraft.getInstance();
        if (pinnedView && client.level != null && result != null) {
            pinnedPlan = new PinnedPlan(
                    new WeakReference<>(client.level),
                    result,
                    enchantmentCalculator$selections(),
                    planItem,
                    optimizationMode,
                    completedSteps,
                    stepIndex
            );
        }
    }

    @Unique
    private void enchantmentCalculator$togglePin() {
        if (autoPending) {
            return;
        }
        if (!pinnedView) {
            if (result == null) {
                return;
            }
            pinnedView = true;
            enchantmentCalculator$savePinnedPlan();
            enchantmentCalculator$show(Page.RESULT);
            return;
        }

        pinnedView = false;
        pinnedPlan = null;
        selectedEnchantments.clear();
        enchantmentCalculator$invalidateResult();
        planItem = ItemStack.EMPTY;
        ItemStack current = menu.getSlot(0).getItem();
        previousItem = current.copy();
        availableEnchantments = EnchantmentData.getApplicableEnchantments(current);
        page = current.isEmpty() ? Page.CLOSED : Page.SELECTION;
        enchantmentCalculator$rebuildWidgets();
    }

    @Unique
    private void enchantmentCalculator$buildSelectionWidgets() {
        enchantmentCalculator$addButton(leftPos + 109, topPos + 4, 18, 18,
                Component.literal("▤"), button -> enchantmentCalculator$openPresets())
                .setTooltip(Tooltip.create(Component.translatable("enchantmentcalculator.preset.open")));
        savePresetButton = enchantmentCalculator$addButton(leftPos + 130, topPos + 4, 18, 18,
                Component.literal("+"), button -> enchantmentCalculator$openPresetDialog());
        savePresetButton.active = !autoPending && !selectedEnchantments.isEmpty();
        savePresetButton.setTooltip(Tooltip.create(Component.translatable("enchantmentcalculator.preset.save")));
        Button scrollUp = enchantmentCalculator$addIconButton(
                leftPos + 8, topPos + 25, 20, 20,
                Component.translatable("enchantmentcalculator.ui.scroll_up"),
                AnvilTextures.UP_TEXTURE,
                AnvilTextures.UP_HOVER_TEXTURE,
                AnvilTextures.UP_DISABLED_TEXTURE,
                button -> enchantmentCalculator$scroll(-1)
        );
        scrollUp.active = !autoPending && scrollOffset > 0;

        Button scrollDown = enchantmentCalculator$addIconButton(
                leftPos + 148, topPos + 25, 20, 20,
                Component.translatable("enchantmentcalculator.ui.scroll_down"),
                AnvilTextures.DOWN_TEXTURE,
                AnvilTextures.DOWN_HOVER_TEXTURE,
                AnvilTextures.DOWN_DISABLED_TEXTURE,
                button -> enchantmentCalculator$scroll(1)
        );
        scrollDown.active = scrollOffset + 4 < availableEnchantments.size();

        for (int row = 0; row < 4; row++) {
            int enchantmentIndex = scrollOffset + row;
            if (enchantmentIndex >= availableEnchantments.size()) {
                break;
            }

            Enchantment enchantment = availableEnchantments.get(enchantmentIndex);
            enchantmentCalculator$addLevelSlider(
                    leftPos + 8,
                    topPos + 47 + row * 20,
                    160,
                    18,
                    enchantment
            );
        }

        enchantmentCalculator$addButton(
                leftPos + 8, topPos + 126, 78, 18,
                Component.translatable("enchantmentcalculator.ui.import_books"),
                button -> enchantmentCalculator$importBooks()
        );
        enchantmentCalculator$addButton(
                leftPos + 90, topPos + 126, 78, 18,
                optimizationMode.getDisplayName(),
                button -> {
                    optimizationMode = optimizationMode == OptimizationMode.LEVELS
                            ? OptimizationMode.EXPERIENCE : OptimizationMode.LEVELS;
                    enchantmentCalculator$rebuildWidgets();
                }
        );
        calculateButton = enchantmentCalculator$addButton(
                leftPos + 8, topPos + 146, 78, 18,
                Component.translatable("enchantmentcalculator.ui.calculate_short"),
                button -> enchantmentCalculator$calculate()
        );
        calculateButton.active = !selectedEnchantments.isEmpty();

        resetButton = enchantmentCalculator$addButton(
                leftPos + 90, topPos + 146, 78, 18,
                Component.translatable("enchantmentcalculator.ui.reset"),
                button -> {
                    selectedEnchantments.clear();
                    enchantmentCalculator$invalidateResult();
                    enchantmentCalculator$rebuildWidgets();
                }
        );
        resetButton.active = !selectedEnchantments.isEmpty();
    }

    @Unique
    private void enchantmentCalculator$buildResultWidgets() {
        Button previous = enchantmentCalculator$addButton(
                leftPos + 8, topPos + 104, 76, 18,
                Component.translatable("enchantmentcalculator.ui.previous_step"),
                button -> {
                    stepIndex--;
                    enchantmentCalculator$savePinnedPlan();
                    enchantmentCalculator$rebuildWidgets();
                }
        );
        previous.active = !autoPending && stepIndex > 0;

        Button next = enchantmentCalculator$addButton(
                leftPos + 92, topPos + 104, 76, 18,
                Component.translatable("enchantmentcalculator.ui.next_step"),
                button -> {
                    stepIndex++;
                    enchantmentCalculator$savePinnedPlan();
                    enchantmentCalculator$rebuildWidgets();
                }
        );
        next.active = !autoPending && result != null
                && stepIndex < result.getSteps().size() - 1;

        Button autoCheck = enchantmentCalculator$addButton(
                leftPos + 8, topPos + 125, 160, 18,
                enchantmentCalculator$autoButtonLabel(),
                button -> enchantmentCalculator$checkReadiness()
        );
        autoCheck.active = !autoPending && result != null
                && completedSteps < result.getSteps().size();
        Button back = enchantmentCalculator$addButton(
                leftPos + 8, topPos + 145, 78, 18,
                Component.translatable(presetWorkspace
                        ? "enchantmentcalculator.preset.open" : "enchantmentcalculator.ui.edit_short"),
                button -> enchantmentCalculator$show(presetWorkspace ? Page.PRESETS : Page.SELECTION)
        );
        back.active = !autoPending && !pinnedView
                && (presetWorkspace || !menu.getSlot(0).getItem().isEmpty());
        back.setTooltip(Tooltip.create(Component.translatable(
                presetWorkspace ? "enchantmentcalculator.preset.open"
                        : "enchantmentcalculator.ui.back_selection"
        )));
        enchantmentCalculator$addPinButton(leftPos + 90, topPos + 145, 78, 18);
    }

    @Unique
    private void enchantmentCalculator$buildReadinessWidgets() {
        if (readiness != null && !readiness.missingBooks().isEmpty()) {
            Button previousBook = enchantmentCalculator$addIconButton(
                    leftPos + 148, topPos + 45, 20, 20,
                    Component.translatable("enchantmentcalculator.ui.scroll_up"),
                    AnvilTextures.UP_TEXTURE,
                    AnvilTextures.UP_HOVER_TEXTURE,
                    AnvilTextures.UP_DISABLED_TEXTURE,
                    button -> {
                        readinessScrollOffset--;
                        enchantmentCalculator$rebuildWidgets();
                    }
            );
            previousBook.active = readinessScrollOffset > 0;

            Button nextBook = enchantmentCalculator$addIconButton(
                    leftPos + 148, topPos + 67, 20, 20,
                    Component.translatable("enchantmentcalculator.ui.scroll_down"),
                    AnvilTextures.DOWN_TEXTURE,
                    AnvilTextures.DOWN_HOVER_TEXTURE,
                    AnvilTextures.DOWN_DISABLED_TEXTURE,
                    button -> {
                        readinessScrollOffset++;
                        enchantmentCalculator$rebuildWidgets();
                    }
            );
            nextBook.active = readinessScrollOffset < readiness.missingBooks().size() - 1;
        }

        Button execute = enchantmentCalculator$addButton(
                leftPos + 8, topPos + 125, 160, 18,
                Component.translatable("enchantmentcalculator.ui.start_auto"),
                button -> enchantmentCalculator$startAutoEnchant()
        );
        execute.active = !autoPending && readiness != null
                && readiness.ready() && readinessProblem == null;
        enchantmentCalculator$addButton(
                leftPos + 8, topPos + 145, 160, 18,
                Component.translatable("enchantmentcalculator.ui.back_plan"),
                button -> enchantmentCalculator$show(Page.RESULT)
        );
    }

    @Unique
    private void enchantmentCalculator$buildSideSelectionWidgets() {
        int panelX = enchantmentCalculator$leftPanelX();
        int panelWidth = enchantmentCalculator$sidePanelWidth();
        int railX = panelX + panelWidth - 28;
        int firstY = topPos + 10;
        int visibleRows = 6;

        Button scrollUp = enchantmentCalculator$addIconButton(
                railX, firstY, 20, 20,
                Component.translatable("enchantmentcalculator.ui.scroll_up"),
                AnvilTextures.UP_TEXTURE, AnvilTextures.UP_HOVER_TEXTURE, AnvilTextures.UP_DISABLED_TEXTURE,
                button -> enchantmentCalculator$scroll(-1)
        );
        scrollUp.active = !autoPending && scrollOffset > 0;

        Button scrollDown = enchantmentCalculator$addIconButton(
                railX, firstY + 22, 20, 20,
                Component.translatable("enchantmentcalculator.ui.scroll_down"),
                AnvilTextures.DOWN_TEXTURE, AnvilTextures.DOWN_HOVER_TEXTURE, AnvilTextures.DOWN_DISABLED_TEXTURE,
                button -> enchantmentCalculator$scroll(1)
        );
        scrollDown.active = !autoPending
                && scrollOffset + visibleRows < availableEnchantments.size();

        calculateButton = enchantmentCalculator$addIconButton(
                railX, firstY + 44, 20, 20,
                Component.translatable("enchantmentcalculator.ui.calculate"),
                AnvilTextures.OPEN_TEXTURE, AnvilTextures.OPEN_HOVER_TEXTURE, AnvilTextures.CALCULATE_DISABLED_TEXTURE,
                button -> enchantmentCalculator$calculate()
        );
        calculateButton.active = !autoPending && !selectedEnchantments.isEmpty();

        resetButton = enchantmentCalculator$addIconButton(
                railX, firstY + 66, 20, 20,
                Component.translatable("enchantmentcalculator.ui.reset"),
                AnvilTextures.RESET_TEXTURE, AnvilTextures.RESET_HOVER_TEXTURE, AnvilTextures.RESET_DISABLED_TEXTURE,
                button -> {
                    selectedEnchantments.clear();
                    enchantmentCalculator$invalidateResult();
                    enchantmentCalculator$rebuildWidgets();
                }
        );
        resetButton.active = !autoPending && !selectedEnchantments.isEmpty();

        savePresetButton = enchantmentCalculator$addButton(
                railX, firstY + 88, 20, 20, Component.literal("+"),
                button -> enchantmentCalculator$openPresetDialog());
        savePresetButton.active = !autoPending && !selectedEnchantments.isEmpty();
        savePresetButton.setTooltip(Tooltip.create(Component.translatable("enchantmentcalculator.preset.save")));
        enchantmentCalculator$addButton(
                railX, firstY + 110, 20, 20, Component.literal("▤"),
                button -> enchantmentCalculator$openPresets())
                .setTooltip(Tooltip.create(Component.translatable("enchantmentcalculator.preset.open")));

        for (int row = 0; row < visibleRows; row++) {
            int enchantmentIndex = scrollOffset + row;
            if (enchantmentIndex >= availableEnchantments.size()) {
                break;
            }

            Enchantment enchantment = availableEnchantments.get(enchantmentIndex);
            enchantmentCalculator$addLevelSlider(
                    panelX + 8,
                    firstY + row * 22,
                    panelWidth - 44,
                    20,
                    enchantment
            );
        }

        int footerWidth = (panelWidth - 20) / 2;
        Button importBooks = enchantmentCalculator$addButton(
                panelX + 8, topPos + 144, footerWidth, 18,
                Component.translatable("enchantmentcalculator.ui.import_books"),
                button -> enchantmentCalculator$importBooks()
        );
        importBooks.setTooltip(Tooltip.create(importBooks.getMessage()));
        importBooks.active = !autoPending;

        Button mode = enchantmentCalculator$addButton(
                panelX + 12 + footerWidth, topPos + 144, footerWidth, 18,
                optimizationMode.getDisplayName(),
                button -> {
                    optimizationMode = optimizationMode == OptimizationMode.LEVELS
                            ? OptimizationMode.EXPERIENCE : OptimizationMode.LEVELS;
                    enchantmentCalculator$invalidateResult();
                    enchantmentCalculator$rebuildWidgets();
                }
        );
        mode.setTooltip(Tooltip.create(mode.getMessage()));
        mode.active = !autoPending;
    }

    @Unique
    private void enchantmentCalculator$buildSideResultWidgets() {
        int panelX = enchantmentCalculator$rightPanelX();
        int panelWidth = enchantmentCalculator$sidePanelWidth();

        Button previous = enchantmentCalculator$addIconButton(
                panelX + 9, topPos + 145, 12, 17,
                Component.translatable("enchantmentcalculator.ui.previous_step"),
                AnvilTextures.BACK_TEXTURE, AnvilTextures.BACK_HOVER_TEXTURE, AnvilTextures.BACK_DISABLED_TEXTURE,
                button -> {
                    stepIndex--;
                    enchantmentCalculator$savePinnedPlan();
                    enchantmentCalculator$rebuildWidgets();
                }
        );
        previous.active = !autoPending && stepIndex > 0;

        Button next = enchantmentCalculator$addIconButton(
                panelX + panelWidth - 21, topPos + 145, 12, 17,
                Component.translatable("enchantmentcalculator.ui.next_step"),
                AnvilTextures.FORWARD_TEXTURE, AnvilTextures.FORWARD_HOVER_TEXTURE, AnvilTextures.FORWARD_DISABLED_TEXTURE,
                button -> {
                    stepIndex++;
                    enchantmentCalculator$savePinnedPlan();
                    enchantmentCalculator$rebuildWidgets();
                }
        );
        next.active = !autoPending && result != null
                && stepIndex < result.getSteps().size() - 1;

        int autoX = panelX + 8;
        int autoWidth = panelWidth - 16;
        if (!enchantmentCalculator$hasTwoSides()) {
            Button edit = enchantmentCalculator$addButton(
                    panelX + 8, topPos + 125, 20, 18,
                    Component.literal("←"),
                    button -> enchantmentCalculator$show(presetWorkspace ? Page.PRESETS : Page.SELECTION)
            );
            edit.setTooltip(Tooltip.create(
                    Component.translatable(presetWorkspace
                            ? "enchantmentcalculator.preset.open"
                            : "enchantmentcalculator.ui.back_selection")
            ));
            edit.active = !autoPending && !pinnedView
                    && (presetWorkspace || !menu.getSlot(0).getItem().isEmpty());
            autoX = panelX + 31;
            autoWidth = panelWidth - 39;
        }

        Button autoCheck = enchantmentCalculator$addButton(
                autoX, topPos + 125, autoWidth, 18,
                enchantmentCalculator$autoButtonLabel(),
                button -> enchantmentCalculator$checkReadiness()
        );
        autoCheck.setTooltip(Tooltip.create(autoCheck.getMessage()));
        autoCheck.active = !autoPending && result != null
                && completedSteps < result.getSteps().size();

        if (!enchantmentCalculator$pinFitsBelow()) {
            enchantmentCalculator$addPinButton(
                    panelX + 25, topPos + 145, panelWidth - 50, 18
            );
        }
    }

    @Unique
    private void enchantmentCalculator$buildSideReadinessWidgets() {
        int panelX = enchantmentCalculator$rightPanelX();
        int panelWidth = enchantmentCalculator$sidePanelWidth();

        if (readiness != null && !readiness.missingBooks().isEmpty()) {
            Button previousBook = enchantmentCalculator$addIconButton(
                    panelX + panelWidth - 28, topPos + 43, 20, 20,
                    Component.translatable("enchantmentcalculator.ui.scroll_up"),
                    AnvilTextures.UP_TEXTURE, AnvilTextures.UP_HOVER_TEXTURE, AnvilTextures.UP_DISABLED_TEXTURE,
                    button -> {
                        readinessScrollOffset--;
                        enchantmentCalculator$rebuildWidgets();
                    }
            );
            previousBook.active = readinessScrollOffset > 0;

            Button nextBook = enchantmentCalculator$addIconButton(
                    panelX + panelWidth - 28, topPos + 65, 20, 20,
                    Component.translatable("enchantmentcalculator.ui.scroll_down"),
                    AnvilTextures.DOWN_TEXTURE, AnvilTextures.DOWN_HOVER_TEXTURE, AnvilTextures.DOWN_DISABLED_TEXTURE,
                    button -> {
                        readinessScrollOffset++;
                        enchantmentCalculator$rebuildWidgets();
                    }
            );
            nextBook.active = readinessScrollOffset < readiness.missingBooks().size() - 1;
        }

        Button execute = enchantmentCalculator$addButton(
                panelX + 8, topPos + 125, panelWidth - 16, 18,
                Component.translatable("enchantmentcalculator.ui.start_auto"),
                button -> enchantmentCalculator$startAutoEnchant()
        );
        execute.active = !autoPending && readiness != null
                && readiness.ready() && readinessProblem == null;
        execute.setTooltip(Tooltip.create(execute.getMessage()));

        Button back = enchantmentCalculator$addButton(
                panelX + 8, topPos + 145, panelWidth - 16, 18,
                Component.translatable("enchantmentcalculator.ui.back_plan"),
                button -> enchantmentCalculator$show(Page.RESULT)
        );
        back.setTooltip(Tooltip.create(back.getMessage()));
    }

    @Unique
    private String enchantmentCalculator$itemId(ItemStack item) {
        return item.isEmpty() ? "" : BuiltInRegistries.ITEM.getKey(item.getItem()).toString();
    }

    @Unique
    private ItemStack enchantmentCalculator$presetCategoryItem() {
        ItemStack current = menu.getSlot(0).getItem();
        return current.isEmpty() && presetWorkspace && result != null ? planItem : current;
    }

    @Unique
    private void enchantmentCalculator$openPresetDialog() {
        if (autoPending || selectedEnchantments.isEmpty() || menu.getSlot(0).getItem().isEmpty()) {
            return;
        }
        presetDraft = "";
        presetSaveError = false;
        presetDialog = true;
        enchantmentCalculator$rebuildWidgets();
    }

    @Unique
    private void enchantmentCalculator$closePresetDialog() {
        presetDialog = false;
        presetName = null;
        setFocused(null);
        enchantmentCalculator$rebuildWidgets();
    }

    @Unique
    private void enchantmentCalculator$savePreset() {
        String title = presetDraft.trim();
        if (title.isEmpty() || selectedEnchantments.isEmpty()) {
            return;
        }
        Map<String, Integer> enchantments = new LinkedHashMap<>();
        for (Map.Entry<Enchantment, Integer> entry : selectedEnchantments.entrySet()) {
            String id = EnchantmentData.getId(entry.getKey());
            if (id == null) {
                return;
            }
            enchantments.put(id, entry.getValue());
        }
        PresetStore.Preset preset = PresetStore.save(title,
                enchantmentCalculator$itemId(menu.getSlot(0).getItem()),
                enchantments, optimizationMode.name());
        if (preset == null) {
            presetSaveError = true;
            return;
        }
        presetDialog = false;
        presetName = null;
        presetWorkspace = true;
        List<PresetStore.Preset> presets = PresetStore.forItem(preset.item());
        int index = presets.indexOf(preset);
        presetScrollOffset = Math.clamp(index, 0, Math.max(0, presets.size() - 4));
        setFocused(null);
        enchantmentCalculator$applyPreset(preset);
    }

    @Unique
    private void enchantmentCalculator$buildPresetDialog() {
        String itemId = enchantmentCalculator$itemId(menu.getSlot(0).getItem());
        presetName = AnvilPresetView.buildDialog(
                font, width, height, presetDraft,
                value -> PresetStore.forItem(itemId).stream()
                        .anyMatch(preset -> preset.name().equalsIgnoreCase(value)),
                value -> {
                    presetDraft = value;
                    presetSaveError = false;
                },
                this::enchantmentCalculator$closePresetDialog,
                this::enchantmentCalculator$savePreset,
                this::enchantmentCalculator$addButton,
                input -> {
                    calculatorWidgets.add(input);
                    addRenderableWidget(input);
                },
                presetDialogWidgets);
        setFocused(presetName);
        presetName.setFocused(true);
    }

    @Unique
    private void enchantmentCalculator$drawPresetDialog(
            GuiGraphics context, AnvilPanelRenderer painter) {
        AnvilPresetView.drawDialog(context, painter, width, height,
                menu.getSlot(0).getItem(), selectedEnchantments.size(),
                optimizationMode.getDisplayName(), presetSaveError);
    }

    @Unique
    private PresetStore.Preset enchantmentCalculator$selectedPreset(ItemStack item) {
        if (selectedPresetId == null || item.isEmpty()) {
            return null;
        }
        return PresetStore.forItem(enchantmentCalculator$itemId(item)).stream()
                .filter(preset -> preset.id().equals(selectedPresetId)).findFirst().orElse(null);
    }

    @Unique
    private void enchantmentCalculator$openPresets() {
        if (autoPending) {
            return;
        }
        presetWorkspace = true;
        presetScrollOffset = 0;
        presetMessage = null;
        enchantmentCalculator$show(Page.PRESETS);
    }

    @Unique
    private void enchantmentCalculator$leavePresets() {
        if (menu.getSlot(0).getItem().isEmpty()) {
            return;
        }
        presetWorkspace = false;
        selectedPresetId = null;
        deletePresetId = null;
        presetMessage = null;
        enchantmentCalculator$invalidateResult();
        enchantmentCalculator$show(Page.SELECTION);
    }

    @Unique
    private void enchantmentCalculator$scrollPresets(int direction) {
        int count = PresetStore.forItem(
                enchantmentCalculator$itemId(enchantmentCalculator$presetCategoryItem())).size();
        presetScrollOffset = Math.clamp(presetScrollOffset + direction, 0, Math.max(0, count - 4));
        enchantmentCalculator$rebuildWidgets();
    }

    @Unique
    private void enchantmentCalculator$applyPreset(PresetStore.Preset preset) {
        ItemStack item = menu.getSlot(0).getItem();
        if (item.isEmpty() || !preset.item().equals(enchantmentCalculator$itemId(item))) {
            return;
        }
        Map<Enchantment, Integer> restored = new LinkedHashMap<>();
        for (Map.Entry<String, Integer> entry : preset.enchantments().entrySet()) {
            Enchantment enchantment = EnchantmentData.byId(entry.getKey());
            int level = entry.getValue() == null ? 0 : entry.getValue();
            if (enchantment == null || !availableEnchantments.contains(enchantment)
                    || level < 1 || level > enchantment.getMaxLevel()) {
                enchantmentCalculator$failPreset("enchantmentcalculator.preset.unavailable");
                return;
            }
            EnchantmentInfo info = EnchantmentData.getEnchantmentInfo(enchantment);
            if (info == null || restored.keySet().stream()
                    .anyMatch(info.incompatibleEnchantments()::contains)) {
                enchantmentCalculator$failPreset("enchantmentcalculator.preset.unavailable");
                return;
            }
            restored.put(enchantment, level);
        }
        if (restored.isEmpty()) {
            enchantmentCalculator$failPreset("enchantmentcalculator.preset.unavailable");
            return;
        }
        selectedEnchantments.clear();
        selectedEnchantments.putAll(restored);
        optimizationMode = "EXPERIENCE".equals(preset.mode())
                ? OptimizationMode.EXPERIENCE : OptimizationMode.LEVELS;
        selectedPresetId = preset.id();
        deletePresetId = null;
        presetMessage = null;
        enchantmentCalculator$invalidateResult();
        enchantmentCalculator$calculate();
    }

    @Unique
    private void enchantmentCalculator$failPreset(String message) {
        selectedPresetId = null;
        selectedEnchantments.clear();
        enchantmentCalculator$invalidateResult();
        presetMessage = message;
        page = Page.PRESETS;
        enchantmentCalculator$rebuildWidgets();
    }

    @Unique
    private void enchantmentCalculator$deletePreset() {
        if (selectedPresetId == null || autoPending || menu.getSlot(0).getItem().isEmpty()) {
            return;
        }
        if (!selectedPresetId.equals(deletePresetId)) {
            deletePresetId = selectedPresetId;
            enchantmentCalculator$rebuildWidgets();
            return;
        }
        if (!PresetStore.delete(selectedPresetId)) {
            presetMessage = "enchantmentcalculator.preset.save_failed";
            enchantmentCalculator$rebuildWidgets();
            return;
        }
        selectedPresetId = null;
        deletePresetId = null;
        presetMessage = null;
        enchantmentCalculator$invalidateResult();
        page = Page.PRESETS;
        enchantmentCalculator$rebuildWidgets();
    }

    @Unique
    private void enchantmentCalculator$buildPresetList(int panelX, int panelWidth) {
        ItemStack item = enchantmentCalculator$presetCategoryItem();
        List<PresetStore.Preset> presets = PresetStore.forItem(enchantmentCalculator$itemId(item));
        presetScrollOffset = Math.min(presetScrollOffset, Math.max(0, presets.size() - 4));
        AnvilPresetView.buildList(this::enchantmentCalculator$addButton,
                new AnvilPresetView.Actions(
                        this::enchantmentCalculator$applyPreset,
                        this::enchantmentCalculator$scrollPresets,
                        this::enchantmentCalculator$leavePresets,
                        this::enchantmentCalculator$deletePreset,
                        () -> enchantmentCalculator$show(Page.RESULT)),
                presets, panelX, topPos, panelWidth, presetScrollOffset,
                selectedPresetId, deletePresetId, autoPending,
                !menu.getSlot(0).getItem().isEmpty(),
                page == Page.PRESETS && result != null);
    }

    @Unique
    private void enchantmentCalculator$buildCompactPresetWidgets() {
        enchantmentCalculator$buildPresetList(leftPos, imageWidth);
    }

    @Unique
    private void enchantmentCalculator$buildSidePresetWidgets() {
        enchantmentCalculator$buildPresetList(enchantmentCalculator$leftPanelX(),
                enchantmentCalculator$sidePanelWidth());
    }

    @Unique
    private void enchantmentCalculator$drawPresetContents(
            GuiGraphics context, AnvilPanelRenderer painter,
            int panelX, int panelY, int panelWidth) {
        ItemStack item = enchantmentCalculator$presetCategoryItem();
        AnvilPresetView.drawList(context, painter, item,
                PresetStore.forItem(enchantmentCalculator$itemId(item)), presetMessage,
                panelX, panelY, panelWidth, page == Page.PRESETS && result != null,
                !menu.getSlot(0).getItem().isEmpty());
    }

    @Unique
    private void enchantmentCalculator$drawCompactPresets(
            GuiGraphics context, AnvilPanelRenderer painter) {
        enchantmentCalculator$drawPresetContents(context, painter, 0, 0, imageWidth);
    }

    @Unique
    private void enchantmentCalculator$drawSidePresets(
            GuiGraphics context, AnvilPanelRenderer painter, int panelX, int panelWidth) {
        enchantmentCalculator$drawPresetContents(context, painter, panelX, topPos, panelWidth);
    }

    @Unique
    private void enchantmentCalculator$scroll(int direction) {
        int visibleRows = enchantmentCalculator$isCompact() ? 4 : 6;
        int maxOffset = Math.max(0, availableEnchantments.size() - visibleRows);
        scrollOffset = Math.clamp(scrollOffset + direction, 0, maxOffset);
        enchantmentCalculator$rebuildWidgets();
    }

    @Unique
    private boolean enchantmentCalculator$isCompatible(Enchantment candidate) {
        if (selectedEnchantments.containsKey(candidate)) {
            return true;
        }

        EnchantmentInfo info = EnchantmentData.getEnchantmentInfo(candidate);
        if (info == null) {
            return false;
        }
        for (Enchantment selected : selectedEnchantments.keySet()) {
            if (info.incompatibleEnchantments().contains(selected)) {
                return false;
            }
        }
        return true;
    }

    @Unique
    private void enchantmentCalculator$setLevel(Enchantment enchantment, int level) {
        boolean alreadySelecting = page == Page.SELECTION;
        if (level == 0) {
            selectedEnchantments.remove(enchantment);
        } else {
            selectedEnchantments.put(enchantment, level);
        }
        enchantmentCalculator$invalidateResult();
        if (alreadySelecting) {
            enchantmentCalculator$refreshSelectionControls();
        } else {
            setDragging(false);
            setFocused(null);
            enchantmentCalculator$rebuildWidgets();
        }
    }

    @Unique
    private void enchantmentCalculator$refreshSelectionControls() {
        for (EnchantmentLevelSlider slider : levelSliders) {
            slider.active = !autoPending
                    && enchantmentCalculator$isCompatible(slider.enchantment());
        }
        boolean hasSelection = !autoPending && !selectedEnchantments.isEmpty();
        if (calculateButton != null) {
            calculateButton.active = hasSelection;
        }
        if (resetButton != null) {
            resetButton.active = hasSelection;
        }
        if (savePresetButton != null) {
            savePresetButton.active = hasSelection;
        }
    }

    @Unique
    private void enchantmentCalculator$invalidateResult() {
        result = null;
        readiness = null;
        readinessProblem = null;
        errorMessage = null;
        automaticStatus = null;
        preservePlan = false;
        completedSteps = 0;
        autoPending = false;
        autoPhase = AutoPhase.IDLE;
        if (page == Page.RESULT || page == Page.READINESS) {
            page = Page.SELECTION;
        }
    }

    @Unique
    private void enchantmentCalculator$importBooks() {
        Minecraft client = Minecraft.getInstance();
        if (client.player == null || client.level == null) {
            return;
        }

        for (int slot = 0; slot < client.player.getInventory().getContainerSize(); slot++) {
            ItemStack stack = client.player.getInventory().getItem(slot);
            enchantmentCalculator$importBook(stack);
        }
        enchantmentCalculator$importBook(menu.getSlot(1).getItem());
        enchantmentCalculator$invalidateResult();
        enchantmentCalculator$rebuildWidgets();
    }

    @Unique
    private void enchantmentCalculator$importBook(ItemStack stack) {
        if (!stack.is(Items.ENCHANTED_BOOK)) {
            return;
        }

        ItemEnchantments enchantments = stack.getOrDefault(
                DataComponents.STORED_ENCHANTMENTS,
                ItemEnchantments.EMPTY
        );
        if (enchantments.size() != 1) {
            return;
        }

        for (Holder<Enchantment> entry : enchantments.keySet()) {
            Enchantment enchantment = entry.value();
            if (!availableEnchantments.contains(enchantment)) {
                continue;
            }
            if (!enchantmentCalculator$isCompatible(enchantment)) {
                continue;
            }

            int level = enchantments.getLevel(entry);
            int current = selectedEnchantments.getOrDefault(enchantment, 0);
            selectedEnchantments.put(enchantment, Math.max(level, current));
        }
    }

    @Unique
    private List<EnchantmentCombination> enchantmentCalculator$selections() {
        List<EnchantmentCombination> selections = new ArrayList<>();
        for (Map.Entry<Enchantment, Integer> entry : selectedEnchantments.entrySet()) {
            selections.add(new EnchantmentCombination(entry.getKey(), entry.getValue()));
        }
        return selections;
    }

    @Unique
    private void enchantmentCalculator$calculate() {
        try {
            ItemStack target = menu.getSlot(0).getItem();
            result = EnchantmentCalculator.calculate(
                    target,
                    enchantmentCalculator$selections(),
                    optimizationMode
            );
            planItem = target.copy();
            preservePlan = true;
            stepIndex = 0;
            completedSteps = 0;
            autoPending = false;
            autoPhase = AutoPhase.IDLE;
            errorMessage = null;
            automaticStatus = null;
            enchantmentCalculator$show(Page.RESULT);
        } catch (IllegalArgumentException exception) {
            errorMessage = exception.getMessage();
            if (presetWorkspace) {
                presetMessage = errorMessage;
                page = Page.PRESETS;
                enchantmentCalculator$rebuildWidgets();
            }
            EnchantmentCalculatorMod.LOGGER.warn("Cannot calculate anvil path: {}", errorMessage);
        }
    }

    @Unique
    private Component enchantmentCalculator$autoButtonLabel() {
        if (result != null && completedSteps >= result.getSteps().size()) {
            return Component.translatable("enchantmentcalculator.ui.auto_complete");
        }
        return Component.translatable(completedSteps == 0
                ? "enchantmentcalculator.ui.auto_enchant"
                : "enchantmentcalculator.ui.resume_auto");
    }

    @Unique
    private void enchantmentCalculator$checkReadiness() {
        if (result == null || autoPending || completedSteps >= result.getSteps().size()) {
            return;
        }
        stepIndex = completedSteps;
        automaticStatus = null;
        enchantmentCalculator$updateReadiness();
        enchantmentCalculator$show(Page.READINESS);
    }

    @Unique
    private void enchantmentCalculator$updateReadiness() {
        Minecraft client = Minecraft.getInstance();
        if (client.player == null || result == null || stepIndex >= result.getSteps().size()) {
            return;
        }

        InventoryReadiness.Report updated = InventoryReadiness.check(
                client.player,
                menu,
                enchantmentCalculator$selections(),
                result,
                stepIndex
        );
        String problem = null;
        if (updated.ready()) {
            AutomaticAnvil.Preparation preflight = AutomaticAnvil.check(
                    client,
                    menu,
                    planItem,
                    enchantmentCalculator$selections(),
                    result.getSteps().get(stepIndex)
            );
            problem = preflight.problem();
        }

        if (!updated.equals(readiness) || !java.util.Objects.equals(problem, readinessProblem)) {
            readiness = updated;
            readinessProblem = problem;
            readinessScrollOffset = Math.min(
                    readinessScrollOffset,
                    Math.max(0, updated.missingBooks().size() - 1)
            );
            if (page == Page.READINESS) {
                enchantmentCalculator$rebuildWidgets();
            }
        }
    }

    @Unique
    private void enchantmentCalculator$startAutoEnchant() {
        Minecraft client = Minecraft.getInstance();
        if (result == null || autoPending || completedSteps >= result.getSteps().size()) {
            return;
        }
        if (client.player == null || client.gameMode == null) {
            return;
        }

        // проверяем предметы перед первым кликом
        stepIndex = completedSteps;
        enchantmentCalculator$updateReadiness();
        if (readiness == null || !readiness.ready() || readinessProblem != null) {
            enchantmentCalculator$show(Page.READINESS);
            return;
        }

        autoPending = true;
        preservePlan = true;
        enchantmentCalculator$prepareAutomaticStep();
    }

    @Unique
    private void enchantmentCalculator$prepareAutomaticStep() {
        Minecraft client = Minecraft.getInstance();
        if (client.player == null || client.gameMode == null || result == null
                || completedSteps >= result.getSteps().size()) {
            enchantmentCalculator$stopAutomaticRun(
                    Component.translatable("enchantmentcalculator.auto.player_unavailable")
            );
            return;
        }

        stepIndex = completedSteps;
        CalculationResult.Step step = result.getSteps().get(stepIndex);
        enchantmentCalculator$savePinnedPlan();
        autoPhase = AutoPhase.PREPARING;
        pendingTicks = 0;
        AuthoritativeAnvilState sync = enchantmentCalculator$serverSync();
        outputRevisionBeforePreparation = sync.enchantmentCalculator$outputRevision();
        costRevisionBeforePreparation = sync.enchantmentCalculator$costRevision();
        AutomaticAnvil.Preparation preparation = AutomaticAnvil.prepare(
                client,
                menu,
                planItem,
                enchantmentCalculator$selections(),
                step
        );
        if (!preparation.started()) {
            enchantmentCalculator$stopAutomaticRun(
                    Component.translatable(preparation.problem())
            );
            return;
        }
        preparationClicks = preparation.clicks();
        preparationClickIndex = 0;
        preparationMismatchTicks = 0;
        expectedSlotsAfterClick.clear();

        automaticStatus = Component.translatable(
                "enchantmentcalculator.auto.progress",
                stepIndex + 1,
                result.getSteps().size()
        );
        enchantmentCalculator$show(Page.RESULT);
    }

    @Unique
    private void enchantmentCalculator$advancePreparation() {
        Minecraft client = Minecraft.getInstance();
        if (client.player == null || client.gameMode == null) {
            enchantmentCalculator$stopAutomaticRun(
                    Component.translatable("enchantmentcalculator.auto.player_unavailable")
            );
            return;
        }
        if (preparationClickIndex > 0) {
            int mismatchSlot = enchantmentCalculator$preparationMismatchSlot();
            if (mismatchSlot != Integer.MIN_VALUE) {
                // ждём обновление после прошлого клика
                if (++preparationMismatchTicks < enchantmentCalculator$settleTicks(client)) {
                    return;
                }
                EnchantmentCalculatorMod.LOGGER.warn(
                        "Auto anvil input mismatch at step {} click {}/{} slot {} after {} ticks",
                        stepIndex + 1, preparationClickIndex, preparationClicks.size(),
                        mismatchSlot, preparationMismatchTicks
                );
                enchantmentCalculator$stopAutomaticRun(
                        Component.translatable("enchantmentcalculator.auto.input_rejected")
                );
                return;
            }
            preparationMismatchTicks = 0;
        }
        if (preparationClickIndex >= preparationClicks.size()) {
            autoPhase = AutoPhase.WAIT_RESULT;
            pendingTicks = 0;
            return;
        }

        AutomaticAnvil.Click click = preparationClicks.get(preparationClickIndex++);
        client.gameMode.handleInventoryMouseClick(
                menu.containerId, click.slot(), 0, click.type(), client.player
        );
        expectedCarriedAfterClick = menu.getCarried().copy();
        expectedSlotsAfterClick.put(click.slot(), menu.getSlot(click.slot()).getItem().copy());
        // по одному клику за тик
    }

    @Unique
    private int enchantmentCalculator$preparationMismatchSlot() {
        if (!ItemStack.matches(menu.getCarried(), expectedCarriedAfterClick)) {
            return -1;
        }
        for (Map.Entry<Integer, ItemStack> entry : expectedSlotsAfterClick.entrySet()) {
            if (!ItemStack.matches(menu.getSlot(entry.getKey()).getItem(), entry.getValue())) {
                return entry.getKey();
            }
        }
        return Integer.MIN_VALUE;
    }

    @Unique
    private int enchantmentCalculator$settleTicks(Minecraft client) {
        int latency = 0;
        if (client.player != null && client.getConnection() != null
                && client.getConnection().getPlayerInfo(client.player.getUUID()) != null) {
            latency = Math.max(0, client.getConnection()
                    .getPlayerInfo(client.player.getUUID()).getLatency());
        }
        // ждём поздние обновления инвентаря
        return Math.clamp((latency + 49) / 50 + 4, 4, 40);
    }

    @Unique
    private void enchantmentCalculator$takeAutomaticResult() {
        Minecraft client = Minecraft.getInstance();
        if (client.player == null || client.gameMode == null || result == null) {
            enchantmentCalculator$stopAutomaticRun(
                    Component.translatable("enchantmentcalculator.auto.player_unavailable")
            );
            return;
        }

        CalculationResult.Step step = result.getSteps().get(stepIndex);
        AuthoritativeAnvilState sync = enchantmentCalculator$serverSync();
        // цену сервер присылает даже без обновления слота
        if (sync.enchantmentCalculator$costRevision() <= costRevisionBeforePreparation) {
            return;
        }

        int serverCost = sync.enchantmentCalculator$serverCost();
        if (serverCost <= 0) {
            return;
        }
        if (serverCost != step.getLevels()) {
            enchantmentCalculator$stopAutomaticRun(Component.translatable(
                    "enchantmentcalculator.auto.server_cost", serverCost
            ));
            return;
        }
        if ((sync.enchantmentCalculator$outputRevision() > outputRevisionBeforePreparation
                && !AutomaticAnvil.matchesPlannedResult(
                        sync.enchantmentCalculator$serverOutput(),
                        step,
                        planItem,
                        enchantmentCalculator$selections()
                )) || !AutomaticAnvil.matchesPlannedResult(
                menu.getSlot(2).getItem(),
                step,
                planItem,
                enchantmentCalculator$selections()
        )) {
            enchantmentCalculator$stopAutomaticRun(
                    Component.translatable("enchantmentcalculator.auto.server_result")
            );
            return;
        }
        if (client.player.experienceLevel < serverCost) {
            enchantmentCalculator$stopAutomaticRun(
                    Component.translatable("enchantmentcalculator.auto.server_levels")
            );
            return;
        }

        costRevisionBeforePickup = sync.enchantmentCalculator$costRevision();
        experienceRevisionBeforePickup = sync.enchantmentCalculator$experienceRevision();
        levelsBeforePickup = client.player.experienceLevel;
        outputRevisionBeforePickup = sync.enchantmentCalculator$outputRevision();
        autoPhase = AutoPhase.WAIT_COLLECTION;
        pendingTicks = 0;
        collectionStableTicks = 0;
        client.gameMode.handleInventoryMouseClick(
                menu.containerId,
                2,
                0,
                ClickType.PICKUP,
                client.player
        );
    }

    @Unique
    private void enchantmentCalculator$continueAutomaticRun() {
        if (result == null) {
            enchantmentCalculator$stopAutomaticRun(
                    Component.translatable("enchantmentcalculator.auto.server_result")
            );
            return;
        }

        CalculationResult.Step step = result.getSteps().get(stepIndex);
        AuthoritativeAnvilState sync = enchantmentCalculator$serverSync();
        if (sync.enchantmentCalculator$outputRevision() > outputRevisionBeforePickup
                && !sync.enchantmentCalculator$serverOutput().isEmpty()) {
            enchantmentCalculator$stopAutomaticRun(
                    Component.translatable("enchantmentcalculator.auto.collection_rejected")
            );
            return;
        }

        Minecraft client = Minecraft.getInstance();
        if (client.player == null || client.gameMode == null) {
            enchantmentCalculator$stopAutomaticRun(
                    Component.translatable("enchantmentcalculator.auto.player_unavailable")
            );
            return;
        }
        boolean experienceDeducted = sync.enchantmentCalculator$experienceRevision()
                > experienceRevisionBeforePickup
                && sync.enchantmentCalculator$serverExperienceLevel()
                <= levelsBeforePickup - step.getLevels();
        boolean costCleared = sync.enchantmentCalculator$costRevision()
                > costRevisionBeforePickup && sync.enchantmentCalculator$serverCost() == 0;
        boolean serverCompletedTake = experienceDeducted
                || client.player.hasInfiniteMaterials() && costCleared;
        if (!serverCompletedTake) {
            return;
        }
        if (!menu.getSlot(2).getItem().isEmpty()
                || !AutomaticAnvil.matchesPlannedResult(
                        menu.getCarried(), step, planItem,
                        enchantmentCalculator$selections())) {
            enchantmentCalculator$stopAutomaticRun(
                    Component.translatable("enchantmentcalculator.auto.server_result")
            );
            return;
        }

        resultInventorySlot = -1;
        for (int slot = 3; slot < menu.slots.size(); slot++) {
            if (menu.getSlot(slot).getItem().isEmpty()) {
                resultInventorySlot = slot;
                break;
            }
        }
        if (resultInventorySlot < 0) {
            enchantmentCalculator$stopAutomaticRun(
                    Component.translatable("enchantmentcalculator.auto.free_slot")
            );
            return;
        }

        client.gameMode.handleInventoryMouseClick(
                menu.containerId, resultInventorySlot, 0, ClickType.PICKUP, client.player
        );
        autoPhase = AutoPhase.WAIT_STOW;
        pendingTicks = 0;
        collectionStableTicks = 0;
    }

    @Unique
    private void enchantmentCalculator$finishStowingResult() {
        if (result == null) {
            enchantmentCalculator$stopAutomaticRun(
                    Component.translatable("enchantmentcalculator.auto.server_result")
            );
            return;
        }
        CalculationResult.Step step = result.getSteps().get(stepIndex);
        boolean stowed = menu.getCarried().isEmpty()
                && AutomaticAnvil.matchesPlannedResult(
                        menu.getSlot(resultInventorySlot).getItem(), step, planItem,
                        enchantmentCalculator$selections());
        collectionStableTicks = stowed ? collectionStableTicks + 1 : 0;
        Minecraft client = Minecraft.getInstance();
        if (collectionStableTicks < enchantmentCalculator$settleTicks(client)) {
            return;
        }

        completedSteps++;
        enchantmentCalculator$savePinnedPlan();
        if (completedSteps >= result.getSteps().size()) {
            autoPending = false;
            autoPhase = AutoPhase.IDLE;
            automaticStatus = Component.translatable("enchantmentcalculator.auto.finished");
            enchantmentCalculator$show(Page.RESULT);
            return;
        }

        enchantmentCalculator$prepareAutomaticStep();
    }

    @Unique
    private AuthoritativeAnvilState enchantmentCalculator$serverSync() {
        return (AuthoritativeAnvilState) menu;
    }

    @Unique
    private void enchantmentCalculator$stopAutomaticRun(Component reason) {
        autoPending = false;
        autoPhase = AutoPhase.IDLE;
        automaticStatus = reason;
        enchantmentCalculator$show(Page.RESULT);
    }

    @Unique
    private void enchantmentCalculator$drawSideSelection(
            GuiGraphics context,
            int panelX,
            int panelWidth,
            int mouseX,
            int mouseY
    ) {
        if (errorMessage != null && calculateButton != null) {
            calculateButton.setTooltip(Tooltip.create(Component.translatable(errorMessage)));
        }

    }

}
