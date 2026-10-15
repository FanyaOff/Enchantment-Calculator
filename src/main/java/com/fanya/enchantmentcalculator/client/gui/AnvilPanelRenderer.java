package com.fanya.enchantmentcalculator.client.gui;

import com.fanya.enchantmentcalculator.calculator.CalculationResult;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.item.enchantment.Enchantment;

import java.util.List;

/** отрисовка плана, проверки и выбора чар */
public final class AnvilPanelRenderer {
    public record Readiness(List<String> missingBooks, int levelsNeededNow, int levelsNeededForPlan) {}

    private final Font font;
    private final int leftPos;
    private final int topPos;
    private final List<Enchantment> availableEnchantments;
    private final int scrollOffset;
    private final String errorMessage;
    private final CalculationResult result;
    private final int stepIndex;
    private final Component automaticStatus;
    private final Readiness readiness;
    private final int readinessScrollOffset;
    private final String readinessProblem;
    private final boolean pinFitsBelow;

    public AnvilPanelRenderer(
            Font font, int leftPos, int topPos,
            List<Enchantment> availableEnchantments, int scrollOffset, String errorMessage,
            CalculationResult result, int stepIndex, Component automaticStatus,
            Readiness readiness, int readinessScrollOffset,
            String readinessProblem, boolean pinFitsBelow
    ) {
        this.font = font;
        this.leftPos = leftPos;
        this.topPos = topPos;
        this.availableEnchantments = availableEnchantments;
        this.scrollOffset = scrollOffset;
        this.errorMessage = errorMessage;
        this.result = result;
        this.stepIndex = stepIndex;
        this.automaticStatus = automaticStatus;
        this.readiness = readiness;
        this.readinessScrollOffset = readinessScrollOffset;
        this.readinessProblem = readinessProblem;
        this.pinFitsBelow = pinFitsBelow;
    }

    public void drawSideResult(
            GuiGraphics context,
            int panelX,
            int panelWidth,
            int mouseX,
            int mouseY
    ) {
        if (result == null || result.getSteps().isEmpty()) {
            return;
        }

        Component totals = Component.translatable(
                "enchantmentcalculator.ui.summary",
                result.getTotalLevels(),
                result.getTotalExperience()
        );
        drawCenteredFit(context, totals,
                panelX + panelWidth / 2, topPos + 8, panelWidth - 16, 0xFF404040);

        CalculationResult.Step step = result.getSteps().get(stepIndex);
        Component stepLabel = Component.translatable(
                "enchantmentcalculator.ui.step_cost",
                stepIndex + 1, result.getSteps().size(), step.getLevels()
        );
        drawCenteredFit(context, stepLabel,
                panelX + panelWidth / 2, topPos + 23, panelWidth - 16, 0xFF404040);

        int insetX = panelX + 8;
        int insetY = topPos + 35;
        int insetWidth = panelWidth - 16;
        context.fill(insetX, insetY, insetX + insetWidth, insetY + 88, 0xFFE0E0E0);
        context.fill(insetX, insetY, insetX + insetWidth, insetY + 1, 0xFF777777);
        context.fill(insetX, insetY, insetX + 1, insetY + 88, 0xFF777777);
        context.fill(insetX, insetY + 87, insetX + insetWidth, insetY + 88, 0xFFFFFFFF);
        context.fill(insetX + insetWidth - 1, insetY, insetX + insetWidth, insetY + 88,
                0xFFFFFFFF);

        List<FormattedCharSequence> description = font.split(
                Component.literal(step.getDescription()), insetWidth - 14
        );
        int maxLines = automaticStatus == null ? 6 : 4;
        for (int line = 0; line < Math.min(description.size(), maxLines); line++) {
            drawText(context, description.get(line),
                    insetX + 7, insetY + 7 + line * 12, 0xFF292929);
        }
        if (automaticStatus != null) {
            List<FormattedCharSequence> status = font.split(automaticStatus, insetWidth - 14);
            for (int line = 0; line < Math.min(status.size(), 2); line++) {
                drawText(context, status.get(line),
                        insetX + 7, insetY + 60 + line * 11, 0xFF8B2F1D);
            }
        }

        if (mouseX >= insetX && mouseX < insetX + insetWidth
                && mouseY >= insetY && mouseY < insetY + 88) {
            context.setTooltipForNextFrame(
                    Component.literal(step.getDescription()), mouseX, mouseY
            );
        }
        drawCenteredText(context,
                Component.translatable("enchantmentcalculator.ui.step",
                        stepIndex + 1, result.getSteps().size()),
                panelX + panelWidth / 2,
                topPos + (pinFitsBelow ? 148 : 130),
                0xFF404040
        );
    }

