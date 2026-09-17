package com.anuraaaan.cigarettes.listener;

import com.anuraaaan.cigarettes.Cigarettes;
import com.anuraaaan.cigarettes.definition.CigaretteDefinition;
import com.anuraaaan.cigarettes.definition.RefillItem;
import com.anuraaaan.cigarettes.parsers.ItemIdentifier;
import com.anuraaaan.cigarettes.registry.CigaretteRegistry;
import com.anuraaaan.cigarettes.registry.ConfigRegistry;
import com.anuraaaan.cigarettes.utils.TextFormatter;
import dev.lone.itemsadder.api.CustomStack;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.components.CustomModelDataComponent;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

public class RefillEvent implements Listener {

    private final Cigarettes plugin;
    private final CigaretteRegistry cigaretteRegistry;
    private final ConfigRegistry configRegistry;

    private final NamespacedKey KEY_ID;
    private final NamespacedKey KEY_TIME;
    private final NamespacedKey KEY_REMAINING_PUFFS;

    public RefillEvent(Cigarettes plugin, CigaretteRegistry cigaretteRegistry, ConfigRegistry configRegistry) {
        this.plugin = plugin;
        this.cigaretteRegistry = cigaretteRegistry;

        KEY_ID = new NamespacedKey(plugin, "cigarette");
        KEY_TIME = new NamespacedKey(plugin, "last_use");
        KEY_REMAINING_PUFFS = new NamespacedKey(plugin, "remaining_puffs");
        this.configRegistry = configRegistry;
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent e){
        if (e.getHand() != EquipmentSlot.HAND) return;

        Player player = e.getPlayer();

        if (!e.getAction().isRightClick()) return;

        ItemStack cigaretteItemStack = null;
        EquipmentSlot refillSlot = null;

        ItemStack right = player.getInventory().getItemInMainHand();
        ItemStack left = player.getInventory().getItemInOffHand();

        if (cigaretteRegistry.getLitCigarettes().get(ItemIdentifier.getItemId(right, KEY_ID)) != null) {
            cigaretteItemStack = right;
            refillSlot = EquipmentSlot.OFF_HAND;
        } else if (cigaretteRegistry.getLitCigarettes().get(ItemIdentifier.getItemId(left, KEY_ID)) != null) {
            cigaretteItemStack = left;
            refillSlot = EquipmentSlot.HAND;
        } else {
            return;
        }

        ItemStack refillItemStack = refillSlot == EquipmentSlot.HAND
                ? player.getInventory().getItemInMainHand() : player.getInventory().getItemInOffHand();

        String idCigarette = ItemIdentifier.getItemId(cigaretteItemStack, KEY_ID);
        if (idCigarette == null) return;

        ItemMeta cigaretteItemMeta = cigaretteItemStack.getItemMeta();
        PersistentDataContainer pdcItemCigarette = cigaretteItemMeta.getPersistentDataContainer();
        if (pdcItemCigarette.has(KEY_TIME, PersistentDataType.LONG)) return;
        if (!pdcItemCigarette.has(KEY_REMAINING_PUFFS, PersistentDataType.INTEGER)) return;

        CigaretteDefinition cigarette = cigaretteRegistry.getLitCigarettes().get(idCigarette);

        if (cigarette == null) {
            return;
        }

        ItemStack returnItem = null;
        int addedPuffs = 0;

        HashMap<String, RefillItem> refillItems = cigarette.getRefillItems();
        for (String item : refillItems.keySet()) {
            if (isRefillItem(refillItemStack, item)) {
                String returnId = refillItems.get(item).returnItem();
                if (returnId != null) {
                    returnItem = plugin.getItemParser().parseItemIA(returnId);
                }
                addedPuffs = refillItems.get(item).restoredPuffs();
                break;
            }
        }
        if (addedPuffs <= 0) return;

        int puff = pdcItemCigarette.getOrDefault(KEY_REMAINING_PUFFS, PersistentDataType.INTEGER, 0);
        int newPuffs = puff + addedPuffs;
        if (newPuffs > cigarette.getMaxPuffs()) {

            player.sendActionBar(Component.text(TextFormatter.colorize(configRegistry.getLangMap().get("tank_full"))));
            return;
        }
        pdcItemCigarette.set(KEY_REMAINING_PUFFS, PersistentDataType.INTEGER, newPuffs);

        if (!cigarette.getRemainingItemModels().isEmpty()) {
            int percent = (newPuffs * 100) / cigarette.getMaxPuffs();

            Integer threshold = cigarette.getRemainingItemModels().ceilingKey(percent);

            if (threshold != null) {
                cigaretteItemMeta.setItemModel(NamespacedKey.fromString(cigarette.getRemainingItemModels().get(threshold)));
            }
        }

        if (!cigarette.getRemainingCustomModelData().isEmpty()) {
            int percent = (newPuffs * 100) / cigarette.getMaxPuffs();

            Integer threshold = cigarette.getRemainingCustomModelData().ceilingKey(percent);

            if (threshold != null) {
                CustomModelDataComponent cmdComponentLit = cigaretteItemMeta.getCustomModelDataComponent();
                cmdComponentLit.setStrings(Collections.singletonList(cigarette.getRemainingCustomModelData().get(threshold)));
                cigaretteItemMeta.setCustomModelDataComponent(cmdComponentLit);
            }
        }

        List<String> lore = cigaretteItemMeta.getLore();
        String line = TextFormatter.colorize(
                TextFormatter.setPlaceholdersString(
                        configRegistry.getLangMap().get("remaining_puffs"),
                        "%puffs%", Integer.toString(newPuffs)));


        if (lore == null) {
            lore = new ArrayList<>();
        }

        if (lore.isEmpty()) {
            lore.add(line);
        } else {
            lore.set(lore.size() - 1, line);
        }

        cigaretteItemMeta.setLore(lore);
        cigaretteItemStack.setItemMeta(cigaretteItemMeta);

        if (returnItem != null) {
            player.getInventory().addItem(returnItem);
        }
        refillItemStack.setAmount(refillItemStack.getAmount() - 1);
        plugin.getCigaretteManagers().getSoundManager().playSound(cigarette, player.getLocation(), "refill_sound");
    }

    private boolean isRefillItem(ItemStack item, String itemId) {
        if (item == null || item.isEmpty() || itemId == null) {
            return false;
        }

        ItemStack itemStackInList = plugin.getItemParser().parseItemIA(itemId);

        if (itemStackInList == null) {
            return false;
        }

        String itemInHandId = ItemIdentifier.getItemId(item, KEY_ID);
        Material itemInHandMaterial = item.getType();

        if (plugin.isEnabledItemsAdder()) {
            CustomStack csInHand = CustomStack.byItemStack(item);
            CustomStack csId = CustomStack.byItemStack(itemStackInList);
            if (csInHand != null) {
                return csId != null && csId.getId().equals(csInHand.getId());
            }
        }
        String idId = ItemIdentifier.getItemId(itemStackInList, KEY_ID);
        if (itemInHandId != null) {
            return idId != null && idId.equals(itemInHandId);
        }
        if (plugin.isEnabledItemsAdder()) {
            CustomStack csId = CustomStack.byItemStack(itemStackInList);
            Material materialInList = itemStackInList.getType();
            return materialInList.equals(itemInHandMaterial) && csId == null && idId == null;
        }
        Material materialInList = itemStackInList.getType();
        return materialInList.equals(itemInHandMaterial) && idId == null;
    }
}
