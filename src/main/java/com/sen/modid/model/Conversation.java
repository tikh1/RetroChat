package com.sen.modid.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

public class Conversation {
    private final String playerName;
    private UUID playerUuid;
    private final List<DirectMessage> messages = new ArrayList<>();
    private int unreadCount = 0;
    private long lastMessageTimestamp = 0;
    private boolean pinned = false;

    public Conversation(String playerName, UUID playerUuid) {
        this.playerName = playerName;
        this.playerUuid = playerUuid;
    }

    public String getPlayerName() {
        return playerName;
    }

    public UUID getPlayerUuid() {
        return playerUuid;
    }

    public void setPlayerUuid(UUID playerUuid) {
        if (this.playerUuid == null && playerUuid != null) {
            this.playerUuid = playerUuid;
        }
    }

    public List<DirectMessage> getMessages() {
        return messages;
    }

    public int getUnreadCount() {
        return unreadCount;
    }

    public long getLastMessageTimestamp() {
        return lastMessageTimestamp;
    }

    public boolean isPinned() {
        return pinned;
    }

    public void setPinned(boolean pinned) {
        this.pinned = pinned;
    }

    public synchronized boolean addMessage(DirectMessage message) {
        messages.add(message);
        this.lastMessageTimestamp = message.getTimestamp();
        if (message.isIncoming() && !message.isRead()) {
            unreadCount++;
        }
        return true;
    }

    public synchronized void pruneExpiredMessages(long cutoffTimestamp) {
        messages.removeIf(m -> m.getTimestamp() < cutoffTimestamp);
        int unread = 0;
        for (DirectMessage m : messages) {
            if (m.isIncoming() && !m.isRead()) {
                unread++;
            }
        }
        this.unreadCount = unread;
    }

    public synchronized void cleanDuplicateMessages() {
        if (messages.size() <= 1) return;
        List<DirectMessage> cleaned = new ArrayList<>();
        DirectMessage prev = null;
        for (DirectMessage m : messages) {
            // skip exact duplicates with matching timestamp
            if (prev != null
                && prev.isIncoming() == m.isIncoming()
                && Objects.equals(prev.getContent(), m.getContent())
                && prev.getTimestamp() == m.getTimestamp()) {
                continue;
            }
            cleaned.add(m);
            prev = m;
        }
        messages.clear();
        messages.addAll(cleaned);
    }

    public void markAllAsRead() {
        for (DirectMessage msg : messages) {
            msg.setRead(true);
        }
        unreadCount = 0;
    }
}
