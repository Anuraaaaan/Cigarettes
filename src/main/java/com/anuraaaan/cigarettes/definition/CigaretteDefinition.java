package com.anuraaaan.cigarettes.definition;

import lombok.Getter;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;

import java.util.*;

@Getter
public class CigaretteDefinition {

    private final ItemStack itemStack;

    private final TreeMap<Integer, String> remainingItemModels;
    private final TreeMap<Integer, String> remainingCustomModelData;

    private final double nicotine;
    private final double addiction;

    private final long burnDuration;
    private final long burnTimeReductionPerPuff;
    private final boolean requireIgnition;

    private final int maxPuffs;
    private final boolean keepAfterDepletion;
    private final HashMap<String, RefillItem> refillItems;

    private final String returnItem;

    private final float cooldownSeconds;

    private final List<String> particles;

    private final List<String> effects;

    private final Map<String, String> sounds;

    public CigaretteDefinition(ItemStack itemStack, TreeMap<Integer, String> remainingItemModels,
                               TreeMap<Integer, String> remainingCustomModelData, double nicotine, double addiction,
                               long burnDuration, long burnTimeReductionPerPuff, boolean requireIgnition, int maxPuffs,
                               boolean keepAfterDepletion, HashMap<String, RefillItem> refillItems, String returnItem,
                               float cooldownSeconds, List<String> particles, List<String> effects, Map<String, String> sounds) {
        this.itemStack = itemStack;
        this.remainingItemModels = remainingItemModels;
        this.remainingCustomModelData = remainingCustomModelData;
        this.nicotine = nicotine;
        this.addiction = addiction;
        this.burnDuration = burnDuration;
        this.burnTimeReductionPerPuff = burnTimeReductionPerPuff;
        this.requireIgnition = requireIgnition;
        this.maxPuffs = maxPuffs;
        this.keepAfterDepletion = keepAfterDepletion;
        this.refillItems = refillItems;
        this.returnItem = returnItem;
        this.cooldownSeconds = cooldownSeconds;
        this.particles = particles;
        this.effects = effects;
        this.sounds = sounds;
    }

    private final List<PotionEffect> effectCache = new ArrayList<>();
    private final List<SmokeParticle> particleCache = new ArrayList<>();
}
