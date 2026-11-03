package com.sen.modid;

import com.mojang.blaze3d.platform.InputConstants;
import com.sen.modid.config.RetroChatConfig;
import com.sen.modid.manager.ConversationManager;
import com.sen.modid.model.DirectMessage;
import com.sen.modid.ui.RetroChatOverlay;
import com.sen.modid.ui.RetroChatOverlayScreen;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.ChatType;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraftforge.client.event.*;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.listener.SubscribeEvent;

import java.time.Instant;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class RetroChatClientEventHandler {

    private static Screen lastScreen = null;

    private static final Pattern[] INCOMING_PATTERNS = new Pattern[] {
        Pattern.compile("^(\\w+) whispers to you: (.*)$", Pattern.CASE_INSENSITIVE),
        Pattern.compile("^(\\w+) sana fısıldıyor: (.*)$", Pattern.CASE_INSENSITIVE),
        Pattern.compile("^\\[(\\w+) -> (?:me|you|bana)\\]:? (.*)$", Pattern.CASE_INSENSITIVE),
        Pattern.compile("^(\\w+) -> (?:me|you|bana): (.*)$", Pattern.CASE_INSENSITIVE),
        Pattern.compile("^\\[from (\\w+)\\]:? (.*)$", Pattern.CASE_INSENSITIVE),
        Pattern.compile("^(\\w+) whispers: (.*)$", Pattern.CASE_INSENSITIVE)
    };

    private static final Pattern[] OUTGOING_PATTERNS = new Pattern[] {
        Pattern.compile("^You whisper to (\\w+): (.*)$", Pattern.CASE_INSENSITIVE),
        Pattern.compile("^(?:Sen|Siz) (\\w+) kişisine fısıldıyorsunuz: (.*)$", Pattern.CASE_INSENSITIVE),
        Pattern.compile("^\\[(?:me|you|sen|ben) -> (\\w+)\\]:? (.*)$", Pattern.CASE_INSENSITIVE),
        Pattern.compile("^(?:me|you|sen|ben) -> (\\w+): (.*)$", Pattern.CASE_INSENSITIVE),
        Pattern.compile("^\\[to (\\w+)\\]:? (.*)$", Pattern.CASE_INSENSITIVE)
    };

    @SubscribeEvent
    public void registerKeyMappings(RegisterKeyMappingsEvent event) {
        KeyMapping.Category category = KeyMapping.Category.register(Identifier.parse("retrochat:category"));
        KeyMapping toggleChatKey = new KeyMapping(
            "key.retrochat.toggle",
            InputConstants.UNKNOWN.getValue(),
            category
        );
        event.register(toggleChatKey);
        RetroChatClient.setToggleChatKey(toggleChatKey);
    }

    @SubscribeEvent
    public void onLoggingIn(ClientPlayerNetworkEvent.LoggingIn event) {
        ConversationManager.getInstance().updateServerScope();
    }

    @SubscribeEvent
    public void onLoggingOut(ClientPlayerNetworkEvent.LoggingOut event) {
        ConversationManager.getInstance().resetScope();
    }

    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent.Post event) {
        Minecraft client = Minecraft.getInstance();
        Screen currentScreen = client.screen;
        if (lastScreen != null && currentScreen == null) {
            ConversationManager.getInstance().minimizeUnpinnedWindows();
            RetroChatOverlay.getInstance().unfocusAll();
        }
        lastScreen = currentScreen;

        KeyMapping key = RetroChatClient.getToggleChatKey();
        while (key != null && key.consumeClick()) {
            if (!RetroChatConfig.getInstance().modEnabled) continue;
            if (client.level != null) {
                if (client.screen == null) {
                    client.setScreen(new RetroChatOverlayScreen());
                } else if (client.screen instanceof RetroChatOverlayScreen) {
                    client.setScreen(null);
                }
            }
        }
    }

    @SubscribeEvent
    public boolean onChatReceived(ClientChatReceivedEvent event) {
        if (!RetroChatConfig.getInstance().modEnabled) {
            return false;
        }
        ChatType.Bound params = event.getBoundChatType();
        if (params != null) {
            if (params.chatType().is(ChatType.MSG_COMMAND_INCOMING)) {
                String senderName = params.name().getString();
                boolean isBlocked = RetroChatConfig.getInstance().isPlayerBlocked(senderName);
                UUID senderUuid = event.getSender();
                String content;
                if (event instanceof ClientChatReceivedEvent.Player playerEvent) {
                    content = playerEvent.getPlayerChatMessage().signedContent();
                } else {
                    content = event.getMessage().getString();
                }
                long timestamp = Instant.now().toEpochMilli();

                DirectMessage dm = new DirectMessage(senderUuid, senderName, null, "me", content, timestamp, true, isBlocked, isBlocked);
                ConversationManager.getInstance().addMessage(senderName, senderUuid, dm);

                if (isBlocked || RetroChatConfig.getInstance().hideDmsInVanillaChat) {
                    return true;
                }
                return false;
            } else if (params.chatType().is(ChatType.MSG_COMMAND_OUTGOING)) {
                String targetName = params.targetName().map(Component::getString).orElse("Unknown");
                boolean isBlocked = RetroChatConfig.getInstance().isPlayerBlocked(targetName);
                String content;
                if (event instanceof ClientChatReceivedEvent.Player playerEvent) {
                    content = playerEvent.getPlayerChatMessage().signedContent();
                } else {
                    content = event.getMessage().getString();
                }
                long timestamp = Instant.now().toEpochMilli();

                if (!ConversationManager.getInstance().consumePendingOutgoing(targetName, content)) {
                    DirectMessage dm = new DirectMessage(null, "me", null, targetName, content, timestamp, false, true, false);
                    ConversationManager.getInstance().addMessage(targetName, null, dm);
                }

                if (isBlocked || RetroChatConfig.getInstance().hideDmsInVanillaChat) {
                    return true;
                }
                return false;
            }
        }

        String raw = event.getMessage().getString();

        for (Pattern p : INCOMING_PATTERNS) {
            Matcher m = p.matcher(raw);
            if (m.matches()) {
                String sender = m.group(1);
                boolean isBlocked = RetroChatConfig.getInstance().isPlayerBlocked(sender);
                String content = m.group(2);
                DirectMessage dm = new DirectMessage(null, sender, null, "me", content, Instant.now().toEpochMilli(), true, isBlocked, isBlocked);
                ConversationManager.getInstance().addMessage(sender, null, dm);
                if (isBlocked || RetroChatConfig.getInstance().hideDmsInVanillaChat) {
                    return true;
                }
                return false;
            }
        }

        for (Pattern p : OUTGOING_PATTERNS) {
            Matcher m = p.matcher(raw);
            if (m.matches()) {
                String target = m.group(1);
                boolean isBlocked = RetroChatConfig.getInstance().isPlayerBlocked(target);
                String content = m.group(2);
                if (!ConversationManager.getInstance().consumePendingOutgoing(target, content)) {
                    DirectMessage dm = new DirectMessage(null, "me", null, target, content, Instant.now().toEpochMilli(), false, true, false);
                    ConversationManager.getInstance().addMessage(target, null, dm);
                }
                if (isBlocked || RetroChatConfig.getInstance().hideDmsInVanillaChat) {
                    return true;
                }
                return false;
            }
        }
        return false;
    }

    @SubscribeEvent
    public void onCustomizeChat(CustomizeGuiOverlayEvent.Chat event) {
        if (!RetroChatConfig.getInstance().modEnabled) return;
        Minecraft client = Minecraft.getInstance();
        if (client.level != null && client.screen == null) {
            RetroChatOverlay.getInstance().render(event.getGuiGraphics(), -1000, -1000, event.getPartialTick());
        }
    }

    @SubscribeEvent
    public void onScreenRender(ScreenEvent.Render.Post event) {
        if (!RetroChatConfig.getInstance().modEnabled) return;
        Minecraft client = Minecraft.getInstance();
        if (client.level == null) return;
        Screen screen = event.getScreen();
        if (screen instanceof ChatScreen) {
            RetroChatOverlay.getInstance().render(event.getGuiGraphics(), event.getMouseX(), event.getMouseY(), event.getPartialTick());
        }
    }

    @SubscribeEvent
    public boolean onScreenMouseClick(ScreenEvent.MouseButtonPressed.Pre event) {
        if (!RetroChatConfig.getInstance().modEnabled) return false;
        Minecraft client = Minecraft.getInstance();
        if (client.level == null) return false;
        Screen screen = event.getScreen();
        if (!(screen instanceof ChatScreen)) return false;
        return RetroChatOverlay.getInstance().mouseClicked(event.getMouseX(), event.getMouseY(), event.getInfo().button());
    }

    @SubscribeEvent
    public boolean onScreenMouseRelease(ScreenEvent.MouseButtonReleased.Pre event) {
        if (!RetroChatConfig.getInstance().modEnabled) return false;
        Minecraft client = Minecraft.getInstance();
        if (client.level == null) return false;
        Screen screen = event.getScreen();
        if (!(screen instanceof ChatScreen)) return false;
        return RetroChatOverlay.getInstance().mouseReleased(event.getMouseX(), event.getMouseY(), event.getButton());
    }

    @SubscribeEvent
    public boolean onScreenMouseDrag(ScreenEvent.MouseDragged.Pre event) {
        if (!RetroChatConfig.getInstance().modEnabled) return false;
        Minecraft client = Minecraft.getInstance();
        if (client.level == null) return false;
        Screen screen = event.getScreen();
        if (!(screen instanceof ChatScreen)) return false;
        return RetroChatOverlay.getInstance().mouseDragged(event.getMouseX(), event.getMouseY(), event.getMouseButton(), event.getDragX(), event.getDragY());
    }

    @SubscribeEvent
    public boolean onScreenMouseScroll(ScreenEvent.MouseScrolled.Pre event) {
        if (!RetroChatConfig.getInstance().modEnabled) return false;
        Minecraft client = Minecraft.getInstance();
        if (client.level == null) return false;
        Screen screen = event.getScreen();
        if (!(screen instanceof ChatScreen)) return false;
        return RetroChatOverlay.getInstance().mouseScrolled(event.getMouseX(), event.getMouseY(), event.getDeltaX(), event.getDeltaY());
    }

    @SubscribeEvent
    public boolean onScreenKeyPress(ScreenEvent.KeyPressed.Pre event) {
        if (!RetroChatConfig.getInstance().modEnabled) return false;
        Minecraft client = Minecraft.getInstance();
        if (client.level == null) return false;
        Screen screen = event.getScreen();
        if (!(screen instanceof ChatScreen)) return false;
        return RetroChatOverlay.getInstance().onKeyPressed(event.getInfo());
    }

    @SubscribeEvent
    public boolean onScreenCharType(ScreenEvent.CharacterTyped.Pre event) {
        if (!RetroChatConfig.getInstance().modEnabled) return false;
        Minecraft client = Minecraft.getInstance();
        if (client.level == null) return false;
        Screen screen = event.getScreen();
        if (!(screen instanceof ChatScreen)) return false;
        return RetroChatOverlay.getInstance().onCharTyped(event.getInfo());
    }
}
