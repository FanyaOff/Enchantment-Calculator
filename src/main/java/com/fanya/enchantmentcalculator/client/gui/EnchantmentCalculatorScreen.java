package com.fanya.enchantmentcalculator.client.gui;

import com.fanya.enchantmentcalculator.calculator.CalculationResult;
import com.fanya.enchantmentcalculator.calculator.EnchantmentCalculator;
import com.fanya.enchantmentcalculator.calculator.EnchantmentCombination;
import com.fanya.enchantmentcalculator.calculator.OptimizationMode;
import com.fanya.enchantmentcalculator.client.gui.widget.EnchantmentButton;
import com.fanya.enchantmentcalculator.client.gui.widget.ScrollableWidget;
import com.fanya.enchantmentcalculator.data.EnchantmentData;
import com.fanya.enchantmentcalculator.data.EnchantmentInfo;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.ingame.AnvilScreen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.CyclingButtonWidget;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;

import java.util.*;

public class EnchantmentCalculatorScreen extends Screen {
    private static final int BACKGROUND_COLOR = 0xC0101010;
    private static final int BORDER_COLOR = 0xFFC0C0C0;
    private static final int BUTTON_COLOR = 0xFF8B8B8B;
    private static final int BUTTON_HOVER_COLOR = 0xFFA0A0A0;

    private final AnvilScreen parent;
    private final ItemStack targetItem;
    private final List<Enchantment> availableEnchantments;
    private final Map<Enchantment, Integer> selectedEnchantments = new HashMap<>();
    private final List<EnchantmentButton> enchantmentButtons = new ArrayList<>();

    private ScrollableWidget enchantmentList;
    private ScrollableWidget resultList;
    private ButtonWidget calculateButton;
    private ButtonWidget backButton;
    private CyclingButtonWidget<OptimizationMode> optimizationButton;

    private CalculationResult lastResult;
    private OptimizationMode optimizationMode = OptimizationMode.LEVELS;

    // Для прокрутки результатов
    private int resultScrollOffset = 0;
    private final List<Text> resultLines = new ArrayList<>();

    public EnchantmentCalculatorScreen(AnvilScreen parent, ItemStack targetItem) {
        super(Text.translatable("gui.enchantmentcalculator.title"));
        this.parent = parent;
        this.targetItem = targetItem.copy();
        this.availableEnchantments = EnchantmentData.getApplicableEnchantments(targetItem);
    }

    @Override
    protected void init() {
        super.init();

        int centerX = this.width / 2;
        int centerY = this.height / 2;

        // Список зачарований (левая часть)
        setupEnchantmentList(centerX, centerY);

        // Кнопки управления
        setupControlButtons(centerX, centerY);

        updateCalculateButton();
    }

    private void setupEnchantmentList(int centerX, int centerY) {
        enchantmentButtons.clear();

        int startY = centerY - 120;
        int currentY = startY;

        for (Enchantment enchantment : availableEnchantments) {
            EnchantmentInfo info = EnchantmentData.getEnchantmentInfo(enchantment);
            if (info != null) {
                EnchantmentButton button = new EnchantmentButton(
                        centerX - 200, currentY, 180, 20,
                        enchantment, info.maxLevel(),
                        this::onEnchantmentChanged
                );
                enchantmentButtons.add(button);
                this.addDrawableChild(button);
                currentY += 22;
            }
        }
    }

    // Замените метод setupControlButtons на этот:
    private void setupControlButtons(int centerX, int centerY) {
        // Кнопка оптимизации
        optimizationButton = CyclingButtonWidget.builder(OptimizationMode::getDisplayName)
                .values(OptimizationMode.values())
                .initially(optimizationMode)
                .build(centerX - 100, centerY + 90, 200, 20,
                        Text.translatable("gui.enchantmentcalculator.optimization"),
                        (button, mode) -> {
                            this.optimizationMode = mode;
                            if (lastResult != null) {
                                try {
                                    calculate(); // Пересчитываем при смене режима
                                } catch (Exception e) {
                                    throw new RuntimeException(e);
                                }
                            }
                        });
        this.addDrawableChild(optimizationButton);

        // Кнопка расчета
        calculateButton = ButtonWidget.builder(
                        Text.translatable("gui.enchantmentcalculator.calculate"),
                        button -> {
                            try {
                                calculate();
                            } catch (Exception e) {
                                throw new RuntimeException(e);
                            }
                        }
                )
                .dimensions(centerX - 100, centerY + 115, 200, 20)
                .build();
        this.addDrawableChild(calculateButton);

        // Кнопка назад
        backButton = ButtonWidget.builder(
                        Text.translatable("gui.done"),
                        button -> this.close()
                )
                .dimensions(centerX - 50, centerY + 140, 100, 20)
                .build();
        this.addDrawableChild(backButton);
    }


    private void onEnchantmentChanged(Enchantment enchantment, int level) {
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

            // Включаем обратно совместимые зачарования
            updateEnchantmentCompatibility();
        }

