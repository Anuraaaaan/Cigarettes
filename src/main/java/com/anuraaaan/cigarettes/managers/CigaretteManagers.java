package com.anuraaaan.cigarettes.managers;

import com.anuraaaan.cigarettes.Cigarettes;
import lombok.Getter;


public class CigaretteManagers {

    @Getter
    private final EffectManager effectManager;
    @Getter
    private final ParticleManager particleManager;
    @Getter
    private final SoundManager soundManager;


    public CigaretteManagers(Cigarettes plugin) {
        effectManager = new EffectManager(plugin);
        particleManager = new ParticleManager(plugin);
        soundManager = new SoundManager(plugin);
    }
}
