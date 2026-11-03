package com.sen.modid;

import com.sen.modid.config.RetroChatConfig;
import com.sen.modid.manager.ConversationManager;
import com.sen.modid.model.DirectMessage;
import com.sen.modid.ui.RetroChatOverlay;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.ChatType;
import net.minecraft.network.chat.Component;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.client.event.*;

import java.time.Instant;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class RetroChatClientEventHandler {

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
    public void onLoggingIn(ClientPlayerNetworkEvent.LoggingIn event) {
        ConversationManager.getInstance().updateServerScope();
    }

    @SubscribeEvent
    public void onLoggingOut(ClientPlayerNetworkEvent.LoggingOut event) {
        ConversationManager.getInstance().resetScope();
    }

    @SubscribeEvent
    public void onChatReceived(ClientChatReceivedEvent event) {
        if (!RetroChatConfig.getInstance().modEnabled) {
            return;
        }
        ChatType.Bound params = event.getBoundChatType();
        if (params != null) {
            if (params.chatType().is(ChatType.MSG_COMMAND_INCOMING)) {
                String senderName = params.name().getString();
                boolean isBlocked = RetroChatConfig.getInstance().isPlayerBlocked(senderName);
                if (isBlocked) {
                    event.setCanceled(true);
                }
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

                if (RetroChatConfig.getInstance().hideDmsInVanillaChat) {
                    event.setCanceled(true);
                }
                return;
            } else if (params.chatType().is(ChatType.MSG_COMMAND_OUTGOING)) {
                String targetName = params.targetName().map(Component::getString).orElse("Unknown");
                boolean isBlocked = RetroChatConfig.getInstance().isPlayerBlocked(targetName);
                if (isBlocked) {
                    event.setCanceled(true);
                }
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

                if (RetroChatConfig.getInstance().hideDmsInVanillaChat) {
                    event.setCanceled(true);
                }
                return;
            }
        }

        if (event instanceof ClientChatReceivedEvent.System sys && sys.isOverlay()) {
            return;
        }

        String raw = event.getMessage().getString();

        for (Pattern p : INCOMING_PATTERNS) {
            Matcher m = p.matcher(raw);
            if (m.matches()) {
                String sender = m.group(1);
                boolean isBlocked = RetroChatConfig.getInstance().isPlayerBlocked(sender);
                if (isBlocked) {
                    event.setCanceled(true);
                }
                String content = m.group(2);
                DirectMessage dm = new DirectMessage(null, sender, null, "me", content, Instant.now().toEpochMilli(), true, isBlocked, isBlocked);
                ConversationManager.getInstance().addMessage(sender, null, dm);
                if (RetroChatConfig.getInstance().hideDmsInVanillaChat) {
                    event.setCanceled(true);
                }
                return;
            }
        }

        for (Pattern p : OUTGOING_PATTERNS) {
            Matcher m = p.matcher(raw);
            if (m.matches()) {
                String target = m.group(1);
                boolean isBlocked = RetroChatConfig.getInstance().isPlayerBlocked(target);
                if (isBlocked) {
                    event.setCanceled(true);
                }
                String content = m.group(2);
                if (!ConversationManager.getInstance().consumePendingOutgoing(target, content)) {
                    DirectMessage dm = new DirectMessage(null, "me", null, target, content, Instant.now().toEpochMilli(), false, true, false);
                    ConversationManager.getInstance().addMessage(target, null, dm);
                }
                if (RetroChatConfig.getInstance().hideDmsInVanillaChat) {
                    event.setCanceled(true);
                }
                return;
            }
        }
    }

    @SubscribeEvent
    public void onRenderGui(RenderGuiEvent.Post event) {
        if (!RetroChatConfig.getInstance().modEnabled) return;
        Minecraft client = Minecraft.getInstance();
        if (client.level != null && client.screen == null) {
            RetroChatOverlay.getInstance().render(event.getGuiGraphics(), -1000, -1000, event.getPartialTick().getGameTimeDeltaPartialTick(true));
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
    public void onScreenMouseClick(ScreenEvent.MouseButtonPressed.Pre event) {
        if (!RetroChatConfig.getInstance().modEnabled) return;
        Minecraft client = Minecraft.getInstance();
        if (client.level == null) return;
        Screen screen = event.getScreen();
        if (!(screen instanceof ChatScreen)) return;
        if (RetroChatOverlay.getInstance().mouseClicked(event.getMouseX(), event.getMouseY(), event.getButton())) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public void onScreenMouseRelease(ScreenEvent.MouseButtonReleased.Pre event) {
        if (!RetroChatConfig.getInstance().modEnabled) return;
        Minecraft client = Minecraft.getInstance();
        if (client.level == null) return;
        Screen screen = event.getScreen();
        if (!(screen instanceof ChatScreen)) return;
        if (RetroChatOverlay.getInstance().mouseReleased(event.getMouseX(), event.getMouseY(), event.getButton())) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public void onScreenMouseDrag(ScreenEvent.MouseDragged.Pre event) {
        if (!RetroChatConfig.getInstance().modEnabled) return;
        Minecraft client = Minecraft.getInstance();
        if (client.level == null) return;
        Screen screen = event.getScreen();
        if (!(screen instanceof ChatScreen)) return;
        if (RetroChatOverlay.getInstance().mouseDragged(event.getMouseX(), event.getMouseY(), event.getMouseButton(), event.getDragX(), event.getDragY())) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public void onScreenMouseScroll(ScreenEvent.MouseScrolled.Pre event) {
        if (!RetroChatConfig.getInstance().modEnabled) return;
        Minecraft client = Minecraft.getInstance();
        if (client.level == null) return;
        Screen screen = event.getScreen();
        if (!(screen instanceof ChatScreen)) return;
        if (RetroChatOverlay.getInstance().mouseScrolled(event.getMouseX(), event.getMouseY(), event.getScrollDeltaX(), event.getScrollDeltaY())) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public void onScreenKeyPress(ScreenEvent.KeyPressed.Pre event) {
        if (!RetroChatConfig.getInstance().modEnabled) return;
        Minecraft client = Minecraft.getInstance();
        if (client.level == null) return;
        Screen screen = event.getScreen();
        if (!(screen instanceof ChatScreen)) return;
        if (RetroChatOverlay.getInstance().keyPressed(event.getKeyCode(), event.getScanCode(), event.getModifiers())) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public void onScreenCharType(ScreenEvent.CharacterTyped.Pre event) {
        if (!RetroChatConfig.getInstance().modEnabled) return;
        Minecraft client = Minecraft.getInstance();
        if (client.level == null) return;
        Screen screen = event.getScreen();
        if (!(screen instanceof ChatScreen)) return;
        if (RetroChatOverlay.getInstance().charTyped(event.getCodePoint(), event.getModifiers())) {
            event.setCanceled(true);
        }
    }
}
