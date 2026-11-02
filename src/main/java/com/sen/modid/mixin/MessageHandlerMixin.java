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

    @Inject(method = "onChatMessage", at = @At("HEAD"), cancellable = true)
    private void onChatMessage(SignedMessage message, GameProfile senderProfile, MessageType.Parameters params, CallbackInfo ci) {
        if (params.type().matchesKey(MessageType.MSG_COMMAND_INCOMING)) {
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
        } else if (params.type().matchesKey(MessageType.MSG_COMMAND_OUTGOING)) {
            String targetName = params.targetName().map(Text::getString).orElse("Unknown");
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
        if (params.type().matchesKey(MessageType.MSG_COMMAND_INCOMING)) {
            String senderName = params.name().getString();
            boolean isBlocked = RetroChatConfig.getInstance().isPlayerBlocked(senderName);
            String content = message.getString();
            long timestamp = Instant.now().toEpochMilli();

            DirectMessage dm = new DirectMessage(null, senderName, null, "me", content, timestamp, true, isBlocked, isBlocked);
            ConversationManager.getInstance().addMessage(senderName, null, dm);

            if (isBlocked || RetroChatConfig.getInstance().hideDmsInVanillaChat) {
                ci.cancel();
            }
        } else if (params.type().matchesKey(MessageType.MSG_COMMAND_OUTGOING)) {
            String targetName = params.targetName().map(Text::getString).orElse("Unknown");
            if (RetroChatConfig.getInstance().isPlayerBlocked(targetName)) {
                ci.cancel();
                return;
            }
            String content = message.getString();
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
        if (overlay) return;
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
