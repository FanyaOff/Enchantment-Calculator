package com.fanya.enchantmentcalculator.client.mixin;

import com.fanya.enchantmentcalculator.EnchantmentCalculatorMod;
import com.fanya.enchantmentcalculator.calculator.CalculationResult;
import com.fanya.enchantmentcalculator.calculator.EnchantmentCalculator;
import com.fanya.enchantmentcalculator.calculator.EnchantmentCombination;
import com.fanya.enchantmentcalculator.calculator.OptimizationMode;
import com.fanya.enchantmentcalculator.client.gui.CustomButtonHelper;
import com.fanya.enchantmentcalculator.client.gui.EnchantmentButton;
import com.fanya.enchantmentcalculator.data.EnchantmentData;
import com.fanya.enchantmentcalculator.data.EnchantmentInfo;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.AnvilScreen;
import net.minecraft.client.gui.screen.ingame.ForgingScreen;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.AnvilScreenHandler;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.*;


@Mixin(AnvilScreen.class)
public abstract class AnvilScreenMixin extends ForgingScreen<AnvilScreenHandler> {

    @Unique
    private static int LEFT_PANEL_DISTANCE = 21;
    @Unique
    private static int RIGHT_PANEL_DISTANCE = 0;


    @Unique
    private static CalculationResult pinnedResult = null;
    @Unique
    private static Map<Enchantment, Integer> pinnedEnchantments = new HashMap<>();
    @Unique
    private static OptimizationMode pinnedOptimizationMode = OptimizationMode.LEVELS;
    @Unique
    private static ItemStack pinnedItem = ItemStack.EMPTY;
    @Unique
    private static boolean isPinned = false;

    @Unique
    private final List<EnchantmentButton> enchantmentButtons = new ArrayList<>();
    @Unique
    private final Map<Enchantment, Integer> selectedEnchantments = new HashMap<>();

    @Unique
    private ButtonWidget calculateButton;
    @Unique
    private ButtonWidget scrollUpButton;
    @Unique
    private ButtonWidget scrollDownButton;
    @Unique
    private ButtonWidget stepPrevButton;
    @Unique
    private ButtonWidget stepNextButton;
    @Unique
    private ButtonWidget pinButton;
    @Unique
    private ButtonWidget resetButton;

    @Unique
    private OptimizationMode optimizationMode = OptimizationMode.LEVELS;
    @Unique
    private CalculationResult lastResult;
    @Unique
    private int enchantmentScrollOffset = 0;
    @Unique
    private int currentStepIndex = 0;
    @Unique
    private boolean leftPanelVisible = false;
    @Unique
    private boolean rightPanelVisible = false;
    @Unique
    private ItemStack lastItem = ItemStack.EMPTY;
    @Unique
    private List<Enchantment> availableEnchantments = new ArrayList<>();

    public AnvilScreenMixin(AnvilScreenHandler handler, PlayerInventory playerInventory, Text title) {
        super(handler, playerInventory, title, null);
    }

    @Inject(method = "setup", at = @At("TAIL"))
    private void setupCalculatorPanel(CallbackInfo ci) {
        if (isPinned && pinnedResult != null) {
            lastResult = pinnedResult;
            currentStepIndex = 0;
            rightPanelVisible = true;
            leftPanelVisible = false;
            enchantmentCalculator$setupStepNavigation();
            enchantmentCalculator$setupPinButton();
            return;
        }

        enchantmentCalculator$updatePanelVisibility();
        if (leftPanelVisible) {
            enchantmentCalculator$setupLeftPanel();
        }
    }

    @Inject(method = "onSlotUpdate", at = @At("TAIL"))
    private void updatePanelOnSlotChange(ScreenHandler handler, int slotId, ItemStack stack, CallbackInfo ci) {
        if (isPinned) return;

        if (slotId == 0) {
            ItemStack currentItem = this.handler.getSlot(0).getStack();
            if (!ItemStack.areEqual(currentItem, lastItem)) {
                lastItem = currentItem.copy();
                enchantmentCalculator$clearInterface();
                enchantmentCalculator$updatePanelVisibility();
                if (leftPanelVisible) {
                    enchantmentCalculator$setupLeftPanel();
                }
            }
        }
    }

