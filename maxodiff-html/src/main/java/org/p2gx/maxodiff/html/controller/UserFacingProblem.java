package org.p2gx.maxodiff.html.controller;

/**
 * A problem described for the person using the page, not for a developer.
 *
 * @param title       short statement of what went wrong
 * @param explanation why it happened, in plain words
 * @param action      what the person can do next
 */
public record UserFacingProblem(String title, String explanation, String action) {
}