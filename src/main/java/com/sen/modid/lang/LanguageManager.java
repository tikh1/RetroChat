package com.sen.modid.lang;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.sen.modid.config.RetroChatConfig;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.util.*;

public class LanguageManager {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Type MAP_TYPE = new TypeToken<Map<String, String>>() {}.getType();

    private static LanguageManager instance;

    private final File langDirectory;
    private final Map<String, LanguageEntry> availableLanguages = new LinkedHashMap<>();
    private String currentLanguageCode = "en_us";

    public static LanguageManager getInstance() {
        if (instance == null) {
            instance = new LanguageManager();
        }
        return instance;
    }

    private LanguageManager() {
        this.langDirectory = FabricLoader.getInstance().getConfigDir().resolve("retrochat").resolve("lang").toFile();
        initDefaultLanguageFiles();
        reloadLanguages();
        detectInitialLanguage();
    }

    public static String get(String key) {
        return getInstance().translate(key);
    }

    public static String get(String key, Object... args) {
        return getInstance().translate(key, args);
    }

    public String translate(String key) {
        LanguageEntry current = availableLanguages.get(currentLanguageCode);
        if (current != null && current.translations.containsKey(key)) {
            return current.translations.get(key);
        }

        // fallback to english
        LanguageEntry fallback = availableLanguages.get("en_us");
        if (fallback != null && fallback.translations.containsKey(key)) {
            return fallback.translations.get(key);
        }

        return key;
    }

    public String translate(String key, Object... args) {
        String template = translate(key);
        try {
            return String.format(template, args);
        } catch (Exception e) {
            return template;
        }
    }

    public void setLanguage(String code) {
        if (availableLanguages.containsKey(code)) {
            this.currentLanguageCode = code;
            RetroChatConfig config = RetroChatConfig.getInstance();
            config.language = code;
            config.save();
        }
    }

    public void cycleLanguage() {
        if (availableLanguages.isEmpty()) return;

        List<String> codes = new ArrayList<>(availableLanguages.keySet());
        int currentIndex = codes.indexOf(currentLanguageCode);
        int nextIndex = (currentIndex + 1) % codes.size();
        setLanguage(codes.get(nextIndex));
    }

    public String getCurrentLanguageCode() {
        return currentLanguageCode;
    }

    public String getCurrentLanguageName() {
        LanguageEntry entry = availableLanguages.get(currentLanguageCode);
        return entry != null ? entry.name : "English";
    }

    public Map<String, LanguageEntry> getAvailableLanguages() {
        return Collections.unmodifiableMap(availableLanguages);
    }

    private void detectInitialLanguage() {
        RetroChatConfig config = RetroChatConfig.getInstance();
        String savedLang = config.language;

        if (savedLang != null && !savedLang.isEmpty() && !"auto".equalsIgnoreCase(savedLang)) {
            if (availableLanguages.containsKey(savedLang.toLowerCase())) {
                this.currentLanguageCode = savedLang.toLowerCase();
                return;
            }
        }

        // detect client language or match prefix
        String detected = detectMinecraftLanguage();
        if (availableLanguages.containsKey(detected)) {
            this.currentLanguageCode = detected;
        } else {
            String prefix = detected.length() >= 2 ? detected.substring(0, 2) : detected;
            String matched = null;
            for (String code : availableLanguages.keySet()) {
                if (code.startsWith(prefix)) {
                    matched = code;
                    break;
                }
            }
            if (matched != null) {
                this.currentLanguageCode = matched;
            } else {
                this.currentLanguageCode = "en_us";
            }
        }

        config.language = this.currentLanguageCode;
        config.save();
    }

    private String detectMinecraftLanguage() {
        try {
            MinecraftClient client = MinecraftClient.getInstance();
            if (client != null && client.getLanguageManager() != null) {
                String mcLang = client.getLanguageManager().getLanguage();
                if (mcLang != null && !mcLang.isEmpty()) {
                    return mcLang.toLowerCase();
                }
            }
        } catch (Exception ignored) {}
        return "en_us";
    }