    @Inject(method = "drawBackground", at = @At("TAIL"))
    private void renderCalculatorPanels(DrawContext context, float delta, int mouseX, int mouseY, CallbackInfo ci) {
        if (leftPanelVisible && !isPinned) {
            enchantmentCalculator$renderLeftPanel(context);
        }
        if (rightPanelVisible) {
            enchantmentCalculator$renderRightPanel(context);
        }
    }

    @Unique
    private void enchantmentCalculator$setupLeftPanel() {
        ItemStack stack = this.handler.getSlot(0).getStack();
        if (stack.isEmpty()) return;

        availableEnchantments = EnchantmentData.getApplicableEnchantments(stack);

        int leftPanelX = this.x - this.backgroundWidth - LEFT_PANEL_DISTANCE;
        int leftPanelY = this.y;
        int startY = leftPanelY + 10;

        scrollUpButton = CustomButtonHelper.createButton(
                leftPanelX + 165, startY, 20, 20,
                CustomButtonHelper.ButtonType.UP,
                button -> {
                    enchantmentScrollOffset = Math.max(0, enchantmentScrollOffset - 1);
                    enchantmentCalculator$updateEnchantmentButtons();
                }
        );
        this.addDrawableChild(scrollUpButton);

        scrollDownButton = CustomButtonHelper.createButton(
                leftPanelX + 165, startY + 20 + 2, 20, 20,
                CustomButtonHelper.ButtonType.DOWN,
                button -> {
                    int maxScroll = Math.max(0, availableEnchantments.size() - 6);
                    enchantmentScrollOffset = Math.min(maxScroll, enchantmentScrollOffset + 1);
                    enchantmentCalculator$updateEnchantmentButtons();
                }
        );
        this.addDrawableChild(scrollDownButton);

        calculateButton = CustomButtonHelper.createButton(
                leftPanelX + 165, startY + 44, 20, 20,
                CustomButtonHelper.ButtonType.CALCULATE,
                button -> enchantmentCalculator$calculate()
        );
        this.addDrawableChild(calculateButton);

        resetButton = CustomButtonHelper.createButton(
                leftPanelX + 165, startY + 66, 20, 20,
                CustomButtonHelper.ButtonType.RESET,
                button -> enchantmentCalculator$resetToDefault()
        );
        this.addDrawableChild(resetButton);

        enchantmentCalculator$updateEnchantmentButtons();
        enchantmentCalculator$updateCalculateButton();
        enchantmentCalculator$updateResetButton();
    }

    @Unique
    private void enchantmentCalculator$setupStepNavigation() {
        if (lastResult == null || lastResult.getSteps().isEmpty()) return;

        int rightPanelX = this.x + this.backgroundWidth + RIGHT_PANEL_DISTANCE;
        int rightPanelY = this.y + 166 - 30;

        stepPrevButton = CustomButtonHelper.createButton(
                rightPanelX + 10, rightPanelY, 12, 17,
                CustomButtonHelper.ButtonType.BACKWARD,
                button -> {
                    if (currentStepIndex > 0) {
                        currentStepIndex--;
                    }
                }
        );
        this.addDrawableChild(stepPrevButton);

        stepNextButton = CustomButtonHelper.createButton(
                rightPanelX + 200 - 22, rightPanelY, 12, 17,
                CustomButtonHelper.ButtonType.FORWARD,
                button -> {
                    if (lastResult != null && currentStepIndex < lastResult.getSteps().size() - 1) {
                        currentStepIndex++;
                    }
                }
        );
        this.addDrawableChild(stepNextButton);
    }

    @Unique
    private void enchantmentCalculator$setupPinButton() {
        if (pinButton != null) {
            this.remove(pinButton);
            pinButton = null;
        }

        if ((lastResult != null && !lastResult.getSteps().isEmpty()) || isPinned) {
            Text buttonText = isPinned ?
                    Text.translatable("enchantmentcalculator.ui.unpin_result") :
                    Text.translatable("enchantmentcalculator.ui.pin_result");

            pinButton = ButtonWidget.builder(
                            buttonText,
                            button -> enchantmentCalculator$togglePin()
                    )
                    .dimensions(this.x, this.y + this.backgroundHeight + 5, this.backgroundWidth, 20)
                    .build();
            this.addDrawableChild(pinButton);
        }
    }

