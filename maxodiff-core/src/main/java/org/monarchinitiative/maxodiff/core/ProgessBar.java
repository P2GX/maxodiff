package org.monarchinitiative.maxodiff.core;

public class ProgessBar {

    /** smallest value */
    private long min;
    /** largest value */
    private long max;
    /** whether or not to print */
    private boolean doPrint;

    /** Initialize progress bar with the given settings */
    public ProgessBar(long min, long max) {
        ProgressBar(min, max, true);
    }

    /** Initialize progress bar with the given settings */
    void ProgressBar(long min, long max, boolean doPrint) {
        this.min = min;
        this.max = max;
        this.doPrint = doPrint;
    }

    /** @return smallest value to represent */
    long getMin() {
        return min;
    }

    /** @return largest value to represent */
    long getMax() {
        return max;
    }

    /** @return <code>true</code> if the progress bar has printing enabled */
    public boolean doPrint() {
        return doPrint;
    }

    /** print progress up to position <code>pos</code>, if {@link #doPrint} */
    public void print(long pos) {
        if (!doPrint)
            return;
        int percent = (int) Math.ceil(100.0 * (pos - this.min) / (this.max - this.min));
        StringBuilder bar = new StringBuilder("[");

        for (int i = 0; i < 50; i++) {
            if (i < (percent / 2)) {
                bar.append("=");
            } else if (i == (percent / 2)) {
                bar.append(">");
            } else {
                bar.append(" ");
            }
        }

        bar.append("]   ").append(percent).append("%     ");
        System.err.print("\r" + bar);
        if (pos == max)
            System.err.println();
    }

}
