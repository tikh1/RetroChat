package com.sen.modid.mixin;

import com.sen.modid.ui.RetroChatOverlay;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.client.gui.screen.Screen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Screen.class)
public class ScreenMixin {

    @Inject(method = "renderWithTooltip", at = @At("TAIL"))
    private void onRenderWithTooltip(DrawContext context, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        if (MinecraftClient.getInstance().world == null) return;
        Screen screen = (Screen) (Object) this;
        if (screen instanceof ChatScreen) {
            RetroChatOverlay.getInstance().render(context, mouseX, mouseY, delta);
        }
    }
}
