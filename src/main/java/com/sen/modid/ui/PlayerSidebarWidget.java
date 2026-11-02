package com.sen.modid.ui;

import com.sen.modid.config.RetroChatConfig;
import com.sen.modid.lang.LanguageManager;
import com.sen.modid.manager.ConversationManager;
import com.sen.modid.model.Conversation;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.input.MouseButtonInfo;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.world.entity.player.PlayerSkin;
import net.minecraft.network.chat.Component;

import java.util.*;

public class PlayerSidebarWidget {
    private static final int SIDEBAR_WIDTH = 135;
    private static final int HEADER_HEIGHT = 18;
    private static final int SEARCH_HEIGHT = 16;
    private static final int ENTRY_HEIGHT = 18;
    private static final int COLLAPSED_TAB_WIDTH = 22;
    private static final int COLLAPSED_TAB_HEIGHT = 28;

    private boolean collapsed = false;
    private boolean isDraggingScrollbar = false;
    private EditBox searchField;
    private String filterQuery = "";
    private int scrollOffset = 0;
    private int maxScroll = 0;

    private int x;
    private int y;
    private int width;
    private int height;

    private final Runnable onOpenConfig;

    public PlayerSidebarWidget(Runnable onOpenConfig) {
        this.onOpenConfig = onOpenConfig;
        initSearchField();
    }

    private void initSearchField() {
        Minecraft client = Minecraft.getInstance();
        Font font = client.font;

        searchField = new EditBox(font, 0, 0, SIDEBAR_WIDTH - 8, SEARCH_HEIGHT, Component.literal(""));
        searchField.setHint(Component.literal(LanguageManager.get("sidebar.search")));
        searchField.setBordered(true);
        searchField.setMaxLength(32);
        searchField.setCanLoseFocus(true);
        searchField.setResponder(query -> {
            this.filterQuery = query.trim().toLowerCase();
            this.scrollOffset = 0;
        });
    }

    public void updatePosition(int screenWidth, int screenHeight) {
        int desiredHeight = Math.min(220, Math.max(140, (int)(screenHeight * 0.55f)));
        this.height = desiredHeight;

        if (collapsed) {
            this.width = COLLAPSED_TAB_WIDTH;
            this.x = screenWidth - COLLAPSED_TAB_WIDTH;
            this.y = screenHeight - COLLAPSED_TAB_HEIGHT;
        } else {
            this.width = SIDEBAR_WIDTH;
            this.x = screenWidth - SIDEBAR_WIDTH;
            this.y = screenHeight - desiredHeight;

            if (searchField != null) {
                searchField.setX(x + 4);
                searchField.setY(y + HEADER_HEIGHT + 3);
                searchField.setWidth(SIDEBAR_WIDTH - 8);
            }
        }
    }

    public boolean isCollapsed() {
        return collapsed;
    }

    public void setCollapsed(boolean collapsed) { setCollapsed(collapsed, true); }

    public void setCollapsed(boolean collapsed, boolean saveConfig) {
        this.collapsed = collapsed;
        if (searchField != null) {
            searchField.setFocused(false);
        }
        if (saveConfig) {
            ConversationManager.getInstance().setSidebarCollapsed(collapsed);
        }
    }

    public void unfocusSearch() {
        if (searchField != null) {
            searchField.setFocused(false);
        }
    }

    public void render(GuiGraphicsExtractor context, int mouseX, int mouseY, float delta) {
        Minecraft client = Minecraft.getInstance();
        Font font = client.font;

        if (collapsed) {
            renderCollapsedTab(context, font, mouseX, mouseY);
            return;
        }

        GuiUtils.drawBeveledBox(context, x, y, width, height, GuiUtils.WINDOW_BG, GuiUtils.WINDOW_BORDER_LIGHT, GuiUtils.WINDOW_BORDER_DARK);
        GuiUtils.drawWindowHeader(context, x + 1, y + 1, width - 2, HEADER_HEIGHT, true);

        context.text(font, LanguageManager.get("sidebar.title"), x + 5, y + 5, 0xFFFFFFFF, true);

        int gearX = x + width - 30;
        int btnY = y + 3;
        boolean gearHovered = mouseX >= gearX && mouseX <= gearX + 12 && mouseY >= btnY && mouseY <= btnY + 12;
        GuiUtils.drawSettingsButton(context, gearX, btnY, 12, 12, gearHovered);

        int collapseX = x + width - 15;
        boolean colHovered = mouseX >= collapseX && mouseX <= collapseX + 12 && mouseY >= btnY && mouseY <= btnY + 12;
        GuiUtils.drawChevronButton(context, collapseX, btnY, 12, 12, false, colHovered);

        if (searchField != null) {
            searchField.extractWidgetRenderState(context, mouseX, mouseY, delta);
        }

        int listY = y + HEADER_HEIGHT + SEARCH_HEIGHT + 6;
        int listHeight = height - (HEADER_HEIGHT + SEARCH_HEIGHT + 10);
        renderPlayerList(context, font, x + 2, listY, width - 4, listHeight, mouseX, mouseY);
    }

