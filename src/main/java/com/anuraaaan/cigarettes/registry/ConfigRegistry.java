package com.anuraaaan.cigarettes.registry;

import com.anuraaaan.cigarettes.Cigarettes;
import com.anuraaaan.cigarettes.managers.LanguageManager;
import com.anuraaaan.cigarettes.nicotine.WithdrawalStageData;
import com.anuraaaan.cigarettes.utils.TextFormatter;
import lombok.Getter;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.potion.PotionEffect;

import java.util.*;

public class ConfigRegistry {

    private final Cigarettes plugin;

    @Getter
    private Map<String, String> langMap = new HashMap<>();

    private final LanguageManager languageManager;

    @Getter
    private List<String> listIgniters= new ArrayList<>();
    @Getter
    private double tolerance;
    @Getter
    private int databaseUpdateMinutes;
    @Getter
    private double nicotineDecayPerSecond;
    @Getter
    private double toleranceDecayPerSecond;
    @Getter
    private double addictionDecayPerSecond;
    @Getter
    private final Map<Integer, WithdrawalStageData> withdrawalStages = new TreeMap<>();

    public ConfigRegistry(Cigarettes plugin) {
        this.plugin = plugin;
        languageManager = new LanguageManager(plugin);
        reload();
    }


    public void reload() {
        loadFromConfig();
    }

    private void loadFromConfig() {
        plugin.reloadConfig();
        langMap.clear();
        listIgniters.clear();
        withdrawalStages.clear();

        String langName = plugin.getConfig().getString("lang");
        langMap = languageManager.loadFromConfig(langName);

        plugin.getLogger().info("Loaded language: " + langName);

        listIgniters = plugin.getConfig().getStringList("igniters");

        double nicotineDecayAmount = plugin.getConfig().getDouble("nicotine_decay.nicotine_amount");
        int nicotineDecayIntervalSeconds = plugin.getConfig().getInt("nicotine_decay.nicotine_interval");
        nicotineDecayPerSecond = nicotineDecayAmount/nicotineDecayIntervalSeconds;

        double toleranceDecayAmount = plugin.getConfig().getDouble("nicotine_decay.tolerance_amount");
        int toleranceDecayIntervalSeconds = plugin.getConfig().getInt("nicotine_decay.tolerance_interval");
        toleranceDecayPerSecond = toleranceDecayAmount/toleranceDecayIntervalSeconds;

        double addictionDecayAmount = plugin.getConfig().getDouble("nicotine_decay.addiction_amount");
        int addictionDecayIntervalSeconds = plugin.getConfig().getInt("nicotine_decay.addiction_interval");
        addictionDecayPerSecond = addictionDecayAmount/addictionDecayIntervalSeconds;

        tolerance = plugin.getConfig().getDouble("tolerance", 0.5);

        databaseUpdateMinutes = plugin.getConfig().getInt("database_update_minutes", 5);
        if (databaseUpdateMinutes <= 0) {
            plugin.getLogger().warning("Database update interval was set to " + databaseUpdateMinutes + ". It has been changed to 5 minutes");
            databaseUpdateMinutes = 5;
            plugin.getConfig().set("database_update_minutes", databaseUpdateMinutes);
            plugin.saveConfig();
        }

        registryWithdrawal();
    }

    private void registryWithdrawal() {
        ConfigurationSection withdrawalSection = plugin.getConfig().getConfigurationSection("nicotine_withdrawal");
        if (withdrawalSection == null) return;
        for (String key : withdrawalSection.getKeys(false)) {
            int level;
            try {
                level = Integer.parseInt(key);
            } catch (NumberFormatException e) {
                plugin.getLogger().warning("Withdrawal stage key must be a number: " + key);
                continue;
            }

            ConfigurationSection conditionsSection = withdrawalSection.getConfigurationSection(key + ".conditions");
            Map<String, Double> conditions = new HashMap<>();
            if (conditionsSection == null) continue;
            for (String conditionKey : conditionsSection.getKeys(false)) {
                double threshold = conditionsSection.getDouble(conditionKey);
                conditions.put(conditionKey, threshold);
            }

            long minWithdrawalDelay = withdrawalSection.getLong(key + ".withdrawal_delay.min_seconds");
            long maxWithdrawalDelay = withdrawalSection.getLong(key + ".withdrawal_delay.max_seconds");

            if (minWithdrawalDelay < 0 || minWithdrawalDelay > maxWithdrawalDelay) {
                plugin.getLogger().warning("Invalid time specified for level: " + key);
                continue;
            }

            List<String> thoughts = TextFormatter.colorize(withdrawalSection.getStringList(key + ".thoughts"));

            List<String> effectsString = withdrawalSection.getStringList(key + ".effects");
            List<PotionEffect> effects = new ArrayList<>();

            for (String effect : effectsString) {

                PotionEffect parsedEffect = plugin.getCigaretteManagers().getEffectManager().getEffect(effect);

                if (parsedEffect != null) {
                    effects.add(parsedEffect);
                }
            }
            withdrawalStages.put(level, new WithdrawalStageData(
                    conditions, minWithdrawalDelay, maxWithdrawalDelay, thoughts, effects));
        }
    }
}
