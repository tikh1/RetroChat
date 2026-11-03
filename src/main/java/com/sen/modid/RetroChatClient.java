package com.sen.modid;

import com.sen.modid.config.RetroChatConfig;
import com.sen.modid.manager.ConversationManager;
import com.sen.modid.ui.RetroChatConfigScreen;
import net.minecraft.client.KeyMapping;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.Mod;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Mod(RetroChatClient.MOD_ID)
public class RetroChatClient {
    public static final String MOD_ID = "retrochat";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    private static KeyMapping toggleChatKey;

    public static KeyMapping getToggleChatKey() {
        return toggleChatKey;
    }

    public static void setToggleChatKey(KeyMapping key) {
        toggleChatKey = key;
    }

    public RetroChatClient() {
        LOGGER.info("[RetroChat] Initializing client mod...");

        RetroChatConfig.getInstance();
        com.sen.modid.lang.LanguageManager.getInstance();
        ConversationManager.getInstance();

        MinecraftForge.registerConfigScreen(RetroChatConfigScreen::new);

        MinecraftForge.EVENT_BUS.register(new RetroChatClientEventHandler());

        LOGGER.info("[RetroChat] Initialized successfully on Forge 26.1!");
    }
}
