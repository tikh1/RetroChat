package com.sen.modid;

import com.mojang.blaze3d.platform.InputConstants;
import com.sen.modid.config.RetroChatConfig;
import com.sen.modid.lang.LanguageManager;
import com.sen.modid.manager.ConversationManager;
import com.sen.modid.model.DirectMessage;
import com.sen.modid.ui.RetroChatConfigScreen;
import com.sen.modid.ui.RetroChatOverlay;
import com.sen.modid.ui.RetroChatOverlayScreen;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.fabricmc.fabric.api.client.screen.v1.ScreenKeyboardEvents;
import net.fabricmc.fabric.api.client.screen.v1.ScreenMouseEvents;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.ChatType;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class RetroChatClient implements ClientModInitializer {
    public static final String MOD_ID = "retrochat";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    private static KeyMapping toggleChatKey;
    private static Screen lastScreen = null;

    private static final Pattern[] INCOMING_PATTERNS = new Pattern[] {
        Pattern.compile("^(\\w+) whispers to you: (.*)$", Pattern.CASE_INSENSITIVE),
        Pattern.compile("^(\\w+) (?:size|sana) fısıldıyor: (.*)$", Pattern.CASE_INSENSITIVE),
        Pattern.compile("^\\[(\\w+) -> (?:me|you|bana|size)\\]:? (.*)$", Pattern.CASE_INSENSITIVE),
        Pattern.compile("^(\\w+) -> (?:me|you|bana|size): (.*)$", Pattern.CASE_INSENSITIVE),
        Pattern.compile("^\\[from (\\w+)\\]:? (.*)$", Pattern.CASE_INSENSITIVE),
        Pattern.compile("^(\\w+) whispers: (.*)$", Pattern.CASE_INSENSITIVE)
    };

    private static final Pattern[] OUTGOING_PATTERNS = new Pattern[] {
        Pattern.compile("^You whisper to (\\w+): (.*)$", Pattern.CASE_INSENSITIVE),
        Pattern.compile("^(\\w+) kişisine fısıldadınız: (.*)$", Pattern.CASE_INSENSITIVE),
        Pattern.compile("^(?:Sen|Siz) (\\w+) kişisine fısıldıyorsunuz: (.*)$", Pattern.CASE_INSENSITIVE),
        Pattern.compile("^\\[(?:me|you|sen|ben) -> (\\w+)\\]:? (.*)$", Pattern.CASE_INSENSITIVE),
        Pattern.compile("^(?:me|you|sen|ben) -> (\\w+): (.*)$", Pattern.CASE_INSENSITIVE),
        Pattern.compile("^\\[to (\\w+)\\]:? (.*)$", Pattern.CASE_INSENSITIVE)
    };

    public static KeyMapping getToggleChatKey() {
        return toggleChatKey;
    }

    @Override
    public void onInitializeClient() {
        LOGGER.info("[RetroChat] Initializing client mod...");

        RetroChatConfig.getInstance();
        LanguageManager.getInstance();
        ConversationManager.getInstance();

        KeyMapping.Category category = KeyMapping.Category.register(Identifier.parse("retrochat:category"));
        toggleChatKey = KeyMappingHelper.registerKeyMapping(new KeyMapping(
            "key.retrochat.toggle",
            InputConstants.UNKNOWN.getValue(),
            category
        ));

        // Register tick event for key toggles and window minimization
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            Screen currentScreen = client.gui.screen();
            if (lastScreen != null && currentScreen == null) {
                ConversationManager.getInstance().minimizeUnpinnedWindows();
                RetroChatOverlay.getInstance().unfocusAll();
            }
            lastScreen = currentScreen;

            while (toggleChatKey.consumeClick()) {
                if (!RetroChatConfig.getInstance().modEnabled) continue;
                if (client.level != null) {
                    if (client.gui.screen() == null) {
                        client.gui.setScreen(new RetroChatOverlayScreen());
                    } else if (client.gui.screen() instanceof RetroChatOverlayScreen) {
                        client.gui.setScreen(null);
                    }
                }
            }
        });

        // Register HUD element rendering
        HudElementRegistry.addLast(Identifier.parse("retrochat:hud"), (extractor, deltaTracker) -> {
            if (!RetroChatConfig.getInstance().modEnabled) return;
            Minecraft client = Minecraft.getInstance();
            if (client.gui.screen() == null) {
                RetroChatOverlay.getInstance().renderHud(extractor, deltaTracker);
            }
        });

        // Register Screen rendering and input events for non-RetroChat screens
        ScreenEvents.BEFORE_INIT.register((client, screen, scaledWidth, scaledHeight) -> {
            if (!(screen instanceof ChatScreen)) {
                return;
            }

            ScreenEvents.afterExtract(screen).register((scr, extractor, mouseX, mouseY, delta) -> {
                if (!RetroChatConfig.getInstance().modEnabled) return;
                RetroChatOverlay.getInstance().renderOnScreen(extractor, mouseX, mouseY, delta);
            });

            ScreenMouseEvents.allowMouseClick(screen).register((scr, event) -> {
                if (!RetroChatConfig.getInstance().modEnabled) return true;
                return !RetroChatOverlay.getInstance().onMouseClicked(event);
            });

            ScreenMouseEvents.allowMouseRelease(screen).register((scr, event) -> {
                if (!RetroChatConfig.getInstance().modEnabled) return true;
                return !RetroChatOverlay.getInstance().onMouseReleased(event);
            });

            ScreenMouseEvents.allowMouseDrag(screen).register((scr, event, dx, dy) -> {
                if (!RetroChatConfig.getInstance().modEnabled) return true;
                return !RetroChatOverlay.getInstance().onMouseDragged(event, dx, dy);
            });

            ScreenMouseEvents.allowMouseScroll(screen).register((scr, mx, my, hScroll, vScroll) -> {
                if (!RetroChatConfig.getInstance().modEnabled) return true;
                return !RetroChatOverlay.getInstance().onMouseScrolled(mx, my, hScroll, vScroll);
            });

            ScreenKeyboardEvents.allowKeyPress(screen).register((scr, event) -> {
                if (!RetroChatConfig.getInstance().modEnabled) return true;
                return !RetroChatOverlay.getInstance().onKeyPressed(event);
            });

            ScreenKeyboardEvents.allowCharType(screen).register((scr, event) -> {
                if (!RetroChatConfig.getInstance().modEnabled) return true;
                return !RetroChatOverlay.getInstance().onCharTyped(event);
            });
        });

        // Chat message listening
        ClientReceiveMessageEvents.ALLOW_CHAT.register((message, signedMessage, senderProfile, type, timestamp) -> {
            if (!RetroChatConfig.getInstance().modEnabled) return true;
            if (type.chatType().is(ChatType.MSG_COMMAND_INCOMING)) {
                String senderName = senderProfile != null ? senderProfile.name() : (type.name() != null ? type.name().getString() : "Unknown");
                boolean isBlocked = RetroChatConfig.getInstance().isPlayerBlocked(senderName);
                UUID senderUuid = senderProfile != null ? senderProfile.id() : null;
                String content = signedMessage != null ? signedMessage.signedContent() : message.getString();
                long time = timestamp != null ? timestamp.toEpochMilli() : Instant.now().toEpochMilli();

                DirectMessage dm = new DirectMessage(senderUuid, senderName, null, "me", content, time, true, isBlocked, isBlocked);
                ConversationManager.getInstance().addMessage(senderName, senderUuid, dm);

                if (isBlocked || RetroChatConfig.getInstance().hideDmsInVanillaChat) {
                    return false;
                }
            } else if (type.chatType().is(ChatType.MSG_COMMAND_OUTGOING)) {
                String targetName = type.targetName().map(Component::getString).orElse("Unknown");
                if (RetroChatConfig.getInstance().isPlayerBlocked(targetName)) {
                    return false;
                }
                String content = signedMessage != null ? signedMessage.signedContent() : message.getString();
                long time = timestamp != null ? timestamp.toEpochMilli() : Instant.now().toEpochMilli();

                if (!ConversationManager.getInstance().consumePendingOutgoing(targetName, content)) {
                    DirectMessage dm = new DirectMessage(null, "me", null, targetName, content, time, false, true);
                    ConversationManager.getInstance().addMessage(targetName, null, dm);
                }

                if (RetroChatConfig.getInstance().hideDmsInVanillaChat) {
                    return false;
                }
            }
            return true;
        });

        ClientReceiveMessageEvents.ALLOW_GAME.register((message, overlay) -> {
            if (!RetroChatConfig.getInstance().modEnabled || overlay) return true;

            if (message.getContents() instanceof net.minecraft.network.chat.contents.TranslatableContents tc) {
                String key = tc.getKey();
                if ("commands.message.display.incoming".equals(key)) {
                    Object[] args = tc.getArgs();
                    if (args.length >= 2) {
                        String sender = (args[0] instanceof Component c) ? c.getString() : args[0].toString();
                        String content = (args[1] instanceof Component c) ? c.getString() : args[1].toString();
                        boolean isBlocked = RetroChatConfig.getInstance().isPlayerBlocked(sender);
                        long timestamp = Instant.now().toEpochMilli();
                        DirectMessage dm = new DirectMessage(null, sender, null, "me", content, timestamp, true, isBlocked, isBlocked);
                        ConversationManager.getInstance().addMessage(sender, null, dm);
                        if (isBlocked || RetroChatConfig.getInstance().hideDmsInVanillaChat) {
                            return false;
                        }
                        return true;
                    }
                } else if ("commands.message.display.outgoing".equals(key)) {
                    Object[] args = tc.getArgs();
                    if (args.length >= 2) {
                        String target = (args[0] instanceof Component c) ? c.getString() : args[0].toString();
                        String content = (args[1] instanceof Component c) ? c.getString() : args[1].toString();
                        boolean isBlocked = RetroChatConfig.getInstance().isPlayerBlocked(target);
                        long timestamp = Instant.now().toEpochMilli();
                        if (!ConversationManager.getInstance().consumePendingOutgoing(target, content)) {
                            DirectMessage dm = new DirectMessage(null, "me", null, target, content, timestamp, false, true);
                            ConversationManager.getInstance().addMessage(target, null, dm);
                        }
                        if (isBlocked || RetroChatConfig.getInstance().hideDmsInVanillaChat) {
                            return false;
                        }
                        return true;
                    }
                }
            }

            String raw = message.getString();

            for (Pattern p : INCOMING_PATTERNS) {
                Matcher m = p.matcher(raw);
                if (m.matches()) {
                    String sender = m.group(1);
                    boolean isBlocked = RetroChatConfig.getInstance().isPlayerBlocked(sender);
                    String content = m.group(2);
                    DirectMessage dm = new DirectMessage(null, sender, null, "me", content, Instant.now().toEpochMilli(), true, isBlocked, isBlocked);
                    ConversationManager.getInstance().addMessage(sender, null, dm);
                    if (isBlocked || RetroChatConfig.getInstance().hideDmsInVanillaChat) {
                        return false;
                    }
                    return true;
                }
            }

            for (Pattern p : OUTGOING_PATTERNS) {
                Matcher m = p.matcher(raw);
                if (m.matches()) {
                    String target = m.group(1);
                    if (RetroChatConfig.getInstance().isPlayerBlocked(target)) {
                        return false;
                    }
                    String content = m.group(2);
                    if (!ConversationManager.getInstance().consumePendingOutgoing(target, content)) {
                        DirectMessage dm = new DirectMessage(null, "me", null, target, content, Instant.now().toEpochMilli(), false, true);
                        ConversationManager.getInstance().addMessage(target, null, dm);
                    }
                    if (RetroChatConfig.getInstance().hideDmsInVanillaChat) {
                        return false;
                    }
                    return true;
                }
            }
            return true;
        });

        // Connection lifecycle events
        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> {
            ConversationManager.getInstance().updateServerScope();
        });

        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> {
            ConversationManager.getInstance().resetScope();
        });

        LOGGER.info("[RetroChat] Initialized successfully on Fabric 26.3!");
    }
}
