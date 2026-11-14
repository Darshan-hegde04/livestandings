package com.phinixlabs.livestandings.kafka;

import java.time.Instant;

/** Fan-out signal after standings commit. Key = leagueId. */
public record StandingsUpdatedEvent(
        String leagueId,
        long version,
        Instant updatedAt
) {
}
