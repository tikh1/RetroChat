package com.sen.modid.model;

import java.util.UUID;

public class DirectMessage {
    private final String id;
    private final UUID senderUuid;
    private final String senderName;
    private final UUID receiverUuid;
    private final String receiverName;
    private final String content;
    private final long timestamp;
    private final boolean incoming;
    private boolean read;
    private final boolean wasBlocked;

    public DirectMessage(UUID senderUuid, String senderName, UUID receiverUuid, String receiverName, String content, long timestamp, boolean incoming, boolean read) {
        this(senderUuid, senderName, receiverUuid, receiverName, content, timestamp, incoming, read, false);
    }

    public DirectMessage(UUID senderUuid, String senderName, UUID receiverUuid, String receiverName, String content, long timestamp, boolean incoming, boolean read, boolean wasBlocked) {
        this.id = UUID.randomUUID().toString();
        this.senderUuid = senderUuid;
        this.senderName = senderName;
        this.receiverUuid = receiverUuid;
        this.receiverName = receiverName;
        this.content = content;
        this.timestamp = timestamp;
        this.incoming = incoming;
        this.read = read;
        this.wasBlocked = wasBlocked;
    }

    public boolean wasBlocked() {
        return wasBlocked;
    }

    public String getId() {
        return id;
    }

    public UUID getSenderUuid() {
        return senderUuid;
    }

    public String getSenderName() {
        return senderName;
    }

    public UUID getReceiverUuid() {
        return receiverUuid;
    }

    public String getReceiverName() {
        return receiverName;
    }

    public String getContent() {
        return content;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public boolean isIncoming() {
        return incoming;
    }

    public boolean isRead() {
        return read;
    }

    public void setRead(boolean read) {
        this.read = read;
    }
}
