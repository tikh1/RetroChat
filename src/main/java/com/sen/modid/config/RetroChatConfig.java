package com.sen.modid.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.neoforged.fml.loading.FMLPaths;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

public class RetroChatConfig {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final File CONFIG_FILE = FMLPaths.CONFIGDIR.get().resolve("retrochat.json").toFile();

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

    public static class ServerUiState {
        public boolean sidebarCollapsed = false;
        public Map<String, String> windowStates = new LinkedHashMap<>();
    }

    public Map<String, ServerUiState> serverStates = new LinkedHashMap<>();

    public static RetroChatConfig getInstance() {
        if (instance == null) {
            instance = load();
        }
        return instance;
    }

    public static RetroChatConfig load() {
        if (CONFIG_FILE.exists()) {
            try (FileReader reader = new FileReader(CONFIG_FILE, java.nio.charset.StandardCharsets.UTF_8)) {
                RetroChatConfig config = GSON.fromJson(reader, RetroChatConfig.class);
                if (config != null) {
                    if (config.blockedPlayers == null) {
                        config.blockedPlayers = new HashSet<>();
                    }
                    if (config.serverStates == null) {
                        config.serverStates = new LinkedHashMap<>();
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
            try (FileWriter writer = new FileWriter(CONFIG_FILE, java.nio.charset.StandardCharsets.UTF_8)) {
                GSON.toJson(this, writer);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public synchronized ServerUiState getServerUiState(String serverKey) {
        if (serverKey == null || serverKey.isEmpty()) {
            serverKey = "default";
        }
        if (serverStates == null) {
            serverStates = new LinkedHashMap<>();
        }
        ServerUiState state = serverStates.get(serverKey);
        if (state == null) {
            state = new ServerUiState();
            serverStates.put(serverKey, state);
        }
        if (state.windowStates == null) {
            state.windowStates = new LinkedHashMap<>();
        }
        return state;
    }

    public synchronized void setServerSidebarCollapsed(String serverKey, boolean collapsed) {
        ServerUiState state = getServerUiState(serverKey);
        state.sidebarCollapsed = collapsed;
        save();
    }

    public synchronized void setServerWindowState(String serverKey, String playerName, String windowState) {
        if (playerName == null) return;
        ServerUiState state = getServerUiState(serverKey);
        if ("CLOSED".equalsIgnoreCase(windowState) || windowState == null) {
            state.windowStates.remove(playerName.toLowerCase());
        } else {
            state.windowStates.put(playerName.toLowerCase(), windowState.toUpperCase());
        }
        save();
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
