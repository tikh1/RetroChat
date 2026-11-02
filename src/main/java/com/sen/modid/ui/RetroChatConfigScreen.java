package com.sen.modid.ui;

import com.sen.modid.config.RetroChatConfig;
import com.sen.modid.lang.LanguageManager;
import com.sen.modid.manager.ConversationManager;
import com.sen.modid.model.Conversation;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import net.minecraftforge.fml.loading.FMLPaths;

import java.io.File;
import java.util.*;

public class RetroChatConfigScreen extends Screen {
    private final Screen parent;
    private final RetroChatConfig config = RetroChatConfig.getInstance();

    private boolean isDraggingPreview = false;
    private boolean isDraggingListScrollbar = false;
    private int previewWidth = 220;
    private int previewHeight = 160;

    private EditBox searchField;
    private String searchQuery = "";
    private int playerListScroll = 0;
    private int maxPlayerListScroll = 0;

    public RetroChatConfigScreen(Screen parent) {
        super(Component.literal("RetroChat Settings"));
        this.parent = parent;
        this.previewWidth = config.windowWidth;
        this.previewHeight = config.windowHeight;
    }

    @Override
    protected void init() {
        int centerX = width / 2;
        int startY = 20;

        addRenderableWidget(Button.builder(
            Component.literal(LanguageManager.get("config.mod_enabled") + ": " + (config.modEnabled ? "ON" : "OFF")),
            button -> {
                config.modEnabled = !config.modEnabled;
                button.setMessage(Component.literal(LanguageManager.get("config.mod_enabled") + ": " + (config.modEnabled ? "ON" : "OFF")));
                config.save();
            }
        ).bounds(centerX - 155, startY, 310, 20).build());

        addRenderableWidget(Button.builder(
            Component.literal(LanguageManager.get("config.hide_dms") + ": " + (config.hideDmsInVanillaChat ? "ON" : "OFF")),
            button -> {
                config.hideDmsInVanillaChat = !config.hideDmsInVanillaChat;
                button.setMessage(Component.literal(LanguageManager.get("config.hide_dms") + ": " + (config.hideDmsInVanillaChat ? "ON" : "OFF")));
                config.save();
            }
        ).bounds(centerX - 155, startY + 22, 150, 20).build());

        addRenderableWidget(Button.builder(
            Component.literal(LanguageManager.get("config.sound") + ": " + (config.soundEnabled ? "ON" : "OFF")),
            button -> {
                config.soundEnabled = !config.soundEnabled;
                button.setMessage(Component.literal(LanguageManager.get("config.sound") + ": " + (config.soundEnabled ? "ON" : "OFF")));
                config.save();
            }
        ).bounds(centerX + 5, startY + 22, 150, 20).build());

        addRenderableWidget(new AbstractSliderButton(centerX - 155, startY + 44, 150, 20, Component.literal(LanguageManager.get("config.volume") + ": " + (int)(config.soundVolume * 100) + "%"), config.soundVolume) {
            @Override
            protected void updateMessage() {
                setMessage(Component.literal(LanguageManager.get("config.volume") + ": " + (int)(this.value * 100) + "%"));
            }

            @Override
            protected void applyValue() {
                config.soundVolume = (float) this.value;
                config.save();
            }
        });

        double scaleValue = (config.uiScale - 0.5f) / 1.0f;
        addRenderableWidget(new AbstractSliderButton(centerX + 5, startY + 44, 150, 20, Component.literal(LanguageManager.get("config.ui_scale") + ": " + (int)(config.uiScale * 100) + "%"), scaleValue) {
            @Override
            protected void updateMessage() {
                float currentScale = 0.5f + (float) this.value * 1.0f;
                setMessage(Component.literal(LanguageManager.get("config.ui_scale") + ": " + (int)(currentScale * 100) + "%"));
            }

            @Override
            protected void applyValue() {
                config.uiScale = 0.5f + (float) this.value * 1.0f;
                config.save();
            }
        });

        double widthValue = (double)(config.windowWidth - 160) / 160.0;
        addRenderableWidget(new PreviewSlider(centerX - 155, startY + 66, 108, 20, Component.literal(LanguageManager.get("config.width") + ": " + config.windowWidth + "px"), widthValue, true));

        addRenderableWidget(Button.builder(Component.literal(LanguageManager.get("config.reset")), b -> {
            config.windowWidth = 220;
            previewWidth = 220;
            config.save();
            rebuildWidgets();
        }).bounds(centerX - 43, startY + 66, 38, 20).build());

        double heightValue = (double)(config.windowHeight - 120) / 140.0;
        addRenderableWidget(new PreviewSlider(centerX + 5, startY + 66, 108, 20, Component.literal(LanguageManager.get("config.height") + ": " + config.windowHeight + "px"), heightValue, false));

        addRenderableWidget(Button.builder(Component.literal(LanguageManager.get("config.reset")), b -> {
            config.windowHeight = 160;
            previewHeight = 160;
            config.save();
            rebuildWidgets();
        }).bounds(centerX + 117, startY + 66, 38, 20).build());

        addRenderableWidget(Button.builder(
            Component.literal(LanguageManager.get("config.timestamps") + ": " + (config.showTimestamps ? "ON" : "OFF")),
            button -> {
                config.showTimestamps = !config.showTimestamps;
                button.setMessage(Component.literal(LanguageManager.get("config.timestamps") + ": " + (config.showTimestamps ? "ON" : "OFF")));
                config.save();
            }
        ).bounds(centerX - 155, startY + 88, 150, 20).build());

        addRenderableWidget(Button.builder(
            Component.literal(LanguageManager.get("config.player_heads") + ": " + (config.showPlayerHeads ? "ON" : "OFF")),
            button -> {
                config.showPlayerHeads = !config.showPlayerHeads;
                button.setMessage(Component.literal(LanguageManager.get("config.player_heads") + ": " + (config.showPlayerHeads ? "ON" : "OFF")));
                config.save();
            }
        ).bounds(centerX + 5, startY + 88, 150, 20).build());

        addRenderableWidget(Button.builder(
            Component.literal(LanguageManager.get("config.language") + ": " + LanguageManager.getInstance().getCurrentLanguageName()),
            button -> {
                LanguageManager.getInstance().cycleLanguage();
                rebuildWidgets();
            }
        ).bounds(centerX - 155, startY + 110, 126, 20).build());

        File langDir = FMLPaths.CONFIGDIR.get().resolve("retrochat").resolve("lang").toFile();
        addRenderableWidget(Button.builder(Component.literal("?"), b -> {
            if (langDir.exists()) {
                Util.getPlatform().openFile(langDir);
            }
        })
        .tooltip(Tooltip.create(Component.literal(LanguageManager.get("config.language_help"))))
        .bounds(centerX - 25, startY + 110, 20, 20)
        .build());

        addRenderableWidget(Button.builder(
            Component.literal(LanguageManager.get("config.compact_tabs") + ": " + (config.compactTabs ? "ON" : "OFF")),
            button -> {
                config.compactTabs = !config.compactTabs;
                button.setMessage(Component.literal(LanguageManager.get("config.compact_tabs") + ": " + (config.compactTabs ? "ON" : "OFF")));
                config.save();
            }
        ).bounds(centerX + 5, startY + 110, 150, 20).build());

        int boxX = centerX - 155;
        int boxY = 154;
        int boxWidth = 310;
        searchField = new EditBox(font, boxX + 4, boxY + 4, boxWidth - 8, 16, Component.literal(""));
        searchField.setHint(Component.literal(LanguageManager.get("config.search_player")));
        searchField.setBordered(true);
        searchField.setMaxLength(32);
        searchField.setValue(searchQuery);
        searchField.setResponder(q -> {
            this.searchQuery = q;
            this.playerListScroll = 0;
        });
        addRenderableWidget(searchField);

        addRenderableWidget(Button.builder(Component.literal(LanguageManager.get("config.done")), b -> {
            config.save();
            if (minecraft != null) {
                minecraft.setScreen(parent);
            }
        }).bounds(centerX - 75, height - 24, 150, 18).build());
    }

