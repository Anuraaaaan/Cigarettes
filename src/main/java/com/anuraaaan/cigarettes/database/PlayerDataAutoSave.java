package com.anuraaaan.cigarettes.database;

import com.anuraaaan.cigarettes.Cigarettes;
import com.anuraaaan.cigarettes.managers.NicotineManager;
import com.anuraaaan.cigarettes.managers.NicotineWithdrawalManager;
import com.anuraaaan.cigarettes.nicotine.NicotineData;
import com.anuraaaan.cigarettes.nicotine.WithdrawalData;
import org.bukkit.scheduler.BukkitRunnable;

import java.sql.SQLException;
import java.util.Map;
import java.util.UUID;

public class PlayerDataAutoSave extends BukkitRunnable {

    private final Cigarettes plugin;
    private final Database database;
    private final NicotineManager nicotineManager;
    private final NicotineWithdrawalManager withdrawalManager;

    public PlayerDataAutoSave(Cigarettes plugin, Database database, NicotineManager nicotineManager,
                              NicotineWithdrawalManager withdrawalManager) {
        this.plugin = plugin;
        this.database = database;
        this.nicotineManager = nicotineManager;
        this.withdrawalManager = withdrawalManager;
    }

    @Override
    public void run() {
        saveAll();
    }

    public synchronized void saveAll() {
        plugin.getLogger().info("Auto-save started");

        for (Map.Entry<UUID, NicotineData> entry : nicotineManager.getNicotineDataMap().entrySet()) {

            UUID uuid = entry.getKey();
            NicotineData nicotineData = entry.getValue();
            WithdrawalData withdrawalData = withdrawalManager.getWithdrawalData().get(uuid);
            try {
                database.savePlayer(uuid, nicotineData, withdrawalData);
            } catch (SQLException e) {
                plugin.getLogger().warning("Failed to save player data: " + uuid);
            }
        }
        plugin.getLogger().info("Auto-save finished");
    }
}
