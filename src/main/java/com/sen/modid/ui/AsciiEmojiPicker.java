package com.sen.modid.ui;

import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.util.math.MathHelper;

import java.util.List;
import java.util.function.Consumer;

public class AsciiEmojiPicker {
    private static final String[] CATEGORIES = {"Classic", "Kaomoji", "Action"};

    private static final List<String> CLASSIC = List.of(
            ":)", ":-)", ":D", ":-D", ";)", ";-)",
            ":P", ":-P", "xD", "XD", "<3", "</3",
            "B)", "8)", ":3", ":/", ":(", ":'(",
            ":O", ":-O", "*_*", "-_-", ">_<", "\\o/"
    );

    private static final List<String> KAOMOJI = List.of(
            "¯\\_(ツ)_/¯", "( ͡° ͜ʖ ͡°)", "ಠ_ಠ", "ʕ•ᴥ•ʔ",
            "(づ｡◕‿◕｡)づ", "(>‿◠)✌", "(ง'̀-'́)ง", "(▀̿Ĺ̯▀̿ ̿)",
            "(•‿•)", "(•_•)", "(¬_¬)", "(｡◕‿◕｡)",
            "(⊙_⊙)", "(•ω•)", "(T_T)", "ಥ_ಥ",
            "(°ロ°)", "(•ิ_•ิ)", "(◕‿◕)", "(=^･^=)"
    );

    private static final List<String> ACTION = List.of(
            "(╯°□°)╯︵ ┻━┻", "┬─┬ノ(º_ºノ)", "(ﾉ◕ヮ◕)ﾉ*:･ﾟ✧", "＼(＾O＾)／",
            "(づ￣ ³￣)づ", "(ノಠ益ಠ)ノ", "¯\\(°_o)/¯", "(*^▽^*)",
            "ლ(ಠ益ಠლ)", "ᕙ(⇀‸↼‶)ᕗ", "(ง︡'-'︠)ง", "(づ｡◕‿‿◕｡)づ"
    );

    private int selectedCategory = 0;
    private int scrollOffset = 0;

    public void render(DrawContext context, TextRenderer tr, int mouseX, int mouseY, int x, int y, int width, int height) {
        // Draw solid opaque panel background and border
        context.fill(x, y, x + width, y + height, 0xFF121A14);
        GuiUtils.drawBeveledBox(context, x, y, width, height, 0xFF121A14, GuiUtils.WINDOW_BORDER_LIGHT, GuiUtils.WINDOW_BORDER_DARK);
        context.fill(x + 1, y + 15, x + width - 1, y + 28, 0xFF18241D);

        // Header
        GuiUtils.drawWindowHeader(context, x + 1, y + 1, width - 2, 13, true);
        context.drawText(tr, "ASCII Emojis", x + 4, y + 3, 0xFFFFFFFF, true);

        // Close button
        int closeX = x + width - 12;
        int closeY = y + 2;
        boolean closeHover = mouseX >= closeX && mouseX <= closeX + 9 && mouseY >= closeY && mouseY <= closeY + 9;
        GuiUtils.drawCloseButton(context, closeX, closeY, 9, 9, closeHover);

        // Category tabs
        int tabY = y + 15;
        int tabH = 13;
        int tabW = (width - 6) / CATEGORIES.length;
        for (int i = 0; i < CATEGORIES.length; i++) {
            int tx = x + 3 + i * tabW;
            boolean active = (i == selectedCategory);
            boolean tabHover = mouseX >= tx && mouseX <= tx + tabW - 1 && mouseY >= tabY && mouseY <= tabY + tabH;
            int bg = active ? 0xFF4A6B56 : (tabHover ? 0xFF3D5846 : 0xFF2A3C30);
            context.fill(tx, tabY, tx + tabW - 1, tabY + tabH, bg);
            if (active) {
                context.fill(tx, tabY + tabH - 1, tx + tabW - 1, tabY + tabH, 0xFF8AE0A4);
            }
            int textW = tr.getWidth(CATEGORIES[i]);
            context.drawText(tr, CATEGORIES[i], tx + (tabW - 1 - textW) / 2, tabY + 3, active ? 0xFFFFFFFF : 0xFFAAAAAA, false);
        }

        // Emoji grid area
        int gridX = x + 4;
        int gridY = y + 30;
        int gridW = width - 8;
        int gridH = height - 34;

        context.fill(gridX, gridY, gridX + gridW, gridY + gridH, 0xFF0A100C);
        GuiUtils.drawBeveledBox(context, gridX, gridY, gridW, gridH, 0xFF0A100C, GuiUtils.INNER_BOX_LIGHT, GuiUtils.INNER_BOX_DARK);

        List<String> list = getList(selectedCategory);
        int cols = selectedCategory == 0 ? 4 : (selectedCategory == 1 ? 2 : 1);
        int itemH = 15;
        int gap = 2;
        int itemW = (gridW - 6 - (cols - 1) * gap) / cols;

        int totalRows = (int) Math.ceil((double) list.size() / cols);
        int contentHeight = totalRows * (itemH + gap);
        int maxScroll = Math.max(0, contentHeight - (gridH - 4));
        scrollOffset = MathHelper.clamp(scrollOffset, 0, maxScroll);

        context.enableScissor(gridX + 1, gridY + 1, gridX + gridW - 1, gridY + gridH - 1);

        for (int i = 0; i < list.size(); i++) {
            int col = i % cols;
            int row = i / cols;

            int ix = gridX + 3 + col * (itemW + gap);
            int iy = gridY + 3 + row * (itemH + gap) - scrollOffset;

            if (iy + itemH < gridY || iy > gridY + gridH) continue;

            String emoji = list.get(i);
            boolean itemHover = mouseX >= ix && mouseX <= ix + itemW && mouseY >= iy && mouseY <= iy + itemH &&
                    mouseY >= gridY && mouseY <= gridY + gridH;

            GuiUtils.drawButtonBox(context, ix, iy, itemW, itemH, itemHover);

            int textW = tr.getWidth(emoji);
            int tx = ix + (itemW - textW) / 2;
            int ty = iy + (itemH - tr.fontHeight) / 2 + 1;
            int textColor = itemHover ? GuiUtils.ORANGE_ACCENT_HOVER : 0xFFFFFFFF;
            context.drawText(tr, emoji, tx, ty, textColor, false);
        }

        context.disableScissor();

        // Scrollbar if needed
        if (maxScroll > 0) {
            int sbX = gridX + gridW - 4;
            int sbY = gridY + 2;
            int sbH = gridH - 4;
            int thumbH = Math.max(8, (sbH * (gridH - 4)) / contentHeight);
            int thumbY = sbY + (scrollOffset * (sbH - thumbH)) / maxScroll;
            context.fill(sbX, sbY, sbX + 2, sbY + sbH, 0x55000000);
            context.fill(sbX, thumbY, sbX + 2, thumbY + thumbH, 0xFFAAAAAA);
        }
    }