    @Override
    public void render(GuiGraphics context, int mouseX, int mouseY, float delta) {
        if (searchField != null) {
            searchField.setVisible(!isDraggingPreview);
            if (isDraggingPreview) {
                searchField.setFocused(false);
            }
        }

        renderBackground(context);

        if (isDraggingPreview) {
            super.render(context, mouseX, mouseY, delta);

            context.fill(0, 0, width, height, 0xCC000000);

            int previewY = Math.max(86, (height - previewHeight) / 2 + 15);
            int previewX = (width - previewWidth) / 2;

            String dimText = "✦ " + LanguageManager.get("config.live_preview") + ": " + previewWidth + " x " + previewHeight + " px ✦";
            context.drawCenteredString(font, dimText, width / 2, previewY - 14, GuiUtils.ORANGE_ACCENT_HOVER);

            renderLivePreviewWindow(context, previewX, previewY, previewWidth, previewHeight);
        } else {
            super.render(context, mouseX, mouseY, delta);

            context.drawCenteredString(font, LanguageManager.get("config.title"), width / 2, 10, 0xFFFFFFFF);

            int centerX = width / 2;
            int boxX = centerX - 155;
            int boxY = 154;
            int boxWidth = 310;
            int boxHeight = Math.max(70, height - 42 - boxY);

            GuiUtils.drawBeveledBox(context, boxX, boxY, boxWidth, boxHeight, GuiUtils.INNER_BOX_BG, GuiUtils.INNER_BOX_LIGHT, GuiUtils.INNER_BOX_DARK);

            int listY = boxY + 24;
            int listHeight = boxHeight - 27;

            renderPlayerManagementList(context, boxX, listY, boxWidth, listHeight, mouseX, mouseY);

            String hintText = "💡 " + LanguageManager.get("config.keybind_hint");
            String trimmedHint = font.plainSubstrByWidth(hintText, Math.max(10, width - 20));
            context.drawCenteredString(font, trimmedHint, width / 2, height - 36, GuiUtils.ORANGE_ACCENT_HOVER);
        }
    }

