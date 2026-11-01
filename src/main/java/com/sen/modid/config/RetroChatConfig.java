package com.sen.modid.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.HashSet;
import java.util.Set;

public class RetroChatConfig {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final File CONFIG_FILE = new File(FabricLoader.getInstance().getConfigDir().toFile(), "retrochat.json");

    private static RetroChatConfig instance;

    public boolean hideDmsInVanillaChat = true;
    public boolean soundEnabled = true;
    public float soundVolume = 0.5f;

    public Set<String> blockedPlayers = new HashSet<>();

    public int windowWidth = 220;
    public int windowHeight = 160;
    public float uiScale = 1.0f;

    public boolean showTimestamps = true;
    public boolean showPlayerHeads = true;
    public String language = "auto";

    public static RetroChatConfig getInstance() {
        if (instance == null) {
            instance = load();
        }
        return instance;
    }

    public static RetroChatConfig load() {
        if (CONFIG_FILE.exists()) {
            try (FileReader reader = new FileReader(CONFIG_FILE)) {
                RetroChatConfig config = GSON.fromJson(reader, RetroChatConfig.class);
                if (config != null) {
                    if (config.blockedPlayers == null) {
                        config.blockedPlayers = new HashSet<>();
                    }
                    return config;
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        RetroChatConfig config = new RetroChatConfig();
        config.save();
        return config;
    }

    public void save() {
        try {
            CONFIG_FILE.getParentFile().mkdirs();
            try (FileWriter writer = new FileWriter(CONFIG_FILE)) {
                GSON.toJson(this, writer);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public boolean isPlayerBlocked(String playerName) {
        if (playerName == null) return false;
        return blockedPlayers.contains(playerName.toLowerCase());
    }

    public void blockPlayer(String playerName) {
        if (playerName != null) {
            blockedPlayers.add(playerName.toLowerCase());
            save();
        }
    }

    public void unblockPlayer(String playerName) {
        if (playerName != null) {
            blockedPlayers.remove(playerName.toLowerCase());
            save();
        }
    }
}