    public boolean mouseClicked(double mouseX, double mouseY, int button, int x, int y, int width, int height,
                                Consumer<String> onSelectEmoji, Runnable onClose) {
        if (mouseX < x || mouseX > x + width || mouseY < y || mouseY > y + height) {
            return false;
        }

        // Close button
        int closeX = x + width - 12;
        int closeY = y + 2;
        if (mouseX >= closeX && mouseX <= closeX + 9 && mouseY >= closeY && mouseY <= closeY + 9) {
            GuiUtils.playClickSound();
            onClose.run();
            return true;
        }

        // Category tabs
        int tabY = y + 15;
        int tabH = 13;
        int tabW = (width - 6) / CATEGORIES.length;
        if (mouseY >= tabY && mouseY <= tabY + tabH) {
            for (int i = 0; i < CATEGORIES.length; i++) {
                int tx = x + 3 + i * tabW;
                if (mouseX >= tx && mouseX <= tx + tabW - 1) {
                    if (selectedCategory != i) {
                        selectedCategory = i;
                        scrollOffset = 0;
                        GuiUtils.playClickSound();
                    }
                    return true;
                }
            }
        }

        // Emoji grid area
        int gridX = x + 4;
        int gridY = y + 30;
        int gridW = width - 8;
        int gridH = height - 34;

        if (mouseX >= gridX && mouseX <= gridX + gridW && mouseY >= gridY && mouseY <= gridY + gridH) {
            List<String> list = getList(selectedCategory);
            int cols = selectedCategory == 0 ? 4 : (selectedCategory == 1 ? 2 : 1);
            int itemH = 15;
            int gap = 2;
            int itemW = (gridW - 6 - (cols - 1) * gap) / cols;

            for (int i = 0; i < list.size(); i++) {
                int col = i % cols;
                int row = i / cols;

                int ix = gridX + 3 + col * (itemW + gap);
                int iy = gridY + 3 + row * (itemH + gap) - scrollOffset;

                if (iy + itemH < gridY || iy > gridY + gridH) continue;

                if (mouseX >= ix && mouseX <= ix + itemW && mouseY >= iy && mouseY <= iy + itemH) {
                    GuiUtils.playClickSound();
                    onSelectEmoji.accept(list.get(i));
                    return true;
                }
            }
            return true;
        }

        return true;
    }

    public boolean mouseScrolled(double mouseX, double mouseY, double verticalAmount, int x, int y, int width, int height) {
        if (mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + height) {
            scrollOffset -= (int) (verticalAmount * 16);
            return true;
        }
        return false;
    }

    private List<String> getList(int category) {
        switch (category) {
            case 1: return KAOMOJI;
            case 2: return ACTION;
            default: return CLASSIC;
        }
    }
}
