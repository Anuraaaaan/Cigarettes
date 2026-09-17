package com.anuraaaan.cigarettes.definition;

import com.destroystokyo.paper.ParticleBuilder;

public record SmokeParticle(
        ParticleBuilder builder,
        double particleDistance,
        double addY
) {}
