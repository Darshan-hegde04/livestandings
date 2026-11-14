package com.phinixlabs.livestandings.dto;

import java.time.Instant;
import java.util.List;

public record StandingsSnapshotDto(
        String leagueId,
        long version,
        Instant updatedAt,
        List<StandingRowDto> rows
) {
}