    private void renderCollapsedTab(GuiGraphicsExtractor context, Font font, int mouseX, int mouseY) {
        boolean hovered = mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + height;
        int bg = hovered ? 0xFF2A5738 : 0xFF183321;
        int topBorder = hovered ? 0xFF438A58 : 0xFF285437;
        int bottomBorder = hovered ? 0xFF0F2417 : 0xFF0D1D13;

        GuiUtils.drawBeveledBox(context, x, y, width, height, bg, topBorder, bottomBorder);

        if (hovered) {
            context.fill(x, y, x + width, y + 1, GuiUtils.ORANGE_ACCENT);
        }

        int chevronX = x + (width - 12) / 2;
        int chevronY = y + 6;
        GuiUtils.drawChevronButton(context, chevronX, chevronY, 12, 12, true, hovered);

        int totalUnread = ConversationManager.getInstance().getTotalUnreadCount();
        if (totalUnread > 0) {
            GuiUtils.drawBadge(context, font, x + 2, y + 15, totalUnread);
        }
    }

    private void renderPlayerList(GuiGraphicsExtractor context, Font font, int listX, int listY, int listWidth, int listHeight, int mouseX, int mouseY) {
        context.fill(listX, listY, listX + listWidth, listY + listHeight, GuiUtils.INNER_BOX_BG);
        context.fill(listX, listY, listX + listWidth, listY + 1, GuiUtils.INNER_BOX_LIGHT);
        context.fill(listX, listY, listX + 1, listY + listHeight, GuiUtils.INNER_BOX_LIGHT);
        context.fill(listX + listWidth - 1, listY, listX + listWidth, listY + listHeight, GuiUtils.INNER_BOX_DARK);
        context.fill(listX, listY + listHeight - 1, listX + listWidth, listY + listHeight, GuiUtils.INNER_BOX_DARK);

        List<PlayerEntryData> entries = getSortedPlayerEntries();
        int totalHeight = entries.size() * ENTRY_HEIGHT;
        maxScroll = Math.max(0, totalHeight - listHeight);
        scrollOffset = Math.max(0, Math.min(scrollOffset, maxScroll));

        context.enableScissor(listX + 1, listY + 1, listX + listWidth - 1, listY + listHeight - 1);

        int scrollbarWidth = 6;
        int contentWidth = maxScroll > 0 ? (listWidth - scrollbarWidth - 2) : listWidth;

        int currentY = listY + 1 - scrollOffset;

        for (int i = 0; i < entries.size(); i++) {
            PlayerEntryData entry = entries.get(i);

            if (currentY + ENTRY_HEIGHT > listY && currentY < listY + listHeight) {
                boolean itemHovered = mouseX >= listX + 1 && mouseX <= listX + contentWidth &&
                        mouseY >= currentY && mouseY <= currentY + ENTRY_HEIGHT &&
                        mouseY >= listY && mouseY <= listY + listHeight;

                if (itemHovered) {
                    context.fill(listX + 1, currentY, listX + contentWidth, currentY + ENTRY_HEIGHT, 0x22FFFFFF);
                }

                int dotColor = entry.isOnline ? GuiUtils.ONLINE_DOT : GuiUtils.OFFLINE_DOT;
                int dotX = listX + 4;
                int dotY = currentY + (ENTRY_HEIGHT - 4) / 2;
                context.fill(dotX, dotY, dotX + 4, dotY + 4, dotColor);

                int headSize = 12;
                int headX = dotX + 7;
                int headY = currentY + (ENTRY_HEIGHT - headSize) / 2;
                if (RetroChatConfig.getInstance().showPlayerHeads) {
                    GuiUtils.drawPlayerHead(context, entry.skinTextures, headX, headY, headSize);
                }

                int textX = RetroChatConfig.getInstance().showPlayerHeads ? (headX + headSize + 4) : (dotX + 8);
                int textY = currentY + (ENTRY_HEIGHT - 8) / 2;
                int textColor = entry.isOnline ? 0xFFFFFFFF : 0xFF888888;

                int maxTextWidth = contentWidth - (textX - listX) - (entry.unreadCount > 0 ? 18 : 4);
                String displayName = entry.playerName;
                if (font.width(displayName) > maxTextWidth) {
                    displayName = font.plainSubstrByWidth(displayName, maxTextWidth - 6) + "..";
                }

                context.text(font, displayName, textX, textY, textColor, false);

                if (entry.unreadCount > 0) {
                    GuiUtils.drawBadge(context, font, listX + contentWidth - 16, currentY + 4, entry.unreadCount);
                }
            }

            currentY += ENTRY_HEIGHT;
        }

        context.disableScissor();

        if (maxScroll > 0) {
            int trackX = listX + listWidth - scrollbarWidth - 1;
            int trackY = listY + 1;
            int trackHeight = listHeight - 2;

            context.fill(trackX, trackY, trackX + scrollbarWidth, trackY + trackHeight, 0x33000000);

            int thumbHeight = Math.max(14, (int) ((float) trackHeight / totalHeight * trackHeight));
            int thumbY = trackY + (int) ((float) scrollOffset / maxScroll * (trackHeight - thumbHeight));

            context.fill(trackX, thumbY, trackX + scrollbarWidth, thumbY + thumbHeight, 0x88FFFFFF);
        }
    }