    public void reloadLanguages() {
        availableLanguages.clear();

        if (!langDirectory.exists()) {
            langDirectory.mkdirs();
        }

        File[] files = langDirectory.listFiles((dir, name) -> name.toLowerCase().endsWith(".json"));
        if (files != null) {
            for (File file : files) {
                loadLanguageFile(file);
            }
        }

        // ensure en_us always exists as base
        if (!availableLanguages.containsKey("en_us")) {
            writeDefaultFile("en_us.json", getDefaultEnglish());
            loadLanguageFile(new File(langDirectory, "en_us.json"));
        }
    }

    private void loadLanguageFile(File file) {
        try (FileReader reader = new FileReader(file, StandardCharsets.UTF_8)) {
            Map<String, String> data = GSON.fromJson(reader, MAP_TYPE);
            if (data != null && !data.isEmpty()) {
                String fileName = file.getName();
                String code = data.getOrDefault("language_code", fileName.substring(0, fileName.lastIndexOf('.'))).toLowerCase();
                String name = data.getOrDefault("language_name", code);
                availableLanguages.put(code, new LanguageEntry(code, name, data));
            }
        } catch (Exception e) {
            System.err.println("[RetroChat] Failed to load language file: " + file.getName() + " - " + e.getMessage());
        }
    }

    private void initDefaultLanguageFiles() {
        if (!langDirectory.exists()) {
            langDirectory.mkdirs();
        }

        updateOrWriteDefaultFile("en_us.json", getDefaultEnglish());
        updateOrWriteDefaultFile("tr_tr.json", getDefaultTurkish());
        updateOrWriteDefaultFile("de_de.json", getDefaultGerman());
        updateOrWriteDefaultFile("ru_ru.json", getDefaultRussian());
        updateOrWriteDefaultFile("es_es.json", getDefaultSpanish());
        updateOrWriteDefaultFile("fr_fr.json", getDefaultFrench());
        writeGuideTextFile();
    }