    private static class SectionHeader {
        final String title;
        final boolean isBlocked;
        SectionHeader(String title, boolean isBlocked) {
            this.title = title;
            this.isBlocked = isBlocked;
        }
    }

    private static class EmptyNotice {
        final String text;
        EmptyNotice(String text) {
            this.text = text;
        }
    }

    private static class PlayerListItem {
        final String name;
        final UUID uuid;
        final boolean isBlocked;
        final boolean isOnline;
        final boolean isCustomQuery;

        PlayerListItem(String name, UUID uuid, boolean isBlocked, boolean isOnline, boolean isCustomQuery) {
            this.name = name;
            this.uuid = uuid;
            this.isBlocked = isBlocked;
            this.isOnline = isOnline;
            this.isCustomQuery = isCustomQuery;
        }
    }

    private List<Object> getDisplayEntries() {
        List<Object> entries = new ArrayList<>();
        String query = searchQuery.trim().toLowerCase();

        List<String> blockedMatching = new ArrayList<>();
        for (String p : config.blockedPlayers) {
            if (query.isEmpty() || p.toLowerCase().contains(query)) {
                blockedMatching.add(p);
            }
        }

        entries.add(new SectionHeader(LanguageManager.get("config.blocked_players") + " (" + config.blockedPlayers.size() + ")", true));
        if (blockedMatching.isEmpty()) {
            entries.add(new EmptyNotice(LanguageManager.get("config.no_blocked")));
        } else {
            for (String name : blockedMatching) {
                entries.add(new PlayerListItem(name, null, true, false, false));
            }
        }

        Set<String> knownPlayers = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
        Map<String, UUID> onlineUuids = new HashMap<>();

        Minecraft mc = Minecraft.getInstance();
        if (mc.getConnection() != null) {
            for (PlayerInfo entry : mc.getConnection().getOnlinePlayers()) {
                String name = entry.getProfile().getName();
                if (name != null && (mc.player == null || !name.equalsIgnoreCase(mc.player.getName().getString()))) {
                    knownPlayers.add(name);
                    onlineUuids.put(name.toLowerCase(), entry.getProfile().getId());
                }
            }
        }

        for (Conversation conv : ConversationManager.getInstance().getConversationsMru()) {
            String name = conv.getPlayerName();
            if (name != null && (mc.player == null || !name.equalsIgnoreCase(mc.player.getName().getString()))) {
                knownPlayers.add(name);
            }
        }

        for (String b : config.blockedPlayers) {
            knownPlayers.remove(b);
        }

        List<PlayerListItem> unblockedMatching = new ArrayList<>();
        boolean exactMatchFound = false;

        for (String name : knownPlayers) {
            if (query.isEmpty() || name.toLowerCase().contains(query)) {
                boolean isOnline = onlineUuids.containsKey(name.toLowerCase());
                UUID uuid = onlineUuids.get(name.toLowerCase());
                unblockedMatching.add(new PlayerListItem(name, uuid, false, isOnline, false));
            }
            if (name.equalsIgnoreCase(query)) {
                exactMatchFound = true;
            }
        }

        for (String b : config.blockedPlayers) {
            if (b.equalsIgnoreCase(query)) {
                exactMatchFound = true;
            }
        }

        entries.add(new SectionHeader(LanguageManager.get("config.player_list") + " (" + knownPlayers.size() + ")", false));
        if (unblockedMatching.isEmpty()) {
            entries.add(new EmptyNotice(LanguageManager.get("config.no_players")));
        } else {
            entries.addAll(unblockedMatching);
        }

        // allow blocking custom username typed in search
        if (!query.isEmpty() && !exactMatchFound) {
            entries.add(new PlayerListItem(searchQuery.trim(), null, false, false, true));
        }

        return entries;
    }

