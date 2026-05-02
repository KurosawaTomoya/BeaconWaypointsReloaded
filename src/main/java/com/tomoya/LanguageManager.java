package com.tomoya;

import org.bukkit.configuration.file.YamlConfiguration;

import java.util.HashMap;

public class LanguageManager {
    private HashMap<String, String> messages;
    YamlConfiguration defaults;

    public LanguageManager() {
        messages = new HashMap<>();
    }

    /**
     * @param config
     * @param defaults
     */
    public LanguageManager(YamlConfiguration config, YamlConfiguration defaults) {
        messages = new HashMap<>();
        this.defaults = defaults;
        for (String key : config.getKeys(false))
            messages.put(key, config.getString(key));
    }

    /**
     * @param defaults
     */
    public LanguageManager(YamlConfiguration defaults) {
        messages = new HashMap<>();
        this.defaults = defaults;
        for (String key : defaults.getKeys(false))
            messages.put(key, defaults.getString(key));
    }

    /**
     * @param key
     * @return string
     */
    public String getString(String key) {
        String string = messages.get(key);
        if (string == null) {
            BeaconWaypointsReloaded.getPlugin().getLogger().warning("Missing language entry for \"" + key + "\", using default");
            return defaults.getString(key);
        }
        return string;
    }

    /**
     * Returns the default language configuration
     * @return defaults
     */
    public YamlConfiguration getDefaults() {
        return defaults;
    }
}