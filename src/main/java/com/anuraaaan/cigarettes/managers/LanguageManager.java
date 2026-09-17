package com.anuraaaan.cigarettes.managers;

import com.anuraaaan.cigarettes.Cigarettes;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

public class LanguageManager {

    private final Cigarettes plugin;

    public LanguageManager(Cigarettes plugin) {
        this.plugin = plugin;
    }

    public Map<String, String> loadFromConfig(String lang) {

        if (lang == null || lang.isBlank()) {
            plugin.getLogger().warning("Language is not specified in config.yml");
            return null;
        }

        File fileLang = new File(plugin.getDataFolder(), "lang/" + lang + ".yml");
        InputStream inputStream = plugin.getResource("lang/ru.yml");



        if (!fileLang.exists()) {
            plugin.saveResource("lang/" + lang + ".yml", false);
        }

        FileConfiguration configuration = new YamlConfiguration();

        if (inputStream == null) {
            plugin.getLogger().warning("Language file was not found inside the jar");
            return null;
        }
        YamlConfiguration configDefault = YamlConfiguration.loadConfiguration(
                new InputStreamReader(inputStream, StandardCharsets.UTF_8));
        try {
            configuration .load(fileLang);

        } catch (IOException | InvalidConfigurationException e) {
            plugin.getLogger().warning("Failed to load language file: " + fileLang.getPath());
        }

        Map<String, String> langMap = new HashMap<>();

        for (String key : configuration.getKeys(false)) {
            langMap.put(key, configuration.getString(key));
        }
        boolean changed = false;
        for (String key : configDefault.getKeys(false)) {
            if (!langMap.containsKey(key)) {
                String message = configDefault.getString(key);
                configuration.set(key, message);
                langMap.put(key, message);

                changed = true;
            }
        }
        if (changed) {
            try {
                configuration.save(fileLang);
            } catch (IOException e) {
                plugin.getLogger().warning("Failed to save language file: " + fileLang.getPath());
            }
        }

        return langMap;
    }
}
