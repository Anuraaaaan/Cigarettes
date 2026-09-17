package com.anuraaaan.cigarettes.registry;

import com.anuraaaan.cigarettes.Cigarettes;
import com.anuraaaan.cigarettes.definition.CustomItemDefinition;
import com.anuraaaan.cigarettes.utils.TextFormatter;
import io.papermc.paper.datacomponent.DataComponentTypes;
import io.papermc.paper.datacomponent.item.Consumable;
import io.papermc.paper.datacomponent.item.consumable.ItemUseAnimation;
import lombok.Getter;
import net.kyori.adventure.key.Key;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.components.CustomModelDataComponent;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import java.io.File;
import java.util.*;

public class CustomItemsRegistry {

    private final Cigarettes plugin;

    private final NamespacedKey KEY_ID;

    @Getter
    private final Map<String, CustomItemDefinition> customItems = new HashMap<>();

    public CustomItemsRegistry(Cigarettes plugin) {
        this.plugin = plugin;
        KEY_ID = new NamespacedKey(plugin, "cigarette");
        reload();
    }

    public void reload() {
        loadFromConfig();
    }

    private void loadFromConfig() {
        customItems.clear();

        File recipesFolder = new File(plugin.getDataFolder(), "custom_items");
        if (!recipesFolder.exists()) {
            recipesFolder.mkdirs();
        }
        File[] files = recipesFolder.listFiles((dir, name) -> name.endsWith(".yml"));


        if (files == null || files.length == 0) {
            plugin.saveResource("custom_items/custom_items.yml", false);
            files = recipesFolder.listFiles((dir, name) -> name.endsWith(".yml"));
            plugin.getLogger().warning("Custom item files were not found. Copying the default custom_items.yml");
        }

        for (File file : files) {

            FileConfiguration fileConfiguration = YamlConfiguration.loadConfiguration(file);

            ConfigurationSection configurationSection = fileConfiguration.getConfigurationSection("custom_items");
            if (configurationSection == null) continue;

            for (String path : configurationSection.getKeys(false)) {

                boolean enabled = configurationSection.getBoolean("custom_items." + path + ".enabled", true);
                if (!enabled) continue;

                String name = TextFormatter.colorize(fileConfiguration.getString("custom_items." + path + ".name"));
                List<String> lore = TextFormatter.colorize(fileConfiguration.getStringList("custom_items." + path + ".lore"));

                String materialName = fileConfiguration.getString("custom_items." + path + ".base_item", "stick").toUpperCase();
                Material baseMaterial;
                try {
                    baseMaterial = Material.valueOf(materialName);
                } catch (IllegalArgumentException e) {
                    plugin.getLogger().warning("Unknown item specified for: " + path);
                    continue;
                }

                String itemModel = fileConfiguration.getString("custom_items." + path + ".item_model");
                String customModelData = fileConfiguration.getString("custom_items." + path + ".custom_model_data");
                int maxStackSize = fileConfiguration.getInt("custom_items." + path + ".max_stack", baseMaterial.getMaxStackSize());

                boolean stackable = baseMaterial.getMaxStackSize() > 1;

                if (stackable) {
                    if (maxStackSize < 1 || maxStackSize > 99) {
                        maxStackSize = baseMaterial.getMaxStackSize();
                    }
                } else {
                    maxStackSize = 1;
                }

                double nicotine = fileConfiguration.getDouble("custom_items." + path + ".nicotine", 0);
                double tolerance = fileConfiguration.getDouble("custom_items." + path + ".tolerance", 0);
                double addiction = fileConfiguration.getDouble("custom_items." + path + ".addiction", 0);

                boolean useOnRightClick = fileConfiguration.getBoolean("custom_items." + path + ".use_on_right_click", false);

                ConfigurationSection sectionConsume = fileConfiguration.getConfigurationSection("custom_items." + path + ".consumable");
                boolean hasConsumable = false;
                float consumeSeconds = 1.6f;
                ItemUseAnimation itemUseAnimation = ItemUseAnimation.EAT;
                String sound = "minecraft:entity.generic.eat";
                boolean particles = true;

                if (sectionConsume != null) {
                    hasConsumable = true;
                    consumeSeconds = (float) sectionConsume.getDouble("consume_seconds");
                    String animationName = Objects.requireNonNull(sectionConsume.getString("animation", "EAT")).toUpperCase();
                    try {
                        itemUseAnimation = ItemUseAnimation.valueOf(animationName);
                    } catch (IllegalArgumentException e) {
                        plugin.getLogger().warning("Unknown animation:" + path);
                        continue;
                    }
                    sound = sectionConsume.getString("sound", "minecraft:entity.generic.eat");
                    particles = sectionConsume.getBoolean("particles",true);
                }

                if (useOnRightClick && hasConsumable) {
                    plugin.getLogger().warning("Item " + path + " must have either use_on_right_click or consumable configured, not both");
                    continue;
                }

                String returnItem = fileConfiguration.getString("custom_items." + path + ".return_item", null);

                ItemStack itemStack = new ItemStack(baseMaterial);
                if (sectionConsume != null) {
                    Consumable consumable = Consumable.consumable()
                            .consumeSeconds(consumeSeconds)
                            .animation(itemUseAnimation)
                            .sound(Key.key(sound))
                            .hasConsumeParticles(particles).build();

                    itemStack.setData(DataComponentTypes.CONSUMABLE, consumable);
                }

                ItemMeta meta = itemStack.getItemMeta();
                if (meta == null) continue;
                meta.setItemName(name);

                if (!lore.isEmpty()) {
                    meta.setLore(lore);
                }

                meta.setMaxStackSize(maxStackSize);

                if (itemModel != null) {
                    meta.setItemModel(NamespacedKey.fromString(itemModel));
                }
                if (customModelData != null) {
                    CustomModelDataComponent cmdComponent = meta.getCustomModelDataComponent();
                    cmdComponent.setStrings(Collections.singletonList(customModelData));
                    meta.setCustomModelDataComponent(cmdComponent);

                }

                PersistentDataContainer pdc = meta.getPersistentDataContainer();
                pdc.set(KEY_ID, PersistentDataType.STRING, path);
                itemStack.setItemMeta(meta);
                customItems.put(path, new CustomItemDefinition(itemStack, nicotine, tolerance, addiction, hasConsumable, useOnRightClick, returnItem));
            }
        }
    }
}
