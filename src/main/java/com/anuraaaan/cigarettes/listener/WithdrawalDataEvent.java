package com.anuraaaan.cigarettes.listener;

import com.anuraaaan.cigarettes.managers.NicotineWithdrawalManager;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.UUID;

public class WithdrawalDataEvent implements Listener {

    private final NicotineWithdrawalManager nicotineWithdrawalManager;


    public WithdrawalDataEvent(NicotineWithdrawalManager nicotineWithdrawalManager) {
        this.nicotineWithdrawalManager = nicotineWithdrawalManager;
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent e){

        Player player = e.getPlayer();
        UUID uuid = player.getUniqueId();

        if (!nicotineWithdrawalManager.hasWithdrawalData(uuid)) return;

        nicotineWithdrawalManager.saveRemainingWithdrawalTime(uuid);

    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent e){

        Player player = e.getPlayer();
        UUID uuid = player.getUniqueId();

        if (!nicotineWithdrawalManager.hasWithdrawalData(uuid)) return;

        nicotineWithdrawalManager.restoreWithdrawalTimer(uuid);

    }

}
