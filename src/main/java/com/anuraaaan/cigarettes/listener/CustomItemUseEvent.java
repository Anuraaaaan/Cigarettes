package com.anuraaaan.cigarettes.listener;

import com.anuraaaan.cigarettes.Cigarettes;
import com.anuraaaan.cigarettes.definition.CustomItemDefinition;
import com.anuraaaan.cigarettes.managers.NicotineManager;
import com.anuraaaan.cigarettes.managers.NicotineWithdrawalManager;
import com.anuraaaan.cigarettes.nicotine.NicotineData;
import com.anuraaaan.cigarettes.parsers.ItemIdentifier;
import com.anuraaaan.cigarettes.registry.ConfigRegistry;
import com.anuraaaan.cigarettes.registry.CustomItemsRegistry;
import com.anuraaaan.cigarettes.utils.TextFormatter;
import net.kyori.adventure.text.Component;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

import java.util.UUID;

public class CustomItemUseEvent implements Listener {

    private final Cigarettes plugin;
    private final CustomItemsRegistry customItemsRegistry;
    private final NicotineManager nicotineManager;
    private final ConfigRegistry configRegistry;
    private final NicotineWithdrawalManager nicotineWithdrawalManager;

    private final NamespacedKey KEY_ID;

    public CustomItemUseEvent(Cigarettes plugin, CustomItemsRegistry customItemsRegistry, NicotineManager nicotineManager,
                              ConfigRegistry configRegistry, NicotineWithdrawalManager nicotineWithdrawalManager) {
        this.plugin = plugin;
        this.customItemsRegistry = customItemsRegistry;
        this.nicotineManager = nicotineManager;
        this.configRegistry = configRegistry;

        KEY_ID = new NamespacedKey(plugin, "cigarette");

        this.nicotineWithdrawalManager = nicotineWithdrawalManager;
    }

    @EventHandler
    public void onPlayerItemConsume(PlayerItemConsumeEvent e) {

        Player player = e.getPlayer();
        EquipmentSlot equipmentSlot = e.getHand();

        ItemStack item = equipmentSlot == EquipmentSlot.HAND ?
                player.getInventory().getItemInMainHand() : player.getInventory().getItemInOffHand();

        String id = ItemIdentifier.getItemId(item, KEY_ID);
        if (id == null) return;

        CustomItemDefinition customItem = customItemsRegistry.getCustomItems().get(id);
        if (customItem == null || customItem.isUseOnRightClick() || !customItem.isConsumable()) return;

        addNicotine(player, customItem);

        ItemStack returnItem = plugin.getItemParser().parseItemIA(customItem.getReturnItem());

        if (returnItem != null) {
            player.getInventory().addItem(returnItem);
        }
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent e) {

        Player player = e.getPlayer();
        if (!e.getAction().isRightClick()) return;
        EquipmentSlot equipmentSlot = e.getHand();

        ItemStack item = equipmentSlot == EquipmentSlot.HAND ?
                player.getInventory().getItemInMainHand() : player.getInventory().getItemInOffHand();

        String id = ItemIdentifier.getItemId(item, KEY_ID);
        if (id == null) return;

        CustomItemDefinition customItem = customItemsRegistry.getCustomItems().get(id);
        if (customItem == null || customItem.isConsumable() || !customItem.isUseOnRightClick()) return;

        item.setAmount(item.getAmount() - 1);
        addNicotine(player, customItem);
        ItemStack returnItem = plugin.getItemParser().parseItemIA(customItem.getReturnItem());

        if (returnItem != null) {
            player.getInventory().addItem(returnItem);
        }

    }

    private void addNicotine(Player player, CustomItemDefinition customItem) {
        UUID uuid = player.getUniqueId();
        nicotineManager.addNicotineData(uuid, customItem.getNicotine(), customItem.getNicotine() * configRegistry.getTolerance(),
                customItem.getAddiction());

        nicotineWithdrawalManager.setWithdrawalData(uuid);

        NicotineData nicotineData = nicotineManager.getNicotineData(uuid);
        player.sendActionBar(Component.text(TextFormatter.colorize(
                TextFormatter.setPlaceholdersString(
                        configRegistry.getLangMap().get("nicotine_status"),
                        "%nicotine%", TextFormatter.formatDouble(nicotineData.getNicotine()),
                        "%tolerance%", TextFormatter.formatDouble(nicotineData.getTolerance()),
                        "%addiction%", TextFormatter.formatDouble(nicotineData.getAddiction())))));
    }
}
