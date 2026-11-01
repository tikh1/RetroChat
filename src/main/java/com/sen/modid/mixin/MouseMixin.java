package com.sen.modid.mixin;

import com.sen.modid.ui.RetroChatConfigScreen;
import com.sen.modid.ui.RetroChatOverlay;
import com.sen.modid.ui.RetroChatOverlayScreen;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.Mouse;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Mouse.class)
public class MouseMixin {

    @Shadow @Final private MinecraftClient client;
    @Shadow private double x;
    @Shadow private double y;

    @Inject(method = "onMouseButton", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/screen/Screen;mouseClicked(DDI)Z"), cancellable = true)
    private void onScreenMouseClicked(long window, int button, int action, int mods, CallbackInfo ci) {
        if (client.currentScreen instanceof RetroChatConfigScreen || client.currentScreen instanceof RetroChatOverlayScreen) return;

        double mouseX = this.x * (double)client.getWindow().getScaledWidth() / (double)client.getWindow().getWidth();
        double mouseY = this.y * (double)client.getWindow().getScaledHeight() / (double)client.getWindow().getHeight();

        if (action == 1) { // GLFW_PRESS
            if (RetroChatOverlay.getInstance().mouseClicked(mouseX, mouseY, button)) {
                ci.cancel();
            }
        }
    }

    @Inject(method = "onMouseScroll", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/screen/Screen;mouseScrolled(DDDD)Z"), cancellable = true)
    private void onScreenMouseScrolled(long window, double horizontal, double vertical, CallbackInfo ci) {
        if (client.currentScreen instanceof RetroChatConfigScreen || client.currentScreen instanceof RetroChatOverlayScreen) return;

        double mouseX = this.x * (double)client.getWindow().getScaledWidth() / (double)client.getWindow().getWidth();
        double mouseY = this.y * (double)client.getWindow().getScaledHeight() / (double)client.getWindow().getHeight();

        if (RetroChatOverlay.getInstance().mouseScrolled(mouseX, mouseY, horizontal, vertical)) {
            ci.cancel();
        }
    }
}