    @Unique
    private void enchantmentCalculator$renderLeftPanel(DrawContext context) {
        int leftPanelX = this.x - this.backgroundWidth - LEFT_PANEL_DISTANCE;
        int leftPanelY = this.y;

        CustomButtonHelper.drawPanelTexture(context, leftPanelX, leftPanelY, 220, 166);

        if (availableEnchantments.size() > 6) {
            String scrollText = "(" + (enchantmentScrollOffset + 1) + "-" +
                    Math.min(enchantmentScrollOffset + 6, availableEnchantments.size()) +
                    "/" + availableEnchantments.size() + ")";
            context.drawTextWithShadow(this.textRenderer, Text.literal(scrollText),
                    leftPanelX + 10, leftPanelY + 166 - 15, 0xFF888888);
        }
    }

    @Unique
    private void enchantmentCalculator$renderRightPanel(DrawContext context) {
        int rightPanelX = this.x + this.backgroundWidth + RIGHT_PANEL_DISTANCE;
        int rightPanelY = this.y;

        CustomButtonHelper.drawPanelTexture(context, rightPanelX, rightPanelY, 200, 166);

        if (lastResult != null) {
            context.fill(rightPanelX + 20, rightPanelY + 5, rightPanelX + 180, rightPanelY + 18, 0x00000000);

            Text costText = Text.translatable("enchantmentcalculator.ui.levels", lastResult.getTotalLevels())
                    .formatted(Formatting.BOLD, Formatting.YELLOW);
            context.drawCenteredTextWithShadow(this.textRenderer, costText,
                    rightPanelX + 100, rightPanelY + 8, 0xFFFFFFFF);

            if (!lastResult.getSteps().isEmpty() && currentStepIndex >= 0 && currentStepIndex < lastResult.getSteps().size()) {
                CalculationResult.Step currentStep = lastResult.getSteps().get(currentStepIndex);

                context.fill(rightPanelX + 20, rightPanelY + 20, rightPanelX + 180, rightPanelY + 33, 0x00000000);

                Text stepCostText = Text.translatable("enchantmentcalculator.ui.cost", currentStep.getLevels());
                context.drawCenteredTextWithShadow(this.textRenderer, stepCostText,
                        rightPanelX + 100, rightPanelY + 23, 0xFFFFFFFF);
            }
        }

        enchantmentCalculator$renderStepsAreaWithBackground(context, rightPanelX + 10, rightPanelY + 35);

        enchantmentCalculator$renderStepNavigationWithBackground(context, rightPanelX, rightPanelY + 166 - 20);
    }

    @Unique
    private void enchantmentCalculator$renderStepsAreaWithBackground(DrawContext context, int x, int y) {
        if (lastResult == null || lastResult.getSteps().isEmpty()) return;

        if (currentStepIndex < 0 || currentStepIndex >= lastResult.getSteps().size()) {
            currentStepIndex = 0;
        }

        context.fill(x, y, x + 180, y + 96, 0xFF8B8B8B);
        context.fill(x, y, x + 180 - 1, y + 1, 0xFF555555);
        context.fill(x, y, x + 1, y + 96 - 1, 0xFF555555);
        context.fill(x + 1, y + 96 - 1, x + 180, y + 96, 0xFFFFFFFF);
        context.fill(x + 180 - 1, y + 1, x + 180, y + 96 - 1, 0xFFFFFFFF);

        CalculationResult.Step currentStep = lastResult.getSteps().get(currentStepIndex);
        String description = currentStep.getDescription();
        List<String> wrappedLines = enchantmentCalculator$wrapText(description);

        int textY = y + 10;

        for (String line : wrappedLines) {
            int lineWidth = this.textRenderer.getWidth(Text.literal(line));
            context.fill(x + 8, textY - 2, x + 12 + lineWidth, textY + 10, 0x00000000);

            context.drawTextWithShadow(this.textRenderer, Text.literal(line),
                    x + 10, textY, 0xFFFFFFFF);
            textY += 12;
        }
    }

