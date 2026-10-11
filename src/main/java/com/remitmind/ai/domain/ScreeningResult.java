package com.remitmind.ai.domain;

/**
 * Sanctions screening result for one or more names. matchedEntity is the
 * matched list entry's name on a HIT, null otherwise.
 */
public record ScreeningResult(
        ScreeningOutcome screeningOutcome,
        String matchedEntity
) {
    public static final ScreeningResult CLEAR = new ScreeningResult(ScreeningOutcome.CLEAR, null);
}
