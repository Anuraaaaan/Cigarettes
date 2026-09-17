package com.anuraaaan.cigarettes.managers;

import com.anuraaaan.cigarettes.Cigarettes;
import com.anuraaaan.cigarettes.nicotine.NicotineData;
import com.anuraaaan.cigarettes.nicotine.WithdrawalData;
import com.anuraaaan.cigarettes.nicotine.WithdrawalStageData;
import com.anuraaaan.cigarettes.registry.ConfigRegistry;
import lombok.Getter;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

public class NicotineWithdrawalManager extends BukkitRunnable {

    private final Cigarettes plugin;
    private final ConfigRegistry configRegistry;
    private final NicotineManager nicotineManager;

    @Getter
    private final Map<UUID, WithdrawalData> withdrawalData = new HashMap<> ();


    public NicotineWithdrawalManager(Cigarettes plugin, ConfigRegistry configRegistry, NicotineManager nicotineManager) {
        this.plugin = plugin;
        this.configRegistry = configRegistry;
        this.nicotineManager = nicotineManager;
    }

    @Override
    public void run() {
        for (Player player : plugin.getServer().getOnlinePlayers()) {
            UUID uuid = player.getUniqueId();
            WithdrawalData withdrawalDataPlayer = withdrawalData.get(uuid);
            if (withdrawalDataPlayer == null) {
                setWithdrawalData(uuid);
                withdrawalDataPlayer = withdrawalData.get(uuid);
            }

            NicotineData nicotineData = nicotineManager.getNicotineData(uuid);

            int oldLevel = withdrawalDataPlayer.getWithdrawalLevel();
            int newLevel = updateWithdrawalLevel(nicotineData);

            if (newLevel == 0) {
                if (oldLevel != 0) {
                    withdrawalDataPlayer.setWithdrawalLevel(0);
                    withdrawalDataPlayer.setNextWithdrawalDelay(0);
                    withdrawalDataPlayer.setNextWithdrawalTime(0);
                    withdrawalDataPlayer.setLastSmokeTime(System.currentTimeMillis());
                }
                continue;
            }
            if (oldLevel != newLevel) {
                long delay = randomNextWithdrawalDelay(newLevel);
                long now = System.currentTimeMillis();

                withdrawalDataPlayer.setWithdrawalLevel(newLevel);
                withdrawalDataPlayer.setNextWithdrawalDelay(delay);
                withdrawalDataPlayer.setNextWithdrawalTime(now + delay);
                withdrawalDataPlayer.setLastSmokeTime(now);
            }

            long currentTime = System.currentTimeMillis();
            long nextWithdrawalTime = withdrawalDataPlayer.getNextWithdrawalTime();
            if (currentTime < nextWithdrawalTime) continue;

            WithdrawalStageData stage = configRegistry.getWithdrawalStages().get(newLevel);

            List<String> thoughts = stage.getThoughts();

            if (!thoughts.isEmpty()) {

                String thought = thoughts.get(ThreadLocalRandom.current().nextInt(thoughts.size()));

                player.sendActionBar(thought);
            }

            for (PotionEffect effect : stage.getEffects()) {

                player.addPotionEffect(effect);
            }

            long delay = randomNextWithdrawalDelay(newLevel);

            withdrawalDataPlayer.setNextWithdrawalDelay(delay);
            withdrawalDataPlayer.setNextWithdrawalTime(currentTime + delay);

        }
    }

    public void setWithdrawalData(UUID uuid) {
        if (withdrawalData.containsKey(uuid)) {
            WithdrawalData withdrawalDataPlayer = withdrawalData.get(uuid);
            updateWithdrawalData(uuid, withdrawalDataPlayer);
            return;
        }
        createWithdrawalData(uuid);
    }

    private void updateWithdrawalData(UUID uuid, WithdrawalData withdrawalDataPlayer) {
        long updateLastSmokeTime = System.currentTimeMillis();
        NicotineData nicotineData = nicotineManager.getNicotineData(uuid);

        int level = updateWithdrawalLevel(nicotineData);

        if (level == 0) {
            long updateNextWithdrawalDelay = 0;
            long updateNextWithdrawalTime = 0;
            withdrawalData.put(uuid, new WithdrawalData(updateLastSmokeTime, updateNextWithdrawalDelay,
                    updateNextWithdrawalTime, level, 0));
            return;
        }

        if (withdrawalDataPlayer.getWithdrawalLevel() == level) {

            long updateNextWithdrawalTime = updateLastSmokeTime + withdrawalDataPlayer.getNextWithdrawalDelay();
            withdrawalDataPlayer.setNextWithdrawalTime(updateNextWithdrawalTime);
            withdrawalDataPlayer.setLastSmokeTime(updateLastSmokeTime);
            withdrawalData.put(uuid, withdrawalDataPlayer);
            return;
        }
        long updateNextWithdrawalDelay = randomNextWithdrawalDelay(level);
        long updateNextWithdrawalTime = updateLastSmokeTime + updateNextWithdrawalDelay;

        withdrawalDataPlayer.setWithdrawalLevel(level);
        withdrawalDataPlayer.setLastSmokeTime(updateLastSmokeTime);
        withdrawalDataPlayer.setNextWithdrawalDelay(updateNextWithdrawalDelay);
        withdrawalDataPlayer.setNextWithdrawalTime(updateNextWithdrawalTime);
        withdrawalData.put(uuid, withdrawalDataPlayer);
    }

