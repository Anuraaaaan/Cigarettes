package com.anuraaaan.cigarettes.nicotine;

import com.anuraaaan.cigarettes.Cigarettes;
import com.anuraaaan.cigarettes.managers.NicotineManager;
import com.anuraaaan.cigarettes.registry.ConfigRegistry;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.UUID;

public class NicotineDecayTask extends BukkitRunnable {

    private final Cigarettes plugin;
    private final NicotineManager nicotineManager;
    private final ConfigRegistry configRegistry;

    public NicotineDecayTask(Cigarettes plugin, NicotineManager nicotineManager, ConfigRegistry configRegistry) {
        this.plugin = plugin;
        this.nicotineManager = nicotineManager;
        this.configRegistry = configRegistry;
    }

    @Override
    public void run() {

        for (Player player : plugin.getServer().getOnlinePlayers()) {
            UUID uuid = player.getUniqueId();

            nicotineManager.subtractNicotineData(uuid, configRegistry.getNicotineDecayPerSecond(),
                    configRegistry.getToleranceDecayPerSecond(), configRegistry.getAddictionDecayPerSecond());
        }
    }
}
