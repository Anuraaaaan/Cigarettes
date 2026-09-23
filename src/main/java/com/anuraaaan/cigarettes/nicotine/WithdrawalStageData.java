package com.anuraaaan.cigarettes.nicotine;

import lombok.Getter;
import net.kyori.adventure.text.Component;
import org.bukkit.potion.PotionEffect;

import java.util.List;
import java.util.Map;

@Getter
public class WithdrawalStageData {
    private final Map<String, Double> conditions;

    private final long minWithdrawalDelay;
    private final long maxWithdrawalDelay;

    private final List<Component> thoughts;
    private final List<PotionEffect> effects;

    public WithdrawalStageData(Map<String, Double> conditions, long minWithdrawalDelay, long maxWithdrawalDelay,
                               List<Component> thoughts, List<PotionEffect> effects) {
        this.conditions = conditions;
        this.minWithdrawalDelay = minWithdrawalDelay;
        this.maxWithdrawalDelay = maxWithdrawalDelay;
        this.thoughts = thoughts;
        this.effects = effects;
    }
}