    private void writeGuideTextFile() {
        File guideFile = new File(langDirectory, "HOW_TO_ADD_NEW_LANGUAGE.txt");
        if (!guideFile.exists()) {
            try (FileWriter writer = new FileWriter(guideFile, StandardCharsets.UTF_8)) {
                writer.write("================================================================================\n"
                    + "           RETROCHAT - HOW TO ADD A NEW LANGUAGE / YENI DIL EKLEME REHBERI\n"
                    + "================================================================================\n\n"
                    + "[ENGLISH]\n"
                    + "Welcome! You can easily translate RetroChat into ANY language using this guide.\n"
                    + "RetroChat automatically scans this folder (.minecraft/config/retrochat/lang/)\n"
                    + "for any \".json\" files. When you add a new file here, it will immediately appear\n"
                    + "in the in-game RetroChat Settings!\n\n"
                    + "--------------------------------------------------------------------------------\n"
                    + "STEP 1: Copy the English JSON template below.\n"
                    + "STEP 2: Translate the values into your target language.\n"
                    + "STEP 3: Save the translated content as a JSON file in this folder:\n"
                    + "        .minecraft/config/retrochat/lang/<language_code>.json\n"
                    + "        (Example: fr_fr.json for French, es_es.json for Spanish, ru_ru.json for Russian)\n"
                    + "STEP 4: Launch Minecraft! Your language will be detected automatically or selectable\n"
                    + "        in the RetroChat Settings menu!\n"
                    + "--------------------------------------------------------------------------------\n\n"
                    + "================================================================================\n"
                    + "TRANSLATION TEMPLATE:\n"
                    + "================================================================================\n\n"
                    + "Translate ONLY the values (the text on the right side of the colon).\n"
                    + "DO NOT change the keys (the words on the left).\n"
                    + "Keep punctuation and placeholders intact.\n"
                    + "Ensure the output is valid JSON.\n\n"
                    + "Target Language: [INSERT YOUR TARGET LANGUAGE HERE, e.g. French / Spanish / Russian]\n"
                    + "Language Name: [INSERT NATIVE LANGUAGE NAME, e.g. Français / Español / Русский]\n"
                    + "Language Code: [INSERT MINECRAFT CODE, e.g. fr_fr / es_es / ru_ru]\n\n"
                    + "English JSON to translate:\n"
                    + "{\n"
                    + "  \"language_code\": \"en_us\",\n"
                    + "  \"language_name\": \"English\",\n"
                    + "  \"config.title\": \"RetroChat Configuration\",\n"
                    + "  \"config.mod_enabled\": \"RetroChat Mod\",\n"
                    + "  \"config.hide_dms\": \"Hide DMs in Vanilla Chat\",\n"
                    + "  \"config.sound\": \"Sound Notification\",\n"
                    + "  \"config.volume\": \"Volume\",\n"
                    + "  \"config.ui_scale\": \"UI Scale\",\n"
                    + "  \"config.width\": \"Width\",\n"
                    + "  \"config.height\": \"Height\",\n"
                    + "  \"config.reset\": \"Reset\",\n"
                    + "  \"config.timestamps\": \"Timestamps\",\n"
                    + "  \"config.player_heads\": \"Player Heads\",\n"
                    + "  \"config.compact_tabs\": \"Compact Tabs\",\n"
                    + "  \"chat.insert_coords\": \"Insert Coordinates [XYZ]\",\n"
                    + "  \"chat.search_messages\": \"Search chat...\",\n"
                    + "  \"chat.search_toggle\": \"Search Messages\",\n"
                    + "  \"chat.no_search_results\": \"No matching messages found.\",\n"
                    + "  \"config.language\": \"Language\",\n"
                    + "  \"config.keybind_hint\": \"Tip: Press [T] to chat. An optional key can be set in Controls.\",\n"
                    + "  \"config.player_to_block\": \"Player to block...\",\n"
                    + "  \"config.block\": \"Block\",\n"
                    + "  \"config.unblock\": \"Unblock\",\n"
                    + "  \"config.blocked_players\": \"Blocked Players\",\n"
                    + "  \"config.no_blocked\": \"No blocked players.\",\n"
                    + "  \"config.player_list\": \"Online & Recent Players\",\n"
                    + "  \"config.no_players\": \"No other players found.\",\n"
                    + "  \"config.search_player\": \"Search player to block/unblock...\",\n"
                    + "  \"config.live_preview\": \"LIVE PREVIEW\",\n"
                    + "  \"config.preview_player\": \"Preview Player\",\n"
                    + "  \"config.preview_msg1\": \"Hey! Real-time scaling test.\",\n"
                    + "  \"config.preview_msg2\": \"RetroChat preview is crystal clear!\",\n"
                    + "  \"config.done\": \"Done\",\n"
                    + "  \"chat.type_message\": \"Type a message...\",\n"
                    + "  \"chat.no_messages\": \"No messages yet.\",\n"
                    + "  \"chat.player_blocked\": \"Player is blocked.\",\n"
                    + "  \"chat.you\": \"You\",\n"
                    + "  \"sidebar.title\": \"RetroChat\",\n"
                    + "  \"sidebar.search\": \"Search...\"\n"
                    + "}\n\n"
                    + "================================================================================\n"
                    + "COMMON MINECRAFT LANGUAGE CODES:\n"
                    + "================================================================================\n"
                    + "- French:         fr_fr.json  (\"language_code\": \"fr_fr\", \"language_name\": \"Français\")\n"
                    + "- Spanish:        es_es.json  (\"language_code\": \"es_es\", \"language_name\": \"Español\")\n"
                    + "- Russian:        ru_ru.json  (\"language_code\": \"ru_ru\", \"language_name\": \"Русский\")\n"
                    + "- Portuguese:     pt_br.json  (\"language_code\": \"pt_br\", \"language_name\": \"Português\")\n"
                    + "- Italian:        it_it.json  (\"language_code\": \"it_it\", \"language_name\": \"Italiano\")\n"
                    + "- Polish:         pl_pl.json  (\"language_code\": \"pl_pl\", \"language_name\": \"Polski\")\n"
                    + "- Dutch:          nl_nl.json  (\"language_code\": \"nl_nl\", \"language_name\": \"Nederlands\")\n"
                    + "- Japanese:       ja_jp.json  (\"language_code\": \"ja_jp\", \"language_name\": \"日本語\")\n"
                    + "- Chinese:        zh_cn.json  (\"language_code\": \"zh_cn\", \"language_name\": \"简体中文\")\n"
                    + "- Korean:         ko_kr.json  (\"language_code\": \"ko_kr\", \"language_name\": \"한국어\")\n"
                    + "- Turkish:        tr_tr.json  (\"language_code\": \"tr_tr\", \"language_name\": \"Türkçe\")\n"
                    + "- German:         de_de.json  (\"language_code\": \"de_de\", \"language_name\": \"Deutsch\")\n"
                    + "- English:        en_us.json  (\"language_code\": \"en_us\", \"language_name\": \"English\")\n\n"
                    + "================================================================================\n"
                    + "[TURKCE] YENI DIL NASIL EKLENIR?\n"
                    + "================================================================================\n"
                    + "1. Yukaridaki Ingilizce JSON sablonunu kopyalayin.\n"
                    + "2. Degerleri hedef dilinize cevirin.\n"
                    + "3. Yeni bir metin dosyasi acip icine yapistirin ve dosya adini <dil_kodu>.json\n"
                    + "   olarak bu klasore kaydedin (.minecraft/config/retrochat/lang/fr_fr.json).\n"
                    + "4. Oyunu actiginizda yeni dil otomatik olarak listeye eklenecek ve RetroChat\n"
                    + "   Ayarlar menusunden secilebilir olacaktir!\n"
                    + "================================================================================\n");
            } catch (Exception e) {
                System.err.println("[RetroChat] Failed to write language guide file: " + e.getMessage());
            }
        }
    }

