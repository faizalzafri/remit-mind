package com.remitmind.ai.domain;

/**
 * The model's contribution to a parsed transfer: conversational text plus the
 * rationale for a compliance decision Java already computed.
 */
public record CopilotReply(String chatResponse, String rationale) {}