    private void createWithdrawalData(UUID uuid) {
        long lastSmokeTime = System.currentTimeMillis();
        NicotineData nicotineData = nicotineManager.getNicotineData(uuid);
        int level = updateWithdrawalLevel(nicotineData);

        if (level == 0) {
            long nextWithdrawalDelay = 0;
            long nextWithdrawalTime = 0;
            withdrawalData.put(uuid, new WithdrawalData(lastSmokeTime, nextWithdrawalDelay,
                    nextWithdrawalTime, level, 0));
            return;
        }

        long nextWithdrawalDelay = randomNextWithdrawalDelay(level);
        long nextWithdrawalTime = lastSmokeTime + nextWithdrawalDelay;

        withdrawalData.put(uuid, new WithdrawalData(lastSmokeTime, nextWithdrawalDelay, nextWithdrawalTime, level, 0));
    }

    private long randomNextWithdrawalDelay(int level) {
        WithdrawalStageData withdrawalStageData = configRegistry.getWithdrawalStages().get(level);

        long minWithdrawalDelay = withdrawalStageData.getMinWithdrawalDelay();
        long maxWithdrawalDelay = withdrawalStageData.getMaxWithdrawalDelay();

        if (minWithdrawalDelay == maxWithdrawalDelay) {
            return minWithdrawalDelay * 1000;
        }

        return ThreadLocalRandom.current().nextLong(minWithdrawalDelay, maxWithdrawalDelay + 1) * 1000;
    }

    private int updateWithdrawalLevel(NicotineData nicotineData) {

        Map<Integer, WithdrawalStageData> withdrawalStages = configRegistry.getWithdrawalStages();

        int level = 0;
        for (int withdrawalStage : withdrawalStages.keySet()) {

            Double minAddiction = withdrawalStages.get(withdrawalStage).getConditions().get("min_addiction");
            if (minAddiction != null && minAddiction > nicotineData.getAddiction()) {
                continue;
            }

            Double maxAddiction = withdrawalStages.get(withdrawalStage).getConditions().get("max_addiction");
            if (maxAddiction != null && maxAddiction < nicotineData.getAddiction()) {
                continue;
            }

            Double minTolerance = withdrawalStages.get(withdrawalStage).getConditions().get("min_tolerance");
            if (minTolerance != null && minTolerance > nicotineData.getTolerance()) {
                continue;
            }

            Double maxTolerance = withdrawalStages.get(withdrawalStage).getConditions().get("max_tolerance");
            if (maxTolerance != null && maxTolerance < nicotineData.getTolerance()) {
                continue;
            }

            Double minNicotine = withdrawalStages.get(withdrawalStage).getConditions().get("min_nicotine");
            if (minNicotine != null && minNicotine > nicotineData.getNicotine()) {
                continue;
            }

            Double maxNicotine = withdrawalStages.get(withdrawalStage).getConditions().get("max_nicotine");
            if (maxNicotine != null && maxNicotine < nicotineData.getNicotine()) {
                continue;
            }
            level = withdrawalStage;
        }
        return level;
    }

    public boolean hasWithdrawalData(UUID uuid) {
        return withdrawalData.containsKey(uuid);
    }

    public void saveRemainingWithdrawalTime(UUID uuid) {

        WithdrawalData withdrawalDataPlayer = withdrawalData.get(uuid);

        long nextWithdrawalTime = withdrawalDataPlayer.getNextWithdrawalTime();
        long remainingTimeToWithdrawal = Math.max(0, nextWithdrawalTime - System.currentTimeMillis());

        nextWithdrawalTime = 0;

        withdrawalDataPlayer.setLastSmokeTime(0);
        withdrawalDataPlayer.setNextWithdrawalTime(nextWithdrawalTime);
        withdrawalDataPlayer.setRemainingTimeToWithdrawal(remainingTimeToWithdrawal);
        withdrawalData.put(uuid, withdrawalDataPlayer);
    }

    public void restoreWithdrawalTimer(UUID uuid) {

        WithdrawalData withdrawalDataPlayer = withdrawalData.get(uuid);

        long remainingTimeToWithdrawal = withdrawalDataPlayer.getRemainingTimeToWithdrawal();
        long lastSmokeTime = System.currentTimeMillis();
        long nextWithdrawalTime = lastSmokeTime + remainingTimeToWithdrawal;

        remainingTimeToWithdrawal = 0;

        withdrawalDataPlayer.setLastSmokeTime(lastSmokeTime);
        withdrawalDataPlayer.setNextWithdrawalTime(nextWithdrawalTime);
        withdrawalDataPlayer.setRemainingTimeToWithdrawal(remainingTimeToWithdrawal);
        withdrawalData.put(uuid, withdrawalDataPlayer);
    }
}
