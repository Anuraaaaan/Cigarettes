package com.anuraaaan.cigarettes.listener;

import com.anuraaaan.cigarettes.Cigarettes;
import com.anuraaaan.cigarettes.definition.CigaretteDefinition;
import com.anuraaaan.cigarettes.parsers.ItemIdentifier;
import com.anuraaaan.cigarettes.registry.CigaretteRegistry;
import com.anuraaaan.cigarettes.registry.ConfigRegistry;
import com.anuraaaan.cigarettes.utils.TextFormatter;
import net.kyori.adventure.text.Component;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockIgniteEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.components.CustomModelDataComponent;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class SmokingItemActivateEvent implements Listener {

    private final Cigarettes plugin;

    private final CigaretteRegistry cigaretteRegistry;
    private final ConfigRegistry configRegistry;

    private final NamespacedKey KEY_ID;
    private final NamespacedKey KEY_TIME;
    private final NamespacedKey KEY_REMAINING_PUFFS;

    public SmokingItemActivateEvent(Cigarettes plugin, CigaretteRegistry cigaretteRegistry, ConfigRegistry configRegistry) {
        this.plugin = plugin;

        this.cigaretteRegistry = cigaretteRegistry;
        this.configRegistry = configRegistry;

        KEY_ID = new NamespacedKey(plugin, "cigarette");
        KEY_TIME = new NamespacedKey(plugin, "last_use");
        KEY_REMAINING_PUFFS = new NamespacedKey(plugin, "remaining_puffs");


    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent e) {
        if (e.getHand() != EquipmentSlot.HAND) return;

        Player player = e.getPlayer();
        if (!e.getAction().isRightClick()) return;

        ItemStack cigaretteItemStack = null;
        EquipmentSlot igniterSlot = null;

        ItemStack right = player.getInventory().getItemInMainHand();
        ItemStack left = player.getInventory().getItemInOffHand();

        if (cigaretteRegistry.getLitCigarettes().get(ItemIdentifier.getItemId(right, KEY_ID)) != null) {
            cigaretteItemStack = right;
            igniterSlot = EquipmentSlot.OFF_HAND;
        } else if (cigaretteRegistry.getLitCigarettes().get(ItemIdentifier.getItemId(left, KEY_ID)) != null) {
            cigaretteItemStack = left;
            igniterSlot = EquipmentSlot.HAND;
        } else {
            return;
        }

        ItemStack igniterItemStack = igniterSlot == EquipmentSlot.HAND
                ? player.getInventory().getItemInMainHand() : player.getInventory().getItemInOffHand();

        String id = ItemIdentifier.getItemId(cigaretteItemStack, KEY_ID);
        if (id == null) return;

        if (cigaretteItemStack.getItemMeta().getPersistentDataContainer().has(KEY_TIME, PersistentDataType.LONG) ||
                cigaretteItemStack.getItemMeta().getPersistentDataContainer().has(KEY_REMAINING_PUFFS, PersistentDataType.INTEGER)) return;

        if (cigaretteRegistry.getLitCigarettes().get(id) == null) return;

        CigaretteDefinition litCigarette = cigaretteRegistry.getLitCigarettes().get(id);
        if (litCigarette == null) return;

        if (litCigarette.getBurnDuration() > 0) {
            if (!plugin.getItemParser().isMatchingAny(igniterItemStack, configRegistry.getListIgniters(), KEY_ID) &&
                    litCigarette.isRequireIgnition()) return;
        }

        e.setCancelled(true);

        ItemStack litCigaretteItem = litCigarette.getItemStack().clone();

        ItemMeta litCigaretteMeta = litCigaretteItem.getItemMeta();

        PersistentDataContainer pdcLitCigarette = litCigaretteMeta.getPersistentDataContainer();

        if (litCigarette.getBurnDuration() > 0) {

            pdcLitCigarette.set(KEY_TIME, PersistentDataType.LONG, System.currentTimeMillis());
        }
        if (litCigarette.getMaxPuffs() > 0) {
            pdcLitCigarette.set(KEY_REMAINING_PUFFS, PersistentDataType.INTEGER, litCigarette.getMaxPuffs());

            List<Component> lore = litCigaretteMeta.lore();
            Component line = TextFormatter.colorize(
                    TextFormatter.setPlaceholdersString(
                            configRegistry.getLangMap().get("remaining_puffs"),
                            "%puffs%", Integer.toString(litCigarette.getMaxPuffs())));

            if (lore == null) {
                lore = new ArrayList<>();
            }

            if (lore.isEmpty()) {
                lore.add(line);
            } else {
                lore.set(lore.size() - 1, line);
            }
            litCigaretteMeta.lore(lore);
        }

        Integer thresholdItemModel = litCigarette.getRemainingItemModels().ceilingKey(100);
        if (!litCigarette.getRemainingItemModels().isEmpty() && thresholdItemModel != null) {
            litCigaretteMeta.setItemModel(NamespacedKey.fromString(litCigarette.getRemainingItemModels().get(thresholdItemModel)));
        }

        Integer thresholdCMD = litCigarette.getRemainingCustomModelData().ceilingKey(100);
        if (!litCigarette.getRemainingCustomModelData().isEmpty() && thresholdCMD != null) {
            CustomModelDataComponent cmdComponentLit = litCigaretteMeta.getCustomModelDataComponent();
            cmdComponentLit.setStrings(Collections.singletonList(litCigarette.getRemainingCustomModelData().get(thresholdCMD)));
            litCigaretteMeta.setCustomModelDataComponent(cmdComponentLit);
        }

        litCigaretteItem.setItemMeta(litCigaretteMeta);
        player.swingMainHand();

        if (litCigarette.getBurnDuration() > 0 && litCigarette.isRequireIgnition()) {
            player.swingOffHand();

            ItemMeta metaLeft = igniterItemStack.getItemMeta();

            if (metaLeft instanceof Damageable damageable && igniterItemStack.getType().getMaxDurability() > 0) {

                int damage = damageable.getDamage();
                int maxDamage = igniterItemStack.getType().getMaxDurability();

                if (damage + 1 >= maxDamage) {
                    igniterItemStack.setAmount(igniterItemStack.getAmount() - 1);
                } else {
                    damageable.setDamage(damage + 1);
                    igniterItemStack.setItemMeta(metaLeft);
                }

            } else {
                igniterItemStack.setAmount(igniterItemStack.getAmount() - 1);
            }
        }

        if (cigaretteItemStack.getAmount() <= 1) {
            cigaretteItemStack.setAmount(cigaretteItemStack.getAmount() - 1);
            if (igniterSlot == EquipmentSlot.HAND) {
                player.getInventory().setItemInOffHand(litCigaretteItem);
            }
            if (igniterSlot == EquipmentSlot.OFF_HAND) {
                player.getInventory().setItemInMainHand(litCigaretteItem);
            }
        } else {
            cigaretteItemStack.setAmount(cigaretteItemStack.getAmount() - 1);
            player.getInventory().addItem(litCigaretteItem);
        }

        Location location = player.getLocation();
        plugin.getCigaretteManagers().getSoundManager().playSound(litCigarette, location, "ignite");
    }

    @EventHandler
    public void onBlockIgnite(BlockIgniteEvent e) {
        Player player = e.getPlayer();

        if (player == null) return;

        ItemStack cigaretteItemStack = null;
        EquipmentSlot igniterSlot = null;

        ItemStack right = player.getInventory().getItemInMainHand();
        ItemStack left = player.getInventory().getItemInOffHand();

        if (cigaretteRegistry.getLitCigarettes().get(ItemIdentifier.getItemId(right, KEY_ID)) != null) {
            cigaretteItemStack = right;
            igniterSlot = EquipmentSlot.OFF_HAND;
        } else if (cigaretteRegistry.getLitCigarettes().get(ItemIdentifier.getItemId(left, KEY_ID)) != null) {
            cigaretteItemStack = left;
            igniterSlot = EquipmentSlot.HAND;
        } else {
            return;
        }

        ItemStack igniterItemStack = igniterSlot == EquipmentSlot.HAND
                ? player.getInventory().getItemInMainHand() : player.getInventory().getItemInOffHand();

        String id = ItemIdentifier.getItemId(cigaretteItemStack, KEY_ID);
        if (id == null) return;

        if (cigaretteItemStack.getItemMeta().getPersistentDataContainer().has(KEY_TIME, PersistentDataType.LONG) ||
                cigaretteItemStack.getItemMeta().getPersistentDataContainer().has(KEY_REMAINING_PUFFS, PersistentDataType.INTEGER)) return;

        if (cigaretteRegistry.getLitCigarettes().get(id).getBurnDuration() > 0) {
            if (!plugin.getItemParser().isMatchingAny(igniterItemStack, configRegistry.getListIgniters(), KEY_ID)
                    && cigaretteRegistry.getLitCigarettes().get(id).isRequireIgnition()) return;
        }
        e.setCancelled(true);
    }
}
