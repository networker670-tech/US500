package cz.leo.us500screenbot;

import java.math.BigDecimal;
import java.math.RoundingMode;

public final class TradeCalculator {
    private TradeCalculator() {}

    public static final class Result {
        public final BigDecimal offset;
        public final BigDecimal high;
        public final BigDecimal low;
        public final BigDecimal midpoint;

        Result(BigDecimal offset, BigDecimal high, BigDecimal low, BigDecimal midpoint) {
            this.offset = offset;
            this.high = high;
            this.low = low;
            this.midpoint = midpoint;
        }
    }

    public static Result calculate(BigDecimal mesZoneHigh, BigDecimal mesZoneLow,
                                   BigDecimal mesClose, BigDecimal us500Close) {
        if (mesZoneHigh == null || mesZoneLow == null || mesClose == null || us500Close == null) {
            throw new IllegalArgumentException("Chybí vstupní hodnota.");
        }
        if (mesZoneHigh.compareTo(mesZoneLow) <= 0) {
            throw new IllegalArgumentException("Horní hranice musí být vyšší než dolní.");
        }
        BigDecimal offset = us500Close.subtract(mesClose).setScale(2, RoundingMode.HALF_UP);
        BigDecimal high = mesZoneHigh.add(offset).setScale(2, RoundingMode.HALF_UP);
        BigDecimal low = mesZoneLow.add(offset).setScale(2, RoundingMode.HALF_UP);
        BigDecimal midpoint = high.add(low).divide(new BigDecimal("2"), 2, RoundingMode.HALF_UP);
        return new Result(offset, high, low, midpoint);
    }
}
