package com.remo.realestatemaintainceoptimizer.ai;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * A candidate the AI selected, with its short justification for the user.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record AdvisorSelection(String candidateId, String reason) {
}