    public void drawSideReadiness(
            GuiGraphics context,
            int panelX,
            int panelWidth,
            int mouseX,
            int mouseY
    ) {
        drawCenteredFit(
                context,
                Component.translatable("enchantmentcalculator.ui.auto_title"),
                panelX + panelWidth / 2, topPos + 9, panelWidth - 16, 0xFF404040
        );
        if (readiness == null) {
            return;
        }

        int textX = panelX + 10;
        int contentWidth = panelWidth - 42;
        if (readiness.missingBooks().isEmpty()) {
            drawSideLines(
                    context,
                    Component.translatable("enchantmentcalculator.ui.books_ready"),
                    textX, topPos + 35, panelWidth - 20, 2, 0xFF32652A
            );
        } else {
            drawText(context,
                    Component.translatable(
                            "enchantmentcalculator.ui.missing_books_count",
                            readiness.missingBooks().size()
                    ),
                    textX, topPos + 29, 0xFFA33A26);

            int textY = topPos + 43;
            for (int index = readinessScrollOffset;
                    index < readiness.missingBooks().size() && textY <= topPos + 79;
                    index++) {
                Component book = Component.literal("• " + readiness.missingBooks().get(index));
                List<FormattedCharSequence> lines = font.split(book, contentWidth);
                for (FormattedCharSequence line : lines) {
                    if (textY > topPos + 79) {
                        break;
                    }
                    drawText(context, line, textX, textY, 0xFF404040);
                    textY += 11;
                }
            }
        }

        Component problem = automaticStatus != null
                ? automaticStatus
                : readinessProblem == null ? null : Component.translatable(readinessProblem);
        if (problem != null) {
            drawSideLines(context, problem,
                    textX, topPos + 90, panelWidth - 20, 1, 0xFFA33A26);
        }

        if (mouseX >= panelX + 8 && mouseX < panelX + panelWidth - 8) {
            if (mouseY >= topPos + 40 && mouseY < topPos + 86
                    && !readiness.missingBooks().isEmpty()) {
                context.setTooltipForNextFrame(
                        Component.literal(String.join(", ", readiness.missingBooks())),
                        mouseX, mouseY
                );
            } else if (mouseY >= topPos + 88 && mouseY < topPos + 101
                    && problem != null) {
                context.setTooltipForNextFrame(problem, mouseX, mouseY);
            }
        }

        drawLevelRow(
                context,
                Component.translatable("enchantmentcalculator.ui.next_step_label"),
                readiness.levelsNeededNow(),
                textX,
                panelX + panelWidth - 10,
                topPos + 101
        );
        drawLevelRow(
                context,
                Component.translatable("enchantmentcalculator.ui.whole_plan_label"),
                readiness.levelsNeededForPlan(),
                textX,
                panelX + panelWidth - 10,
                topPos + 113
        );
    }

    private void drawLevelRow(
            GuiGraphics context,
            Component label,
            int missingLevels,
            int leftX,
            int rightX,
            int y
    ) {
        Component value = missingLevels == 0
                ? Component.translatable("enchantmentcalculator.ui.enough_levels")
                : Component.translatable("enchantmentcalculator.ui.more_levels", missingLevels);
        int valueColor = missingLevels == 0 ? 0xFF32652A : 0xFF9B3A24;
        drawText(context, label, leftX, y, 0xFF303030);
        drawText(
                context, value, rightX - font.width(value), y, valueColor
        );
    }

    public void drawCenteredFit(
            GuiGraphics context,
            Component text,
            int centerX,
            int y,
            int maxWidth,
            int color
    ) {
        List<FormattedCharSequence> lines = font.split(text, maxWidth);
        if (!lines.isEmpty()) {
            drawCenteredText(context, lines.getFirst(), centerX, y, color);
        }
    }

    private void drawSideLines(
            GuiGraphics context,
            Component text,
            int x,
            int y,
            int maxWidth,
            int maxLines,
            int color
    ) {
        List<FormattedCharSequence> lines = font.split(text, maxWidth);
        for (int line = 0; line < Math.min(lines.size(), maxLines); line++) {
            drawText(context, lines.get(line), x, y + line * 11, color);
        }
    }

    private void drawText(
            GuiGraphics context,
            Component text,
            int x,
            int y,
            int color
    ) {
        context.drawString(font, text, x, y, color, false);
    }

    private void drawText(
            GuiGraphics context,
            FormattedCharSequence text,
            int x,
            int y,
            int color
    ) {
        context.drawString(font, text, x, y, color, false);
    }

    private void drawCenteredText(
            GuiGraphics context,
            Component text,
            int centerX,
            int y,
            int color
    ) {
        drawText(context, text, centerX - font.width(text) / 2, y, color);
    }