    private void renderPlayerManagementList(GuiGraphics context, int boxX, int listY, int boxWidth, int listHeight, int mouseX, int mouseY) {
        List<Object> entries = getDisplayEntries();
        int totalHeight = 0;
        for (Object obj : entries) {
            if (obj instanceof SectionHeader) totalHeight += 16;
            else if (obj instanceof EmptyNotice) totalHeight += 14;
            else if (obj instanceof PlayerListItem) totalHeight += 18;
        }

        maxPlayerListScroll = Math.max(0, totalHeight - listHeight);
        playerListScroll = Math.max(0, Math.min(playerListScroll, maxPlayerListScroll));

        int scrollbarWidth = 6;
        int contentWidth = maxPlayerListScroll > 0 ? (boxWidth - scrollbarWidth - 6) : (boxWidth - 2);

        context.enableScissor(boxX + 2, listY, boxX + 2 + contentWidth, listY + listHeight);

        int currY = listY + 2 - playerListScroll;
        for (Object obj : entries) {
            if (obj instanceof SectionHeader sh) {
                if (currY + 14 >= listY && currY <= listY + listHeight) {
                    int headerColor = sh.isBlocked ? GuiUtils.ORANGE_ACCENT : 0xFF4ADE80;
                    context.fill(boxX + 4, currY + 11, boxX + contentWidth - 4, currY + 12, 0x30FFFFFF);
                    context.drawString(font, sh.title, boxX + 6, currY + 2, headerColor, true);
                }
                currY += 16;
            } else if (obj instanceof EmptyNotice en) {
                if (currY + 12 >= listY && currY <= listY + listHeight) {
                    context.drawString(font, en.text, boxX + 16, currY + 2, 0xFF7D9C86, false);
                }
                currY += 14;
            } else if (obj instanceof PlayerListItem item) {
                if (currY + 16 >= listY && currY <= listY + listHeight) {
                    renderPlayerRow(context, item, boxX, currY, contentWidth + 2, mouseX, mouseY);
                }
                currY += 18;
            }
        }

        context.disableScissor();

        if (maxPlayerListScroll > 0) {
            int trackX = boxX + boxWidth - scrollbarWidth - 4;
            int trackY = listY + 1;
            int trackHeight = listHeight - 2;
            int thumbHeight = Math.max(16, (int) ((float) trackHeight / totalHeight * trackHeight));
            int thumbY = trackY + (int) ((float) playerListScroll / maxPlayerListScroll * (trackHeight - thumbHeight));

            context.fill(trackX, trackY, trackX + scrollbarWidth, trackY + trackHeight, 0x60000000);
            context.renderOutline(trackX, trackY, scrollbarWidth, trackHeight, 0x25FFFFFF);

            boolean hoveredThumb = mouseX >= trackX - 1 && mouseX <= trackX + scrollbarWidth + 1 && mouseY >= thumbY && mouseY <= thumbY + thumbHeight;
            int thumbBg = (isDraggingListScrollbar || hoveredThumb) ? GuiUtils.ORANGE_ACCENT : 0xFF2A5738;
            int thumbLight = (isDraggingListScrollbar || hoveredThumb) ? GuiUtils.ORANGE_ACCENT_HOVER : 0xFF438A58;
            int thumbDark = (isDraggingListScrollbar || hoveredThumb) ? GuiUtils.ORANGE_BORDER : 0xFF0F2417;

            GuiUtils.drawBeveledBox(context, trackX, thumbY, scrollbarWidth, thumbHeight, thumbBg, thumbLight, thumbDark);
        }
    }

