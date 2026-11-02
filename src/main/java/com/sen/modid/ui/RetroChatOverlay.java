package com.sen.modid.ui;

import com.sen.modid.config.RetroChatConfig;
import com.sen.modid.manager.ConversationManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;

import java.util.*;

public class RetroChatOverlay {
    private static RetroChatOverlay instance;

    private final Map<String, ChatWindowWidget> windowWidgets = new LinkedHashMap<>();
    private final PlayerSidebarWidget sidebarWidget;

    public static RetroChatOverlay getInstance() {
        if (instance == null) {
            instance = new RetroChatOverlay();
        }
        return instance;
    }

    private RetroChatOverlay() {
        this.sidebarWidget = new PlayerSidebarWidget(this::openConfigScreen);

        ConversationManager.getInstance().addListener(new ConversationManager.ConversationListener() {
            @Override
            public void onMessageReceived(com.sen.modid.model.DirectMessage message) {}

            @Override
            public void onConversationUpdated(com.sen.modid.model.Conversation conversation) {}

            @Override
            public void onWindowStateChanged(String playerName, ConversationManager.WindowState state) {
                if (state != ConversationManager.WindowState.CLOSED) {
                    getOrCreateWindow(playerName);
                }
            }
        });
    }

    public PlayerSidebarWidget getSidebarWidget() {
        return sidebarWidget;
    }

    public synchronized void clearWindows() {
        windowWidgets.clear();
    }

    public synchronized ChatWindowWidget getOrCreateWindow(String playerName) {
        String key = playerName.toLowerCase();
        ChatWindowWidget widget = windowWidgets.get(key);
        if (widget == null) {
            widget = new ChatWindowWidget(playerName, null);
            windowWidgets.put(key, widget);
        }
        return widget;
    }

    public void openConfigScreen() {
        Minecraft client = Minecraft.getInstance();
        if (client != null) {
            client.setScreen(new RetroChatConfigScreen(client.screen));
        }
    }

    public void render(GuiGraphics context, int mouseX, int mouseY, float delta) {
        Minecraft client = Minecraft.getInstance();
        if (client.level == null || client.options.hideGui) return;

        ConversationManager.getInstance().updateServerScope();

        float scale = RetroChatConfig.getInstance().uiScale;
        if (scale <= 0.1f) scale = 1.0f;

        int rawScreenWidth = client.getWindow().getGuiScaledWidth();
        int rawScreenHeight = client.getWindow().getGuiScaledHeight();

        int scaledWidth = (int) (rawScreenWidth / scale);
        int scaledHeight = (int) (rawScreenHeight / scale);

        int scaledMouseX = (int) (mouseX / scale);
        int scaledMouseY = (int) (mouseY / scale);

        context.pose().pushPose();
        context.pose().scale(scale, scale, 1.0f);

        sidebarWidget.updatePosition(scaledWidth, scaledHeight);
        sidebarWidget.render(context, scaledMouseX, scaledMouseY, delta);

        layoutAndRenderWindows(context, scaledWidth, scaledHeight, scaledMouseX, scaledMouseY, delta);

        context.pose().popPose();
    }

    private void layoutAndRenderWindows(GuiGraphics context, int screenWidth, int screenHeight, int mouseX, int mouseY, float delta) {
        ConversationManager cm = ConversationManager.getInstance();
        Map<String, ConversationManager.WindowState> activeStates = cm.getActiveWindowStates();

        int sidebarOccupiedWidth = sidebarWidget.isCollapsed() ? 24 : 135;
        int availableWidth = screenWidth - sidebarOccupiedWidth - 8;

        List<ChatWindowWidget> activeWindows = new ArrayList<>();
        for (Map.Entry<String, ConversationManager.WindowState> entry : activeStates.entrySet()) {
            if (entry.getValue() != ConversationManager.WindowState.CLOSED) {
                activeWindows.add(getOrCreateWindow(entry.getKey()));
            }
        }

        if (activeWindows.isEmpty()) return;

        List<ChatWindowWidget> openCandidates = new ArrayList<>();
        List<ChatWindowWidget> minimizedCandidates = new ArrayList<>();

        for (ChatWindowWidget w : activeWindows) {
            ConversationManager.WindowState state = cm.getWindowState(w.getPlayerName());
            if (state == ConversationManager.WindowState.OPEN) {
                openCandidates.add(w);
            } else {
                minimizedCandidates.add(w);
            }
        }

        openCandidates.sort((a, b) -> {
            if (a.isFocused() != b.isFocused()) return a.isFocused() ? -1 : 1;
            boolean pinA = cm.isWindowPinned(a.getPlayerName());
            boolean pinB = cm.isWindowPinned(b.getPlayerName());
            if (pinA != pinB) return pinA ? -1 : 1;
            return 0;
        });

        List<ChatWindowWidget> finalOpenWindows = new ArrayList<>();
        int currentUsedWidth = 0;

        for (ChatWindowWidget w : openCandidates) {
            w.updateDimensions();
            int wWidth = w.getWidth();
            int neededForTabs = Math.min(minimizedCandidates.size(), 3) * 32;
            if (finalOpenWindows.isEmpty() || (currentUsedWidth + wWidth + neededForTabs <= availableWidth)) {
                finalOpenWindows.add(w);
                currentUsedWidth += wWidth + 4;
            } else {
                minimizedCandidates.add(w);
            }
        }

        int numTabs = minimizedCandidates.size();
        int spaceLeftForTabs = Math.max(0, availableWidth - currentUsedWidth);
        boolean compact = RetroChatConfig.getInstance().compactTabs;
        int maxTabWidth = compact ? 22 : 100;
        int tabWidth = maxTabWidth;
        if (numTabs > 0) {
            tabWidth = Math.max(22, Math.min(maxTabWidth, (spaceLeftForTabs - (numTabs * 4)) / numTabs));
        }

        int currentRightX = screenWidth - sidebarOccupiedWidth - 4;

        int hotbarLeft = screenWidth / 2 - 120;
        int hotbarRight = screenWidth / 2 + 95;
        int hotbarClearance = 26;

        for (ChatWindowWidget window : finalOpenWindows) {
            int wWidth = window.getWidth();
            int wHeight = window.getHeight();
            int windowX = currentRightX - wWidth;
            boolean overlapsHotbar = windowX < hotbarRight && (windowX + wWidth) > hotbarLeft;
            int windowY = overlapsHotbar ? (screenHeight - wHeight - hotbarClearance) : (screenHeight - wHeight);

            window.setPosition(windowX, windowY);
            currentRightX = windowX - 4;
        }

        for (int i = finalOpenWindows.size() - 1; i >= 0; i--) {
            finalOpenWindows.get(i).render(context, mouseX, mouseY, delta);
        }

        int tabY = screenHeight - 18;
        for (ChatWindowWidget tabWindow : minimizedCandidates) {
            int windowX = currentRightX - tabWidth;

            if (windowX >= 2) {
                tabWindow.setPosition(windowX, tabY);
                tabWindow.setTabWidth(tabWidth);
                tabWindow.renderAsTab(context, mouseX, mouseY, delta, tabWidth);
                currentRightX = windowX - 4;
            }
        }
    }

