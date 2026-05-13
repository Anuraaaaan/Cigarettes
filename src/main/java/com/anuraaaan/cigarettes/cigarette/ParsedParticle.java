package com.anuraaaan.cigarettes.cigarette;

import com.destroystokyo.paper.ParticleBuilder;
// класс для установки значений партикла
public record ParsedParticle(
        ParticleBuilder builder,
        double particleDistance,
        double addY
) {}
