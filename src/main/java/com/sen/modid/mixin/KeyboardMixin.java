package com.sen.modid.mixin;

import com.sen.modid.ui.RetroChatConfigScreen;
import com.sen.modid.ui.RetroChatOverlay;
import com.sen.modid.ui.RetroChatOverlayScreen;
import net.minecraft.client.Keyboard;
import net.minecraft.client.MinecraftClient;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Keyboard.class)
public class KeyboardMixin {

    @Shadow @Final private MinecraftClient client;

    @Inject(method = "onKey", at = @At("HEAD"), cancellable = true)
    private void onScreenKeyPressed(long window, int key, int scancode, int action, int mods, CallbackInfo ci) {
        if (client.world == null) return;
        if (client.currentScreen == null) return;
        if (client.currentScreen instanceof RetroChatConfigScreen || client.currentScreen instanceof RetroChatOverlayScreen) return;

        if (action != 0) { // press or repeat
            if (RetroChatOverlay.getInstance().keyPressed(key, scancode, mods)) {
                ci.cancel();
            }
        }
    }

    @Inject(method = "onChar", at = @At("HEAD"), cancellable = true)
    private void onScreenCharTyped(long window, int codePoint, int modifiers, CallbackInfo ci) {
        if (client.world == null) return;
        if (client.currentScreen == null) return;
        if (client.currentScreen instanceof RetroChatConfigScreen || client.currentScreen instanceof RetroChatOverlayScreen) return;

        char[] chars = Character.toChars(codePoint);
        for (char c : chars) {
            if (RetroChatOverlay.getInstance().charTyped(c, modifiers)) {
                ci.cancel();
                return;
            }
        }
    }
}
