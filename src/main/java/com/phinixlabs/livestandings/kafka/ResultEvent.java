package com.phinixlabs.livestandings.kafka;

import java.time.Instant;

/**
 * Payload published to {@code match-results}. Key must be {@code leagueId}.
 */
public record ResultEvent(
        String matchId,
        String leagueId,
        String homeTeam,
        String awayTeam,
        int homeScore,
        int awayScore,
        Instant reportedAt,
        Instant submittedAt,
        boolean correction
) {
}
