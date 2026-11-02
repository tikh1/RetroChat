package com.sen.modid.manager;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.sen.modid.config.RetroChatConfig;
import com.sen.modid.model.Conversation;
import com.sen.modid.model.DirectMessage;
import com.sen.modid.ui.RetroChatOverlay;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ServerInfo;
import net.minecraft.client.sound.PositionedSoundInstance;
import net.minecraft.sound.SoundEvents;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.lang.reflect.Type;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;

public class ConversationManager {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final File STORAGE_DIR = new File(FabricLoader.getInstance().getGameDir().toFile(), "retrochat/history");
    private static final long MESSAGE_RETENTION_MS = 30L * 24 * 60 * 60 * 1000L;

    private static ConversationManager instance;

    public enum WindowState {
        CLOSED,
        OPEN,
        MINIMIZED
    }

    private final Map<String, Conversation> conversations = new LinkedHashMap<>();
    private final Map<String, WindowState> windowStates = new LinkedHashMap<>();
    private final List<ConversationListener> listeners = new CopyOnWriteArrayList<>();
    private final List<PendingOutgoing> pendingOutgoings = new ArrayList<>();

    private static class PendingOutgoing {
        final String target;
        final String content;
        final long timestamp;

        PendingOutgoing(String target, String content, long timestamp) {
            this.target = target.toLowerCase();
            this.content = content.trim();
            this.timestamp = timestamp;
        }
    }

    private String currentServerKey = "default";

    public interface ConversationListener {
        void onMessageReceived(DirectMessage message);
        void onConversationUpdated(Conversation conversation);
        void onWindowStateChanged(String playerName, WindowState state);
    }

    public static ConversationManager getInstance() {
        if (instance == null) {
            instance = new ConversationManager();
        }
        return instance;
    }

    private ConversationManager() {
        STORAGE_DIR.mkdirs();
    }

    public String getCurrentServerKey() {
        return currentServerKey;
    }

    public void updateServerScope() {
        MinecraftClient client = MinecraftClient.getInstance();
        String newKey = "default";
        if (client.isInSingleplayer() && client.getServer() != null) {
            newKey = "sp_" + client.getServer().getSaveProperties().getLevelName();
        } else {
            ServerInfo server = client.getCurrentServerEntry();
            if (server != null && server.address != null && !server.address.trim().isEmpty()) {
                newKey = "mp_" + server.address.trim().replace(':', '_').replace('/', '_');
            } else if (client.getNetworkHandler() != null && client.getNetworkHandler().getConnection() != null) {
                java.net.SocketAddress socketAddr = client.getNetworkHandler().getConnection().getAddress();
                if (socketAddr instanceof java.net.InetSocketAddress inet) {
                    newKey = "mp_" + inet.getHostString() + "_" + inet.getPort();
                } else if (socketAddr != null) {
                    newKey = "mp_" + socketAddr.toString().replace(':', '_').replace('/', '_');
                }
            }
        }

        if (!newKey.equals("default") && !newKey.equals(currentServerKey)) {
            saveCurrentScope();
            currentServerKey = newKey;
            RetroChatOverlay overlay = RetroChatOverlay.getInstance();
            if (overlay != null) {
                overlay.clearWindows();
            }
            loadCurrentScope();
            restoreServerUiState();
        }
    }

    private void restoreServerUiState() {
        RetroChatConfig config = RetroChatConfig.getInstance();
        RetroChatConfig.ServerUiState uiState = config.getServerUiState(currentServerKey);

        RetroChatOverlay overlay = RetroChatOverlay.getInstance();
        if (overlay != null && overlay.getSidebarWidget() != null) {
            overlay.getSidebarWidget().setCollapsed(uiState.sidebarCollapsed, false);
        }

        windowStates.clear();
        for (Map.Entry<String, String> entry : uiState.windowStates.entrySet()) {
            try {
                WindowState state = WindowState.valueOf(entry.getValue());
                if (state != WindowState.CLOSED) {
                    windowStates.put(entry.getKey().toLowerCase(), state);
                    if (overlay != null) {
                        overlay.getOrCreateWindow(entry.getKey());
                    }
                    for (ConversationListener listener : listeners) {
                        listener.onWindowStateChanged(entry.getKey(), state);
                    }
                }
            } catch (Exception ignored) {}
        }
    }

