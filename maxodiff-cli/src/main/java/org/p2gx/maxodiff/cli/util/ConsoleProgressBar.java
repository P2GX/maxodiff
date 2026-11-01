package org.p2gx.maxodiff.cli.util;

import org.p2gx.maxodiff.core.service.ProgressListener;

import java.io.PrintStream;

/** Draws a text progress bar on a console as MAxO terms are evaluated. */
public final class ConsoleProgressBar implements ProgressListener {

    private static final int WIDTH = 50;

    private final String label;
    private final PrintStream out;
    private int lastCompleted = 0;

    public ConsoleProgressBar() {
        this("Running maxodiff calculation", System.out);
    }

    public ConsoleProgressBar(String label, PrintStream out) {
        this.label = label;
        this.out = out;
    }

    /** Synchronized because tasks finish on several threads at the same time. */
    @Override
    public synchronized void onTaskCompleted(int completed, int total) {
        if (total <= 0 || completed <= lastCompleted) {
            return; // nothing to show, or a report that arrived late and is already out of date
        }
        lastCompleted = completed;
        int percent = (int) (100L * completed / total);
        int filled = percent * WIDTH / 100;
        StringBuilder bar = new StringBuilder(label).append(": [");
        for (int i = 0; i < WIDTH; i++) {
            bar.append(i < filled ? '=' : (i == filled ? '>' : ' '));
        }
        bar.append("] ").append(percent).append('%');
        out.print("\r" + bar);
        if (completed >= total) {
            out.println();
        }
    }
}