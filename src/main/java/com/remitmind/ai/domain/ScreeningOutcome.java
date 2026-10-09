package com.remitmind.ai.domain;

/**
 * Result of sanctions screening. Declared in order of severity, worst last,
 * so the worst of several outcomes can be picked with compareTo.
 */
public enum ScreeningOutcome {
    CLEAR,
    UNAVAILABLE,
    HIT
}
