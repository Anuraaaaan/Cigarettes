package com.anuraaaan.cigarettes.nicotine;

import lombok.Getter;
import lombok.Setter;


@Setter
@Getter
public class NicotineData {

    private double nicotine;
    private double tolerance;
    private double addiction;

    public NicotineData(double nicotine, double tolerance, double addiction) {
        this.nicotine = nicotine;
        this.tolerance = tolerance;
        this.addiction = addiction;
    }

}