    private void renderPlayerRow(GuiGraphics context, PlayerListItem item, int boxX, int y, int width, int mouseX, int mouseY) {
        boolean rowHovered = mouseX >= boxX + 4 && mouseX <= boxX + width - 6 && mouseY >= y && mouseY <= y + 17;
        if (rowHovered) {
            context.fill(boxX + 4, y, boxX + width - 6, y + 17, 0x1FFFFFFF);
        }

        int btnWidth = item.isBlocked ? 50 : 46;
        int btnX = boxX + width - btnWidth - 8;
        int btnY = y + 2;

        if (item.isCustomQuery) {
            context.drawString(font, "+ \"" + item.name + "\"", boxX + 8, y + 4, 0xFFFCD34D, true);
            boolean btnHovered = mouseX >= btnX && mouseX <= btnX + btnWidth && mouseY >= btnY && mouseY <= btnY + 12;
            GuiUtils.drawMiniButton(context, font, LanguageManager.get("config.block"), btnX, btnY, btnWidth, 12, btnHovered);
            return;
        }

        ResourceLocation skin = GuiUtils.getPlayerSkin(item.uuid, item.name);
        GuiUtils.drawPlayerHead(context, skin, boxX + 8, y + 3, 11);

        if (!item.isBlocked) {
            int dotColor = item.isOnline ? 0xFF22C55E : 0xFF64748B;
            context.fill(boxX + 22, y + 7, boxX + 25, y + 10, dotColor);
        }

        int nameX = item.isBlocked ? boxX + 23 : boxX + 28;
        int maxNameWidth = btnX - nameX - 4;
        String trimmedName = font.plainSubstrByWidth(item.name, Math.max(10, maxNameWidth));
        int nameColor = item.isBlocked ? 0xFFFF7777 : (item.isOnline ? 0xFFFFFFFF : 0xFFB8D5C0);
        context.drawString(font, trimmedName, nameX, y + 4, nameColor, true);

        boolean btnHovered = mouseX >= btnX && mouseX <= btnX + btnWidth && mouseY >= btnY && mouseY <= btnY + 12;
        String btnText = item.isBlocked ? LanguageManager.get("config.unblock") : LanguageManager.get("config.block");
        GuiUtils.drawMiniButton(context, font, btnText, btnX, btnY, btnWidth, 12, btnHovered);
    }

