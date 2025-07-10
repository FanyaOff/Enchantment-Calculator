package com.fanya.enchantmentcalculator.client.mixin;

import com.fanya.enchantmentcalculator.calculator.CalculationResult;
import com.fanya.enchantmentcalculator.calculator.EnchantmentCalculator;
import com.fanya.enchantmentcalculator.calculator.EnchantmentCombination;
import com.fanya.enchantmentcalculator.calculator.OptimizationMode;
import com.fanya.enchantmentcalculator.client.gui.widget.EnchantmentButton;
import com.fanya.enchantmentcalculator.data.EnchantmentData;
import com.fanya.enchantmentcalculator.data.EnchantmentInfo;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.AnvilScreen;
import net.minecraft.client.gui.screen.ingame.ForgingScreen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.CyclingButtonWidget;
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
    // Приятные цвета в стиле Minecraft
    @Unique
    private static final int PANEL_BG = 0xE0C6C6C6;           // Светло-серый фон
    @Unique
    private static final int PANEL_BORDER_LIGHT = 0xFFFFFFFF;  // Белая граница
    @Unique
    private static final int PANEL_BORDER_DARK = 0xFF555555;   // Темная граница
    @Unique
    private static final int INSET_BG = 0xFF8B8B8B;           // Вдавленный фон
    @Unique
    private static final int TEXT_COLOR     = 0xFFFFFFFF;

    @Unique
    private final List<EnchantmentButton> enchantmentButtons = new ArrayList<>();
    @Unique
    private final Map<Enchantment, Integer> selectedEnchantments = new HashMap<>();
    @Unique
    private final List<Text> resultLines = new ArrayList<>();

    @Unique
    private ButtonWidget calculateButton;
    @Unique
    private ButtonWidget scrollUpButton;
    @Unique
    private ButtonWidget scrollDownButton;
    @Unique
    private ButtonWidget resultScrollUpButton;
    @Unique
    private ButtonWidget resultScrollDownButton;
    @Unique
    private CyclingButtonWidget<OptimizationMode> optimizationButton;
    @Unique
    private OptimizationMode optimizationMode = OptimizationMode.LEVELS;
    @Unique
    private CalculationResult lastResult;
    @Unique
    private int enchantmentScrollOffset = 0;
    @Unique
    private int resultScrollOffset = 0;
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
        enchantmentCalculator$updatePanelVisibility();
        if (leftPanelVisible) {
            enchantmentCalculator$setupLeftPanel();
        }
    }

    @Inject(method = "onSlotUpdate", at = @At("TAIL"))
    private void updatePanelOnSlotChange(ScreenHandler handler, int slotId, ItemStack stack, CallbackInfo ci) {
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
        if (leftPanelVisible) {
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

        // Позиция левой панели - исправлено позиционирование
        int leftPanelX = this.x - 185;
        int startY = this.y + 40;

        // Создаем видимые кнопки зачарований
        enchantmentCalculator$updateEnchantmentButtons();

        // Кнопки прокрутки для зачарований
        scrollUpButton = ButtonWidget.builder(
                        Text.literal("↑"),
                        button -> {
                            enchantmentScrollOffset = Math.max(0, enchantmentScrollOffset - 1);
                            enchantmentCalculator$updateEnchantmentButtons();
                        }
                )
                .dimensions(leftPanelX + 160, startY, 18, 15)
                .build();
        this.addDrawableChild(scrollUpButton);

        scrollDownButton = ButtonWidget.builder(
                        Text.literal("↓"),
                        button -> {
                            int maxScroll = Math.max(0, availableEnchantments.size() - 6);
                            enchantmentScrollOffset = Math.min(maxScroll, enchantmentScrollOffset + 1);
                            enchantmentCalculator$updateEnchantmentButtons();
                        }
                )
                .dimensions(leftPanelX + 160, startY + 17, 18, 15)
                .build();
        this.addDrawableChild(scrollDownButton);

        // Кнопка оптимизации
        optimizationButton = CyclingButtonWidget.builder(OptimizationMode::getDisplayName)
                .values(OptimizationMode.values())
                .initially(optimizationMode)
                .build(leftPanelX + 8, startY + 135, 150, 18,
                        Text.literal("Optimize: "),
                        (button, mode) -> {
                            this.optimizationMode = mode;
                        });
        this.addDrawableChild(optimizationButton);

        // Кнопка расчета
        calculateButton = ButtonWidget.builder(
                        Text.literal("Calculate"),
                        button -> enchantmentCalculator$calculate()
                )
                .dimensions(leftPanelX + 8, startY + 157, 150, 18)
                .build();
        this.addDrawableChild(calculateButton);

        enchantmentCalculator$updateCalculateButton();
    }

    @Unique
    private void enchantmentCalculator$updateEnchantmentButtons() {
        // Очищаем старые кнопки
        enchantmentButtons.forEach(this::remove);
        enchantmentButtons.clear();

        int leftPanelX = this.x - 185;
        int startY = this.y + 40;
        int currentY = startY;

        // Создаем только видимые кнопки (6 штук максимум)
        int visibleCount = 0;
        for (int i = enchantmentScrollOffset; i < availableEnchantments.size() && visibleCount < 6; i++) {
            Enchantment enchantment = availableEnchantments.get(i);
            EnchantmentInfo info = EnchantmentData.getEnchantmentInfo(enchantment);
            if (info != null) {
                EnchantmentButton button = new EnchantmentButton(
                        leftPanelX + 8, currentY, 150, 18,
                        enchantment, info.maxLevel(),
                        this::enchantmentCalculator$onEnchantmentChanged
                );

                // Восстанавливаем состояние кнопки
                if (selectedEnchantments.containsKey(enchantment)) {
                    button.setLevel(selectedEnchantments.get(enchantment));
                }

                enchantmentButtons.add(button);
                this.addDrawableChild(button);
                currentY += 20;
                visibleCount++;
            }
        }

        // Обновляем совместимость
        enchantmentCalculator$updateEnchantmentCompatibility();

        // Обновляем состояние кнопок прокрутки
        if (scrollUpButton != null) {
            scrollUpButton.active = enchantmentScrollOffset > 0;
        }
        if (scrollDownButton != null) {
            scrollDownButton.active = enchantmentScrollOffset < availableEnchantments.size() - 6;
        }
    }

    @Unique
    private void enchantmentCalculator$clearInterface() {
        // Удаляем все элементы интерфейса
        enchantmentButtons.forEach(this::remove);
        enchantmentButtons.clear();

        if (optimizationButton != null) {
            this.remove(optimizationButton);
            optimizationButton = null;
        }

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

        if (resultScrollUpButton != null) {
            this.remove(resultScrollUpButton);
            resultScrollUpButton = null;
        }

        if (resultScrollDownButton != null) {
            this.remove(resultScrollDownButton);
            resultScrollDownButton = null;
        }

        selectedEnchantments.clear();
        enchantmentCalculator$clearResults();
        rightPanelVisible = false;
    }

    @Unique
    private void enchantmentCalculator$onEnchantmentChanged(Enchantment enchantment, int level) {
        if (level > 0) {
            selectedEnchantments.put(enchantment, level);

            // Отключаем несовместимые зачарования
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
        // Скрываем правую панель при изменении зачарований
        rightPanelVisible = false;
        enchantmentCalculator$clearResults();
    }

    @Unique
    private void enchantmentCalculator$updateEnchantmentCompatibility() {
        // Сначала включаем все кнопки
        for (EnchantmentButton button : enchantmentButtons) {
            button.setEnabled(true);
        }

        // Затем отключаем несовместимые с выбранными
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
    private void enchantmentCalculator$updateCalculateButton() {
        if (calculateButton != null) {
            calculateButton.active = !selectedEnchantments.isEmpty();
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
            enchantmentCalculator$updateResultLines();
            rightPanelVisible = true;
            enchantmentCalculator$setupResultScrollButtons();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Unique
    private void enchantmentCalculator$setupResultScrollButtons() {
        int rightPanelX = this.x + this.backgroundWidth + 8;
        int rightPanelY = this.y;

        // Кнопки прокрутки результатов
        resultScrollUpButton = ButtonWidget.builder(
                        Text.literal("↑"),
                        button -> {
                            resultScrollOffset = Math.max(0, resultScrollOffset - 3);
                        }
                )
                .dimensions(rightPanelX + 175, rightPanelY + 30, 18, 15)
                .build();
        this.addDrawableChild(resultScrollUpButton);

        resultScrollDownButton = ButtonWidget.builder(
                        Text.literal("↓"),
                        button -> {
                            int maxScroll = Math.max(0, resultLines.size() - 8);
                            resultScrollOffset = Math.min(maxScroll, resultScrollOffset + 3);
                        }
                )
                .dimensions(rightPanelX + 175, rightPanelY + 47, 18, 15)
                .build();
        this.addDrawableChild(resultScrollDownButton);
    }

    @Unique
    private void enchantmentCalculator$updateResultLines() {
        resultLines.clear();
        resultScrollOffset = 0;

        if (lastResult == null) return;

        // Заголовок
        resultLines.add(Text.translatable("gui.enchantmentcalculator.result.found")
                .formatted(Formatting.BOLD, Formatting.GREEN));

        resultLines.add(Text.empty());

        // Общая стоимость
        resultLines.add(Text.translatable("gui.enchantmentcalculator.result.total_cost",
                        lastResult.getTotalLevels(), lastResult.getTotalExperience())
                .formatted(Formatting.BOLD, Formatting.YELLOW));

        resultLines.add(Text.empty());

        // Шаги
        resultLines.add(Text.translatable("gui.enchantmentcalculator.result.steps")
                .formatted(Formatting.BOLD, Formatting.WHITE));

        for (CalculationResult.Step step : lastResult.getSteps()) {
            // Основное описание
            String description = step.getDescription();
            List<String> lines = wrapTextByWords(description, 28);
            for (String line : lines) {
                resultLines.add(Text.literal(line).formatted(Formatting.WHITE));
            }

            // Стоимость
            resultLines.add(Text.translatable("gui.enchantmentcalculator.result.cost",
                            step.getLevels(), step.getExperience())
                    .formatted(Formatting.GRAY));

            // Предыдущий штраф работы
            if (step.getPriorWorkPenalty() > 0) {
                resultLines.add(Text.translatable("gui.enchantmentcalculator.result.prior_work",
                                step.getPriorWorkPenalty())
                        .formatted(Formatting.GRAY));
            }

            resultLines.add(Text.empty());
        }

        // Примечание
        String noteText = Text.translatable("gui.enchantmentcalculator.result.note").getString();
        List<String> noteLines = wrapTextByWords(noteText, 28);
        for (String line : noteLines) {
            resultLines.add(Text.literal(line).formatted(Formatting.ITALIC, Formatting.GRAY));
        }
    }


    @Unique
    private List<String> wrapTextByWords(String text, int maxLength) {
        List<String> lines = new ArrayList<>();
        String[] words = text.split(" ");
        StringBuilder currentLine = new StringBuilder();

        for (String word : words) {
            if (currentLine.length() + word.length() + 1 <= maxLength) {
                if (currentLine.length() > 0) {
                    currentLine.append(" ");
                }
                currentLine.append(word);
            } else {
                if (currentLine.length() > 0) {
                    lines.add(currentLine.toString());
                    currentLine = new StringBuilder(word);
                } else {
                    lines.add(word); // Слово слишком длинное, но добавляем как есть
                }
            }
        }

        if (currentLine.length() > 0) {
            lines.add(currentLine.toString());
        }

        return lines;
    }




    @Unique
    private void enchantmentCalculator$clearResults() {
        lastResult = null;
        resultLines.clear();
        resultScrollOffset = 0;
    }

    @Unique
    private void enchantmentCalculator$updatePanelVisibility() {
        ItemStack stack = this.handler.getSlot(0).getStack();
        leftPanelVisible = !stack.isEmpty() && EnchantmentData.isEnchantable(stack);
    }

    @Unique
    private void enchantmentCalculator$renderLeftPanel(DrawContext context) {
        int leftPanelX = this.x - 185;
        int leftPanelY = this.y;
        int panelWidth = 180;
        int panelHeight = this.backgroundHeight;

        // Рисуем фон левой панели
        enchantmentCalculator$drawMinecraftPanel(context, leftPanelX, leftPanelY, panelWidth, panelHeight);

        // Заголовок с тенью
        context.drawTextWithShadow(this.textRenderer,
                Text.literal("Enchantment Calculator"),
                leftPanelX + 8, leftPanelY + 10, TEXT_COLOR);

        // Название предмета
        ItemStack stack = this.handler.getSlot(0).getStack();
        if (!stack.isEmpty()) {
            Text itemName = stack.getName();
            String displayName = itemName.getString();
            if (displayName.length() > 22) {
                displayName = displayName.substring(0, 19) + "...";
            }
            context.drawTextWithShadow(this.textRenderer,
                    Text.literal(displayName), leftPanelX + 8, leftPanelY + 25, TEXT_COLOR);
        }

        // Индикатор прокрутки
        if (availableEnchantments.size() > 6) {
            context.drawTextWithShadow(this.textRenderer,
                    Text.literal("(" + (enchantmentScrollOffset + 1) + "-" +
                            Math.min(enchantmentScrollOffset + 6, availableEnchantments.size()) +
                            "/" + availableEnchantments.size() + ")"),
                    leftPanelX + 8, leftPanelY + panelHeight - 15, 0xFF666666);
        }
    }

    @Unique
    private void enchantmentCalculator$renderRightPanel(DrawContext context) {
        int rightPanelX = this.x + this.backgroundWidth + 8;
        int rightPanelY = this.y;
        int panelWidth = 195;
        int panelHeight = this.backgroundHeight;

        // Рисуем фон правой панели
        enchantmentCalculator$drawMinecraftPanel(context, rightPanelX, rightPanelY, panelWidth, panelHeight);

        // Заголовок
        context.drawTextWithShadow(this.textRenderer,
                Text.literal("Calculation Results"),
                rightPanelX + 8, rightPanelY + 10, TEXT_COLOR);

        enchantmentCalculator$renderResultsArea(context,
                rightPanelX + 8, rightPanelY + 30,
                panelWidth - 20, panelHeight - 40);

    }

    @Unique
    private void enchantmentCalculator$drawMinecraftPanel(DrawContext context, int x, int y, int width, int height) {
        // Фон панели
        context.fill(x, y, x + width, y + height, PANEL_BG);

        // Верхняя и левая светлые границы
        context.fill(x, y, x + width - 1, y + 1, PANEL_BORDER_LIGHT);
        context.fill(x, y, x + 1, y + height - 1, PANEL_BORDER_LIGHT);

        // Нижняя и правая темные границы
        context.fill(x + 1, y + height - 1, x + width, y + height, PANEL_BORDER_DARK);
        context.fill(x + width - 1, y + 1, x + width, y + height - 1, PANEL_BORDER_DARK);

        // Внутренние границы для 3D эффекта
        context.fill(x + 1, y + 1, x + width - 2, y + 2, PANEL_BORDER_LIGHT);
        context.fill(x + 1, y + 1, x + 2, y + height - 2, PANEL_BORDER_LIGHT);
        context.fill(x + 2, y + height - 2, x + width - 1, y + height - 1, PANEL_BORDER_DARK);
        context.fill(x + width - 2, y + 2, x + width - 1, y + height - 2, PANEL_BORDER_DARK);
    }

    @Unique
    private void enchantmentCalculator$renderResultsArea(DrawContext context, int x, int y, int width, int height) {
        // Фон области результатов (вдавленная панель)
        context.fill(x, y, x + width, y + height, INSET_BG);

        // Рамки
        context.fill(x, y, x + width - 1, y + 1, PANEL_BORDER_DARK);
        context.fill(x, y, x + 1, y + height - 1, PANEL_BORDER_DARK);
        context.fill(x + 1, y + height - 1, x + width, y + height, PANEL_BORDER_LIGHT);
        context.fill(x + width - 1, y + 1, x + width, y + height - 1, PANEL_BORDER_LIGHT);

        if (resultLines.isEmpty()) {
            Text hint = Text.literal("Нажмите Calculate").formatted(Formatting.ITALIC);
            int hintX = x + (width - textRenderer.getWidth(hint)) / 2;
            int hintY = y + height / 2 - 4;
            context.drawTextWithShadow(textRenderer, hint, hintX, hintY, 0xFF666666);
            return;
        }

        // Отображаем результаты с прокруткой
        context.enableScissor(x + 3, y + 3, x + width - 3, y + height - 3);

        int currentY = y + 8 - (resultScrollOffset * 11);
        int visibleLines = 0;
        int maxVisibleLines = (height - 10) / 11; // 11 пикселей на строку

        for (Text line : resultLines) {
            if (currentY > y - 15 && currentY < y + height + 15 && visibleLines < maxVisibleLines) {
                String lineText = line.getString();

                // Автоматический перенос длинных строк
                if (this.textRenderer.getWidth(line) > width - 10) {
                    List<String> wrappedLines = wrapText(lineText, width - 10);
                    for (String wrappedLine : wrappedLines) {
                        if (currentY > y - 15 && currentY < y + height + 15) {
                            Text wrappedText = Text.literal(wrappedLine).setStyle(line.getStyle());
                            context.drawTextWithShadow(textRenderer, wrappedText, x + 5, currentY, 0xFFFFFFFF);
                        }
                        currentY += 11;
                        visibleLines++;
                    }
                } else {
                    context.drawTextWithShadow(textRenderer, line, x + 5, currentY, 0xFFFFFFFF);
                    currentY += 11;
                    visibleLines++;
                }
            } else if (this.textRenderer.getWidth(line) > width - 10) {
                // Пропускаем строки, но учитываем перенос для правильной прокрутки
                int wrappedCount = (int) Math.ceil((double) this.textRenderer.getWidth(line) / (width - 10));
                currentY += 11 * wrappedCount;
            } else {
                currentY += 11;
            }
        }

        context.disableScissor();

        // Обновляем кнопки прокрутки
        if (resultScrollUpButton != null) {
            resultScrollUpButton.active = resultScrollOffset > 0;
        }
        if (resultScrollDownButton != null) {
            int totalLines = calculateTotalDisplayLines();
            resultScrollDownButton.active = resultScrollOffset < Math.max(0, totalLines - maxVisibleLines);
        }
    }

    @Unique
    private List<String> wrapText(String text, int maxWidth) {
        List<String> lines = new ArrayList<>();
        String[] words = text.split(" ");
        StringBuilder currentLine = new StringBuilder();

        for (String word : words) {
            String testLine = currentLine.length() == 0 ? word : currentLine + " " + word;
            if (textRenderer.getWidth(Text.literal(testLine)) <= maxWidth) {
                currentLine = new StringBuilder(testLine);
            } else {
                if (currentLine.length() > 0) {
                    lines.add(currentLine.toString());
                    currentLine = new StringBuilder(word);
                } else {
                    // Слово слишком длинное, принудительно обрезаем
                    lines.add(word.substring(0, Math.min(word.length(), 25)) + "...");
                }
            }
        }

        if (currentLine.length() > 0) {
            lines.add(currentLine.toString());
        }

        return lines;
    }

    @Unique
    private int calculateTotalDisplayLines() {
        int total = 0;
        for (Text line : resultLines) {
            if (this.textRenderer.getWidth(line) > 160) { // примерная ширина - 10
                total += (int) Math.ceil((double) this.textRenderer.getWidth(line) / 160);
            } else {
                total += 1;
            }
        }
        return total;
    }

}
