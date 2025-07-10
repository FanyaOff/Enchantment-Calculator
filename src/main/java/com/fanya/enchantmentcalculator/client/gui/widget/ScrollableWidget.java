package com.fanya.enchantmentcalculator.client.gui.widget;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.Drawable;
import net.minecraft.client.gui.Element;
import net.minecraft.client.gui.Selectable;
import net.minecraft.client.gui.widget.ClickableWidget;
import net.minecraft.client.gui.screen.narration.NarrationMessageBuilder;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.util.math.MathHelper;
import net.minecraft.text.Text;
import net.minecraft.client.MinecraftClient;

import java.util.ArrayList;
import java.util.List;

public class ScrollableWidget extends ClickableWidget {
    private final List<Element> children = new ArrayList<>();
    private final List<Drawable> drawables = new ArrayList<>();
    private final List<TextEntry> textEntries = new ArrayList<>();
    private final Text title;
    private int scrollOffset = 0;
    private int maxScrollOffset = 0;
    private static final int SCROLL_SPEED = 10;
    private final TextRenderer textRenderer;

    public ScrollableWidget(int x, int y, int width, int height, Text title) {
        super(x, y, width, height, title);
        this.title = title;
        this.textRenderer = MinecraftClient.getInstance().textRenderer;
    }

    public void addChild(Element child) {
        children.add(child);
        if (child instanceof Drawable drawable) {
            drawables.add(drawable);
        }
        updateMaxScroll();
    }

    public void addText(int x, int y, Text text) {
        textEntries.add(new TextEntry(x, y, text));
        updateMaxScroll();
    }

    public void clearChildren() {
        children.clear();
        drawables.clear();
        textEntries.clear();
        scrollOffset = 0;
        maxScrollOffset = 0;
    }

    private void updateMaxScroll() {
        int totalHeight = 0;

        // Подсчитываем высоту всех элементов
        for (Element child : children) {
            if (child instanceof ClickableWidget widget) {
                totalHeight += widget.getHeight() + 2; // добавляем небольшой отступ
            }
        }

        // Добавляем высоту текстовых элементов
        totalHeight += textEntries.size() * 12; // примерная высота строки текста

        maxScrollOffset = Math.max(0, totalHeight - (this.height - 40)); // -40 для заголовка и отступов
    }

    @Override
    public void renderWidget(DrawContext context, int mouseX, int mouseY, float delta) {
        // Фон
        context.fill(this.getX(), this.getY(), this.getX() + this.width,
                this.getY() + this.height, 0x80000000);

        // Рамка
        context.drawBorder(this.getX(), this.getY(), this.width, this.height, 0xFFFFFFFF);

        // Заголовок
        context.drawTextWithShadow(this.textRenderer,
                title, this.getX() + 5, this.getY() + 5, 0xFFFFFF);

        // Включаем обрезку для содержимого
        context.enableScissor(this.getX() + 2, this.getY() + 20,
                this.getX() + this.width - 2, this.getY() + this.height - 2);

        // Отрисовываем содержимое с учетом прокрутки
        context.getMatrices().push();
        context.getMatrices().translate(0, -scrollOffset, 0);

        // Отрисовываем drawable элементы
        for (Drawable drawable : drawables) {
            drawable.render(context, mouseX, mouseY + scrollOffset, delta);
        }

        // Отрисовываем текстовые элементы
        for (TextEntry entry : textEntries) {
            int textY = this.getY() + 20 + entry.y;
            if (textY >= this.getY() + 20 - 20 && textY <= this.getY() + this.height + 20) {
                context.drawTextWithShadow(this.textRenderer,
                        entry.text, this.getX() + entry.x, textY, 0xFFFFFF);
            }
        }

        context.getMatrices().pop();
        context.disableScissor();

        // Отрисовываем полосу прокрутки если нужно
        if (maxScrollOffset > 0) {
            drawScrollbar(context);
        }
    }

    private void drawScrollbar(DrawContext context) {
        int scrollbarX = this.getX() + this.width - 6;
        int scrollbarY = this.getY() + 20;
        int scrollbarHeight = this.height - 22;

        // Фон полосы прокрутки
        context.fill(scrollbarX, scrollbarY, scrollbarX + 4, scrollbarY + scrollbarHeight, 0x80000000);

        // Ползунок
        int knobHeight = Math.max(10, scrollbarHeight * scrollbarHeight / (scrollbarHeight + maxScrollOffset));
        int knobY = scrollbarY + (maxScrollOffset > 0 ? (scrollOffset * (scrollbarHeight - knobHeight) / maxScrollOffset) : 0);

        context.fill(scrollbarX, knobY, scrollbarX + 4, knobY + knobHeight, 0xFFFFFFFF);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        if (this.isMouseOver(mouseX, mouseY)) {
            scrollOffset = Math.max(0, Math.min(maxScrollOffset,
                    (int) (scrollOffset - verticalAmount * SCROLL_SPEED)));
            return true;
        }
        return false;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        // Передаем клики дочерним элементам с учетом прокрутки
        for (Element child : children) {
            if (child instanceof ClickableWidget widget) {
                if (widget.mouseClicked(mouseX, mouseY + scrollOffset, button)) {
                    return true;
                }
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    public List<? extends Element> children() {
        return children;
    }

    @Override
    protected void appendClickableNarrations(NarrationMessageBuilder builder) {
        this.appendDefaultNarrations(builder);
    }

    private static class TextEntry {
        final int x, y;
        final Text text;

        TextEntry(int x, int y, Text text) {
            this.x = x;
            this.y = y;
            this.text = text;
        }
    }
}
