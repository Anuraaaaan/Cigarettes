package com.anuraaaan.cigarettes.managers;

import com.anuraaaan.cigarettes.nicotine.NicotineData;
import lombok.Getter;

import java.util.HashMap;
import java.util.UUID;

public class NicotineManager {

    @Getter
    private final HashMap<UUID, NicotineData> nicotineDataMap = new HashMap<>();

    public NicotineData getNicotineData(UUID uuid) {
        return nicotineDataMap.get(uuid);
    }

    public void initializeNicotineData(UUID uuid) {
        nicotineDataMap.put(uuid, new NicotineData(0, 0, 0));
    }

    public void addNicotineData(UUID uuid, double nicotine, double tolerance, double addiction) {
        NicotineData nicotineData = nicotineDataMap.get(uuid);
        nicotineData.setNicotine(Math.clamp(nicotine + nicotineData.getNicotine(), 0, 100));
        nicotineData.setTolerance(Math.clamp(tolerance + nicotineData.getTolerance(), 0, 100));
        nicotineData.setAddiction(Math.clamp(addiction + nicotineData.getAddiction(), 0, 100));
        nicotineDataMap.replace(uuid, nicotineData);
    }



    public boolean hasNicotineData(UUID uuid) {
        return nicotineDataMap.containsKey(uuid);
    }

    public void subtractNicotineData(UUID uuid, double nicotineReductionRate, double toleranceReductionRate, double addictionReductionRate) {
        NicotineData nicotineData = nicotineDataMap.get(uuid);

        nicotineData.setNicotine(Math.clamp(nicotineData.getNicotine() - nicotineReductionRate, 0, 100));
        nicotineData.setTolerance(Math.clamp(nicotineData.getTolerance() - toleranceReductionRate, 0, 100));
        nicotineData.setAddiction(Math.clamp(nicotineData.getAddiction() - addictionReductionRate, 0, 100));
        nicotineDataMap.replace(uuid, nicotineData);
    }
}