    private void updateOrWriteDefaultFile(String filename, Map<String, String> defaults) {
        File target = new File(langDirectory, filename);
        Map<String, String> current = new LinkedHashMap<>(defaults);
        if (target.exists()) {
            try (FileReader reader = new FileReader(target, StandardCharsets.UTF_8)) {
                Map<String, String> loaded = GSON.fromJson(reader, MAP_TYPE);
                if (loaded != null) {
                    for (Map.Entry<String, String> entry : loaded.entrySet()) {
                        current.put(entry.getKey(), entry.getValue());
                    }
                }
            } catch (Exception ignored) {}
        }
        writeDefaultFile(filename, current);
    }

    private void writeDefaultFile(String filename, Map<String, String> content) {
        File target = new File(langDirectory, filename);
        try (FileWriter writer = new FileWriter(target, StandardCharsets.UTF_8)) {
            GSON.toJson(content, writer);
        } catch (Exception e) {
            System.err.println("[RetroChat] Failed to write default language file: " + filename + " - " + e.getMessage());
        }
    }

    private Map<String, String> getDefaultEnglish() {
        Map<String, String> map = new LinkedHashMap<>();
        map.put("language_code", "en_us");
        map.put("language_name", "English");
        map.put("config.title", "RetroChat Configuration");
        map.put("config.mod_enabled", "RetroChat Mod");
        map.put("config.hide_dms", "Hide DMs in Vanilla Chat");
        map.put("config.sound", "Sound Notification");
        map.put("config.volume", "Volume");
        map.put("config.ui_scale", "UI Scale");
        map.put("config.width", "Width");
        map.put("config.height", "Height");
        map.put("config.reset", "Reset");
        map.put("config.timestamps", "Timestamps");
        map.put("config.player_heads", "Player Heads");
        map.put("config.compact_tabs", "Compact Tabs");
        map.put("config.language", "Language");
        map.put("config.language_help", "How to add new languages:\nCheck config/retrochat/lang/ and read HOW_TO_ADD_NEW_LANGUAGE.txt\n(Click to open folder)");
        map.put("config.keybind_hint", "Tip: Press [T] to chat. An optional key can be set in Controls.");
        map.put("config.player_to_block", "Player to block...");
        map.put("config.block", "Block");
        map.put("config.unblock", "Unblock");
        map.put("config.blocked_players", "Blocked Players");
        map.put("config.no_blocked", "No blocked players.");
        map.put("config.player_list", "Online & Recent Players");
        map.put("config.no_players", "No other players found.");
        map.put("config.search_player", "Search player to block/unblock...");
        map.put("config.live_preview", "LIVE PREVIEW");
        map.put("config.preview_player", "Preview Player");
        map.put("config.preview_msg1", "Hey! Real-time scaling test.");
        map.put("config.preview_msg2", "RetroChat preview is crystal clear!");
        map.put("config.done", "Done");
        map.put("chat.type_message", "Type a message...");
        map.put("chat.insert_coords", "Insert Coordinates [XYZ]");
        map.put("chat.search_messages", "Search chat...");
        map.put("chat.search_toggle", "Search Messages");
        map.put("chat.no_search_results", "No matching messages found.");
        map.put("chat.no_messages", "No messages yet.");
        map.put("chat.player_blocked", "Player is blocked.");
        map.put("chat.sent_while_blocked", "Sent while player was blocked");
        map.put("chat.you", "You");
        map.put("sidebar.title", "RetroChat");
        map.put("sidebar.search", "Search...");
        return map;
    }

