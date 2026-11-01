package org.p2gx.maxodiff.html.results.maxoHpo;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class RankScale {

    private static final int MAX_TICKS_PER_SIDE = 4;
    private static final int[] STEPS = {1, 2, 5, 10, 20, 50, 100};

    private RankScale() {
    }

    /** Tick marks for the axis: 0 in the middle, negative = moves up the list, positive = moves down. */
    public static List<ScaleTick> ticks(int nDiseases) {
        int largest = Math.max(1, nDiseases - 1);          // largest possible change in rank positions
        int step = stepFor(largest);
        int outermost = (largest / step) * step;
        List<ScaleTick> ticks = new ArrayList<>();
        for (int value = -outermost; value <= outermost; value += step) {
            double left = 50.0 + 50.0 * value / largest;
            ticks.add(new ScaleTick(label(value), String.format(Locale.ROOT, "left:%.1f%%", left)));
        }
        return ticks;
    }

    private static int stepFor(int largest) {
        for (int step : STEPS) {
            if (largest / step <= MAX_TICKS_PER_SIDE) {
                return step;
            }
        }
        return STEPS[STEPS.length - 1];
    }

    private static String label(int value) {
        return value == 0 ? "0" : (value > 0 ? "+" : "−") + Math.abs(value);   // U+2212 is a real minus sign
    }
}
