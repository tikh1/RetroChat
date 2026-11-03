package com.sen.modid.mixin;

import com.mojang.authlib.GameProfile;
import com.sen.modid.config.RetroChatConfig;
import com.sen.modid.manager.ConversationManager;
import com.sen.modid.model.DirectMessage;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.message.MessageHandler;
import net.minecraft.network.message.MessageType;
import net.minecraft.network.message.SignedMessage;
import net.minecraft.text.Text;
import net.minecraft.text.TranslatableTextContent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.time.Instant;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Mixin(MessageHandler.class)
public class MessageHandlerMixin {

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

    private static boolean isMessageType(MessageType.Parameters params, net.minecraft.registry.RegistryKey<MessageType> key) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.world == null) return false;
        net.minecraft.registry.Registry<MessageType> registry = client.world.getRegistryManager().get(net.minecraft.registry.RegistryKeys.MESSAGE_TYPE);
        return registry.getKey(params.type()).map(k -> k.equals(key)).orElse(false);
    }

    @Inject(method = "onChatMessage", at = @At("HEAD"), cancellable = true)
    private void onChatMessage(SignedMessage message, GameProfile senderProfile, MessageType.Parameters params, CallbackInfo ci) {
        if (!RetroChatConfig.getInstance().modEnabled) return;
        if (isMessageType(params, MessageType.MSG_COMMAND_INCOMING)) {
            String senderName = senderProfile.getName();
            boolean isBlocked = RetroChatConfig.getInstance().isPlayerBlocked(senderName);
            UUID senderUuid = senderProfile.getId();
            String content = message.getContent().getString();
            long timestamp = Instant.now().toEpochMilli();

            DirectMessage dm = new DirectMessage(senderUuid, senderName, null, "me", content, timestamp, true, isBlocked, isBlocked);
            ConversationManager.getInstance().addMessage(senderName, senderUuid, dm);

            if (isBlocked || RetroChatConfig.getInstance().hideDmsInVanillaChat) {
                ci.cancel();
            }
        } else if (isMessageType(params, MessageType.MSG_COMMAND_OUTGOING)) {
            String targetName = params.targetName() != null ? params.targetName().getString() : "Unknown";
            if (RetroChatConfig.getInstance().isPlayerBlocked(targetName)) {
                ci.cancel();
                return;
            }
            String content = message.getContent().getString();
            long timestamp = Instant.now().toEpochMilli();

            if (!ConversationManager.getInstance().consumePendingOutgoing(targetName, content)) {
                DirectMessage dm = new DirectMessage(null, "me", null, targetName, content, timestamp, false, true);
                ConversationManager.getInstance().addMessage(targetName, null, dm);
            }

            if (RetroChatConfig.getInstance().hideDmsInVanillaChat) {
                ci.cancel();
            }
        }
    }

    @Inject(method = "onProfilelessMessage", at = @At("HEAD"), cancellable = true)
    private void onProfilelessMessage(Text message, MessageType.Parameters params, CallbackInfo ci) {
        if (!RetroChatConfig.getInstance().modEnabled) return;
        if (isMessageType(params, MessageType.MSG_COMMAND_INCOMING)) {
            String senderName = params.name().getString();
            boolean isBlocked = RetroChatConfig.getInstance().isPlayerBlocked(senderName);
            String content;
            if (message.getContent() instanceof TranslatableTextContent tc && tc.getArgs().length >= 2) {
                Object arg1 = tc.getArgs()[1];
                content = (arg1 instanceof Text t) ? t.getString() : arg1.toString();
            } else {
                content = message.getString();
            }
            long timestamp = Instant.now().toEpochMilli();

            DirectMessage dm = new DirectMessage(null, senderName, null, "me", content, timestamp, true, isBlocked, isBlocked);
            ConversationManager.getInstance().addMessage(senderName, null, dm);

            if (isBlocked || RetroChatConfig.getInstance().hideDmsInVanillaChat) {
                ci.cancel();
            }
        } else if (isMessageType(params, MessageType.MSG_COMMAND_OUTGOING)) {
            String targetName = params.targetName() != null ? params.targetName().getString() : "Unknown";
            if (RetroChatConfig.getInstance().isPlayerBlocked(targetName)) {
                ci.cancel();
                return;
            }
            String content;
            if (message.getContent() instanceof TranslatableTextContent tc && tc.getArgs().length >= 2) {
                Object arg1 = tc.getArgs()[1];
                content = (arg1 instanceof Text t) ? t.getString() : arg1.toString();
            } else {
                content = message.getString();
            }
            long timestamp = Instant.now().toEpochMilli();

            if (!ConversationManager.getInstance().consumePendingOutgoing(targetName, content)) {
                DirectMessage dm = new DirectMessage(null, "me", null, targetName, content, timestamp, false, true);
                ConversationManager.getInstance().addMessage(targetName, null, dm);
            }

            if (RetroChatConfig.getInstance().hideDmsInVanillaChat) {
                ci.cancel();
            }
        }
    }

    @Inject(method = "onGameMessage", at = @At("HEAD"), cancellable = true)
    private void onGameMessage(Text message, boolean overlay, CallbackInfo ci) {
        if (!RetroChatConfig.getInstance().modEnabled) return;
        if (overlay) return;

        if (message.getContent() instanceof TranslatableTextContent tc) {
            String key = tc.getKey();
            if ("commands.message.display.incoming".equals(key)) {
                Object[] args = tc.getArgs();
                if (args.length >= 2) {
                    String sender = (args[0] instanceof Text t) ? t.getString() : args[0].toString();
                    String content = (args[1] instanceof Text t) ? t.getString() : args[1].toString();
                    boolean isBlocked = RetroChatConfig.getInstance().isPlayerBlocked(sender);
                    if (isBlocked) {
                        ci.cancel();
                    }
                    DirectMessage dm = new DirectMessage(null, sender, null, "me", content, Instant.now().toEpochMilli(), true, isBlocked, isBlocked);
                    ConversationManager.getInstance().addMessage(sender, null, dm);
                    if (isBlocked || RetroChatConfig.getInstance().hideDmsInVanillaChat) {
                        ci.cancel();
                    }
                    return;
                }
            } else if ("commands.message.display.outgoing".equals(key)) {
                Object[] args = tc.getArgs();
                if (args.length >= 2) {
                    String target = (args[0] instanceof Text t) ? t.getString() : args[0].toString();
                    String content = (args[1] instanceof Text t) ? t.getString() : args[1].toString();
                    if (!ConversationManager.getInstance().consumePendingOutgoing(target, content)) {
                        DirectMessage dm = new DirectMessage(null, "me", null, target, content, Instant.now().toEpochMilli(), false, true);
                        ConversationManager.getInstance().addMessage(target, null, dm);
                    }
                    if (RetroChatConfig.getInstance().hideDmsInVanillaChat) {
                        ci.cancel();
                    }
                    return;
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
                    ci.cancel();
                }
                return;
            }
        }

        for (Pattern p : OUTGOING_PATTERNS) {
            Matcher m = p.matcher(raw);
            if (m.matches()) {
                String target = m.group(1);
                if (RetroChatConfig.getInstance().isPlayerBlocked(target)) {
                    ci.cancel();
                    return;
                }
                String content = m.group(2);
                if (!ConversationManager.getInstance().consumePendingOutgoing(target, content)) {
                    DirectMessage dm = new DirectMessage(null, "me", null, target, content, Instant.now().toEpochMilli(), false, true);
                    ConversationManager.getInstance().addMessage(target, null, dm);
                }
                if (RetroChatConfig.getInstance().hideDmsInVanillaChat) {
                    ci.cancel();
                }
                return;
            }
        }
    }
}
