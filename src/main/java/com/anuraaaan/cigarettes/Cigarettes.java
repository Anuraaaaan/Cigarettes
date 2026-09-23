package com.anuraaaan.cigarettes;

import com.anuraaaan.cigarettes.commands.CigarettesCommand;
import com.anuraaaan.cigarettes.database.Database;
import com.anuraaaan.cigarettes.database.PlayerData;
import com.anuraaaan.cigarettes.database.PlayerDataAutoSave;
import com.anuraaaan.cigarettes.listener.*;
import com.anuraaaan.cigarettes.managers.CigaretteManagers;
import com.anuraaaan.cigarettes.managers.NicotineManager;
import com.anuraaaan.cigarettes.managers.NicotineWithdrawalManager;
import com.anuraaaan.cigarettes.nicotine.NicotineDecayTask;
import com.anuraaaan.cigarettes.parsers.ItemParser;
import com.anuraaaan.cigarettes.registry.CigaretteRegistry;
import com.anuraaaan.cigarettes.registry.ConfigRegistry;
import com.anuraaaan.cigarettes.registry.CustomItemsRegistry;
import com.anuraaaan.cigarettes.registry.RecipesRegistry;
import lombok.Getter;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

import java.sql.SQLException;
import java.util.List;
import java.util.Objects;

public final class Cigarettes extends JavaPlugin {

    @Getter
    private ItemParser itemParser;
    @Getter
    private boolean enabledItemsAdder;
    @Getter
    private CigaretteManagers cigaretteManagers;

    private Database database;
    private PlayerDataAutoSave playerDataAutoSave;

    @Override
    public void onEnable() {

        String currentVersion = Bukkit.getMinecraftVersion();

        String version = "1.21.5";
        if (isLowerThan(currentVersion, version)) {
            getLogger().severe("This plugin requires Minecraft " + version +" or newer!");
            Bukkit.getPluginManager().disablePlugin(this);
            return;
        }

        saveDefaultConfig();
        enabledItemsAdder = Bukkit.getServer().getPluginManager().isPluginEnabled("ItemsAdder");

        database = new Database(this);
        try {
            database.connect();
        } catch (SQLException e) {
            getLogger().warning("Failed to connect to the database! "  + e);
            getServer().getPluginManager().disablePlugin(this);
            return;
        }
        cigaretteManagers = new CigaretteManagers(this);

        ConfigRegistry configRegistry = new ConfigRegistry(this);

        CigaretteRegistry cigaretteRegistry = new CigaretteRegistry(this);
        CustomItemsRegistry customItemsRegistry = new CustomItemsRegistry(this);

        itemParser = new ItemParser(this, cigaretteRegistry, customItemsRegistry);

        NicotineManager nicotineManager = new NicotineManager();

        NicotineWithdrawalManager nicotineWithdrawalManager = new NicotineWithdrawalManager(this, configRegistry, nicotineManager);
        nicotineWithdrawalManager.runTaskTimer(this, 0, 20);

        RecipesRegistry recipesRegistry = new RecipesRegistry(this);

        if (enabledItemsAdder) {
            Bukkit.getPluginManager().registerEvents(new IALoadDataListener(this, cigaretteRegistry, configRegistry,
                    customItemsRegistry, recipesRegistry), this);
            this.getLogger().info("ItemsAdder integration enabled successfully");
        }
        Bukkit.getPluginManager().registerEvents(new SmokingItemActivateEvent(this,
                cigaretteRegistry, configRegistry), this);
        Bukkit.getPluginManager().registerEvents(new SmokingEvent(this, cigaretteRegistry, nicotineWithdrawalManager,
                nicotineManager, configRegistry), this);
        Bukkit.getPluginManager().registerEvents(new RefillEvent(this, cigaretteRegistry, configRegistry), this);
        Bukkit.getPluginManager().registerEvents(new PlayerDefaultsInitializerEvent(nicotineManager), this);
        Bukkit.getPluginManager().registerEvents(new WithdrawalDataEvent(nicotineWithdrawalManager), this);

        Bukkit.getPluginManager().registerEvents(new CustomItemUseEvent(this, customItemsRegistry, nicotineManager,
                configRegistry, nicotineWithdrawalManager), this);




        new NicotineDecayTask(this, nicotineManager, configRegistry).runTaskTimer(this, 0, 20);

        try {
            List<PlayerData> players = database.loadPlayer();
            for (PlayerData playerData : players) {
                nicotineManager.getNicotineDataMap().put(playerData.uuid(), playerData.nicotineData());
                nicotineWithdrawalManager.getWithdrawalData().put(playerData.uuid(), playerData.withdrawalData());
            }
        } catch (SQLException e) {
            getLogger().warning("Failed to load player data from the database! " + e);

            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        Objects.requireNonNull(getServer().getPluginCommand("cigarettes")).setExecutor(new CigarettesCommand(this, cigaretteRegistry,
                customItemsRegistry, recipesRegistry, configRegistry, nicotineManager));

        playerDataAutoSave = new PlayerDataAutoSave(this, database, nicotineManager, nicotineWithdrawalManager);
        playerDataAutoSave.runTaskTimerAsynchronously(this, 20L * configRegistry.getDatabaseUpdateMinutes() * 60,
                20L * configRegistry.getDatabaseUpdateMinutes() * 60);


    }

    @Override
    public void onDisable() {
        if (playerDataAutoSave != null) {
            playerDataAutoSave.cancel();
            playerDataAutoSave.saveAll();
        }
        try {
            database.close();
        } catch (SQLException e) {
            getLogger().warning("Failed to disconnect from the database! " + e);
        }
    }

    private boolean isLowerThan(String current, String required) {
        String[] currentParts = current.split("\\.");
        String[] requiredParts = required.split("\\.");

        int maxLength = Math.max(currentParts.length, requiredParts.length);

        for (int i = 0; i < maxLength; i++) {
            int currentValue = i < currentParts.length ? Integer.parseInt(currentParts[i]) : 0;

            int requiredValue = i < requiredParts.length ? Integer.parseInt(requiredParts[i]) : 0;

            if (currentValue < requiredValue) {
                return true;
            }

            if (currentValue > requiredValue) {
                return false;
            }
        }

        return false;
    }

}
