package org.p2gx.maxodiff.core.service;

@FunctionalInterface
public interface ProgressListener {
    /** Does nothing: the default for callers that don't care about progress. */
    ProgressListener NONE = (completed, total) -> { };

    /** Called each time the evaluation of one MAxO term has finished. May be called from several threads. */
    void onTaskCompleted(int completed, int total);
}