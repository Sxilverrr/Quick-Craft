package com.sxilverr.quickcraft.craft;

import java.math.BigInteger;

public final class EmcFormat {
    private static final String[] SUFFIXES = {"", "K", "M", "B", "T", "Q", "Qi", "Sx", "Sp", "Oc", "No", "Dc"};
    private static final BigInteger THOUSAND = BigInteger.valueOf(1000);

    private EmcFormat() {
    }

    public static String format(BigInteger value) {
        if (value == null || value.signum() <= 0) return "0";
        int mag = 0;
        BigInteger n = value;
        while (n.compareTo(THOUSAND) >= 0 && mag < SUFFIXES.length - 1) {
            n = n.divide(THOUSAND);
            mag++;
        }
        if (mag == 0) return value.toString();
        BigInteger scale = BigInteger.TEN.pow(mag * 3);
        BigInteger tenths = value.multiply(BigInteger.TEN).divide(scale);
        long whole = tenths.longValue() / 10;
        long frac = tenths.longValue() % 10;
        return frac == 0 ? whole + SUFFIXES[mag] : whole + "." + frac + SUFFIXES[mag];
    }
}
