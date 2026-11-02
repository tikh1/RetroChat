package com.sen.modid.ui;

import com.mojang.authlib.GameProfile;
import com.sen.modid.config.RetroChatConfig;
import com.sen.modid.lang.LanguageManager;
import com.sen.modid.manager.ConversationManager;
import com.sen.modid.model.Conversation;
import com.sen.modid.model.DirectMessage;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.client.util.SkinTextures;
import net.minecraft.text.StringVisitable;
import net.minecraft.text.Text;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;

public class ChatWindowWidget {
    private static final int HEADER_HEIGHT = 18;
    private static final int INPUT_HEIGHT = 16;
    private static final int CHAR_COUNTER_HEIGHT = 10;
    private static final int MAX_CHARACTERS = 256;
    private static final SimpleDateFormat TIME_FORMAT = new SimpleDateFormat("HH:mm");

    private final String playerName;
    private UUID playerUuid;

    private int x;
    private int y;
    private int width;
    private int height;
    private int currentTabWidth = 100;
    private boolean isRenderedAsTab = false;

    private TextFieldWidget inputField;
    private int scrollOffset = 0;
    private int maxScroll = 0;

    private boolean isFocused = false;
    private final AsciiEmojiPicker emojiPicker = new AsciiEmojiPicker();
    private boolean emojiPickerOpen = false;

    public ChatWindowWidget(String playerName, UUID playerUuid) {
        this.playerName = playerName;
        this.playerUuid = playerUuid;
        updateDimensions();
        initInputField();
    }

    public void updateDimensions() {
        RetroChatConfig config = RetroChatConfig.getInstance();
        this.width = Math.max(160, config.windowWidth);
        this.height = Math.max(120, config.windowHeight);
    }

    private void initInputField() {
        MinecraftClient client = MinecraftClient.getInstance();
        TextRenderer textRenderer = client.textRenderer;

        inputField = new TextFieldWidget(textRenderer, x + 4, y + height - 32, width - 8, INPUT_HEIGHT, Text.literal(""));
        inputField.setMaxLength(MAX_CHARACTERS);
        inputField.setPlaceholder(Text.literal(LanguageManager.get("chat.type_message")));
        inputField.setDrawsBackground(true);
        inputField.setFocusUnlocked(true);
    }

    public void setPosition(int x, int y) {
        this.x = x;
        this.y = y;
        if (inputField != null) {
            inputField.setX(x + 4);
            inputField.setY(y + height - 32);
            inputField.setWidth(width - 8);
        }
    }

    public int getX() { return x; }
    public int getY() { return y; }
    public int getWidth() { return width; }
    public int getHeight() { return height; }
    public String getPlayerName() { return playerName; }

    public void setTabWidth(int tabWidth) {
        this.currentTabWidth = tabWidth;
    }

    public int getTabWidth() {
        return this.currentTabWidth;
    }

    public SkinTextures getSkinTextures() {
        return GuiUtils.getPlayerSkin(playerUuid, playerName);
    }