    public synchronized Conversation getOrCreateConversation(String playerName, UUID playerUuid) {
        updateServerScope();
        String key = playerName.toLowerCase();
        Conversation conv = conversations.get(key);
        if (conv == null) {
            conv = new Conversation(playerName, playerUuid);
            conversations.put(key, conv);
        } else if (playerUuid != null) {
            conv.setPlayerUuid(playerUuid);
        }
        return conv;
    }

    public synchronized Conversation getConversation(String playerName) {
        if (playerName == null) return null;
        updateServerScope();
        return conversations.get(playerName.toLowerCase());
    }

    public synchronized List<Conversation> getConversationsMru() {
        updateServerScope();
        List<Conversation> list = new ArrayList<>(conversations.values());
        list.sort((a, b) -> Long.compare(b.getLastMessageTimestamp(), a.getLastMessageTimestamp()));
        return list;
    }

    public synchronized void addLocalOutgoingMessage(String targetName, UUID targetUuid, DirectMessage message) {
        if (targetName != null && message != null && message.getContent() != null) {
            pendingOutgoings.add(new PendingOutgoing(targetName, message.getContent(), System.currentTimeMillis()));
        }
        addMessage(targetName, targetUuid, message);
    }

    public synchronized boolean consumePendingOutgoing(String targetName, String content) {
        if (targetName == null || content == null) return false;
        long now = System.currentTimeMillis();
        pendingOutgoings.removeIf(po -> now - po.timestamp > 5000);

        String targetKey = targetName.toLowerCase();
        String trimmedContent = content.trim();

        for (int i = 0; i < pendingOutgoings.size(); i++) {
            PendingOutgoing po = pendingOutgoings.get(i);
            if (po.target.equals(targetKey) && po.content.equals(trimmedContent)) {
                pendingOutgoings.remove(i);
                return true;
            }
        }
        return false;
    }

    public synchronized void addMessage(String partnerName, UUID partnerUuid, DirectMessage message) {
        updateServerScope();
        boolean isBlocked = RetroChatConfig.getInstance().isPlayerBlocked(partnerName) || message.wasBlocked();

        Conversation conv = getOrCreateConversation(partnerName, partnerUuid);

        boolean added = conv.addMessage(message);
        if (!added) {
            return;
        }

        if (!isBlocked) {
            if (message.isIncoming() && RetroChatConfig.getInstance().soundEnabled) {
                playNotificationSound();
            }

            if (message.isIncoming()) {
                WindowState currentState = getWindowState(partnerName);
                if (currentState == WindowState.CLOSED) {
                    setWindowState(partnerName, WindowState.MINIMIZED);
                }
            }
        }

        saveCurrentScope();

        for (ConversationListener listener : listeners) {
            listener.onMessageReceived(message);
            listener.onConversationUpdated(conv);
        }
    }

