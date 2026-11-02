package com.sen.modid.ui;

import com.sen.modid.config.RetroChatConfig;

import com.sen.modid.lang.LanguageManager;
import com.sen.modid.manager.ConversationManager;
import com.sen.modid.model.Conversation;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.client.util.SkinTextures;
import net.minecraft.text.Text;

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
    private TextFieldWidget searchField;
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
        MinecraftClient client = MinecraftClient.getInstance();
        TextRenderer tr = client.textRenderer;

        searchField = new TextFieldWidget(tr, 0, 0, SIDEBAR_WIDTH - 8, SEARCH_HEIGHT, Text.literal(""));
        searchField.setPlaceholder(Text.literal(LanguageManager.get("sidebar.search")));
        searchField.setDrawsBackground(true);
        searchField.setMaxLength(32);
        searchField.setFocusUnlocked(true);
        searchField.setChangedListener(query -> {
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

    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        MinecraftClient client = MinecraftClient.getInstance();
        TextRenderer tr = client.textRenderer;

        if (collapsed) {
            renderCollapsedTab(context, tr, mouseX, mouseY);
            return;
        }

        GuiUtils.drawBeveledBox(context, x, y, width, height, GuiUtils.WINDOW_BG, GuiUtils.WINDOW_BORDER_LIGHT, GuiUtils.WINDOW_BORDER_DARK);
        GuiUtils.drawWindowHeader(context, x + 1, y + 1, width - 2, HEADER_HEIGHT, true);

        context.drawText(tr, LanguageManager.get("sidebar.title"), x + 5, y + 5, 0xFFFFFFFF, true);

        int gearX = x + width - 30;
        int btnY = y + 3;
        boolean gearHovered = mouseX >= gearX && mouseX <= gearX + 12 && mouseY >= btnY && mouseY <= btnY + 12;
        GuiUtils.drawSettingsButton(context, gearX, btnY, 12, 12, gearHovered);

        int collapseX = x + width - 15;
        boolean colHovered = mouseX >= collapseX && mouseX <= collapseX + 12 && mouseY >= btnY && mouseY <= btnY + 12;
        GuiUtils.drawChevronButton(context, collapseX, btnY, 12, 12, false, colHovered);

        if (searchField != null) {
            searchField.render(context, mouseX, mouseY, delta);
        }

        int listY = y + HEADER_HEIGHT + SEARCH_HEIGHT + 6;
        int listHeight = height - (HEADER_HEIGHT + SEARCH_HEIGHT + 10);
        renderPlayerList(context, tr, x + 2, listY, width - 4, listHeight, mouseX, mouseY);
    }

    private void renderCollapsedTab(DrawContext context, TextRenderer tr, int mouseX, int mouseY) {
        boolean hovered = mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + height;
        int bg = hovered ? 0xFF2A5738 : 0xFF183321;
        int topBorder = hovered ? 0xFF438A58 : 0xFF285437;
        int bottomBorder = hovered ? 0xFF0F2417 : 0xFF0D1D13;

        GuiUtils.drawBeveledBox(context, x, y, width, height, bg, topBorder, bottomBorder);

        if (hovered) {
            context.fill(x, y, x + width, y + 1, GuiUtils.ORANGE_ACCENT);
        }

        GuiUtils.drawChevronButton(context, x + 4, y + 7, 14, 14, true, hovered);

        int totalUnread = 0;
        for (Conversation c : ConversationManager.getInstance().getConversationsMru()) {
            totalUnread += c.getUnreadCount();
        }
        if (totalUnread > 0) {
            GuiUtils.drawBadge(context, tr, x - 4, y - 4, totalUnread);
        }
    }

    private void renderPlayerList(DrawContext context, TextRenderer tr, int listX, int listY, int listWidth, int listHeight, int mouseX, int mouseY) {
        GuiUtils.drawBeveledBox(context, listX, listY, listWidth, listHeight, GuiUtils.INNER_BOX_BG, GuiUtils.INNER_BOX_LIGHT, GuiUtils.INNER_BOX_DARK);

        List<PlayerEntryData> entries = getSortedPlayerEntries();

        int totalHeight = entries.size() * ENTRY_HEIGHT;
        maxScroll = Math.max(0, totalHeight - (listHeight - 4));
        scrollOffset = Math.max(0, Math.min(scrollOffset, maxScroll));

        int scrollbarWidth = 6;
        int contentWidth = maxScroll > 0 ? (listWidth - scrollbarWidth - 4) : (listWidth - 2);

        context.enableScissor(listX + 1, listY + 1, listX + contentWidth, listY + listHeight - 1);

        int currentY = listY + 2 - scrollOffset;
        for (PlayerEntryData entry : entries) {
            if (currentY + ENTRY_HEIGHT >= listY && currentY <= listY + listHeight) {
                boolean itemHovered = mouseX >= listX + 1 && mouseX <= listX + contentWidth && mouseY >= currentY && mouseY <= currentY + ENTRY_HEIGHT;
                if (itemHovered) {
                    context.fill(listX + 1, currentY, listX + contentWidth, currentY + ENTRY_HEIGHT, 0x552D5A3C);
                    context.fill(listX + 1, currentY, listX + 3, currentY + ENTRY_HEIGHT, GuiUtils.ORANGE_ACCENT);
                }

                GuiUtils.drawPlayerHead(context, entry.skinTextures, listX + 4, currentY + 4, 10);

                int dotColor = entry.isOnline ? GuiUtils.ONLINE_DOT : GuiUtils.OFFLINE_DOT;
                context.fill(listX + 12, currentY + 12, listX + 14, currentY + 14, dotColor);

                int nameColor = entry.isOnline ? 0xFFFFFFFF : 0xFF8FA395;
                int maxNameWidth = maxScroll > 0 ? (contentWidth - 40) : (listWidth - 45);
                String trimmedName = tr.trimToWidth(entry.playerName, Math.max(10, maxNameWidth));
                context.drawText(tr, trimmedName, listX + 18, currentY + 5, nameColor, true);

                if (entry.unreadCount > 0) {
                    int badgeX = listX + contentWidth - 20;
                    GuiUtils.drawBadge(context, tr, badgeX, currentY + 4, entry.unreadCount);
                }
            }
            currentY += ENTRY_HEIGHT;
        }

        context.disableScissor();

        if (maxScroll > 0) {
            int trackX = listX + listWidth - scrollbarWidth - 2;
            int trackY = listY + 2;
            int trackHeight = listHeight - 4;
            int thumbHeight = Math.max(14, (int) ((float) trackHeight / totalHeight * trackHeight));
            int thumbY = trackY + (int) ((float) scrollOffset / maxScroll * (trackHeight - thumbHeight));

            context.fill(trackX, trackY, trackX + scrollbarWidth, trackY + trackHeight, 0x60000000);
            context.drawBorder(trackX, trackY, scrollbarWidth, trackHeight, 0x25FFFFFF);

            boolean hoveredThumb = mouseX >= trackX - 1 && mouseX <= trackX + scrollbarWidth + 1 && mouseY >= thumbY && mouseY <= thumbY + thumbHeight;
            int thumbBg = (isDraggingScrollbar || hoveredThumb) ? GuiUtils.ORANGE_ACCENT : 0xFF2A5738;
            int thumbLight = (isDraggingScrollbar || hoveredThumb) ? GuiUtils.ORANGE_ACCENT_HOVER : 0xFF438A58;
            int thumbDark = (isDraggingScrollbar || hoveredThumb) ? GuiUtils.ORANGE_BORDER : 0xFF0F2417;

            GuiUtils.drawBeveledBox(context, trackX, thumbY, scrollbarWidth, thumbHeight, thumbBg, thumbLight, thumbDark);
        }
    }

    private List<PlayerEntryData> getSortedPlayerEntries() {
        MinecraftClient client = MinecraftClient.getInstance();
        ConversationManager cm = ConversationManager.getInstance();
        com.sen.modid.config.RetroChatConfig config = com.sen.modid.config.RetroChatConfig.getInstance();

        Map<String, PlayerEntryData> entryMap = new LinkedHashMap<>();

        String localPlayer = cm.getLocalPlayerName();

        // recent conversations excluding blocked players
        for (Conversation conv : cm.getConversationsMru()) {
            String name = conv.getPlayerName();
            if (name.equalsIgnoreCase(localPlayer)) continue;
            if (client.player != null && name.equalsIgnoreCase(client.player.getName().getString())) continue;
            if (config.isPlayerBlocked(name)) continue;
            if (!filterQuery.isEmpty() && !name.toLowerCase().contains(filterQuery)) continue;

            PlayerListEntry ple = (client.getNetworkHandler() != null) ? client.getNetworkHandler().getPlayerListEntry(name) : null;
            boolean online = ple != null;
            SkinTextures skin = (ple != null && ple.getSkinTextures() != null) ? ple.getSkinTextures() : GuiUtils.getPlayerSkin(conv.getPlayerUuid(), name);

            entryMap.put(name.toLowerCase(), new PlayerEntryData(name, conv.getPlayerUuid(), skin, online, conv.getUnreadCount(), conv.getLastMessageTimestamp()));
        }

        // remaining online players
        if (client.getNetworkHandler() != null) {
            List<PlayerListEntry> onlineList = new ArrayList<>(client.getNetworkHandler().getPlayerList());
            onlineList.sort(Comparator.comparing(a -> a.getProfile().getName().toLowerCase()));

            for (PlayerListEntry ple : onlineList) {
                String name = ple.getProfile().getName();
                if (name.equalsIgnoreCase(localPlayer)) continue;
                if (client.player != null && name.equalsIgnoreCase(client.player.getName().getString())) {
                    continue; // skip self
                }
                if (config.isPlayerBlocked(name)) continue;
                if (!filterQuery.isEmpty() && !name.toLowerCase().contains(filterQuery)) continue;

                if (!entryMap.containsKey(name.toLowerCase())) {
                    entryMap.put(name.toLowerCase(), new PlayerEntryData(name, ple.getProfile().getId(), ple.getSkinTextures(), true, 0, 0));
                }
            }
        }

        return new ArrayList<>(entryMap.values());
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
            if (mouseY >= y && mouseY <= y + HEADER_HEIGHT) {
                int gearX = x + width - 30;
                int collapseX = x + width - 15;

                if (mouseX >= gearX && mouseX <= gearX + 12) {
                    GuiUtils.playClickSound();
                    if (onOpenConfig != null) {
                        onOpenConfig.run();
                    }
                    return true;
                }

                if (mouseX >= collapseX && mouseX <= collapseX + 12) {
                    GuiUtils.playClickSound();
                    setCollapsed(true);
                    return true;
                }
            }

            if (searchField != null) {
                boolean hit = mouseX >= searchField.getX() && mouseX <= searchField.getX() + searchField.getWidth()
                    && mouseY >= searchField.getY() && mouseY <= searchField.getY() + searchField.getHeight();
                searchField.setFocused(hit);
                if (hit) {
                    searchField.mouseClicked(mouseX, mouseY, button);
                }
            }

            int listY = y + HEADER_HEIGHT + SEARCH_HEIGHT + 6;
            int listHeight = height - (HEADER_HEIGHT + SEARCH_HEIGHT + 10);
            if (mouseY >= listY && mouseY <= listY + listHeight) {
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
            return searchField.keyPressed(keyCode, scanCode, modifiers);
        }
        return false;
    }

    public boolean charTyped(char chr, int modifiers) {
        if (!collapsed && searchField != null && searchField.isFocused()) {
            return searchField.charTyped(chr, modifiers);
        }
        return false;
    }

    public boolean isSearchFocused() {
        return !collapsed && searchField != null && searchField.isFocused();
    }

    private static class PlayerEntryData {
        final String playerName;
        final UUID playerUuid;
        final SkinTextures skinTextures;
        final boolean isOnline;
        final int unreadCount;
        final long lastMessageTimestamp;

        PlayerEntryData(String playerName, UUID playerUuid, SkinTextures skinTextures, boolean isOnline, int unreadCount, long lastMessageTimestamp) {
            this.playerName = playerName;
            this.playerUuid = playerUuid;
            this.skinTextures = skinTextures;
            this.isOnline = isOnline;
            this.unreadCount = unreadCount;
            this.lastMessageTimestamp = lastMessageTimestamp;
        }
    }
}