    private void renderLivePreviewWindow(GuiGraphics context, int x, int y, int w, int h) {
        context.fill(x - 2, y - 2, x + w + 2, y + h + 2, 0xFF000000);
        context.renderOutline(x - 1, y - 1, w + 2, h + 2, GuiUtils.ORANGE_ACCENT);

        GuiUtils.drawBeveledBox(context, x, y, w, h, 0xFF142018, 0xFF356142, 0xFF08120B);

        GuiUtils.drawWindowHeader(context, x + 1, y + 1, w - 2, 18, true);
        String headerTitle = font.plainSubstrByWidth(LanguageManager.get("config.preview_player"), Math.max(10, w - 65));
        context.drawString(font, headerTitle, x + 16, y + 5, 0xFFFFFFFF, true);
        GuiUtils.drawPinButton(context, x + w - 43, y + 3, 12, 12, true, false);
        GuiUtils.drawMinimizeButton(context, x + w - 29, y + 3, 12, 12, false);
        GuiUtils.drawCloseButton(context, x + w - 15, y + 3, 12, 12, false);

        int msgY = y + 20;
        int msgH = h - 18 - 37;
        GuiUtils.drawBeveledBox(context, x + 3, msgY, w - 6, msgH, 0xFF0C140E, 0xFF060B08, 0xFF223E2A);

        int availWidth = Math.max(10, w - 6 - 8);

        context.enableScissor(x + 4, msgY + 2, x + w - 4, msgY + msgH - 2);

        int currentLineY = msgY + 5;
        boolean showHeads = config.showPlayerHeads;

        MutableComponent msg1 = Component.empty();
        int timeWidth1 = 0;
        if (config.showTimestamps) {
            String time1 = "[12:34] ";
            msg1.append(Component.literal(time1).withStyle(s -> s.withColor(0x7D9C86)));
            timeWidth1 = font.width(time1);
        }
        if (showHeads) {
            msg1.append(Component.literal("   "));
        }
        msg1.append(Component.literal("Steve: ").withStyle(s -> s.withColor(0x4ADE80)));
        msg1.append(Component.literal(LanguageManager.get("config.preview_msg1")).withStyle(s -> s.withColor(0xFFFFFF)));

        ResourceLocation steveSkin = GuiUtils.getPlayerSkin(null, "Steve");
        List<FormattedCharSequence> lines1 = font.split(msg1, availWidth);
        for (int i = 0; i < lines1.size(); i++) {
            if (currentLineY + 9 <= msgY + msgH - 2) {
                if (i == 0 && showHeads) {
                    GuiUtils.drawPlayerHead(context, steveSkin, x + 6 + timeWidth1 + 2, currentLineY, 8);
                }
                context.drawString(font, lines1.get(i), x + 6, currentLineY, 0xFFFFFFFF, true);
                currentLineY += 10;
            }
        }

        currentLineY += 2;
        MutableComponent msg2 = Component.empty();
        int timeWidth2 = 0;
        if (config.showTimestamps) {
            String time2 = "[12:35] ";
            msg2.append(Component.literal(time2).withStyle(s -> s.withColor(0x7D9C86)));
            timeWidth2 = font.width(time2);
        }
        if (showHeads) {
            msg2.append(Component.literal("   "));
        }
        msg2.append(Component.literal(LanguageManager.get("chat.you") + ": ").withStyle(s -> s.withColor(0xF97316)));
        msg2.append(Component.literal(LanguageManager.get("config.preview_msg2")).withStyle(s -> s.withColor(0xFEF08A)));

        ResourceLocation mySkin = (minecraft != null && minecraft.player != null) ? minecraft.player.getSkinTextureLocation() : null;
        List<FormattedCharSequence> lines2 = font.split(msg2, availWidth);
        for (int i = 0; i < lines2.size(); i++) {
            if (currentLineY + 9 <= msgY + msgH - 2) {
                if (i == 0 && showHeads) {
                    GuiUtils.drawPlayerHead(context, mySkin, x + 6 + timeWidth2 + 2, currentLineY, 8);
                }
                context.drawString(font, lines2.get(i), x + 6, currentLineY, 0xFFFFFFFF, true);
                currentLineY += 10;
            }
        }

        context.disableScissor();

        int inputY = y + h - 32;
        GuiUtils.drawBeveledBox(context, x + 4, inputY, w - 8, 16, 0xFF0D1610, 0xFF1E3826, 0xFF080D0A);
        String inputPlaceholder = font.plainSubstrByWidth(LanguageManager.get("chat.type_message"), Math.max(10, w - 24));
        context.drawString(font, inputPlaceholder, x + 8, inputY + 4, 0xFF6E8F78, false);

        String counter = "0 / 256";
        int counterY = y + h - 12;
        context.drawString(font, counter, x + w - font.width(counter) - 6, counterY, GuiUtils.ORANGE_ACCENT, false);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        int centerX = width / 2;
        int boxX = centerX - 155;
        int boxY = 146;
        int boxWidth = 310;
        int boxHeight = Math.max(70, height - 42 - boxY);
        int listY = boxY + 24;
        int listHeight = boxHeight - 27;

        if (mouseX >= boxX && mouseX <= boxX + boxWidth && mouseY >= listY && mouseY <= listY + listHeight) {
            int scrollbarWidth = 6;
            int trackX = boxX + boxWidth - scrollbarWidth - 4;
            int trackY = listY + 1;
            int trackHeight = listHeight - 2;

            if (maxPlayerListScroll > 0 && mouseX >= trackX - 2 && mouseX <= trackX + scrollbarWidth + 2 && mouseY >= trackY && mouseY <= trackY + trackHeight) {
                isDraggingListScrollbar = true;
                List<Object> entries = getDisplayEntries();
                int totalHeight = 0;
                for (Object obj : entries) {
                    if (obj instanceof SectionHeader) totalHeight += 16;
                    else if (obj instanceof EmptyNotice) totalHeight += 14;
                    else if (obj instanceof PlayerListItem) totalHeight += 18;
                }
                int thumbHeight = Math.max(16, (int) ((float) trackHeight / totalHeight * trackHeight));
                float ratio = (float) (mouseY - trackY - thumbHeight / 2.0f) / Math.max(1, (trackHeight - thumbHeight));
                playerListScroll = Math.max(0, Math.min(maxPlayerListScroll, (int) (ratio * maxPlayerListScroll)));
                return true;
            }

            int currY = listY + 2 - playerListScroll;
            List<Object> entries = getDisplayEntries();

            for (Object obj : entries) {
                if (obj instanceof SectionHeader) {
                    currY += 16;
                } else if (obj instanceof EmptyNotice) {
                    currY += 14;
                } else if (obj instanceof PlayerListItem item) {
                    int contentWidth = maxPlayerListScroll > 0 ? (boxWidth - scrollbarWidth - 6) : (boxWidth - 2);
                    int btnWidth = item.isBlocked ? 50 : 46;
                    int btnX = boxX + contentWidth + 2 - btnWidth - 8;
                    int btnY = currY + 2;

                    if (mouseX >= btnX && mouseX <= btnX + btnWidth && mouseY >= btnY && mouseY <= btnY + 12) {
                        GuiUtils.playClickSound();
                        if (item.isBlocked) {
                            config.unblockPlayer(item.name);
                        } else {
                            config.blockPlayer(item.name);
                            ConversationManager.getInstance().setWindowState(item.name, ConversationManager.WindowState.CLOSED);
                            ConversationManager.getInstance().setWindowPinned(item.name, false);
                            if (item.isCustomQuery && searchField != null) {
                                searchField.setValue("");
                            }
                        }
                        return true;
                    }
                    currY += 18;
                }
            }
        }

        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
        if (isDraggingListScrollbar && maxPlayerListScroll > 0) {
            int centerX = width / 2;
            int boxX = centerX - 155;
            int boxY = 154;
            int boxHeight = Math.max(70, height - 42 - boxY);
            int listY = boxY + 24;
            int listHeight = boxHeight - 27;
            int trackY = listY + 1;
            int trackHeight = listHeight - 2;

            List<Object> entries = getDisplayEntries();
            int totalHeight = 0;
            for (Object obj : entries) {
                if (obj instanceof SectionHeader) totalHeight += 16;
                else if (obj instanceof EmptyNotice) totalHeight += 14;
                else if (obj instanceof PlayerListItem) totalHeight += 18;
            }
            int thumbHeight = Math.max(16, (int) ((float) trackHeight / totalHeight * trackHeight));
            float ratio = (float) (mouseY - trackY - thumbHeight / 2.0f) / Math.max(1, (trackHeight - thumbHeight));
            playerListScroll = Math.max(0, Math.min(maxPlayerListScroll, (int) (ratio * maxPlayerListScroll)));
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, deltaX, deltaY);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (isDraggingPreview) {
            isDraggingPreview = false;
        }
        if (isDraggingListScrollbar) {
            isDraggingListScrollbar = false;
        }
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double amount) {
        int centerX = width / 2;
        int boxX = centerX - 155;
        int boxY = 146;
        int boxWidth = 310;
        int boxHeight = Math.max(70, height - 42 - boxY);

        if (mouseX >= boxX && mouseX <= boxX + boxWidth && mouseY >= boxY && mouseY <= boxY + boxHeight) {
            if (maxPlayerListScroll > 0) {
                int delta = (int) (amount * 18);
                if (delta == 0 && amount != 0) {
                    delta = amount > 0 ? 18 : -18;
                }
                playerListScroll -= delta;
                playerListScroll = Math.max(0, Math.min(playerListScroll, maxPlayerListScroll));
                return true;
            }
        }

        return super.mouseScrolled(mouseX, mouseY, amount);
    }

