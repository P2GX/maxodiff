package org.p2gx.maxodiff.core.mica;

public class ProgressBar {

    private static final int BAR_WIDTH = 30;

    private final String label;
    private final int total;
    private int current = 0;

    public ProgressBar(String label, int total) {
        this.label = label;
        this.total = total;
    }

    public void step() {
        update(current + 1);
    }

    public void update(int current) {
        this.current = Math.min(current, total);
        render();
    }

    public void finish() {
        current = total;
        render();
        System.err.println();
    }

    private void render() {
        double fraction = (double) current / total;
        int completed = (int) (fraction * BAR_WIDTH);

        String bar =
                "█".repeat(completed) +
                "░".repeat(BAR_WIDTH - completed);

        System.err.printf(
                "\r%s: [%s] %5.1f%% (%d/%d)",
                label,
                bar,
                fraction * 100,
                current,
                total);
    }
}
