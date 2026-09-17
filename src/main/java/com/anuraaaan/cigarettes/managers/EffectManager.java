package com.anuraaaan.cigarettes.managers;

import com.anuraaaan.cigarettes.Cigarettes;
import com.anuraaaan.cigarettes.definition.CigaretteDefinition;

import org.apache.commons.lang3.StringUtils;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import javax.annotation.Nullable;
import java.util.List;

public class EffectManager {

    private final Cigarettes plugin;

    public EffectManager(Cigarettes plugin) {
        this.plugin = plugin;
    }

    public List<PotionEffect> getEffects(CigaretteDefinition cigaretteDefinition) {

        if (!cigaretteDefinition.getEffectCache().isEmpty()) return cigaretteDefinition.getEffectCache();
        for (String effect : cigaretteDefinition.getEffects()) {
            PotionEffect potionEffect = parseEffect(effect);

            if (potionEffect == null) continue;

            cigaretteDefinition.getEffectCache().add(potionEffect);
        }

        return cigaretteDefinition.getEffectCache();
    }

    public PotionEffect getEffect(@Nullable String effect) {
        return parseEffect(effect);
    }

    private PotionEffect parseEffect(@Nullable String effect) {

        if (effect == null || effect.isEmpty() || effect.equalsIgnoreCase("null")) return null;

        String[] split = StringUtils.split(StringUtils.deleteWhitespace(effect), ',');

        if(split.length == 0) split = StringUtils.split(effect, ' ');

        if (split.length == 0) return null;

        PotionEffectType type = PotionEffectType.getByName(split[0].toUpperCase());

        if (type == null) return null;

        int duration = 400;
        int amplifier = 0;

        try {
            if (split.length > 1) duration = Integer.parseInt(split[1]) * 20;
            if (split.length > 2) amplifier = Integer.parseInt(split[2]);

        } catch (NumberFormatException e) {
            plugin.getLogger().warning("Invalid number in effect: " + split[0].toUpperCase());
        }

        return new PotionEffect(type, duration, amplifier);
    }
}