    public void playNotificationSound() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client != null && client.getSoundManager() != null) {
            float volume = RetroChatConfig.getInstance().soundVolume;
            if (volume > 0.0f) {
                client.getSoundManager().play(
                    PositionedSoundInstance.master(SoundEvents.BLOCK_NOTE_BLOCK_BELL.value(), 1.0f, volume)
                );
            }
        }
    }

    public WindowState getWindowState(String playerName) {
        if (playerName == null) return WindowState.CLOSED;
        return windowStates.getOrDefault(playerName.toLowerCase(), WindowState.CLOSED);
    }

    public void setWindowState(String playerName, WindowState state) {
        if (playerName == null) return;
        String key = playerName.toLowerCase();
        if (state == WindowState.CLOSED) {
            windowStates.remove(key);
        } else {
            windowStates.put(key, state);
        }
        RetroChatConfig.getInstance().setServerWindowState(currentServerKey, playerName, state.name());
        if (state == WindowState.OPEN) {
            markAsRead(playerName);
        }
        for (ConversationListener listener : listeners) {
            listener.onWindowStateChanged(playerName, state);
        }
    }

    public void markAsRead(String playerName) {
        if (playerName == null) return;
        Conversation conv = getConversation(playerName);
        if (conv != null && conv.getUnreadCount() > 0) {
            conv.markAllAsRead();
            saveCurrentScope();
            for (ConversationListener listener : listeners) {
                listener.onConversationUpdated(conv);
            }
        }
    }

    public boolean isWindowPinned(String playerName) {
        if (playerName == null) return false;
        Conversation conv = getConversation(playerName);
        return conv != null && conv.isPinned();
    }

    public void setWindowPinned(String playerName, boolean pinned) {
        if (playerName == null) return;
        Conversation conv = getOrCreateConversation(playerName, null);
        conv.setPinned(pinned);
        saveCurrentScope();
        for (ConversationListener listener : listeners) {
            listener.onWindowStateChanged(playerName, getWindowState(playerName));
        }
    }

    public void toggleWindowPinned(String playerName) {
        setWindowPinned(playerName, !isWindowPinned(playerName));
    }

    public synchronized void minimizeUnpinnedWindows() {
        for (Map.Entry<String, WindowState> entry : new HashMap<>(windowStates).entrySet()) {
            if (entry.getValue() == WindowState.OPEN) {
                String player = entry.getKey();
                if (!isWindowPinned(player)) {
                    setWindowState(player, WindowState.MINIMIZED);
                }
            }
        }
    }

    public void openWindow(String playerName) {
        updateServerScope();
        setWindowState(playerName, WindowState.OPEN);
    }

    public void minimizeWindow(String playerName) {
        setWindowState(playerName, WindowState.MINIMIZED);
    }

    public void closeWindow(String playerName) {
        setWindowState(playerName, WindowState.CLOSED);
    }

    public Map<String, WindowState> getActiveWindowStates() {
        return Collections.unmodifiableMap(windowStates);
    }

    public synchronized void resetScope() {
        saveCurrentScope();
        currentServerKey = "";
        conversations.clear();
        windowStates.clear();
        RetroChatOverlay overlay = RetroChatOverlay.getInstance();
        if (overlay != null) {
            overlay.clearWindows();
        }
    }

    public void addListener(ConversationListener listener) {
        listeners.add(listener);
    }

    public void removeListener(ConversationListener listener) {
        listeners.remove(listener);
    }

    private File getFileForScope(String scopeKey) {
        String safeName = scopeKey.replaceAll("[^a-zA-Z0-9_.-]", "_") + ".json";
        return new File(STORAGE_DIR, safeName);
    }

    public synchronized void saveCurrentScope() {
        if (currentServerKey == null || currentServerKey.trim().isEmpty() || currentServerKey.equals("default")) {
            return;
        }
        try {
            long cutoff = System.currentTimeMillis() - MESSAGE_RETENTION_MS;
            conversations.values().removeIf(c -> {
                c.pruneExpiredMessages(cutoff);
                return c.getMessages().isEmpty() && !c.isPinned();
            });

            File file = getFileForScope(currentServerKey);
            file.getParentFile().mkdirs();
            try (FileWriter writer = new FileWriter(file, java.nio.charset.StandardCharsets.UTF_8)) {
                GSON.toJson(conversations, writer);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public synchronized void loadCurrentScope() {
        conversations.clear();
        if (currentServerKey == null || currentServerKey.trim().isEmpty() || currentServerKey.equals("default")) {
            return;
        }
        try {
            File file = getFileForScope(currentServerKey);
            if (file.exists()) {
                try (FileReader reader = new FileReader(file, java.nio.charset.StandardCharsets.UTF_8)) {
                    Type type = new TypeToken<Map<String, Conversation>>(){}.getType();
                    Map<String, Conversation> loaded = GSON.fromJson(reader, type);
                    if (loaded != null) {
                        long cutoff = System.currentTimeMillis() - MESSAGE_RETENTION_MS;
                        loaded.values().removeIf(c -> {
                            c.pruneExpiredMessages(cutoff);
                            c.cleanDuplicateMessages();
                            return c.getMessages().isEmpty() && !c.isPinned();
                        });
                        conversations.putAll(loaded);
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public int getTotalUnreadCount() {
        int total = 0;
        for (Conversation c : conversations.values()) {
            total += c.getUnreadCount();
        }
        return total;
    }
}