    @Unique
    private void enchantmentCalculator$renderStepNavigationWithBackground(DrawContext context, int x, int y) {
        if (lastResult == null || lastResult.getSteps().isEmpty()) return;

        int totalSteps = lastResult.getSteps().size();
        Text stepIndicator = Text.translatable("enchantmentcalculator.ui.step",
                currentStepIndex + 1, totalSteps);
        int leftArrowEnd = x + 10 + 12;
        int rightArrowStart = x + 200 - 22;
        int centerBetweenArrows = (leftArrowEnd + rightArrowStart) / 2;

        int textWidth = this.textRenderer.getWidth(stepIndicator);
        context.fill(centerBetweenArrows - textWidth/2 - 5, y - 8,
                centerBetweenArrows + textWidth/2 + 5, y + 5, 0x00000000);

        context.drawCenteredTextWithShadow(this.textRenderer, stepIndicator,
                centerBetweenArrows, y - 5, 0xFFFFFFFF);

        if (stepPrevButton != null) {
            stepPrevButton.active = currentStepIndex > 0;
        }
        if (stepNextButton != null) {
            stepNextButton.active = currentStepIndex < totalSteps - 1;
        }
    }


    @Unique
    private void enchantmentCalculator$renderStepsArea(DrawContext context, int x, int y) {
        if (lastResult == null || lastResult.getSteps().isEmpty()) return;

        if (currentStepIndex < 0 || currentStepIndex >= lastResult.getSteps().size()) {
            currentStepIndex = 0;
        }

        context.fill(x, y, x + 180, y + 96, 0xFF8B8B8B);
        context.fill(x, y, x + 180 - 1, y + 1, 0xFF555555);
        context.fill(x, y, x + 1, y + 96 - 1, 0xFF555555);
        context.fill(x + 1, y + 96 - 1, x + 180, y + 96, 0xFFFFFFFF);
        context.fill(x + 180 - 1, y + 1, x + 180, y + 96 - 1, 0xFFFFFFFF);

        CalculationResult.Step currentStep = lastResult.getSteps().get(currentStepIndex);
        int textY = y + 10;

        String description = currentStep.getDescription();
        List<String> wrappedLines = enchantmentCalculator$wrapText(description);

        for (String line : wrappedLines) {
            context.drawTextWithShadow(this.textRenderer, Text.literal(line),
                    x + 10, textY, 0xFFFFFF);
            textY += 12;
        }
    }

    @Unique
    private void enchantmentCalculator$updateEnchantmentButtons() {
        enchantmentButtons.forEach(this::remove);
        enchantmentButtons.clear();

        int leftPanelX = this.x - this.backgroundWidth - LEFT_PANEL_DISTANCE;
        int currentY = this.y + 10;

        int visibleCount = 0;
        for (int i = enchantmentScrollOffset; i < availableEnchantments.size() && visibleCount < 6; i++) {
            Enchantment enchantment = availableEnchantments.get(i);
            EnchantmentInfo info = EnchantmentData.getEnchantmentInfo(enchantment);
            if (info != null) {
                EnchantmentButton button = new EnchantmentButton(
                        leftPanelX + 10, currentY, 150, 20,
                        enchantment, info.maxLevel(),
                        this::enchantmentCalculator$onEnchantmentChanged
                );

                if (selectedEnchantments.containsKey(enchantment)) {
                    button.setLevel(selectedEnchantments.get(enchantment));
                }

                enchantmentButtons.add(button);
                this.addDrawableChild(button);
                currentY += 22;
                visibleCount++;
            }
        }

        enchantmentCalculator$updateEnchantmentCompatibility();

        if (scrollUpButton != null) {
            scrollUpButton.active = enchantmentScrollOffset > 0;
        }
        if (scrollDownButton != null) {
            scrollDownButton.active = enchantmentScrollOffset < availableEnchantments.size() - 6;
        }
    }