        updateCalculateButton();
        clearResults();
    }

    private void updateEnchantmentCompatibility() {
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

    private void updateCalculateButton() {
        if (calculateButton != null) {
            calculateButton.active = !selectedEnchantments.isEmpty();
        }
    }

    private void calculate() throws Exception {
        if (selectedEnchantments.isEmpty()) return;

        List<EnchantmentCombination> combinations = new ArrayList<>();
        for (Map.Entry<Enchantment, Integer> entry : selectedEnchantments.entrySet()) {
            combinations.add(new EnchantmentCombination(entry.getKey(), entry.getValue()));
        }

        lastResult = EnchantmentCalculator.calculate(targetItem, combinations, optimizationMode);
        updateResultLines();
    }

    private void updateResultLines() {
        resultLines.clear();
        resultScrollOffset = 0;

        if (lastResult == null) return;

        // Общая стоимость
        resultLines.add(Text.translatable("gui.enchantmentcalculator.total_cost",
                        lastResult.getTotalLevels(), lastResult.getTotalExperience())
                .formatted(Formatting.BOLD, Formatting.YELLOW));

        resultLines.add(Text.empty());

        // Шаги
        resultLines.add(Text.translatable("gui.enchantmentcalculator.steps")
                .formatted(Formatting.UNDERLINE, Formatting.WHITE));

        for (int i = 0; i < lastResult.getSteps().size(); i++) {
            CalculationResult.Step step = lastResult.getSteps().get(i);

            resultLines.add(Text.translatable("gui.enchantmentcalculator.step",
                    i + 1, step.getDescription()).formatted(Formatting.GREEN));

            resultLines.add(Text.translatable("gui.enchantmentcalculator.step_cost",
                            step.getLevels(), step.getExperience())
                    .formatted(Formatting.GRAY));

            if (i < lastResult.getSteps().size() - 1) {
                resultLines.add(Text.empty());
            }
        }
    }

    private void clearResults() {
        lastResult = null;
        resultLines.clear();
        resultScrollOffset = 0;
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        // Фон
        context.fill(0, 0, this.width, this.height, 0x80000000);

        int centerX = this.width / 2;
        int centerY = this.height / 2;

        // Основная панель
        context.fill(centerX - 220, centerY - 140, centerX + 220, centerY + 170, BACKGROUND_COLOR);
        context.drawBorder(centerX - 220, centerY - 140, 440, 310, BORDER_COLOR);

        // Заголовок
        context.drawCenteredTextWithShadow(this.textRenderer, this.title,
                centerX, centerY - 130, 0xFFFFFF);

        // Название предмета
        Text itemName = targetItem.getName();
        context.drawCenteredTextWithShadow(this.textRenderer, itemName,
                centerX, centerY - 115, 0xFFFFFF);

        // Заголовок списка зачарований
        context.drawTextWithShadow(this.textRenderer,
                Text.translatable("gui.enchantmentcalculator.enchantments"),
                centerX - 200, centerY - 100, 0xFFFFFF);

        // Заголовок результатов
        context.drawTextWithShadow(this.textRenderer,
                Text.translatable("gui.enchantmentcalculator.results"),
                centerX + 20, centerY - 100, 0xFFFFFF);

        // Область результатов
        drawResultsArea(context, centerX + 20, centerY - 80, 180, 160);

        super.render(context, mouseX, mouseY, delta);
    }

    private void drawResultsArea(DrawContext context, int x, int y, int width, int height) {
        // Фон области результатов
        context.fill(x, y, x + width, y + height, 0x40000000);
        context.drawBorder(x, y, width, height, 0xFF666666);

        if (resultLines.isEmpty()) {
            // Показываем подсказку, если нет результатов
            Text hint = Text.translatable("gui.enchantmentcalculator.select_enchantments")
                    .formatted(Formatting.GRAY, Formatting.ITALIC);
            int hintX = x + (width - textRenderer.getWidth(hint)) / 2;
            int hintY = y + height / 2 - 4;
            context.drawTextWithShadow(textRenderer, hint, hintX, hintY, 0x888888);
            return;
        }

        // Отображаем результаты с прокруткой
        context.enableScissor(x + 2, y + 2, x + width - 2, y + height - 2);

        int currentY = y + 5 - resultScrollOffset;
        for (Text line : resultLines) {
            if (currentY > y - 10 && currentY < y + height + 10) {
                context.drawTextWithShadow(textRenderer, line, x + 5, currentY, 0xFFFFFF);
            }
            currentY += 12;
        }

        context.disableScissor();
    }


    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        int centerX = this.width / 2;
        int centerY = this.height / 2;

        // Проверяем, находится ли курсор в области результатов
        if (mouseX >= centerX + 20 && mouseX <= centerX + 200 &&
                mouseY >= centerY - 80 && mouseY <= centerY + 80) {

            int maxScroll = Math.max(0, resultLines.size() * 12 - 160);
            resultScrollOffset = Math.max(0, Math.min(maxScroll,
                    (int) (resultScrollOffset - verticalAmount * 20)));
            return true;
        }

        return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
    }

    @Override
    public boolean shouldCloseOnEsc() {
        return true;
    }

    @Override
    public void close() {
        this.client.setScreen(parent);
    }
}