    private List<PlayerEntryData> getSortedPlayerEntries() {
        Minecraft client = Minecraft.getInstance();
        List<PlayerEntryData> list = new ArrayList<>();

        Set<String> processed = new HashSet<>();

        if (client.getConnection() != null) {
            for (PlayerInfo info : client.getConnection().getOnlinePlayers()) {
                String name = info.getProfile().name();
                if (client.player != null && name.equalsIgnoreCase(client.player.getName().getString())) {
                    continue;
                }

                if (!filterQuery.isEmpty() && !name.toLowerCase().contains(filterQuery)) {
                    continue;
                }

                processed.add(name.toLowerCase());
                Conversation conv = ConversationManager.getInstance().getConversation(name);
                int unread = conv != null ? conv.getUnreadCount() : 0;
                long lastMsg = conv != null ? conv.getLastMessageTimestamp() : 0;

                list.add(new PlayerEntryData(name, info.getProfile().id(), info.getSkin(), true, unread, lastMsg));
            }
        }

        for (Conversation conv : ConversationManager.getInstance().getConversationsMru()) {
            String name = conv.getPlayerName();
            if (processed.contains(name.toLowerCase())) {
                continue;
            }

            if (!filterQuery.isEmpty() && !name.toLowerCase().contains(filterQuery)) {
                continue;
            }

            PlayerSkin skin = GuiUtils.getPlayerSkin(conv.getPlayerUuid(), name);
            list.add(new PlayerEntryData(name, conv.getPlayerUuid(), skin, false, conv.getUnreadCount(), conv.getLastMessageTimestamp()));
        }

        list.sort((a, b) -> {
            if (a.unreadCount != b.unreadCount) {
                return Integer.compare(b.unreadCount, a.unreadCount);
            }
            if (a.isOnline != b.isOnline) {
                return Boolean.compare(b.isOnline, a.isOnline);
            }
            if (a.lastMessageTimestamp != b.lastMessageTimestamp) {
                return Long.compare(b.lastMessageTimestamp, a.lastMessageTimestamp);
            }
            return a.playerName.compareToIgnoreCase(b.playerName);
        });

        return list;
    }

    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (collapsed) {
            if (mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + height) {
                GuiUtils.playClickSound();
                setCollapsed(false);
                return true;
            }
            return false;
        }

