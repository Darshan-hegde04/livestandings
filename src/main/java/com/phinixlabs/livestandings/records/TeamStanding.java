package com.phinixlabs.livestandings.records;

/** Ranked team row produced by {@link com.phinixlabs.livestandings.service.StandingsCalculator}. */
public record TeamStanding(
        String team,
        int played,
        int won,
        int drawn,
        int lost,
        int goalsFor,
        int goalsAgainst,
        int goalDiff,
        int points,
        int position
) {
}
