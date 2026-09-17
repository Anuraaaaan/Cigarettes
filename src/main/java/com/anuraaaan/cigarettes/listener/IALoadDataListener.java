package com.anuraaaan.cigarettes.listener;

import com.anuraaaan.cigarettes.Cigarettes;
import com.anuraaaan.cigarettes.registry.CigaretteRegistry;
import com.anuraaaan.cigarettes.registry.ConfigRegistry;
import com.anuraaaan.cigarettes.registry.CustomItemsRegistry;
import com.anuraaaan.cigarettes.registry.RecipesRegistry;
import dev.lone.itemsadder.api.Events.ItemsAdderLoadDataEvent;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

public class IALoadDataListener implements Listener {

    private final Cigarettes plugin;

    private final CigaretteRegistry cigaretteRegistry;
    private final ConfigRegistry configRegistry;
    private final CustomItemsRegistry customItemsRegistry;
    private final RecipesRegistry recipesRegistry;

    public IALoadDataListener(Cigarettes plugin, CigaretteRegistry cigaretteRegistry, ConfigRegistry configRegistry,
                              CustomItemsRegistry customItemsRegistry, RecipesRegistry recipesRegistry) {
        this.plugin = plugin;
        this.cigaretteRegistry = cigaretteRegistry;
        this.configRegistry = configRegistry;
        this.customItemsRegistry = customItemsRegistry;
        this.recipesRegistry = recipesRegistry;
    }

    @EventHandler
    public void onItemsAdderLoad(ItemsAdderLoadDataEvent event) {
        configRegistry.reload();

        cigaretteRegistry.reload();
        customItemsRegistry.reload();
        recipesRegistry.reload();
        plugin.getLogger().info("Configuration reloaded after ItemsAdder startup or reload");
    }
}
