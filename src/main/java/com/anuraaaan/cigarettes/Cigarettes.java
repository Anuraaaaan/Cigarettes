package com.anuraaaan.cigarettes;

import com.anuraaaan.cigarettes.cigarette.CigaretteRegistry;
import com.anuraaaan.cigarettes.commands.GiveCommand;
import com.anuraaaan.cigarettes.listener.SmokingListener;
import com.anuraaaan.cigarettes.listener.CigaretteIgniteListener;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Objects;

public final class Cigarettes extends JavaPlugin {

    private static Cigarettes plugin;
    private CigaretteRegistry manager;

    @Override
    public void onEnable() {
        plugin = this;
        // Plugin startup logic

        plugin.getServer().getPluginManager().registerEvents(new CigaretteIgniteListener(), this);

        plugin.getServer().getPluginManager().registerEvents(new SmokingListener(), this);

        Objects.requireNonNull(getServer().getPluginCommand("cig")).setExecutor(new GiveCommand());

        manager = new CigaretteRegistry();



    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
    }

    public static Cigarettes getPlugin() {
        return plugin;
    }
}
