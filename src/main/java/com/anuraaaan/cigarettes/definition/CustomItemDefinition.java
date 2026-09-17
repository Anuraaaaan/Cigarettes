package com.anuraaaan.cigarettes.definition;

import lombok.Getter;
import org.bukkit.inventory.ItemStack;

@Getter
public class CustomItemDefinition {
    private final ItemStack itemStack;
    private final double nicotine;
    private final double tolerance;
    private final double addiction;
    private final boolean consumable;
    private final boolean useOnRightClick;
    private final String returnItem;


    public CustomItemDefinition(ItemStack itemStack, double nicotine, double tolerance, double addiction, boolean consumable,
                                boolean useOnRightClick, String returnItem) {
        this.itemStack = itemStack;
        this.nicotine = nicotine;
        this.tolerance = tolerance;
        this.addiction = addiction;

        this.consumable = consumable;
        this.useOnRightClick = useOnRightClick;
        this.returnItem = returnItem;
    }
}
