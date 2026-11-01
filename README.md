# RetroChat

RetroChat is a client-side Minecraft mod that brings classic, 2000s/2010s instant messaging aesthetics to in-game private direct messages (`/msg`, `/tell`, `/w`).

It replaces clutter in the vanilla chat box with docked, retro-styled windows, collapsible sidebars, player avatars, and sound notifications.

---

## Features

- **Docked & Floating Chat Windows**:
  - Dedicated chat window for each player conversation.
  - **Pin** windows to keep them visible during gameplay.
  - **Minimize** windows into compact tabs docked along the bottom bar.
  - Unread message counters on minimized tabs.
  - Live dimension customization (width & height) with real-time visual preview.

- **Player Sidebar**:
  - Quick-access sidebar displaying recent conversations and online players.
  - Real-time search filter.
  - Online/offline status indicators.
  - Collapsible into a subtle tab on the right edge of the screen.

- **Player Management & Privacy**:
  - Block and unblock players directly from the settings menu.
  - Block custom usernames by typing them into the search bar.
  - Automatic suppression of incoming and outgoing messages from blocked players.
  - Option to hide direct messages from the vanilla chat box.

- **Notifications & Customization**:
  - Configurable sound alerts on incoming messages.
  - Toggle player head avatars and message timestamps.
  - UI scaling support (50% to 150%).
  - Full **Mod Menu** support with in-game configuration access.

- **Multi-Language Support**:
  - Built-in support for English, Turkish, German, French, Spanish, Russian, and Japanese.
  - Custom translations can be added via JSON files in `.minecraft/config/retrochat/lang/`.

- **History & 30-Day Auto Retention**:
  - Chat history is saved locally per server or world in `.minecraft/retrochat/history/`.
  - Messages older than 30 days are automatically pruned to keep disk usage lightweight and clean.

- **100% Client-Side**:
  - Works on standard vanilla and multiplayer servers without needing server-side installation.

---

## Controls

| Key | Action |
| --- | --- |
| `T` (Vanilla Chat) | Open chat and interact with RetroChat windows and the sidebar |
| `Escape` | Close overlay or unfocus input field |
| `Enter` | Send message in focused chat window |
| *Custom Key* | An optional dedicated overlay toggle key can be configured in **Options -> Controls -> Key Binds** (unbound by default) |

---

## Requirements

- **Minecraft**: `1.21.4`
- **Fabric Loader**: `0.16.x` or newer
- **Fabric API**
- **Java**: `21`

---

## Building from Source

To compile the mod from source using the Gradle wrapper:

```bash
./gradlew build
```

The compiled JAR file will be located in `build/libs/`.

---

## License

This project is licensed under the MIT License.
