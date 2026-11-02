package org.p2gx.maxodiff.html.controller;

import java.util.List;

/** The body of an error response of the JSON API. */
public record ApiProblems(List<UserFacingProblem> problems) {
}