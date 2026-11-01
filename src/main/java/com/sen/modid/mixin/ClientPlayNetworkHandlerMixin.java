package com.sen.modid.mixin;

import com.sen.modid.manager.ConversationManager;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.network.packet.s2c.play.ChatMessageS2CPacket;
import net.minecraft.network.packet.s2c.play.GameJoinS2CPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientPlayNetworkHandler.class)
public class ClientPlayNetworkHandlerMixin {

    @Inject(method = "onGameJoin", at = @At("TAIL"))
    private void onGameJoinPacket(GameJoinS2CPacket packet, CallbackInfo ci) {
        ConversationManager.getInstance().updateServerScope();
    }

    @Inject(method = "clearWorld", at = @At("HEAD"))
    private void onClearWorld(CallbackInfo ci) {
        ConversationManager.getInstance().resetScope();
    }

    @Inject(method = "onChatMessage", at = @At("HEAD"))
    private void onChatMessagePacket(ChatMessageS2CPacket packet, CallbackInfo ci) {
        ConversationManager.getInstance().updateServerScope();
    }
}