        if (mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + height) {
            int gearX = x + width - 30;
            int btnY = y + 3;
            if (mouseX >= gearX && mouseX <= gearX + 12 && mouseY >= btnY && mouseY <= btnY + 12) {
                GuiUtils.playClickSound();
                if (onOpenConfig != null) {
                    onOpenConfig.run();
                }
                return true;
            }

            int collapseX = x + width - 15;
            if (mouseX >= collapseX && mouseX <= collapseX + 12 && mouseY >= btnY && mouseY <= btnY + 12) {
                GuiUtils.playClickSound();
                setCollapsed(true);
                return true;
            }

            if (searchField != null) {
                searchField.mouseClicked(new MouseButtonEvent(mouseX, mouseY, new MouseButtonInfo(button, 0)), false);
            }

            int listY = y + HEADER_HEIGHT + SEARCH_HEIGHT + 6;
            int listHeight = height - (HEADER_HEIGHT + SEARCH_HEIGHT + 10);
            if (mouseX >= x + 2 && mouseX <= x + width - 2 && mouseY >= listY && mouseY <= listY + listHeight) {
                int scrollbarWidth = 6;
                int trackX = x + 2 + (width - 4) - scrollbarWidth - 2;
                if (maxScroll > 0 && mouseX >= trackX - 2 && mouseX <= trackX + scrollbarWidth + 2) {
                    isDraggingScrollbar = true;
                    int trackY = listY + 2;
                    int trackHeight = listHeight - 4;
                    int totalHeight = getSortedPlayerEntries().size() * ENTRY_HEIGHT;
                    int thumbHeight = Math.max(14, (int) ((float) trackHeight / totalHeight * trackHeight));
                    float ratio = (float) (mouseY - trackY - thumbHeight / 2.0f) / Math.max(1, (trackHeight - thumbHeight));
                    scrollOffset = Math.max(0, Math.min(maxScroll, (int) (ratio * maxScroll)));
                    return true;
                }

                int clickedIndex = (int) ((mouseY - listY + scrollOffset) / ENTRY_HEIGHT);
                List<PlayerEntryData> entries = getSortedPlayerEntries();
                if (clickedIndex >= 0 && clickedIndex < entries.size()) {
                    GuiUtils.playClickSound();
                    PlayerEntryData selected = entries.get(clickedIndex);
                    ConversationManager.getInstance().openWindow(selected.playerName);
                    ConversationManager.getInstance().markAsRead(selected.playerName);
                    RetroChatOverlay overlay = RetroChatOverlay.getInstance();
                    overlay.unfocusAll();
                    ChatWindowWidget window = overlay.getOrCreateWindow(selected.playerName);
                    window.focusInput();
                    return true;
                }
            }

            return true;
        } else {
            if (searchField != null) {
                searchField.setFocused(false);
            }
        }
        return false;
    }

    public boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
        if (collapsed) return false;
        if (isDraggingScrollbar && maxScroll > 0) {
            int listY = y + HEADER_HEIGHT + SEARCH_HEIGHT + 6;
            int listHeight = height - (HEADER_HEIGHT + SEARCH_HEIGHT + 10);
            int trackY = listY + 2;
            int trackHeight = listHeight - 4;
            int totalHeight = getSortedPlayerEntries().size() * ENTRY_HEIGHT;
            int thumbHeight = Math.max(14, (int) ((float) trackHeight / totalHeight * trackHeight));

            float ratio = (float) (mouseY - trackY - thumbHeight / 2.0f) / Math.max(1, (trackHeight - thumbHeight));
            scrollOffset = Math.max(0, Math.min(maxScroll, (int) (ratio * maxScroll)));
            return true;
        }
        return false;
    }

    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (isDraggingScrollbar) {
            isDraggingScrollbar = false;
            return true;
        }
        return false;
    }

    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        if (collapsed) return false;
        if (mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + height) {
            if (maxScroll > 0) {
                int delta = (int) (verticalAmount * 18);
                if (delta == 0 && verticalAmount != 0) {
                    delta = verticalAmount > 0 ? 18 : -18;
                }
                scrollOffset -= delta;
                scrollOffset = Math.max(0, Math.min(scrollOffset, maxScroll));
            }
            return true;
        }
        return false;
    }

    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (!collapsed && searchField != null && searchField.isFocused()) {
            return searchField.keyPressed(new KeyEvent(keyCode, scanCode, modifiers));
        }
        return false;
    }

    public boolean charTyped(char chr, int modifiers) {
        if (!collapsed && searchField != null && searchField.isFocused()) {
            return searchField.charTyped(new CharacterEvent(chr));
        }
        return false;
    }

    public boolean isSearchFocused() {
        return !collapsed && searchField != null && searchField.isFocused();
    }

    private static class PlayerEntryData {
        final String playerName;
        final UUID playerUuid;
        final PlayerSkin skinTextures;
        final boolean isOnline;
        final int unreadCount;
        final long lastMessageTimestamp;

        PlayerEntryData(String playerName, UUID playerUuid, PlayerSkin skinTextures, boolean isOnline, int unreadCount, long lastMessageTimestamp) {
            this.playerName = playerName;
            this.playerUuid = playerUuid;
            this.skinTextures = skinTextures;
            this.isOnline = isOnline;
            this.unreadCount = unreadCount;
            this.lastMessageTimestamp = lastMessageTimestamp;
        }
    }
}