    public boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
        if (Minecraft.getInstance().level == null) return false;
        float scale = RetroChatConfig.getInstance().uiScale;
        double scaledMouseX = mouseX / scale;
        double scaledMouseY = mouseY / scale;

        return sidebarWidget.mouseDragged(scaledMouseX, scaledMouseY, button, deltaX, deltaY);
    }

    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (Minecraft.getInstance().level == null) return false;
        float scale = RetroChatConfig.getInstance().uiScale;
        double scaledMouseX = mouseX / scale;
        double scaledMouseY = mouseY / scale;

        return sidebarWidget.mouseReleased(scaledMouseX, scaledMouseY, button);
    }

    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (Minecraft.getInstance().level == null) return false;
        float scale = RetroChatConfig.getInstance().uiScale;
        double scaledMouseX = mouseX / scale;
        double scaledMouseY = mouseY / scale;

        if (sidebarWidget.mouseClicked(scaledMouseX, scaledMouseY, button)) {
            return true;
        }

        for (ChatWindowWidget window : windowWidgets.values()) {
            if (window.mouseClicked(scaledMouseX, scaledMouseY, button)) {
                for (ChatWindowWidget other : windowWidgets.values()) {
                    if (other != window) {
                        other.setFocused(false);
                    }
                }
                return true;
            }
        }

        return false;
    }

    public boolean isAnyInputFocused() {
        if (Minecraft.getInstance().level == null) return false;
        if (sidebarWidget.isSearchFocused()) return true;
        for (ChatWindowWidget window : windowWidgets.values()) {
            if (window.isFocused() || window.isSearchFocused()) return true;
        }
        return false;
    }

    public void unfocusAll() {
        for (ChatWindowWidget window : windowWidgets.values()) {
            window.setFocused(false);
        }
        sidebarWidget.unfocusSearch();
    }

    public void focusFirstOpenWindow() {
        if (Minecraft.getInstance().level == null) return;
        ConversationManager cm = ConversationManager.getInstance();
        for (ChatWindowWidget window : windowWidgets.values()) {
            if (cm.getWindowState(window.getPlayerName()) == ConversationManager.WindowState.OPEN) {
                window.focusInput();
                return;
            }
        }
    }

    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        if (Minecraft.getInstance().level == null) return false;
        float scale = RetroChatConfig.getInstance().uiScale;
        double scaledMouseX = mouseX / scale;
        double scaledMouseY = mouseY / scale;

        if (sidebarWidget.mouseScrolled(scaledMouseX, scaledMouseY, horizontalAmount, verticalAmount)) {
            return true;
        }

        for (ChatWindowWidget window : windowWidgets.values()) {
            if (window.mouseScrolled(scaledMouseX, scaledMouseY, horizontalAmount, verticalAmount)) {
                return true;
            }
        }

        return false;
    }

    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (Minecraft.getInstance().level == null) return false;
        if (sidebarWidget.keyPressed(keyCode, scanCode, modifiers)) {
            return true;
        }

        for (ChatWindowWidget window : windowWidgets.values()) {
            if (window.keyPressed(keyCode, scanCode, modifiers)) {
                return true;
            }
        }

        return false;
    }

    public boolean charTyped(char chr, int modifiers) {
        if (Minecraft.getInstance().level == null) return false;
        if (sidebarWidget.charTyped(chr, modifiers)) {
            return true;
        }

        for (ChatWindowWidget window : windowWidgets.values()) {
            if (window.charTyped(chr, modifiers)) {
                return true;
            }
        }

        return false;
    }
}
