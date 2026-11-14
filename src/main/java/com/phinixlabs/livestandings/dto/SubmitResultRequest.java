package com.phinixlabs.livestandings.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import java.time.Instant;

public record SubmitResultRequest(
        @NotBlank String matchId,
        @NotBlank String homeTeam,
        @NotBlank String awayTeam,
        @Min(0) int homeScore,
        @Min(0) int awayScore,
        Instant reportedAt
) {
}