    private Map<String, String> getDefaultTurkish() {
        Map<String, String> map = new LinkedHashMap<>();
        map.put("language_code", "tr_tr");
        map.put("language_name", "Türkçe");
        map.put("config.title", "RetroChat Ayarları");
        map.put("config.mod_enabled", "RetroChat Modu");
        map.put("config.hide_dms", "Normal Chatte DM'leri Gizle");
        map.put("config.sound", "Sesli Bildirim");
        map.put("config.volume", "Ses Seviyesi");
        map.put("config.ui_scale", "Arayüz Ölçeği");
        map.put("config.width", "Genişlik");
        map.put("config.height", "Yükseklik");
        map.put("config.reset", "Sıfırla");
        map.put("config.timestamps", "Zaman Damgası");
        map.put("config.player_heads", "Oyuncu Kafaları");
        map.put("config.compact_tabs", "Kompakt Sekmeler");
        map.put("config.language", "Dil");
        map.put("config.language_help", "Nasıl yeni dil eklenir:\nconfig/retrochat/lang/ klasöründeki HOW_TO_ADD_NEW_LANGUAGE.txt rehberine bakın.\n(Klasörü açmak için tıklayın)");
        map.put("config.keybind_hint", "İpucu: Sohbet için [T] tuşunu kullanın. Özel tuş Kontroller'den atanabilir.");
        map.put("config.player_to_block", "Engellenecek oyuncu...");
        map.put("config.block", "Engelle");
        map.put("config.unblock", "Kaldır");
        map.put("config.blocked_players", "Engellenen Oyuncular");
        map.put("config.no_blocked", "Engellenen oyuncu yok.");
        map.put("config.player_list", "Çevrimiçi & Sohbet Edilen Oyuncular");
        map.put("config.no_players", "Başka oyuncu bulunamadı.");
        map.put("config.search_player", "Engellemek/kaldırmak için oyuncu ara...");
        map.put("config.live_preview", "CANLI ÖNİZLEME");
        map.put("config.preview_player", "Önizleme Oyuncusu");
        map.put("config.preview_msg1", "Selam! Canlı boyutlandırma testi.");
        map.put("config.preview_msg2", "RetroChat önizlemesi harika görünüyor!");
        map.put("config.done", "Tamam");
        map.put("chat.type_message", "Bir mesaj yazın...");
        map.put("chat.insert_coords", "Koordinat Ekle [XYZ]");
        map.put("chat.search_messages", "Sohbette ara...");
        map.put("chat.search_toggle", "Mesajlarda Ara");
        map.put("chat.no_search_results", "Eşleşen mesaj bulunamadı.");
        map.put("chat.no_messages", "Henüz mesaj yok.");
        map.put("chat.player_blocked", "Bu oyuncu engellendi.");
        map.put("chat.sent_while_blocked", "Oyuncu engelliyken gÃ¶nderildi");
        map.put("chat.you", "Sen");
        map.put("sidebar.title", "RetroChat");
        map.put("sidebar.search", "Ara...");
        return map;
    }

