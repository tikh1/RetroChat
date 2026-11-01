package com.sen.modid;

import com.sen.modid.config.RetroChatConfig;
import com.sen.modid.manager.ConversationManager;
import com.sen.modid.ui.RetroChatOverlay;
import com.sen.modid.ui.RetroChatOverlayScreen;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import org.lwjgl.glfw.GLFW;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RetroChatClient implements ClientModInitializer {
    public static final String MOD_ID = "retrochat";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    private static KeyBinding toggleChatKey;
    private static Screen lastScreen = null;

    public static KeyBinding getToggleChatKey() {
        return toggleChatKey;
    }

    @Override
    public void onInitializeClient() {
        LOGGER.info("[RetroChat] Initializing client mod...");

        // Load config
        RetroChatConfig.getInstance().load();

        // Register keybind (Right Shift to toggle RetroChat overlay / sidebar)
        toggleChatKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
            "key.retrochat.toggle",
            InputUtil.Type.KEYSYM,
            GLFW.GLFW_KEY_RIGHT_SHIFT,
            "category.retrochat"
        ));

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            Screen currentScreen = client.currentScreen;
            if (lastScreen != null && currentScreen == null) {
                // minimize unpinned windows when returning to gameplay
                ConversationManager.getInstance().minimizeUnpinnedWindows();
                RetroChatOverlay.getInstance().unfocusAll();
            }
            lastScreen = currentScreen;

            while (toggleChatKey.wasPressed()) {
                if (client.world != null) {
                    if (client.currentScreen == null) {
                        client.setScreen(new RetroChatOverlayScreen());
                    } else if (client.currentScreen instanceof RetroChatOverlayScreen) {
                        client.setScreen(null);
                    }
                }
            }
        });

        LOGGER.info("[RetroChat] Initialized successfully on Fabric 1.21.3!");
    }
}
