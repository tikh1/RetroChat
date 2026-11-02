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
import net.minecraftforge.client.ConfigScreenHandler;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
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

    public RetroChatClient() {
        LOGGER.info("[RetroChat] Initializing client mod...");

        RetroChatConfig.getInstance();
        com.sen.modid.lang.LanguageManager.getInstance();
        ConversationManager.getInstance();

        ModLoadingContext.get().registerExtensionPoint(
            ConfigScreenHandler.ConfigScreenFactory.class,
            () -> new ConfigScreenHandler.ConfigScreenFactory((client, parentScreen) -> new RetroChatConfigScreen(parentScreen))
        );

        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();
        modEventBus.addListener(this::registerKeyMappings);
        MinecraftForge.EVENT_BUS.addListener(this::onClientTick);
        MinecraftForge.EVENT_BUS.register(new RetroChatClientEventHandler());

        LOGGER.info("[RetroChat] Initialized successfully on Forge 1.21.1!");
    }

    private void registerKeyMappings(RegisterKeyMappingsEvent event) {
        toggleChatKey = new KeyMapping(
            "key.retrochat.toggle",
            InputConstants.Type.KEYSYM,
            InputConstants.UNKNOWN.getValue(),
            "category.retrochat"
        );
        event.register(toggleChatKey);
    }

    private void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        Minecraft client = Minecraft.getInstance();
        Screen currentScreen = client.screen;
        if (lastScreen != null && currentScreen == null) {
            ConversationManager.getInstance().minimizeUnpinnedWindows();
            RetroChatOverlay.getInstance().unfocusAll();
        }
        lastScreen = currentScreen;

        while (toggleChatKey != null && toggleChatKey.consumeClick()) {
            if (!RetroChatConfig.getInstance().modEnabled) continue;
            if (client.level != null) {
                if (client.screen == null) {
                    client.setScreen(new RetroChatOverlayScreen());
                } else if (client.screen instanceof RetroChatOverlayScreen) {
                    client.setScreen(null);
                }
            }
        }
    }
}