    private class PreviewSlider extends AbstractSliderButton {
        private final boolean isWidthSlider;

        public PreviewSlider(int x, int y, int width, int height, Component text, double value, boolean isWidthSlider) {
            super(x, y, width, height, text, value);
            this.isWidthSlider = isWidthSlider;
        }

        @Override
        protected void updateMessage() {
            if (isWidthSlider) {
                int w = 160 + (int)(this.value * 160);
                setMessage(Component.literal(LanguageManager.get("config.width") + ": " + w + "px"));
            } else {
                int h = 120 + (int)(this.value * 140);
                setMessage(Component.literal(LanguageManager.get("config.height") + ": " + h + "px"));
            }
        }

        @Override
        protected void applyValue() {
            if (isWidthSlider) {
                config.windowWidth = 160 + (int)(this.value * 160);
                previewWidth = config.windowWidth;
            } else {
                config.windowHeight = 120 + (int)(this.value * 140);
                previewHeight = config.windowHeight;
            }
            config.save();
        }

        @Override
        public void onClick(double mouseX, double mouseY) {
            isDraggingPreview = true;
            super.onClick(mouseX, mouseY);
        }

        @Override
        public void onRelease(double mouseX, double mouseY) {
            super.onRelease(mouseX, mouseY);
            isDraggingPreview = false;
        }
    }
}
