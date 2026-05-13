package com.anuraaaan.cigarettes.listener;

import com.anuraaaan.cigarettes.Cigarettes;
import com.anuraaaan.cigarettes.cigarette.CigaretteRegistry;
import com.anuraaaan.cigarettes.cigarette.ConfiguredCigarette;
import com.anuraaaan.cigarettes.utils.CigaretteSoundPlayer;
import org.bukkit.Location;
import org.bukkit.Material;
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
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

// поджигает сигарету
public class CigaretteIgniteListener implements Listener {
    private static final Cigarettes plugin = Cigarettes.getPlugin();
    private static final NamespacedKey KEY_TIME = new NamespacedKey(plugin, "last_use");

    @EventHandler
    public void onCigaretteIgnite(PlayerInteractEvent event) {
        // проверяет руку выполнения ивента
        if (event.getHand() != EquipmentSlot.HAND) return;
        // получает игрока
        Player player = event.getPlayer();
        // проверяет какой клик происходит
        if (!event.getAction().isRightClick()) return;

        // получает предметы из правой и левой рук
        ItemStack right = event.getPlayer().getInventory().getItemInMainHand();
        ItemStack left = event.getPlayer().getInventory().getItemInOffHand();

        // получает id предмета и если пусто, то ничего не делает
        String id = CigaretteRegistry.getId(right);
        if (id == null) return;

        // проверяет, есть ли у предмета pdc времени
        if (right.getItemMeta().getPersistentDataContainer().has(KEY_TIME, PersistentDataType.LONG)) return;

        // проверяет материал огнива и необходимо ли оно вообще для поджога
        if (left.getType() != Material.FLINT_AND_STEEL
                && CigaretteRegistry.getLitCigarettes().get(id).isRequiresIgnition()) return;

        // отменяет изначальный ивент во избежание проблем
        event.setCancelled(true);
        // получает образец зажжённой сигареты
        ConfiguredCigarette litCigarette = CigaretteRegistry.getLitCigarettes().get(id);
        if (litCigarette == null) return;

        // клонирует предмет, который находится в переменной litCigarette
        ItemStack litCigaretteItem = litCigarette.getItemStack().clone();
        // получает мету
        ItemMeta litCigaretteMeta = litCigaretteItem.getItemMeta();
        // получает pdc
        PersistentDataContainer pdclitCigarette = litCigaretteMeta.getPersistentDataContainer();

        // устанавливает ключ со значением времени
        pdclitCigarette.set(KEY_TIME, PersistentDataType.LONG, System.currentTimeMillis());

        // получает и устанавливает модель с ключом 100%
        Integer threshold = litCigarette.getItemModels().ceilingKey(100);
        if (threshold != null) {
            litCigaretteMeta.setItemModel(NamespacedKey.fromString(litCigarette.getItemModels().get(threshold)));
        }
        // устанавливает мету
        litCigaretteItem.setItemMeta(litCigaretteMeta);

        // проверяет необходимость в поджигании для уменьшения прочности огнива
        if (CigaretteRegistry.getLitCigarettes().get(id).isRequiresIgnition()) {
            // получает мету предмета
            ItemMeta metaLeft = left.getItemMeta();
            // переводит мету в прочность
            Damageable damageable = (Damageable) metaLeft;
            // получает кол-во прочности и уменьшает на 1
            int currentDamage = damageable.getDamage();
            damageable.setDamage(currentDamage + 1);
            // устанавливается мета предмету
            left.setItemMeta(damageable);
        }
        // уменьшает кол-во сигарет в инвентаре
        right.setAmount(right.getAmount() - 1);

        // получает координаты игрока и создаёт звук, также выдаёт зажжённую сигарету
        Location location = player.getLocation();
        CigaretteSoundPlayer.playSound(litCigarette, location, "light");
        player.getInventory().addItem(litCigaretteItem);
    }

    @EventHandler
    public void onBlockIgnite(BlockIgniteEvent igniteEvent) {
        Player player = igniteEvent.getPlayer();

        if (player == null) return;

        ItemStack right = player.getInventory().getItemInMainHand();
        ItemStack left = player.getInventory().getItemInOffHand();

        // получает id предмета и если пусто, то ничего не делает
        String id = CigaretteRegistry.getId(right);
        if (id == null) return;

        // проверяет, есть ли у предмета pdc времени
        if (right.getItemMeta().getPersistentDataContainer().has(KEY_TIME, PersistentDataType.LONG)) return;

        // проверяет материал огнива и необходимо ли оно вообще для поджога
        if (left.getType() != Material.FLINT_AND_STEEL
                && CigaretteRegistry.getLitCigarettes().get(id).isRequiresIgnition()) return;
        igniteEvent.setCancelled(true);
    }
}
