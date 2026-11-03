package com.sen.modid;

import com.mojang.blaze3d.platform.InputConstants;
import com.sen.modid.config.RetroChatConfig;
import com.sen.modid.manager.ConversationManager;
import com.sen.modid.ui.RetroChatConfigScreen;
import com.sen.modid.ui.RetroChatOverlay;
import com.sen.modid.ui.RetroChatOverlayScreen;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.resources.Identifier;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;
import net.neoforged.neoforge.common.NeoForge;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Mod(RetroChatClient.MOD_ID)
public class RetroChatClient {
    public static final String MOD_ID = "retrochat";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    private static KeyMapping toggleChatKey;
    private static Screen lastScreen = null;

    public static KeyMapping getToggleChatKey() {
        return toggleChatKey;
    }

    public RetroChatClient(IEventBus modEventBus, ModContainer modContainer) {
        LOGGER.info("[RetroChat] Initializing client mod...");

        RetroChatConfig.getInstance();
        com.sen.modid.lang.LanguageManager.getInstance();
        ConversationManager.getInstance();

        modContainer.registerExtensionPoint(
            IConfigScreenFactory.class,
            (container, parentScreen) -> new RetroChatConfigScreen(parentScreen)
        );

        modEventBus.addListener(this::registerKeyMappings);
        NeoForge.EVENT_BUS.addListener(this::onClientTick);
        NeoForge.EVENT_BUS.register(new RetroChatClientEventHandler());

        LOGGER.info("[RetroChat] Initialized successfully on NeoForge 26.2!");
    }

    private void registerKeyMappings(RegisterKeyMappingsEvent event) {
        toggleChatKey = new KeyMapping(
            "key.retrochat.toggle",
            InputConstants.Type.KEYSYM,
            InputConstants.UNKNOWN.getValue(),
            KeyMapping.Category.register(Identifier.parse("retrochat:category"))
        );
        event.register(toggleChatKey);
    }

    private void onClientTick(ClientTickEvent.Post event) {
        Minecraft client = Minecraft.getInstance();
        Screen currentScreen = client.gui.screen();
        if (lastScreen != null && currentScreen == null) {
            ConversationManager.getInstance().minimizeUnpinnedWindows();
            RetroChatOverlay.getInstance().unfocusAll();
        }
        lastScreen = currentScreen;

        while (toggleChatKey != null && toggleChatKey.consumeClick()) {
            if (!RetroChatConfig.getInstance().modEnabled) continue;
            if (client.level != null) {
                if (client.gui.screen() == null) {
                    client.gui.setScreen(new RetroChatOverlayScreen());
                } else if (client.gui.screen() instanceof RetroChatOverlayScreen) {
                    client.gui.setScreen(null);
                }
            }
        }
    }
}