    @Unique
    private void enchantmentCalculator$clearLeftPanelInterface() {
        enchantmentButtons.forEach(this::remove);
        enchantmentButtons.clear();

        if (calculateButton != null) {
            this.remove(calculateButton);
            calculateButton = null;
        }
        if (scrollUpButton != null) {
            this.remove(scrollUpButton);
            scrollUpButton = null;
        }
        if (scrollDownButton != null) {
            this.remove(scrollDownButton);
            scrollDownButton = null;
        }
        if (resetButton != null) {
            this.remove(resetButton);
            resetButton = null;
        }
    }

    @Unique
    private void enchantmentCalculator$clearInterface() {
        enchantmentCalculator$clearLeftPanelInterface();

        if (!isPinned) {
            if (stepPrevButton != null) {
                this.remove(stepPrevButton);
                stepPrevButton = null;
            }
            if (stepNextButton != null) {
                this.remove(stepNextButton);
                stepNextButton = null;
            }
            enchantmentCalculator$clearResults();
            rightPanelVisible = false;
        }

        if (pinButton != null) {
            this.remove(pinButton);
            pinButton = null;
        }

        if (!isPinned) {
            selectedEnchantments.clear();
        }
    }

    @Unique
    private void enchantmentCalculator$updateCalculateButton() {
        if (calculateButton != null) {
            calculateButton.active = !selectedEnchantments.isEmpty();
        }
    }

    @Unique
    private void enchantmentCalculator$updateResetButton() {
        if (resetButton != null) {
            resetButton.active = !selectedEnchantments.isEmpty() || (lastResult != null);
        }
    }

    @Unique
    private void enchantmentCalculator$calculate() {
        if (selectedEnchantments.isEmpty()) return;

        List<EnchantmentCombination> combinations = new ArrayList<>();
        for (Map.Entry<Enchantment, Integer> entry : selectedEnchantments.entrySet()) {
            combinations.add(new EnchantmentCombination(entry.getKey(), entry.getValue()));
        }

        try {
            lastResult = EnchantmentCalculator.calculate(lastItem, combinations, optimizationMode);
            currentStepIndex = 0;
            rightPanelVisible = true;

            if (stepPrevButton != null) {
                this.remove(stepPrevButton);
                stepPrevButton = null;
            }
            if (stepNextButton != null) {
                this.remove(stepNextButton);
                stepNextButton = null;
            }

            enchantmentCalculator$setupStepNavigation();
            enchantmentCalculator$setupPinButton();
        } catch (Exception e) {
            EnchantmentCalculatorMod.LOGGER.error(e.toString());
        }
    }

    @Unique
    private void enchantmentCalculator$resetToDefault() {
        selectedEnchantments.clear();
        optimizationMode = OptimizationMode.LEVELS;

        if (lastResult != null) {
            enchantmentCalculator$clearResults();
            rightPanelVisible = false;
        }

        if (stepPrevButton != null) {
            this.remove(stepPrevButton);
            stepPrevButton = null;
        }
        if (stepNextButton != null) {
            this.remove(stepNextButton);
            stepNextButton = null;
        }
        if (pinButton != null) {
            this.remove(pinButton);
            pinButton = null;
        }

        if (isPinned) {
            isPinned = false;
            pinnedResult = null;
            pinnedEnchantments.clear();
            pinnedItem = ItemStack.EMPTY;
        }

        enchantmentScrollOffset = 0;
        currentStepIndex = 0;

        enchantmentCalculator$updateEnchantmentButtons();
        enchantmentCalculator$updateCalculateButton();
        enchantmentCalculator$updateResetButton();

        leftPanelVisible = true;
    }

