package com.anuraaaan.cigarettes.nicotine;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class WithdrawalData {

    private long lastSmokeTime;
    private long nextWithdrawalDelay;
    private long nextWithdrawalTime;
    private int withdrawalLevel;
    private long remainingTimeToWithdrawal;

    public WithdrawalData(long lastSmokeTime, long nextWithdrawalDelay, long nextWithdrawalTime, int withdrawalLevel, long remainingTimeToWithdrawal) {
        this.lastSmokeTime = lastSmokeTime;
        this.nextWithdrawalDelay = nextWithdrawalDelay;
        this.nextWithdrawalTime = nextWithdrawalTime;
        this.withdrawalLevel = withdrawalLevel;
        this.remainingTimeToWithdrawal = remainingTimeToWithdrawal;
    }
}
