package com.anuraaaan.cigarettes.managers;

import com.anuraaaan.cigarettes.Cigarettes;
import com.anuraaaan.cigarettes.definition.CigaretteDefinition;
import com.anuraaaan.cigarettes.definition.SmokeParticle;
import com.destroystokyo.paper.ParticleBuilder;
import org.apache.commons.lang3.StringUtils;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.util.Vector;

import javax.annotation.Nullable;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

public class ParticleManager {

    private final Cigarettes plugin;

    public ParticleManager(Cigarettes plugin) {
        this.plugin = plugin;
    }

    private final Set<Particle> ALLOWED_PARTICLES = EnumSet.of(
            Particle.SMOKE,
            Particle.LARGE_SMOKE,
            Particle.WHITE_SMOKE,
            Particle.CAMPFIRE_SIGNAL_SMOKE,
            Particle.CAMPFIRE_COSY_SMOKE,
            Particle.ASH,
            Particle.CLOUD,
            Particle.FLAME,
            Particle.SMALL_FLAME,
            Particle.DUST
    );


    private List<SmokeParticle> getParsedParticles(CigaretteDefinition cigaretteDefinition) {

        if (!cigaretteDefinition.getParticleCache().isEmpty()) return cigaretteDefinition.getParticleCache();

        for (String particle : cigaretteDefinition.getParticles()) {
            SmokeParticle parsed = parseParticle(particle);
            if (parsed != null) {
                cigaretteDefinition.getParticleCache().add(parsed);
            }
        }

        return cigaretteDefinition.getParticleCache();
    }

    public void spawnParticles(CigaretteDefinition cigaretteDefinition, Location eyeLocation) {
        Vector direction = eyeLocation.getDirection();

        for (SmokeParticle particle : getParsedParticles(cigaretteDefinition)) {
            Location location = eyeLocation.clone().add(
                    direction.multiply(particle.particleDistance())).add(0, particle.addY(), 0);

            particle.builder().location(location).spawn();
        }
    }

    private SmokeParticle parseParticle(@Nullable String particle) {

        if (particle == null || particle.isEmpty() || particle.equalsIgnoreCase("null")) return null;

        String[] split = StringUtils.split(StringUtils.deleteWhitespace(particle),',');

        if (split.length == 0) split = StringUtils.split(particle, ' ');

        if (split.length == 0) return null;

        Particle particleType;
        try {
            particleType = Particle.valueOf(split[0].toUpperCase());
        } catch (IllegalArgumentException e) {
            return null;
        }

        if (!ALLOWED_PARTICLES.contains(particleType)) {
            return null;
        }

        double particleDistance = 0.5;
        double addY = -0.25;

        int count = 4;
        double offsetX = 0;
        double offsetY = 0;
        double offsetZ = 0;
        double extra = 0.4;

        try {

            if (split.length > 1) particleDistance = Double.parseDouble(split[1]);
            if (split.length > 2) addY = Double.parseDouble(split[2]);

            if (split.length > 3) count = Integer.parseInt(split[3]);
            if (split.length > 4) offsetX = Double.parseDouble(split[4]);
            if (split.length > 5) offsetY = Double.parseDouble(split[5]);
            if (split.length > 6) offsetZ = Double.parseDouble(split[6]);
            if (split.length > 7) extra = Double.parseDouble(split[7]);
        } catch (NumberFormatException e) {
            plugin.getLogger().warning("Invalid number in particle: " + particle);
        }

        ParticleBuilder builder = new ParticleBuilder(particleType).count(count).offset(offsetX,offsetY,offsetZ).extra(extra);

        if (particleType == Particle.DUST) {
            int r = 166;
            int g = 166;
            int b = 166;

            float size = 2;
            try {
                if (split.length > 8) r = Integer.parseInt(split[8]);
                if (split.length > 9) g = Integer.parseInt(split[9]);
                if (split.length > 10) b = Integer.parseInt(split[10]);
                if (split.length > 11) size = Float.parseFloat(split[11]);

            } catch (NumberFormatException e) {
                plugin.getLogger().warning("Invalid number in particle: " + particle);
            }
            try {
                builder.data(new Particle.DustOptions(Color.fromRGB(r, g, b), size));
            } catch (IllegalArgumentException e) {
                plugin.getLogger().warning("Invalid number in particle: " + particle);
            }
        }

        return new SmokeParticle(builder, particleDistance, addY);
    }

}
