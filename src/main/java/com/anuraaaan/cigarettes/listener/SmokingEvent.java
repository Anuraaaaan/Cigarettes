package com.anuraaaan.cigarettes.listener;

import com.anuraaaan.cigarettes.Cigarettes;
import com.anuraaaan.cigarettes.definition.CigaretteDefinition;
import com.anuraaaan.cigarettes.managers.NicotineManager;
import com.anuraaaan.cigarettes.managers.NicotineWithdrawalManager;
import com.anuraaaan.cigarettes.nicotine.NicotineData;
import com.anuraaaan.cigarettes.parsers.ItemIdentifier;
import com.anuraaaan.cigarettes.registry.CigaretteRegistry;
import com.anuraaaan.cigarettes.registry.ConfigRegistry;
import com.anuraaaan.cigarettes.utils.TextFormatter;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.components.CustomModelDataComponent;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.potion.PotionEffect;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

public class SmokingEvent implements Listener {

    private final Cigarettes plugin;
    private final CigaretteRegistry cigaretteRegistry;
    private final NicotineWithdrawalManager nicotineWithdrawalManager;
    private final NicotineManager nicotineManager;
    private final ConfigRegistry configRegistry;

    private final NamespacedKey KEY_ID;
    private final NamespacedKey KEY_TIME;
    private final NamespacedKey KEY_REMAINING_PUFFS;

    public SmokingEvent(Cigarettes plugin, CigaretteRegistry cigaretteRegistry,
                        NicotineWithdrawalManager nicotineWithdrawalManager, NicotineManager nicotineManager,
                        ConfigRegistry configRegistry) {
        this.plugin = plugin;
        this.cigaretteRegistry = cigaretteRegistry;

        KEY_ID = new NamespacedKey(plugin, "cigarette");
        KEY_TIME = new NamespacedKey(plugin, "last_use");
        KEY_REMAINING_PUFFS = new NamespacedKey(plugin, "remaining_puffs");
        this.nicotineWithdrawalManager = nicotineWithdrawalManager;
        this.nicotineManager = nicotineManager;
        this.configRegistry = configRegistry;

        cigaretteExpirationTask();
    }

    @EventHandler
    public void onPlayerItemConsume(PlayerItemConsumeEvent e) {
        Player player = e.getPlayer();

        EquipmentSlot equipmentSlot = e.getHand();

        ItemStack item = equipmentSlot == EquipmentSlot.HAND ?
                player.getInventory().getItemInMainHand() : player.getInventory().getItemInOffHand();

        String id = ItemIdentifier.getItemId(item, KEY_ID);
        if (id == null) return;

        CigaretteDefinition cigarette = cigaretteRegistry.getLitCigarettes().get(id);
        if (cigarette == null) return;

        if (equipmentSlot == EquipmentSlot.HAND && plugin.getItemParser().isMatchingAny(player.getInventory().getItemInOffHand(),
                new ArrayList<>(cigarette.getRefillItems().keySet()), KEY_ID)) {

            e.setCancelled(true);
            return;
        }

        if (equipmentSlot == EquipmentSlot.OFF_HAND && plugin.getItemParser().isMatchingAny(player.getInventory().getItemInMainHand(),
                new ArrayList<>(cigarette.getRefillItems().keySet()), KEY_ID)) {

            e.setCancelled(true);
            return;
        }

        ItemMeta meta = item.getItemMeta();
        if (meta == null) return;

        PersistentDataContainer pdc = meta.getPersistentDataContainer();

        if (cigarette.getBurnDuration() > 0) {
            reduceLifetime(player, item, cigarette, meta, pdc, id);
        } else {
            reducePuffs(player, item, cigarette, meta, pdc, id);
        }
        e.setCancelled(true);
    }

