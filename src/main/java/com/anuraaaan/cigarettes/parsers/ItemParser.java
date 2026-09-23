package com.anuraaaan.cigarettes.parsers;

import com.anuraaaan.cigarettes.Cigarettes;
import com.anuraaaan.cigarettes.definition.CustomItemDefinition;
import com.anuraaaan.cigarettes.registry.CigaretteRegistry;
import com.anuraaaan.cigarettes.registry.CustomItemsRegistry;
import dev.lone.itemsadder.api.CustomStack;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;

import java.util.List;

public class ItemParser {

    private final Cigarettes plugin;
    private final CigaretteRegistry cigaretteRegistry;
    private final CustomItemsRegistry customItemsRegistry;

    public ItemParser(Cigarettes plugin, CigaretteRegistry cigaretteRegistry, CustomItemsRegistry customItemsRegistry) {
        this.plugin = plugin;
        this.cigaretteRegistry = cigaretteRegistry;
        this.customItemsRegistry = customItemsRegistry;
    }

    public ItemStack parseItemIA(String item) {

        if (item == null) {
            return null;
        }

        if (item.contains(":") && plugin.isEnabledItemsAdder()) {
            CustomStack cs = CustomStack.getInstance(item);
            if (cs != null) {
                return cs.getItemStack();
            }
        }

        return parseItem(item);
    }

    public ItemStack parseItemNoIA(String item) {
        if (item == null) {
            return null;
        }

        return parseItem(item);
    }

    private ItemStack parseItem(String item) {

        ItemStack cigarette = cigaretteRegistry.getUnlitCigarettes().get(item);
        if (cigarette != null) {
            return cigarette.clone();
        }

        CustomItemDefinition customItemDefinition = customItemsRegistry.getCustomItems().get(item);
        if (customItemDefinition != null) {
            return customItemDefinition.getItemStack().clone();

        }

        Material material = Material.matchMaterial(item.toUpperCase());
        if (material != null) {

            return new ItemStack(material);
        }
        return null;
    }

    public boolean isMatchingAny(ItemStack item, List<String> items, NamespacedKey KEY_ID) {

        if (item == null) return false;

        String itemInHandId = ItemIdentifier.getItemId(item, KEY_ID);
        Material itemInHandMaterial = item.getType();

        for (String itemInList : items) {
            ItemStack itemStackInList = parseItemIA(itemInList);

            if (itemStackInList == null) {
                continue;
            }
            if (plugin.isEnabledItemsAdder()) {
                CustomStack csInHand = CustomStack.byItemStack(item);
                CustomStack csInList = CustomStack.byItemStack(itemStackInList);

                if (csInHand != null) {
                    if (csInList != null && csInList.getId().equals(csInHand.getId())) {
                        return true;
                    }
                    continue;
                }
            }

            String idInList = ItemIdentifier.getItemId(itemStackInList, KEY_ID);


            if (itemInHandId != null) {
                if (idInList != null && idInList.equals(itemInHandId)) {
                    return true;
                }
                continue;
            }

            if (plugin.isEnabledItemsAdder()) {
                CustomStack csInList = CustomStack.byItemStack(itemStackInList);
                Material materialInList = itemStackInList.getType();
                if (materialInList.equals(itemInHandMaterial) && csInList == null && idInList == null) {
                    return true;
                }
            }
            Material materialInList = itemStackInList.getType();
            if (materialInList.equals(itemInHandMaterial) && idInList == null) {
                return true;
            }

        }

        return false;
    }

}