    private Map<String, String> getDefaultGerman() {
        Map<String, String> map = new LinkedHashMap<>();
        map.put("language_code", "de_de");
        map.put("language_name", "Deutsch");
        map.put("config.title", "RetroChat Einstellungen");
        map.put("config.mod_enabled", "RetroChat-Mod");
        map.put("config.hide_dms", "DMs im Vanilla-Chat verbergen");
        map.put("config.sound", "Benachrichtigungston");
        map.put("config.volume", "Lautstärke");
        map.put("config.ui_scale", "UI-Skalierung");
        map.put("config.width", "Breite");
        map.put("config.height", "Höhe");
        map.put("config.reset", "Zurücksetzen");
        map.put("config.timestamps", "Zeitstempel");
        map.put("config.player_heads", "Spielerköpfe");
        map.put("config.compact_tabs", "Kompakte Tabs");
        map.put("config.language", "Sprache");
        map.put("config.language_help", "So fügst du Sprachen hinzu:\nSiehe HOW_TO_ADD_NEW_LANGUAGE.txt im Ordner config/retrochat/lang/\n(Klicken, um Ordner zu öffnen)");
        map.put("config.keybind_hint", "Tipp: Drücke [T] zum Chatten. Eine optionale Taste kann in der Steuerung belegt werden.");
        map.put("config.player_to_block", "Spieler blockieren...");
        map.put("config.block", "Blockieren");
        map.put("config.unblock", "Freigeben");
        map.put("config.blocked_players", "Blockierte Spieler");
        map.put("config.no_blocked", "Keine blockierten Spieler.");
        map.put("config.player_list", "Online- & Chat-Spieler");
        map.put("config.no_players", "Keine weiteren Spieler gefunden.");
        map.put("config.search_player", "Spieler suchen zum Blockieren/Freigeben...");
        map.put("config.live_preview", "LIVE-VORSCHAU");
        map.put("config.preview_player", "Vorschau-Spieler");
        map.put("config.preview_msg1", "Hey! Echtzeit-Skalierungstest.");
        map.put("config.preview_msg2", "RetroChat Vorschau ist kristallklar!");
        map.put("config.done", "Fertig");
        map.put("chat.type_message", "Nachricht schreiben...");
        map.put("chat.insert_coords", "Koordinaten einfügen [XYZ]");
        map.put("chat.search_messages", "Chat durchsuchen...");
        map.put("chat.search_toggle", "Nachrichten suchen");
        map.put("chat.no_search_results", "Keine passenden Nachrichten gefunden.");
        map.put("chat.no_messages", "Noch keine Nachrichten.");
        map.put("chat.player_blocked", "Spieler ist blockiert.");
        map.put("chat.sent_while_blocked", "Gesendet, wÃ¤hrend der Spieler blockiert war");
        map.put("chat.you", "Du");
        map.put("sidebar.title", "RetroChat");
        map.put("sidebar.search", "Suchen...");
        return map;
    }