    private void reduceLifetime(Player player, ItemStack item, CigaretteDefinition cigarette, ItemMeta meta, PersistentDataContainer pdc, String id) {
        long lastUse = pdc.getOrDefault(KEY_TIME, PersistentDataType.LONG, 0L);

        lastUse -= cigarette.getBurnTimeReductionPerPuff();

        pdc.set(KEY_TIME, PersistentDataType.LONG, lastUse);

        item.setItemMeta(meta);

        long burnDuration = cigarette.getBurnDuration();

        long remaining = burnDuration - (System.currentTimeMillis() - lastUse);

        updateItemModel(cigarette, meta, remaining, burnDuration);
        updateCustomModelData(cigarette, meta, remaining, burnDuration);

        spawnParticles(player, cigarette);

        int cooldownSeconds = (int) cigarette.getCooldownSeconds() * 20;
        if (cooldownSeconds > 0) {
            player.setCooldown(Key.key(id), cooldownSeconds);
        }
        addNicotine(player, cigarette);

        Location location = player.getLocation();
        if (System.currentTimeMillis() - lastUse >= burnDuration) {
            item.setAmount(0);

            ItemStack returnItem = plugin.getItemParser().parseItemIA(cigarette.getReturnItem());

            if (returnItem != null) {
                player.getInventory().addItem(returnItem);
            }

            plugin.getCigaretteManagers().getSoundManager().playSound(cigarette, location, "extinguish");
            return;
        }

        plugin.getCigaretteManagers().getSoundManager().playSound(cigarette, location, "puff");
    }

    private void reducePuffs(Player player, ItemStack item, CigaretteDefinition cigarette, ItemMeta meta, PersistentDataContainer pdc, String id) {

        int remainingPuffs = pdc.getOrDefault(KEY_REMAINING_PUFFS, PersistentDataType.INTEGER, 0);
        if (remainingPuffs <= 0 && cigarette.isKeepAfterDepletion()) return;

        if (remainingPuffs > 0) {
            remainingPuffs--;
        }

        pdc.set(KEY_REMAINING_PUFFS, PersistentDataType.INTEGER,remainingPuffs);

        List<Component> lore = (meta.lore() == null) ? new ArrayList<>() : meta.lore();
        Component line = TextFormatter.colorize(
                TextFormatter.setPlaceholdersString(
                        configRegistry.getLangMap().get("remaining_puffs"),
                        "%puffs%", Integer.toString(remainingPuffs)));

        if (meta.lore() == null) {
            lore.add(line);

            meta.lore(lore);
        } else {
            lore.set(lore.size() - 1, line);
            meta.lore(lore);
        }

        updateItemModel(cigarette, meta, remainingPuffs, cigarette.getMaxPuffs());
        updateCustomModelData(cigarette, meta, remainingPuffs, cigarette.getMaxPuffs());

        item.setItemMeta(meta);

        spawnParticles(player, cigarette);

        int cooldownSeconds = (int) cigarette.getCooldownSeconds() * 20;
        if (cooldownSeconds > 0) {
            player.setCooldown(Key.key(id), cooldownSeconds);
        }


        addNicotine(player, cigarette);

        Location location = player.getLocation();
        if (remainingPuffs < 1 && !cigarette.isKeepAfterDepletion()) {
            item.setAmount(0);

            ItemStack returnItem = plugin.getItemParser().parseItemIA(cigarette.getReturnItem());

            if (returnItem != null) {
                player.getInventory().addItem(returnItem);
            }
            plugin.getCigaretteManagers().getSoundManager().playSound(cigarette, location, "extinguish");
            return;
        }

        plugin.getCigaretteManagers().getSoundManager().playSound(cigarette, location, "puff");
    }

    private void addNicotine(Player player, CigaretteDefinition cigarette) {
        UUID uuid = player.getUniqueId();
        nicotineManager.addNicotineData(uuid, cigarette.getNicotine(), cigarette.getNicotine() * configRegistry.getTolerance(),
                cigarette.getAddiction());

        nicotineWithdrawalManager.setWithdrawalData(uuid);

        NicotineData nicotineData = nicotineManager.getNicotineData(uuid);
        player.sendActionBar(TextFormatter.colorize(
                TextFormatter.setPlaceholdersString(
                        configRegistry.getLangMap().get("nicotine_status"),
                        "%nicotine%", TextFormatter.formatDouble(nicotineData.getNicotine()),
                        "%tolerance%", TextFormatter.formatDouble(nicotineData.getTolerance()),
                        "%addiction%", TextFormatter.formatDouble(nicotineData.getAddiction()))));
    }

