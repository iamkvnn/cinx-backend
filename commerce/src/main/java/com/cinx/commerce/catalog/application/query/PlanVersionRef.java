package com.cinx.commerce.catalog.application.query;

/** One version of a plan, with the plan's code. */
public record PlanVersionRef(String id, String planId, String planCode, long versionNumber) {
}
