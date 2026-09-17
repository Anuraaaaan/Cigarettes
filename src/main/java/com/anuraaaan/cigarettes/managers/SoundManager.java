package com.anuraaaan.cigarettes.managers;

import com.anuraaaan.cigarettes.Cigarettes;
import com.anuraaaan.cigarettes.definition.CigaretteDefinition;
import org.apache.commons.lang3.StringUtils;
import org.bukkit.Location;
import org.bukkit.SoundCategory;

public class SoundManager {

    private final Cigarettes plugin;

    public SoundManager(Cigarettes plugin) {
        this.plugin = plugin;
    }

    public void playSound(CigaretteDefinition cigaretteDefinition, Location location, String key) {

        String sound = cigaretteDefinition.getSounds().get(key);

        if (sound == null || sound.isEmpty() || sound.equalsIgnoreCase("null")) return;

        String[] split = StringUtils.split(StringUtils.deleteWhitespace(sound),',');

        if (split.length == 0) split = StringUtils.split(sound, ' ');

        if (split.length == 0) return;

        String soundType = split[0];

        if (soundType == null) return;

        float volume = 1f;
        float pitch = 1f;

        try {

            if (split.length > 1) volume = Float.parseFloat(split[1]);
            if (split.length > 2) pitch = Float.parseFloat(split[2]);
        } catch (NumberFormatException e) {
            plugin.getLogger().warning("Invalid number specified. Check the sounds configuration");
        }

        location.getWorld().playSound(location, soundType, SoundCategory.PLAYERS, volume, pitch);
    }
}
