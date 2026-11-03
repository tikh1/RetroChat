package com.sen.modid.ui;

import com.sen.modid.RetroChatClient;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
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
    public void extractRenderState(GuiGraphicsExtractor context, int mouseX, int mouseY, float delta) {
        super.extractRenderState(context, mouseX, mouseY, delta);
        RetroChatOverlay.getInstance().render(context, mouseX, mouseY, delta);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (RetroChatOverlay.getInstance().mouseClicked(event.x(), event.y(), event.button())) {
            return true;
        }
        return super.mouseClicked(event, doubleClick);
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent event, double deltaX, double deltaY) {
        if (RetroChatOverlay.getInstance().mouseDragged(event.x(), event.y(), event.button(), deltaX, deltaY)) {
            return true;
        }
        return super.mouseDragged(event, deltaX, deltaY);
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        if (RetroChatOverlay.getInstance().mouseReleased(event.x(), event.y(), event.button())) {
            return true;
        }
        return super.mouseReleased(event);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        if (RetroChatOverlay.getInstance().mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount)) {
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        if (RetroChatOverlay.getInstance().keyPressed(event.key(), event.keycode(), event.modifiers())) {
            return true;
        }

        if (RetroChatClient.getToggleChatKey() != null && RetroChatClient.getToggleChatKey().matches(event) && !RetroChatOverlay.getInstance().isAnyInputFocused()) {
            this.onClose();
            return true;
        }
        if (event.key() == 256) {
            this.onClose();
            return true;
        }
        return super.keyPressed(event);
    }

    @Override
    public boolean charTyped(CharacterEvent event) {
        if (RetroChatOverlay.getInstance().charTyped((char) event.codepoint(), 0)) {
            return true;
        }
        return super.charTyped(event);
    }
}