    public SkinTextures getMySkinTextures() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player != null) {
            return client.player.getSkinTextures();
        }
        if (client.getSkinProvider() != null && client.getSession() != null) {
            return client.getSkinProvider().getSkinTextures(client.getGameProfile());
        }
        return null;
    }

    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        ConversationManager cm = ConversationManager.getInstance();
        ConversationManager.WindowState state = cm.getWindowState(playerName);
        if (state == ConversationManager.WindowState.CLOSED) return;
        this.isRenderedAsTab = false;

        if (isFocused && state == ConversationManager.WindowState.OPEN) {
            cm.markAsRead(playerName);
        }

        MinecraftClient client = MinecraftClient.getInstance();
        TextRenderer tr = client.textRenderer;
        SkinTextures skin = getSkinTextures();

        if (state == ConversationManager.WindowState.MINIMIZED || isRenderedAsTab) {
            renderMinimizedTab(context, tr, skin, mouseX, mouseY, currentTabWidth);
            return;
        }

        GuiUtils.drawBeveledBox(context, x, y, width, height, GuiUtils.WINDOW_BG, GuiUtils.WINDOW_BORDER_LIGHT, GuiUtils.WINDOW_BORDER_DARK);
        GuiUtils.drawWindowHeader(context, x + 1, y + 1, width - 2, HEADER_HEIGHT, isFocused);
        GuiUtils.drawPlayerHead(context, skin, x + 4, y + 4, 10);

        boolean isBlocked = RetroChatConfig.getInstance().isPlayerBlocked(playerName);

        String displayName = isBlocked ? playerName + " [X]" : playerName;
        int nameColor = isBlocked ? 0xFFFF7777 : 0xFFFFFFFF;
        String trimmedName = tr.trimToWidth(displayName, width - 65);
        context.drawText(tr, trimmedName, x + 18, y + 5, nameColor, true);

        int btnY = y + 3;
        int closeBtnX = x + width - 15;
        int minBtnX = x + width - 29;
        int pinBtnX = x + width - 43;

        boolean closeHovered = mouseX >= closeBtnX && mouseX <= closeBtnX + 12 && mouseY >= btnY && mouseY <= btnY + 12;
        boolean minHovered = mouseX >= minBtnX && mouseX <= minBtnX + 12 && mouseY >= btnY && mouseY <= btnY + 12;
        boolean pinHovered = mouseX >= pinBtnX && mouseX <= pinBtnX + 12 && mouseY >= btnY && mouseY <= btnY + 12;

        boolean isPinned = cm.isWindowPinned(playerName);
        GuiUtils.drawPinButton(context, pinBtnX, btnY, 12, 12, isPinned, pinHovered);
        GuiUtils.drawMinimizeButton(context, minBtnX, btnY, 12, 12, minHovered);
        GuiUtils.drawCloseButton(context, closeBtnX, btnY, 12, 12, closeHovered);

        int msgAreaX = x + 3;
        int msgAreaY = y + HEADER_HEIGHT + 2;
        int msgAreaWidth = width - 6;
        int msgAreaHeight = height - HEADER_HEIGHT - 37;

        GuiUtils.drawBeveledBox(context, msgAreaX, msgAreaY, msgAreaWidth, msgAreaHeight, GuiUtils.INNER_BOX_BG, GuiUtils.INNER_BOX_LIGHT, GuiUtils.INNER_BOX_DARK);

        renderMessages(context, tr, mouseX, mouseY, msgAreaX + 2, msgAreaY + 2, msgAreaWidth - 4, msgAreaHeight - 4);

        if (inputField != null) {
            if (isBlocked) {
                inputField.setEditable(false);
                inputField.setText("");
                inputField.setPlaceholder(Text.literal(LanguageManager.get("chat.player_blocked")).styled(s -> s.withColor(0xFFFF7777)));
            } else {
                inputField.setEditable(true);
                inputField.setPlaceholder(Text.literal(LanguageManager.get("chat.type_message")));
            }
            inputField.render(context, mouseX, mouseY, delta);
        }

        int emojiBtnX = x + width - 18;
        int emojiBtnY = y + height - 14;
        int emojiBtnW = 14;
        int emojiBtnH = 11;

        int counterY = y + height - 12;
        int currentLength = inputField != null ? inputField.getText().length() : 0;
        String counterText = currentLength + " / " + MAX_CHARACTERS;
        int counterWidth = tr.getWidth(counterText);
        int counterColor = currentLength >= MAX_CHARACTERS ? 0xFFEF4444 : (currentLength >= 230 ? GuiUtils.ORANGE_ACCENT : 0xFF7D9C86);

        int counterX = emojiBtnX - counterWidth - 5;
        context.drawText(tr, counterText, counterX, counterY, counterColor, false);

        boolean emojiHovered = mouseX >= emojiBtnX && mouseX <= emojiBtnX + emojiBtnW && mouseY >= emojiBtnY && mouseY <= emojiBtnY + emojiBtnH;
        GuiUtils.drawButtonBox(context, emojiBtnX, emojiBtnY, emojiBtnW, emojiBtnH, emojiPickerOpen || emojiHovered);
        int smileyW = tr.getWidth(":)");
        int smileyColor = (emojiPickerOpen || emojiHovered) ? GuiUtils.ORANGE_ACCENT_HOVER : 0xFFB8D5C0;
        context.drawText(tr, ":)", emojiBtnX + (emojiBtnW - smileyW) / 2, emojiBtnY + 2, smileyColor, false);

        if (emojiPickerOpen) {
            int panelW = Math.min(width - 8, 175);
            int panelH = Math.min(height - HEADER_HEIGHT - 36, 125);
            int panelX = x + width - panelW - 4;
            int panelY = y + height - 33 - panelH;
            emojiPicker.render(context, tr, mouseX, mouseY, panelX, panelY, panelW, panelH);
        }
    }

    public void renderAsTab(DrawContext context, int mouseX, int mouseY, float delta, int actualTabWidth) {
        this.currentTabWidth = actualTabWidth;
        this.isRenderedAsTab = true;
        MinecraftClient client = MinecraftClient.getInstance();
        TextRenderer tr = client.textRenderer;
        SkinTextures skin = getSkinTextures();
        renderMinimizedTab(context, tr, skin, mouseX, mouseY, actualTabWidth);
    }

    private void renderMinimizedTab(DrawContext context, TextRenderer tr, SkinTextures skin, int mouseX, int mouseY, int tabWidth) {
        this.currentTabWidth = tabWidth;
        int tabHeight = HEADER_HEIGHT;

        boolean hovered = mouseX >= x && mouseX <= x + tabWidth && mouseY >= y && mouseY <= y + tabHeight;
        Conversation conv = ConversationManager.getInstance().getConversation(playerName);
        int unread = (conv != null) ? conv.getUnreadCount() : 0;
        boolean isBlinking = unread > 0 && ((System.currentTimeMillis() / 500) % 2 == 0);
        GuiUtils.drawWindowHeader(context, x, y, tabWidth, tabHeight, hovered || isBlinking);
        if (isBlinking) {
            context.fill(x + 1, y + 1, x + tabWidth - 1, y + 2, GuiUtils.ORANGE_ACCENT_HOVER);
        }

        if (tabWidth >= 75) {
            GuiUtils.drawPlayerHead(context, skin, x + 4, y + 4, 10);

            int nameWidth = tabWidth - (unread > 0 ? 50 : 34);
            String trimmedName = tr.trimToWidth(playerName, Math.max(10, nameWidth));
            context.drawText(tr, trimmedName, x + 18, y + 5, 0xFFFFFFFF, true);

            if (unread > 0) {
                GuiUtils.drawBadge(context, tr, x + tabWidth - 36, y + 4, unread);
            }

            int closeBtnX = x + tabWidth - 15;
            int btnY = y + 3;
            boolean closeHovered = mouseX >= closeBtnX && mouseX <= closeBtnX + 12 && mouseY >= btnY && mouseY <= btnY + 12;
            GuiUtils.drawCloseButton(context, closeBtnX, btnY, 12, 12, closeHovered);
        } else if (tabWidth >= 46) {
            GuiUtils.drawPlayerHead(context, skin, x + 3, y + 4, 10);

            if (unread > 0) {
                GuiUtils.drawBadge(context, tr, x + 16, y + 4, unread);
            } else {
                String trimmedName = tr.trimToWidth(playerName, tabWidth - 30);
                context.drawText(tr, trimmedName, x + 16, y + 5, 0xFFFFFFFF, true);
            }

            int closeBtnX = x + tabWidth - 14;
            int btnY = y + 3;
            boolean closeHovered = mouseX >= closeBtnX && mouseX <= closeBtnX + 11 && mouseY >= btnY && mouseY <= btnY + 11;
            GuiUtils.drawCloseButton(context, closeBtnX, btnY, 11, 11, closeHovered);
        } else {
            int headX = x + (tabWidth - 10) / 2;
            GuiUtils.drawPlayerHead(context, skin, headX, y + 4, 10);

            if (unread > 0) {
                GuiUtils.drawBadge(context, tr, x + tabWidth - 8, y - 3, unread);
            }

            if (hovered) {
                context.drawTooltip(tr, Text.literal(playerName + (unread > 0 ? " (" + unread + ")" : "")), mouseX, mouseY);
            }
        }
    }

    private void renderMessages(DrawContext context, TextRenderer tr, int mouseX, int mouseY, int areaX, int areaY, int areaWidth, int areaHeight) {
        Conversation conv = ConversationManager.getInstance().getConversation(playerName);
        if (conv == null || conv.getMessages().isEmpty()) {
            context.drawText(tr, LanguageManager.get("chat.no_messages"), areaX + 4, areaY + 6, 0xFF5D7866, false);
            return;
        }

        List<DirectMessage> messages = conv.getMessages();
        List<MessageLine> renderedLines = new ArrayList<>();
        boolean showTimestamps = RetroChatConfig.getInstance().showTimestamps;
        boolean showPlayerHeads = RetroChatConfig.getInstance().showPlayerHeads;
        SkinTextures incomingSkin = getSkinTextures();
        SkinTextures mySkin = getMySkinTextures();

        int availWidth = Math.max(10, areaWidth - 8);

        for (DirectMessage msg : messages) {
            net.minecraft.text.MutableText fullMsg = Text.empty();
            int timeWidth = 0;
            if (showTimestamps) {
                String timeStr = "[" + TIME_FORMAT.format(new Date(msg.getTimestamp())) + "] ";
                fullMsg.append(Text.literal(timeStr).styled(s -> s.withColor(0x5C7864)));
                timeWidth = tr.getWidth(timeStr);
            }

            if (showPlayerHeads) {
                // spacing for player head
                fullMsg.append(Text.literal("   "));
            }

            SkinTextures skin = msg.isIncoming() ? incomingSkin : mySkin;
            if (msg.isIncoming()) {
                if (msg.wasBlocked()) {
                    fullMsg.append(Text.literal("[ğŸš«] ").styled(s -> s.withColor(0xFFFF5555)));
                    fullMsg.append(Text.literal(playerName + ": ").styled(s -> s.withColor(0xFFB07070)));
                    fullMsg.append(Text.literal(msg.getContent()).styled(s -> s.withColor(0xFFA59292)));
                } else {
                    fullMsg.append(Text.literal(playerName + ": ").styled(s -> s.withColor(0x4ADE80)));
                    fullMsg.append(Text.literal(msg.getContent()).styled(s -> s.withColor(0xE2E8F0)));
                }
            } else {
                String youStr = LanguageManager.get("chat.you") + ": ";
                fullMsg.append(Text.literal(youStr).styled(s -> s.withColor(0xF97316)));
                fullMsg.append(Text.literal(msg.getContent()).styled(s -> s.withColor(0xFCD34D)));
            }

            List<net.minecraft.text.OrderedText> wrapped = tr.wrapLines(fullMsg, availWidth);
            for (int i = 0; i < wrapped.size(); i++) {
                if (i == 0 && showPlayerHeads) {
                    renderedLines.add(new MessageLine(wrapped.get(i), skin, timeWidth + 2, msg.wasBlocked()));
                } else {
                    renderedLines.add(new MessageLine(wrapped.get(i), null, 0, msg.wasBlocked()));
                }
            }
        }

        int totalHeight = renderedLines.size() * 10;
        maxScroll = Math.max(0, totalHeight - areaHeight);
        scrollOffset = Math.max(0, Math.min(scrollOffset, maxScroll));

        context.enableScissor(areaX + 1, areaY + 1, areaX + areaWidth - 1, areaY + areaHeight - 1);

        String hoveredBlockedTooltip = null;
        int currentY = areaY + areaHeight - totalHeight + scrollOffset;
        for (MessageLine line : renderedLines) {
            if (currentY + 10 >= areaY && currentY <= areaY + areaHeight) {
                if (line.headSkin != null) {
                    GuiUtils.drawPlayerHead(context, line.headSkin, areaX + 4 + line.headXOffset, currentY, 8);
                }
                context.drawText(tr, line.text, areaX + 4, currentY, 0xFFFFFFFF, true);

                if (line.wasBlocked && mouseX >= areaX && mouseX <= areaX + areaWidth && mouseY >= currentY && mouseY < currentY + 10) {
                    hoveredBlockedTooltip = LanguageManager.get("chat.sent_while_blocked");
                }
            }
            currentY += 10;
        }

        context.disableScissor();

        if (hoveredBlockedTooltip != null) {
            context.drawTooltip(tr, Text.literal(hoveredBlockedTooltip), mouseX, mouseY);
        }

        if (maxScroll > 0) {
            int scrollbarHeight = Math.max(10, (int) ((float) areaHeight / totalHeight * areaHeight));
            int scrollbarY = areaY + (int) ((float) (maxScroll - scrollOffset) / maxScroll * (areaHeight - scrollbarHeight));
            context.fill(areaX + areaWidth - 3, areaY, areaX + areaWidth - 1, areaY + areaHeight, 0x40000000);
            context.fill(areaX + areaWidth - 3, scrollbarY, areaX + areaWidth - 1, scrollbarY + scrollbarHeight, GuiUtils.HEADER_ACTIVE_LIGHT);
        }
    }

    private static class MessageLine {
        final net.minecraft.text.OrderedText text;
        final SkinTextures headSkin;
        final int headXOffset;
        final boolean wasBlocked;

        MessageLine(net.minecraft.text.OrderedText text, SkinTextures headSkin, int headXOffset, boolean wasBlocked) {
            this.text = text;
            this.headSkin = headSkin;
            this.headXOffset = headXOffset;
            this.wasBlocked = wasBlocked;
        }
    }

    public void focusInput() {
        this.isFocused = true;
        ConversationManager.getInstance().markAsRead(playerName);
        if (RetroChatConfig.getInstance().isPlayerBlocked(playerName)) {
            return;
        }
        if (this.inputField != null) {
            this.inputField.setFocused(true);
            this.inputField.setCursorToEnd(false);
        }
    }

    public void setFocused(boolean focused) {
        this.isFocused = focused;
        if (!focused) {
            this.emojiPickerOpen = false;
        }
        if (this.inputField != null) {
            this.inputField.setFocused(focused);
        }
    }

    public boolean isFocused() {
        if (ConversationManager.getInstance().getWindowState(playerName) != ConversationManager.WindowState.OPEN) {
            return false;
        }
        return isFocused;
    }

    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        ConversationManager cm = ConversationManager.getInstance();
        ConversationManager.WindowState state = cm.getWindowState(playerName);
        if (state == ConversationManager.WindowState.CLOSED) return false;

        if (state == ConversationManager.WindowState.MINIMIZED || isRenderedAsTab) {
            int currentWidth = this.currentTabWidth;
            if (mouseX >= x && mouseX <= x + currentWidth && mouseY >= y && mouseY <= y + HEADER_HEIGHT) {
                if (button == 2) {
                    GuiUtils.playClickSound();
                    cm.closeWindow(playerName);
                    return true;
                }
                if (button == 0) {
                    if (currentWidth >= 46) {
                        int closeBtnX = x + currentWidth - 15;
                        if (mouseX >= closeBtnX && mouseX <= closeBtnX + 13) {
                            GuiUtils.playClickSound();
                            cm.closeWindow(playerName);
                            return true;
                        }
                    }
                    GuiUtils.playClickSound();
                    cm.openWindow(playerName);
                    focusInput();
                    return true;
                }
            }
            return false;
        }

        if (mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + height) {
            if (mouseY >= y && mouseY <= y + HEADER_HEIGHT) {
                int closeBtnX = x + width - 15;
                int minBtnX = x + width - 29;
                int pinBtnX = x + width - 43;

                if (mouseX >= closeBtnX && mouseX <= closeBtnX + 12) {
                    GuiUtils.playClickSound();
                    cm.closeWindow(playerName);
                    return true;
                }
                if (mouseX >= minBtnX && mouseX <= minBtnX + 12) {
                    GuiUtils.playClickSound();
                    cm.minimizeWindow(playerName);
                    return true;
                }
                if (mouseX >= pinBtnX && mouseX <= pinBtnX + 12) {
                    GuiUtils.playClickSound();
                    cm.toggleWindowPinned(playerName);
                    return true;
                }
                return true;
            }

            int emojiBtnX = x + width - 18;
            int emojiBtnY = y + height - 14;
            int emojiBtnW = 14;
            int emojiBtnH = 11;

            if (mouseX >= emojiBtnX && mouseX <= emojiBtnX + emojiBtnW && mouseY >= emojiBtnY && mouseY <= emojiBtnY + emojiBtnH) {
                GuiUtils.playClickSound();
                emojiPickerOpen = !emojiPickerOpen;
                return true;
            }

            if (emojiPickerOpen) {
                int panelW = Math.min(width - 8, 175);
                int panelH = Math.min(height - HEADER_HEIGHT - 36, 125);
                int panelX = x + width - panelW - 4;
                int panelY = y + height - 33 - panelH;
                if (mouseX >= panelX && mouseX <= panelX + panelW && mouseY >= panelY && mouseY <= panelY + panelH) {
                    emojiPicker.mouseClicked(mouseX, mouseY, button, panelX, panelY, panelW, panelH,
                            emoji -> {
                                if (inputField != null) {
                                    inputField.write(emoji);
                                    inputField.setFocused(true);
                                }
                            },
                            () -> emojiPickerOpen = false);
                    return true;
                } else {
                    emojiPickerOpen = false;
                }
            }

            cm.markAsRead(playerName);
            focusInput();
            if (inputField != null) {
                inputField.mouseClicked(mouseX, mouseY, button);
            }

            return true;
        } else {
            setFocused(false);
        }
        return false;
    }

    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        ConversationManager.WindowState state = ConversationManager.getInstance().getWindowState(playerName);
        if (state != ConversationManager.WindowState.OPEN) return false;

        if (emojiPickerOpen) {
            int panelW = Math.min(width - 8, 175);
            int panelH = Math.min(height - HEADER_HEIGHT - 36, 125);
            int panelX = x + width - panelW - 4;
            int panelY = y + height - 33 - panelH;
            if (emojiPicker.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount, panelX, panelY, panelW, panelH)) {
                return true;
            }
        }

        if (mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + height) {
            if (maxScroll > 0) {
                scrollOffset += (int) (verticalAmount * 12);
                scrollOffset = Math.max(0, Math.min(scrollOffset, maxScroll));
            }
            return true;
        }
        return false;
    }

    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (!isFocused() || inputField == null) return false;

        if (keyCode == 256) {
            if (emojiPickerOpen) {
                emojiPickerOpen = false;
                return true;
            }
            setFocused(false);
            return false;
        }

        if (RetroChatConfig.getInstance().isPlayerBlocked(playerName)) {
            return false;
        }

        if (keyCode == 257 || keyCode == 335) {
            sendMessage();
            return true;
        }

        inputField.setFocused(true);
        return inputField.keyPressed(keyCode, scanCode, modifiers);
    }

    public boolean charTyped(char chr, int modifiers) {
        if (!isFocused() || inputField == null) return false;
        if (RetroChatConfig.getInstance().isPlayerBlocked(playerName)) {
            return false;
        }
        inputField.setFocused(true);
        return inputField.charTyped(chr, modifiers);
    }

    private void sendMessage() {
        if (inputField == null) return;
        if (RetroChatConfig.getInstance().isPlayerBlocked(playerName)) {
            MinecraftClient client = MinecraftClient.getInstance();
            if (client.player != null) {
                client.player.sendMessage(Text.literal("§c[RetroChat] " + LanguageManager.get("chat.player_blocked")), false);
            }
            inputField.setText("");
            return;
        }

        String text = inputField.getText().trim();
        if (text.isEmpty()) return;

        MinecraftClient client = MinecraftClient.getInstance();
        if (client.getNetworkHandler() != null) {
            client.getNetworkHandler().sendChatCommand("msg " + playerName + " " + text);

            DirectMessage dm = new DirectMessage(null, "me", playerUuid, playerName, text, System.currentTimeMillis(), false, true);
            ConversationManager.getInstance().addLocalOutgoingMessage(playerName, playerUuid, dm);
        }

        inputField.setText("");
        scrollOffset = 0;
        focusInput();
    }
}