    private void drawCenteredText(
            GuiGraphics context,
            FormattedCharSequence text,
            int centerX,
            int y,
            int color
    ) {
        drawText(context, text, centerX - font.width(text) / 2, y, color);
    }

    public void drawSelection(GuiGraphics context) {
        drawTitle(
                context,
                Component.translatable("enchantmentcalculator.ui.title")
        );
        String range = availableEnchantments.isEmpty()
                ? "0/0"
                : (scrollOffset + 1) + "-"
                + Math.min(scrollOffset + 4, availableEnchantments.size())
                + "/" + availableEnchantments.size();
        if (errorMessage != null) {
            List<FormattedCharSequence> lines = font.split(
                    Component.translatable(errorMessage),
                    116
            );
            for (int line = 0; line < Math.min(2, lines.size()); line++) {
                drawCenteredText(context, lines.get(line), 88, 27 + line * 10, 0xFFAA2222);
            }
        } else {
            drawCenteredText(context, Component.literal(range), 88, 30, 0xFF404040);
        }
    }

    public void drawResult(
            GuiGraphics context,
            int mouseX,
            int mouseY
    ) {
        drawTitle(
                context,
                Component.translatable("enchantmentcalculator.ui.plan")
        );
        if (result == null || result.getSteps().isEmpty()) {
            return;
        }

        Component totals = Component.translatable(
                "enchantmentcalculator.ui.summary",
                result.getTotalLevels(),
                result.getTotalExperience()
        );
        drawCenteredText(context, totals, 88, 26, 0xFF404040);

        CalculationResult.Step step = result.getSteps().get(stepIndex);
        Component stepLabel = Component.translatable(
                "enchantmentcalculator.ui.step_cost",
                stepIndex + 1,
                result.getSteps().size(),
                step.getLevels()
        );
        drawCenteredText(context, stepLabel, 88, 42, 0xFF404040);
        drawWrapped(context, Component.literal(step.getDescription()), 57, 3, 0xFF303030);
        if (mouseX >= leftPos + 8 && mouseX < leftPos + 168
                && mouseY >= topPos + 54 && mouseY < topPos + 91) {
            context.setTooltipForNextFrame(
                    Component.literal(step.getDescription()),
                    mouseX,
                    mouseY
            );
        }
        if (automaticStatus != null) {
            drawWrapped(context, automaticStatus, 91, 1, 0xFF8B4D16);
        }
    }

    public void drawReadiness(GuiGraphics context) {
        drawTitle(
                context,
                Component.translatable("enchantmentcalculator.ui.auto_title")
        );
        if (readiness == null) {
            return;
        }

        int textY = 30;
        if (readiness.missingBooks().isEmpty()) {
            drawText(context,
                    Component.translatable("enchantmentcalculator.ui.books_ready"),
                    10,
                    textY,
                    0xFF32652A
            );
            textY += 17;
        } else {
            drawText(context,
                    Component.translatable("enchantmentcalculator.ui.missing_books"),
                    10,
                    textY,
                    0xFFA33A26
            );
            textY += 14;
            for (int index = readinessScrollOffset;
                    index < readiness.missingBooks().size() && textY <= 80;
                    index++) {
                Component book = Component.literal("• " + readiness.missingBooks().get(index));
                for (FormattedCharSequence line : font.split(book, 132)) {
                    if (textY > 80) {
                        break;
                    }
                    drawText(context, line, 12, textY, 0xFF404040);
                    textY += 12;
                }
            }
        }

        if (automaticStatus != null) {
            drawWrapped(context, automaticStatus, 91, 1, 0xFF8B4D16);
        } else if (readinessProblem != null) {
            drawWrapped(
                    context,
                    Component.translatable(readinessProblem),
                    91,
                    1,
                    0xFFA33A26
            );
        }
        drawText(context,
                Component.translatable(
                        "enchantmentcalculator.ui.levels_needed_now",
                        readiness.levelsNeededNow()
                ),
                11,
                103,
                0xFF404040
        );
        drawText(context,
                Component.translatable(
                        "enchantmentcalculator.ui.levels_needed_plan",
                        readiness.levelsNeededForPlan()
                ),
                11,
                114,
                0xFF404040
        );
    }

    private void drawTitle(GuiGraphics context, Component title) {
        drawCenteredText(context, title, 82, 9, 0xFF404040);
    }

    private void drawWrapped(
            GuiGraphics context,
            Component text,
            int relativeY,
            int maxLines,
            int color
    ) {
        List<FormattedCharSequence> lines = font.split(text, 154);
        for (int line = 0; line < Math.min(lines.size(), maxLines); line++) {
            drawText(context, lines.get(line), 11, relativeY + line * 11, color);
        }
    }
}