    @Unique
    private void enchantmentCalculator$togglePin() {
        if (isPinned) {
            isPinned = false;
            selectedEnchantments.clear();
            selectedEnchantments.putAll(pinnedEnchantments);
            optimizationMode = pinnedOptimizationMode;
            lastItem = pinnedItem.copy();

            leftPanelVisible = true;
            rightPanelVisible = false;

            enchantmentCalculator$clearInterface();
            enchantmentCalculator$updatePanelVisibility();
            if (leftPanelVisible) {
                enchantmentCalculator$setupLeftPanel();
            }
            enchantmentCalculator$setupPinButton();

            pinnedResult = null;
            pinnedEnchantments.clear();
            pinnedItem = ItemStack.EMPTY;
        } else {
            if (lastResult != null) {
                isPinned = true;

                pinnedResult = lastResult;
                pinnedEnchantments.clear();
                pinnedEnchantments.putAll(selectedEnchantments);
                pinnedOptimizationMode = optimizationMode;
                pinnedItem = lastItem.copy();

                leftPanelVisible = false;
                rightPanelVisible = true;

                enchantmentCalculator$clearLeftPanelInterface();
                enchantmentCalculator$setupPinButton();
            }
        }

        this.remove(pinButton);
        enchantmentCalculator$setupPinButton();
    }

    @Unique
    private void enchantmentCalculator$onEnchantmentChanged(Enchantment enchantment, int level) {
        if (isPinned) return;

        if (level > 0) {
            selectedEnchantments.put(enchantment, level);
            EnchantmentInfo info = EnchantmentData.getEnchantmentInfo(enchantment);
            if (info != null) {
                for (Enchantment incompatible : info.incompatibleEnchantments()) {
                    for (EnchantmentButton button : enchantmentButtons) {
                        if (button.getEnchantment().equals(incompatible)) {
                            button.setEnabled(false);
                        }
                    }
                }
            }
        } else {
            selectedEnchantments.remove(enchantment);
            enchantmentCalculator$updateEnchantmentCompatibility();
        }

        enchantmentCalculator$updateCalculateButton();
        enchantmentCalculator$updateResetButton();

        rightPanelVisible = false;
        if (stepPrevButton != null) {
            this.remove(stepPrevButton);
            stepPrevButton = null;
        }
        if (stepNextButton != null) {
            this.remove(stepNextButton);
            stepNextButton = null;
        }
        enchantmentCalculator$clearResults();

        if (pinButton != null) {
            this.remove(pinButton);
            pinButton = null;
        }
    }

    @Unique
    private void enchantmentCalculator$updateEnchantmentCompatibility() {
        for (EnchantmentButton button : enchantmentButtons) {
            button.setEnabled(true);
        }

        for (Enchantment selected : selectedEnchantments.keySet()) {
            EnchantmentInfo info = EnchantmentData.getEnchantmentInfo(selected);
            if (info != null) {
                for (Enchantment incompatible : info.incompatibleEnchantments()) {
                    for (EnchantmentButton button : enchantmentButtons) {
                        if (button.getEnchantment().equals(incompatible)) {
                            button.setEnabled(false);
                        }
                    }
                }
            }
        }
    }

    @Unique
    private void enchantmentCalculator$clearResults() {
        lastResult = null;
        currentStepIndex = 0;
    }

    @Unique
    private void enchantmentCalculator$updatePanelVisibility() {
        ItemStack stack = this.handler.getSlot(0).getStack();
        leftPanelVisible = !stack.isEmpty() && EnchantmentData.isEnchantable(stack) && !isPinned;
    }

    @Unique
    private List<String> enchantmentCalculator$wrapText(String text) {
        List<String> lines = new ArrayList<>();
        String[] words = text.split(" ");
        StringBuilder currentLine = new StringBuilder();

        for (String word : words) {
            String testLine = currentLine.isEmpty() ? word : currentLine + " " + word;
            if (textRenderer.getWidth(Text.literal(testLine)) <= 160) {
                currentLine = new StringBuilder(testLine);
            } else {
                if (!currentLine.isEmpty()) {
                    lines.add(currentLine.toString());
                    currentLine = new StringBuilder(word);
                } else {
                    lines.add(word.substring(0, Math.min(word.length(), 25)) + "...");
                }
            }
        }

        if (!currentLine.isEmpty()) {
            lines.add(currentLine.toString());
        }

        return lines;
    }

    @Override
    public boolean mouseClicked(Click click, boolean doubled) {
        return super.mouseClicked(click, doubled);
    }
}