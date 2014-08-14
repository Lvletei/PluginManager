package net.obnoxint.mcdev.pluginmanager;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;

public final class Messages {

    private static final String MESSAGES_FILE = "/messages.properties";
    private static final String MESSAGE_MISSING = "Key %s not found.";
    private static final char ALT_CC = '&';

    private static final Messages instance = new Messages();

    public static String getMessage(final String key) {
        final String r = get(key);
        return r == null ? String.format(MESSAGE_MISSING, key) : r;
    }

    public static String getMessageFormatted(final String key, final Object... args) {
        final String r = get(key);
        return r == null ? String.format(MESSAGE_MISSING, key) : String.format(r, args);
    }

    private static String get(final String key) {
        return instance.messages.get(key);
    }
    private final Map<String, String> messages = new HashMap<>();

    private Messages() {
        final Properties props = new Properties();
        try (InputStream in = getClass().getResourceAsStream(MESSAGES_FILE);) {
            props.load(in);
        } catch (final IOException e) {
            Bukkit.getLogger().severe(getClass().getName() + " could not load " + MESSAGES_FILE + ".");
            e.printStackTrace();
            return;
        }

        for (final Entry<?, ?> e : props.entrySet()) {
            messages.put((String) e.getKey(), ChatColor.translateAlternateColorCodes(ALT_CC, (String) e.getValue()));
        }
    }

}
