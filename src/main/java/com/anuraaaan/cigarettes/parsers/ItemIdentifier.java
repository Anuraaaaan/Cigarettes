package com.anuraaaan.cigarettes.parsers;

import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import javax.annotation.Nullable;

public final class ItemIdentifier {

    public static @Nullable String getItemId(ItemStack item, NamespacedKey keyId) {


        if (item == null || !item.hasItemMeta()) return null;

        ItemMeta meta = item.getItemMeta();
        if (meta == null) return null;

        return meta.getPersistentDataContainer().get(keyId, PersistentDataType.STRING);

    }
}
