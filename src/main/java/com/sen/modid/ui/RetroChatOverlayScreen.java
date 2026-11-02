package com.sen.modid.ui;

import com.sen.modid.RetroChatClient;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public class RetroChatOverlayScreen extends Screen {

    public RetroChatOverlayScreen() {
        super(Component.literal("RetroChat Overlay"));
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    protected void init() {
        super.init();
        RetroChatOverlay.getInstance().focusFirstOpenWindow();
    }

    @Override
    public void render(GuiGraphics context, int mouseX, int mouseY, float delta) {
        RetroChatOverlay.getInstance().render(context, mouseX, mouseY, delta);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (RetroChatOverlay.getInstance().mouseClicked(mouseX, mouseY, button)) {
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
        if (RetroChatOverlay.getInstance().mouseDragged(mouseX, mouseY, button, deltaX, deltaY)) {
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, deltaX, deltaY);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (RetroChatOverlay.getInstance().mouseReleased(mouseX, mouseY, button)) {
            return true;
        }
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double amount) {
        if (RetroChatOverlay.getInstance().mouseScrolled(mouseX, mouseY, amount)) {
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, amount);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (RetroChatOverlay.getInstance().keyPressed(keyCode, scanCode, modifiers)) {
            return true;
        }

        if (RetroChatClient.getToggleChatKey() != null && RetroChatClient.getToggleChatKey().matches(keyCode, scanCode) && !RetroChatOverlay.getInstance().isAnyInputFocused()) {
            this.onClose();
            return true;
        }
        if (keyCode == 256) {
            this.onClose();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean charTyped(char chr, int modifiers) {
        if (RetroChatOverlay.getInstance().charTyped(chr, modifiers)) {
            return true;
        }
        return super.charTyped(chr, modifiers);
    }
}
