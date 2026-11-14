package com.phinixlabs.livestandings.dto;

public record StandingRowDto(
        int position,
        String team,
        int played,
        int won,
        int drawn,
        int lost,
        int goalsFor,
        int goalsAgainst,
        int goalDiff,
        int points
) {
}
