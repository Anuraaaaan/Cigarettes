package com.anuraaaan.cigarettes.listener;

import com.anuraaaan.cigarettes.managers.NicotineManager;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

import java.util.UUID;

public class PlayerDefaultsInitializerEvent implements Listener {

    private final NicotineManager nicotineManager;

    public PlayerDefaultsInitializerEvent(NicotineManager nicotineManager) {
        this.nicotineManager = nicotineManager;
    }


    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent e){

        Player player = e.getPlayer();
        UUID uuid = player.getUniqueId();

        if (!nicotineManager.hasNicotineData(uuid)) {
            nicotineManager.initializeNicotineData(uuid);
        }
    }
}
