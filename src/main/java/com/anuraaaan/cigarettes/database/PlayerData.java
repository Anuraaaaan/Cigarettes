package com.anuraaaan.cigarettes.database;

import com.anuraaaan.cigarettes.nicotine.NicotineData;
import com.anuraaaan.cigarettes.nicotine.WithdrawalData;

import java.util.UUID;

public record PlayerData(
        UUID uuid,
        NicotineData nicotineData,
        WithdrawalData withdrawalData
) {}
