package com.cinx.reporting.analytics.application.query;

/**
 * @param accepted   stored now
 * @param duplicates sent before (same event id), not stored again
 * @param dropped    not stored: consent off, too old, or an exposure that does not match a running experiment
 */
public record IngestResult(int accepted, int duplicates, int dropped) {
}
