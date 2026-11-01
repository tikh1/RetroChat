package com.sen.modid.ui;

import com.mojang.authlib.GameProfile;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.PlayerSkinDrawer;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.client.sound.PositionedSoundInstance;
import net.minecraft.client.util.SkinTextures;
import net.minecraft.sound.SoundEvents;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

public class GuiUtils {

    public static final int HEADER_ACTIVE_BG = 0xFF1E462E;
    public static final int HEADER_ACTIVE_LIGHT = 0xFF357A50;
    public static final int HEADER_ACTIVE_DARK = 0xFF0F2618;

    public static final int HEADER_INACTIVE_BG = 0xFF172D1E;
    public static final int HEADER_INACTIVE_LIGHT = 0xFF274D33;
    public static final int HEADER_INACTIVE_DARK = 0xFF0A160E;

    public static final int WINDOW_BG = 0xF2121A14;
    public static final int WINDOW_BORDER_LIGHT = 0xFF2D4B36;
    public static final int WINDOW_BORDER_DARK = 0xFF0A120C;

    public static final int INNER_BOX_BG = 0xF50D140F;
    public static final int INNER_BOX_LIGHT = 0xFF080D0A;
    public static final int INNER_BOX_DARK = 0xFF1E3826;

    public static final int ORANGE_ACCENT = 0xFFE07A22;
    public static final int ORANGE_ACCENT_HOVER = 0xFFF39C38;
    public static final int ORANGE_BORDER = 0xFF8A4106;

    public static final int ACCENT_GOLD = ORANGE_ACCENT;
    public static final int ACCENT_GOLD_HOVER = ORANGE_ACCENT_HOVER;
    public static final int ACCENT_AQUA = 0xFF4ADE80;
    public static final int ACCENT_RED = 0xFFFF7777;

    public static final int BADGE_BG = 0xFFE65C00;
    public static final int BADGE_BORDER = 0xFF993D00;

    public static final int ONLINE_DOT = 0xFF2ECC71;
    public static final int OFFLINE_DOT = 0xFF6B7280;