    private Map<String, String> getDefaultRussian() {
        Map<String, String> map = new LinkedHashMap<>();
        map.put("language_code", "ru_ru");
        map.put("language_name", "Русский");
        map.put("config.title", "Настройки RetroChat");
        map.put("config.mod_enabled", "Мод RetroChat");
        map.put("config.hide_dms", "Скрыть ЛС в обычном чате");
        map.put("config.sound", "Звуковое уведомление");
        map.put("config.volume", "Громкость");
        map.put("config.ui_scale", "Масштаб интерфейса");
        map.put("config.width", "Ширина");
        map.put("config.height", "Высота");
        map.put("config.reset", "Сброс");
        map.put("config.timestamps", "Время сообщений");
        map.put("config.player_heads", "Головы игроков");
        map.put("config.compact_tabs", "Компактные вкладки");
        map.put("config.language", "Язык");
        map.put("config.language_help", "Как добавить новый язык:\nПроверьте config/retrochat/lang/ и прочтите HOW_TO_ADD_NEW_LANGUAGE.txt\n(Нажмите, чтобы открыть папку)");
        map.put("config.keybind_hint", "Подсказка: Нажмите [T] для чата. Клавишу можно переназначить в управлении.");
        map.put("config.player_to_block", "Игрок для блокировки...");
        map.put("config.block", "Блок");
        map.put("config.unblock", "Разблок.");
        map.put("config.blocked_players", "Заблокированные игроки");
        map.put("config.no_blocked", "Нет заблокированных игроков.");
        map.put("config.player_list", "Игроки в сети и недавние");
        map.put("config.no_players", "Другие игроки не найдены.");
        map.put("config.search_player", "Поиск игрока для блок./разблок....");
        map.put("config.live_preview", "ПРЕДПРОСМОТР");
        map.put("config.preview_player", "Игрок предпросмотра");
        map.put("config.preview_msg1", "Привет! Тест масштабирования.");
        map.put("config.preview_msg2", "Предпросмотр RetroChat кристально чистый!");
        map.put("config.done", "Готово");
        map.put("chat.type_message", "Напишите сообщение...");
        map.put("chat.insert_coords", "Вставить координаты [XYZ]");
        map.put("chat.search_messages", "Поиск в чате...");
        map.put("chat.search_toggle", "Поиск сообщений");
        map.put("chat.no_search_results", "Совпадающих сообщений не найдено.");
        map.put("chat.no_messages", "Пока нет сообщений.");
        map.put("chat.player_blocked", "Игрок заблокирован.");
        map.put("chat.you", "Вы");
        map.put("sidebar.title", "RetroChat");
        map.put("sidebar.search", "Поиск...");
        return map;
    }

    private Map<String, String> getDefaultSpanish() {
        Map<String, String> map = new LinkedHashMap<>();
        map.put("language_code", "es_es");
        map.put("language_name", "Español");
        map.put("config.title", "Configuración de RetroChat");
        map.put("config.mod_enabled", "Mod RetroChat");
        map.put("config.hide_dms", "Ocultar MDs en chat normal");
        map.put("config.sound", "Notificación de sonido");
        map.put("config.volume", "Volumen");
        map.put("config.ui_scale", "Escala de interfaz");
        map.put("config.width", "Ancho");
        map.put("config.height", "Alto");
        map.put("config.reset", "Restablecer");
        map.put("config.timestamps", "Marcas de tiempo");
        map.put("config.player_heads", "Cabezas de jugadores");
        map.put("config.compact_tabs", "Pestañas compactas");
        map.put("config.language", "Idioma");
        map.put("config.language_help", "Cómo añadir un nuevo idioma:\nRevisa config/retrochat/lang/ y lee HOW_TO_ADD_NEW_LANGUAGE.txt\n(Haz clic para abrir la carpeta)");
        map.put("config.keybind_hint", "Consejo: Pulsa [T] para chatear. Puedes asignar una tecla en Controles.");
        map.put("config.player_to_block", "Jugador a bloquear...");
        map.put("config.block", "Bloquear");
        map.put("config.unblock", "Desbloquear");
        map.put("config.blocked_players", "Jugadores bloqueados");
        map.put("config.no_blocked", "No hay jugadores bloqueados.");
        map.put("config.player_list", "Jugadores en línea y recientes");
        map.put("config.no_players", "No se encontraron otros jugadores.");
        map.put("config.search_player", "Buscar jugador para bloquear/desbloquear...");
        map.put("config.live_preview", "VISTA PREVIA");
        map.put("config.preview_player", "Jugador de prueba");
        map.put("config.preview_msg1", "¡Hola! Prueba de escalado en tiempo real.");
        map.put("config.preview_msg2", "¡La vista previa de RetroChat es cristalina!");
        map.put("config.done", "Hecho");
        map.put("chat.type_message", "Escribe un mensaje...");
        map.put("chat.insert_coords", "Insertar coordenadas [XYZ]");
        map.put("chat.search_messages", "Buscar en el chat...");
        map.put("chat.search_toggle", "Buscar mensajes");
        map.put("chat.no_search_results", "No se encontraron mensajes coincidentes.");
        map.put("chat.no_messages", "No hay mensajes aún.");
        map.put("chat.player_blocked", "El jugador está bloqueado.");
        map.put("chat.you", "Tú");
        map.put("sidebar.title", "RetroChat");
        map.put("sidebar.search", "Buscar...");
        return map;
    }