    private void applyEffects(CigaretteDefinition cigarette, Player player) {
        List<PotionEffect> effects = plugin.getCigaretteManagers().getEffectManager().getEffects(cigarette);
        for (PotionEffect effect : effects) {
            player.addPotionEffect(effect);
        }
    }

    private void spawnParticles(Player player, CigaretteDefinition cigarette) {
        Location eyeLocation = player.getEyeLocation();

        plugin.getCigaretteManagers().getParticleManager().spawnParticles(cigarette, eyeLocation);

        applyEffects(cigarette, player);
    }

    private void updateItemModel(CigaretteDefinition cigarette, ItemMeta meta, long remaining, long total) {
        if (cigarette.getRemainingItemModels().isEmpty()) {
            return;
        }

        int percent = (int) ((remaining * 100) / total);

        Integer threshold = cigarette.getRemainingItemModels().ceilingKey(percent);

        if (threshold != null) {
            meta.setItemModel(NamespacedKey.fromString(cigarette.getRemainingItemModels().get(threshold)));
        }
    }

    private void updateCustomModelData(CigaretteDefinition cigarette, ItemMeta meta, long remaining, long total) {
        if (cigarette.getRemainingCustomModelData().isEmpty()) {
            return;
        }

        int percent = (int) ((remaining * 100) / total);

        Integer threshold = cigarette.getRemainingCustomModelData().ceilingKey(percent);

        if (threshold != null) {

            CustomModelDataComponent cmdComponentLit = meta.getCustomModelDataComponent();
            cmdComponentLit.setStrings(Collections.singletonList(cigarette.getRemainingCustomModelData().get(threshold)));
            meta.setCustomModelDataComponent(cmdComponentLit);
        }
    }

    private void cigaretteExpirationTask() {
        new BukkitRunnable() {

            @Override
            public void run() {
                for (Player player : Bukkit.getOnlinePlayers()) {
                    for (ItemStack item : player.getInventory().getContents()) {
                        if (item == null) continue;

                        String id = ItemIdentifier.getItemId(item, KEY_ID);
                        if (id == null) continue;

                        CigaretteDefinition cigarette = cigaretteRegistry.getLitCigarettes().get(id);
                        if (cigarette == null) continue;

                        ItemMeta meta = item.getItemMeta();
                        if (meta == null) continue;

                        PersistentDataContainer pdc = meta.getPersistentDataContainer();

                        if (!pdc.has(KEY_ID, PersistentDataType.STRING)) continue;
                        if (!pdc.has(KEY_TIME, PersistentDataType.LONG)) continue;

                        long lastUse = pdc.getOrDefault(KEY_TIME, PersistentDataType.LONG, 0L);

                        long burnDuration = cigarette.getBurnDuration();

                        long remaining = burnDuration - (System.currentTimeMillis() - lastUse);

                        updateItemModel(cigarette, meta, remaining, burnDuration);
                        updateCustomModelData(cigarette, meta, remaining, burnDuration);

                        item.setItemMeta(meta);

                        if (System.currentTimeMillis() - lastUse >= burnDuration) {

                            item.setAmount(0);
                            ItemStack returnItem = plugin.getItemParser().parseItemIA(cigarette.getReturnItem());

                            if (returnItem != null) {
                                player.getInventory().addItem(returnItem);
                            }

                            Location location = player.getLocation();
                            plugin.getCigaretteManagers().getSoundManager().playSound(cigarette, location, "extinguish");
                        }
                    }
                }
            }
        }.runTaskTimer(plugin, 0, 20L);
    }
}
