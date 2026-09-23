package com.anuraaaan.cigarettes.registry;

import com.anuraaaan.cigarettes.Cigarettes;
import com.anuraaaan.cigarettes.definition.CigaretteDefinition;
import com.anuraaaan.cigarettes.definition.RefillItem;
import com.anuraaaan.cigarettes.utils.TextFormatter;
import io.papermc.paper.datacomponent.DataComponentTypes;
import io.papermc.paper.datacomponent.item.Consumable;
import io.papermc.paper.datacomponent.item.UseCooldown;
import io.papermc.paper.datacomponent.item.consumable.ItemUseAnimation;
import lombok.Getter;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
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

public class CigaretteRegistry {

    private final Cigarettes plugin;

    @Getter
    private final Map<String, ItemStack> unlitCigarettes = new HashMap<>();
    @Getter
    private final Map<String, CigaretteDefinition> litCigarettes = new HashMap<>();

    private final NamespacedKey KEY_ID;

    public CigaretteRegistry(Cigarettes plugin) {
        this.plugin = plugin;
        KEY_ID = new NamespacedKey(plugin, "cigarette");
        reload();
    }


    public void reload() {
        loadFromConfig();
    }

    private void loadFromConfig() {
        unlitCigarettes.clear();
        litCigarettes.clear();

        File cigarettesFolder = new File(plugin.getDataFolder(), "cigarettes");
        if (!cigarettesFolder.exists()) {
            cigarettesFolder.mkdirs();
        }
        File[] files = cigarettesFolder.listFiles((dir, name) -> name.endsWith(".yml"));

        if (files == null || files.length == 0) {
            plugin.saveResource("cigarettes/cigarettes.yml", false);
            files = cigarettesFolder.listFiles((dir, name) -> name.endsWith(".yml"));
            plugin.getLogger().warning("Cigarette files were not found. Copying the default cigarettes.yml");
        }

        for (File file : files) {
            FileConfiguration configuration = YamlConfiguration.loadConfiguration(file);

            ConfigurationSection section = configuration.getConfigurationSection("cigarettes");
            if (section == null) continue;

            for (String path : section.getKeys(false)) {

                boolean enabled = configuration.getBoolean("cigarettes." + path + ".enabled", true);
                if (!enabled) continue;


                // ===================================== МАТЕРИАЛ =====================================

                String materialName = configuration.getString("cigarettes." + path + ".material", "stick").toUpperCase();
                Material baseMaterial;
                try {
                    baseMaterial = Material.valueOf(materialName);
                } catch (IllegalArgumentException e) {
                    plugin.getLogger().warning("Unknown item specified for: " + path);
                    continue;
                }



                // ===================================== ИМЯ И ЛОР =====================================

                Component name = TextFormatter.colorize(configuration.getString("cigarettes." + path + ".name"));
                List<Component> lore = TextFormatter.colorize(configuration.getStringList("cigarettes." + path + ".lore"));



                // ===================================== МОДЕЛИ =====================================

                String defaultItemModel = configuration.getString("cigarettes." + path + ".models.default_item_model");

                String defaultCustomModelData = configuration.getString("cigarettes." + path + ".models.default_custom_model_data");

                TreeMap<Integer, String> remainingItemModels = new TreeMap<>();

                TreeMap<Integer, String> remainingCustomModelData = new TreeMap<>();

                ConfigurationSection remainingItemModelsSection = configuration.getConfigurationSection(
                        "cigarettes." + path + ".models.remaining_item_models");

                if (remainingItemModelsSection != null) {

                    for (String key : remainingItemModelsSection.getKeys(false)) {

                        String model = remainingItemModelsSection.getString(key);

                        try {

                            int percent = Integer.parseInt(key);
                            remainingItemModels.put(percent, model);
                        } catch (NumberFormatException e) {
                            plugin.getLogger().warning("Invalid item_models key in: " + path);
                        }
                    }
                }

                ConfigurationSection remainingCustomModelDataSection = configuration.getConfigurationSection(
                        "cigarettes." + path + ".models.remaining_custom_model_data");

                if (remainingCustomModelDataSection != null) {

                    for (String key : remainingCustomModelDataSection.getKeys(false)) {

                        String model = remainingCustomModelDataSection.getString(key);

                        try {

                            int percent = Integer.parseInt(key);
                            remainingCustomModelData.put(percent, model);
                        } catch (NumberFormatException e) {
                            plugin.getLogger().warning("Invalid CustomModelData key in: " + path);
                        }
                    }
                }



                // ===================================== СТАК =====================================

                int maxStackSize = configuration.getInt("cigarettes." + path + ".max_stack_size", baseMaterial.getMaxStackSize());

                boolean stackable = baseMaterial.getMaxStackSize() > 1;

                if (stackable) {
                    if (maxStackSize < 1 || maxStackSize > 99) {
                        maxStackSize = baseMaterial.getMaxStackSize();
                    }
                } else {
                    maxStackSize = 1;
                }



                // ===================================== КУРЕНИЕ =====================================

                double nicotine = configuration.getDouble("cigarettes." + path + ".nicotine", 1.0);
                double addiction = configuration.getDouble("cigarettes." + path + ".addiction", 0.2);

                boolean requireIgnition = true;

                long burnDuration = -1;
                long burnTimeReductionPerPuff = -1;


                int maxPuffs = -1;
                boolean keepAfterDepletion = false;

                HashMap<String, RefillItem> refillItems = new HashMap<>();



                ConfigurationSection burnSection = configuration.getConfigurationSection("cigarettes." + path + ".burn");
                ConfigurationSection puffsSection = configuration.getConfigurationSection("cigarettes." + path + ".puffs");

                if ((burnSection == null && puffsSection == null) || (burnSection != null && puffsSection != null)) {
                    plugin.getLogger().warning("Cigarette " + path + " must have either burn or puffs configured, not both");
                    continue;
                }

                if (burnSection != null) {
                    requireIgnition = configuration.getBoolean("cigarettes." + path + ".burn.require_ignition", true);
                    burnDuration = configuration.getLong("cigarettes." + path + ".burn.burn_duration", 60) * 1000;
                    if (burnDuration <= 0) burnDuration = 60 * 1000;
                    burnTimeReductionPerPuff = configuration.getLong("cigarettes." + path + ".burn.burn_time_reduction_per_puff", 10) * 1000;
                    if (burnTimeReductionPerPuff <= 0) burnTimeReductionPerPuff = 10 * 1000;
                }

                if (puffsSection != null) {
                    maxPuffs = configuration.getInt("cigarettes." + path + ".puffs.max_puffs", 50);
                    if (maxPuffs <= 0) maxPuffs = 50;
                    keepAfterDepletion = configuration.getBoolean("cigarettes." + path + ".puffs.keep_after_depletion", false);

                    ConfigurationSection refillItemsSection = configuration.getConfigurationSection("cigarettes." + path + ".puffs.refill_items");
                    if (refillItemsSection != null) {
                        for (String refillItem : refillItemsSection.getKeys(false)) {
                            int restoredPuffs = refillItemsSection.getInt(refillItem + ".restored_puffs", 1);
                            String returnItem = refillItemsSection.getString(refillItem + ".return_item", null);

                            refillItems.put(refillItem, new RefillItem(restoredPuffs, returnItem));
                        }

                    }
                }




                String returnItem = configuration.getString("cigarettes." + path + ".return_item", null);


                float consumeSeconds = (float) configuration.getDouble("cigarettes." + path + ".consume_seconds", 1);
                ItemUseAnimation itemUseAnimation;
                String animationName = Objects.requireNonNull(configuration.getString(
                        "cigarettes." + path + ".animation", "TOOT_HORN")).toUpperCase();
                try {
                    itemUseAnimation = ItemUseAnimation.valueOf(animationName);
                } catch (IllegalArgumentException e) {
                    plugin.getLogger().warning("Unknown animation:" + path);
                    continue;
                }

                float cooldownSeconds = (float) configuration.getDouble("cigarettes." + path + ".cooldown_seconds", 0);


                // ===================================== ВИЗУАЛ =====================================

                List<String> potionEffects = configuration.getStringList("cigarettes." + path + ".effects");
                List<String> particles = configuration.getStringList("cigarettes." + path + ".particles");

                Map<String, String> sounds = new HashMap<>();

                ConfigurationSection soundSection = configuration.getConfigurationSection("cigarettes." + path + ".sounds");

                if (soundSection == null) continue;
                for (String key : soundSection.getKeys(false)) {

                    String sound = soundSection.getString(key);

                    try {
                        sounds.put(key, sound);
                    } catch (NumberFormatException e) {
                        plugin.getLogger().warning("Invalid sounds key: " + key);
                    }
                }
                if (sounds.isEmpty()) {
                    sounds.put("ignite", "minecraft:item.flintandsteel.use, 2, 1.1");
                    sounds.put("extinguish", "minecraft:block.fire.extinguish, 2, 1.3");
                    sounds.put("puff", "minecraft:block.blastfurnace.fire_crackle, 2, 1.5");
                }
                if (puffsSection != null) {
                    String sound = configuration.getString("cigarettes." + path + ".puffs.refill_sound",
                            "minecraft:block.brewing_stand.brew, 2, 1.1");
                    sounds.put("refill_sound", sound);
                }



                // ===================================== НЕЗАЖЖЕННАЯ СИГАРЕТА =====================================

                ItemStack itemStackCigarettes = new ItemStack(baseMaterial);

                ItemMeta itemMetaCigarettes = itemStackCigarettes.getItemMeta();
                if (itemMetaCigarettes == null) continue;

                itemMetaCigarettes.itemName(name);

                if (!lore.isEmpty()) {
                    itemMetaCigarettes.lore(lore);
                }


                if (defaultItemModel != null) {
                    itemMetaCigarettes.setItemModel(NamespacedKey.fromString(defaultItemModel));
                }

                if (defaultCustomModelData != null) {
                    CustomModelDataComponent cmdComponent = itemMetaCigarettes.getCustomModelDataComponent();
                    cmdComponent.setStrings(Collections.singletonList(defaultCustomModelData));
                    itemMetaCigarettes.setCustomModelDataComponent(cmdComponent);
                }

                itemMetaCigarettes.setMaxStackSize(maxStackSize);

                PersistentDataContainer pdcCigarettes = itemMetaCigarettes.getPersistentDataContainer();
                pdcCigarettes.set(KEY_ID, PersistentDataType.STRING, path.toLowerCase());

                itemStackCigarettes.setItemMeta(itemMetaCigarettes);

                unlitCigarettes.put(path, itemStackCigarettes);



                // ===================================== ЗАЖЖЕННАЯ СИГАРЕТА =====================================

                ItemStack itemStackLitCigarettes = new ItemStack(baseMaterial);

                Consumable consumable = Consumable.consumable()
                        .consumeSeconds(consumeSeconds)
                        .animation(itemUseAnimation)
                        .sound(Key.key("q:q")) // ставится звук-заглушка
                        .hasConsumeParticles(false).build();

                itemStackLitCigarettes.setData(DataComponentTypes.CONSUMABLE, consumable);

                if (cooldownSeconds > 0) {
                    UseCooldown cooldown = UseCooldown.useCooldown(cooldownSeconds).cooldownGroup(Key.key(path)).build();

                    itemStackLitCigarettes.setData(DataComponentTypes.USE_COOLDOWN, cooldown);
                }
                ItemMeta itemMetaLitCigarettes = itemStackLitCigarettes.getItemMeta();

                itemMetaLitCigarettes.itemName(name);

                if (!lore.isEmpty()) {
                    itemMetaCigarettes.lore(lore);
                }

                itemMetaLitCigarettes.setMaxStackSize(1);

                PersistentDataContainer pdcLitCigarettes = itemMetaLitCigarettes.getPersistentDataContainer();

                pdcLitCigarettes.set(KEY_ID, PersistentDataType.STRING, path.toLowerCase());

                if (remainingItemModels.isEmpty() && defaultItemModel != null) {
                    itemMetaLitCigarettes.setItemModel(NamespacedKey.fromString(defaultItemModel));
                }

                if (remainingCustomModelData.isEmpty() && defaultCustomModelData != null) {
                    CustomModelDataComponent cmdComponentLit = itemMetaLitCigarettes.getCustomModelDataComponent();
                    cmdComponentLit.setStrings(Collections.singletonList(defaultCustomModelData));
                    itemMetaLitCigarettes.setCustomModelDataComponent(cmdComponentLit);
                }

                itemStackLitCigarettes.setItemMeta(itemMetaLitCigarettes);

                litCigarettes.put(path, new CigaretteDefinition(
                        itemStackLitCigarettes, remainingItemModels, remainingCustomModelData, nicotine, addiction,
                        burnDuration, burnTimeReductionPerPuff, requireIgnition, maxPuffs, keepAfterDepletion, refillItems,
                        returnItem, cooldownSeconds, particles, potionEffects, sounds
                        )
                );
            }
        }
    }
}