    private Map<String, String> getDefaultFrench() {
        Map<String, String> map = new LinkedHashMap<>();
        map.put("language_code", "fr_fr");
        map.put("language_name", "Français");
        map.put("config.title", "Configuration de RetroChat");
        map.put("config.mod_enabled", "Mod RetroChat");
        map.put("config.hide_dms", "Masquer les MPs dans le chat standard");
        map.put("config.sound", "Notification sonore");
        map.put("config.volume", "Volume");
        map.put("config.ui_scale", "Échelle de l'interface");
        map.put("config.width", "Largeur");
        map.put("config.height", "Hauteur");
        map.put("config.reset", "Réinitialiser");
        map.put("config.timestamps", "Horodatages");
        map.put("config.player_heads", "Têtes de joueurs");
        map.put("config.compact_tabs", "Onglets compacts");
        map.put("config.language", "Langue");
        map.put("config.language_help", "Comment ajouter une nouvelle langue :\nConsultez config/retrochat/lang/ et lisez HOW_TO_ADD_NEW_LANGUAGE.txt\n(Cliquez pour ouvrir le dossier)");
        map.put("config.keybind_hint", "Astuce : Appuyez sur [T] pour discuter. Une touche peut être configurée dans Contrôles.");
        map.put("config.player_to_block", "Joueur à bloquer...");
        map.put("config.block", "Bloquer");
        map.put("config.unblock", "Débloquer");
        map.put("config.blocked_players", "Joueurs bloqués");
        map.put("config.no_blocked", "Aucun joueur bloqué.");
        map.put("config.player_list", "Joueurs en ligne et récents");
        map.put("config.no_players", "Aucun autre joueur trouvé.");
        map.put("config.search_player", "Rechercher un joueur à bloquer/débloquer...");
        map.put("config.live_preview", "APERÇU EN DIRECT");
        map.put("config.preview_player", "Joueur de prévisualisation");
        map.put("config.preview_msg1", "Salut ! Test de redimensionnement en direct.");
        map.put("config.preview_msg2", "L'aperçu de RetroChat est limpide !");
        map.put("config.done", "Terminé");
        map.put("chat.type_message", "Écrivez un message...");
        map.put("chat.insert_coords", "Insérer les coordonnées [XYZ]");
        map.put("chat.search_messages", "Rechercher dans le chat...");
        map.put("chat.search_toggle", "Rechercher des messages");
        map.put("chat.no_search_results", "Aucun message correspondant trouvé.");
        map.put("chat.no_messages", "Aucun message pour le moment.");
        map.put("chat.player_blocked", "Le joueur est bloqué.");
        map.put("chat.you", "Vous");
        map.put("sidebar.title", "RetroChat");
        map.put("sidebar.search", "Rechercher...");
        return map;
    }

    public static class LanguageEntry {
        public final String code;
        public final String name;
        public final Map<String, String> translations;

        public LanguageEntry(String code, String name, Map<String, String> translations) {
            this.code = code;
            this.name = name;
            this.translations = translations;
        }
    }
}