    public static void playClickSound() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client != null && client.getSoundManager() != null) {
            client.getSoundManager().play(
                PositionedSoundInstance.master(SoundEvents.UI_BUTTON_CLICK.value(), 1.0f)
            );
        }
    }

    public static void drawBeveledBox(DrawContext context, int x, int y, int width, int height, int bgColor, int lightBorder, int darkBorder) {
        context.fill(x + 1, y + 1, x + width - 1, y + height - 1, bgColor);
        context.fill(x, y, x + width - 1, y + 1, lightBorder);
        context.fill(x, y, x + 1, y + height - 1, lightBorder);
        context.fill(x + width - 1, y, x + width, y + height, darkBorder);
        context.fill(x, y + height - 1, x + width, y + height, darkBorder);
    }

    public static void drawWindowHeader(DrawContext context, int x, int y, int width, int height, boolean active) {
        int headerBg = active ? HEADER_ACTIVE_BG : HEADER_INACTIVE_BG;
        int lightBorder = active ? HEADER_ACTIVE_LIGHT : HEADER_INACTIVE_LIGHT;
        int darkBorder = active ? HEADER_ACTIVE_DARK : HEADER_INACTIVE_DARK;

        drawBeveledBox(context, x, y, width, height, headerBg, lightBorder, darkBorder);

        if (active) {
            context.fill(x + 2, y + height - 1, x + width - 2, y + height, ORANGE_ACCENT);
        }
    }

    public static void drawButtonBox(DrawContext context, int x, int y, int width, int height, boolean hovered) {
        int bg = hovered ? 0xFF2A5738 : 0xFF183321;
        int topBorder = hovered ? 0xFF438A58 : 0xFF285437;
        int bottomBorder = hovered ? 0xFF0F2417 : 0xFF0D1D13;

        drawBeveledBox(context, x, y, width, height, bg, topBorder, bottomBorder);

        if (hovered) {
            context.fill(x, y, x + width, y + 1, ORANGE_ACCENT);
        }
    }

    public static void drawMiniButton(DrawContext context, TextRenderer textRenderer, String text, int x, int y, int width, int height, boolean hovered) {
        drawButtonBox(context, x, y, width, height, hovered);
        int textWidth = textRenderer.getWidth(text);
        int textX = x + (width - textWidth) / 2;
        int textY = y + (height - 8) / 2;
        int textColor = hovered ? ORANGE_ACCENT_HOVER : 0xFFB8D5C0;
        context.drawText(textRenderer, text, textX, textY, textColor, false);
    }

    public static void drawPinButton(DrawContext context, int x, int y, int width, int height, boolean pinned, boolean hovered) {
        if (pinned) {
            int bg = hovered ? 0xFF4A3018 : 0xFF352010;
            int topBorder = hovered ? 0xFF7A4A25 : 0xFF5C3518;
            int bottomBorder = hovered ? 0xFF241408 : 0xFF1C0E05;
            drawBeveledBox(context, x, y, width, height, bg, topBorder, bottomBorder);
            context.fill(x, y, x + width, y + 1, ORANGE_ACCENT);
        } else {
            drawButtonBox(context, x, y, width, height, hovered);
        }

        int cx = x + width / 2;
        int cy = y + height / 2;

        int pinColor = pinned ? (hovered ? ORANGE_ACCENT_HOVER : ORANGE_ACCENT) : (hovered ? 0xFFFFFFFF : 0xFFB8D5C0);
        int needleColor = pinned ? 0xFFE2E8F0 : 0xFF78909C;

        context.fill(cx - 2, cy - 3, cx + 3, cy - 2, pinColor);
        context.fill(cx - 1, cy - 2, cx + 2, cy - 1, pinColor);
        context.fill(cx - 2, cy - 1, cx + 3, cy + 1, pinColor);
        context.fill(cx - 1, cy + 1, cx + 2, cy + 2, pinColor);
        context.fill(cx, cy + 2, cx + 1, cy + 4, needleColor);
    }

    public static void drawMinimizeButton(DrawContext context, int x, int y, int width, int height, boolean hovered) {
        drawButtonBox(context, x, y, width, height, hovered);

        int cx = x + width / 2;
        int cy = y + height / 2;
        int color = hovered ? 0xFFFFFFFF : 0xFFB8D5C0;

        context.fill(cx - 3, cy + 2, cx + 3, cy + 4, color);
    }

    public static void drawCloseButton(DrawContext context, int x, int y, int width, int height, boolean hovered) {
        drawButtonBox(context, x, y, width, height, hovered);

        int cx = x + width / 2;
        int cy = y + height / 2;
        int color = hovered ? 0xFFFF7777 : 0xFFB8D5C0;

        context.fill(cx - 2, cy - 2, cx - 1, cy - 1, color);
        context.fill(cx + 1, cy - 2, cx + 2, cy - 1, color);

        context.fill(cx - 1, cy - 1, cx, cy, color);
        context.fill(cx, cy - 1, cx + 1, cy, color);

        context.fill(cx, cy, cx + 1, cy + 1, color);

        context.fill(cx - 1, cy + 1, cx, cy + 2, color);
        context.fill(cx, cy + 1, cx + 1, cy + 2, color);

        context.fill(cx - 2, cy + 2, cx - 1, cy + 3, color);
        context.fill(cx + 1, cy + 2, cx + 2, cy + 3, color);
    }

    public static void drawSettingsButton(DrawContext context, int x, int y, int width, int height, boolean hovered) {
        drawButtonBox(context, x, y, width, height, hovered);

        int cx = x + width / 2;
        int cy = y + height / 2;
        int color = hovered ? ORANGE_ACCENT_HOVER : 0xFFB8D5C0;

        context.fill(cx - 2, cy - 2, cx + 2, cy + 2, color);
        context.fill(cx - 1, cy - 3, cx + 1, cy - 2, color);
        context.fill(cx - 1, cy + 2, cx + 1, cy + 3, color);
        context.fill(cx - 3, cy - 1, cx - 2, cy + 1, color);
        context.fill(cx + 2, cy - 1, cx + 3, cy + 1, color);

        int holeBg = hovered ? 0xFF2A5738 : 0xFF183321;
        context.fill(cx, cy, cx + 1, cy + 1, holeBg);
    }

    public static void drawChevronButton(DrawContext context, int x, int y, int width, int height, boolean pointLeft, boolean hovered) {
        drawButtonBox(context, x, y, width, height, hovered);

        int cx = x + width / 2;
        int cy = y + height / 2;
        int color = hovered ? ORANGE_ACCENT_HOVER : 0xFFB8D5C0;

        if (pointLeft) {
            context.fill(cx + 1, cy - 3, cx + 2, cy - 2, color);
            context.fill(cx,     cy - 2, cx + 1, cy - 1, color);
            context.fill(cx - 1, cy - 1, cx,     cy,     color);
            context.fill(cx - 2, cy,     cx - 1, cy + 1, color);
            context.fill(cx - 1, cy + 1, cx,     cy + 2, color);
            context.fill(cx,     cy + 2, cx + 1, cy + 3, color);
            context.fill(cx + 1, cy + 3, cx + 2, cy + 4, color);
        } else {
            context.fill(cx - 2, cy - 3, cx - 1, cy - 2, color);
            context.fill(cx - 1, cy - 2, cx,     cy - 1, color);
            context.fill(cx,     cy - 1, cx + 1, cy,     color);
            context.fill(cx + 1, cy,     cx + 2, cy + 1, color);
            context.fill(cx,     cy + 1, cx + 1, cy + 2, color);
            context.fill(cx - 1, cy + 2, cx,     cy + 3, color);
            context.fill(cx - 2, cy + 3, cx - 1, cy + 4, color);
        }
    }

    public static void drawBadge(DrawContext context, TextRenderer textRenderer, int x, int y, int count) {
        if (count <= 0) return;
        String text = count > 99 ? "99+" : String.valueOf(count);
        int textWidth = textRenderer.getWidth(text);
        int badgeWidth = Math.max(12, textWidth + 4);
        int badgeHeight = 10;

        context.fill(x, y, x + badgeWidth, y + badgeHeight, BADGE_BG);
        context.drawBorder(x, y, badgeWidth, badgeHeight, BADGE_BORDER);

        int textX = x + (badgeWidth - textWidth) / 2;
        int textY = y + 1;
        context.drawText(textRenderer, text, textX, textY, 0xFFFFFFFF, true);
    }

    public static void drawPlayerHead(DrawContext context, SkinTextures skinTextures, int x, int y, int size) {
        if (skinTextures != null) {
            PlayerSkinDrawer.draw(context, skinTextures, x, y, size);
        } else {
            context.fill(x, y, x + size, y + size, 0xFF355440);
            context.drawBorder(x, y, size, size, 0xFF16291E);
        }
    }

    public static SkinTextures getPlayerSkin(UUID uuid, String name) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.getNetworkHandler() != null) {
            PlayerListEntry entry = null;
            if (uuid != null) {
                entry = client.getNetworkHandler().getPlayerListEntry(uuid);
            }
            if (entry == null && name != null) {
                entry = client.getNetworkHandler().getPlayerListEntry(name);
            }
            if (entry != null) {
                return entry.getSkinTextures();
            }
        }
        if (client.player != null && name != null && name.equalsIgnoreCase(client.player.getName().getString())) {
            return client.player.getSkinTextures();
        }
        if (uuid != null && client.getSkinProvider() != null) {
            return client.getSkinProvider().getSkinTextures(new GameProfile(uuid, name != null ? name : ""));
        }
        if (name != null && client.getSkinProvider() != null) {
            UUID offlineUuid = UUID.nameUUIDFromBytes(("OfflinePlayer:" + name).getBytes(StandardCharsets.UTF_8));
            return client.getSkinProvider().getSkinTextures(new GameProfile(offlineUuid, name));
        }
        return null;
    }
}
