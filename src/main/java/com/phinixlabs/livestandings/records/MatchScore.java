package com.phinixlabs.livestandings.records;

/** Input to the standings fold — one match scoreline. */
public record MatchScore(String homeTeam, String awayTeam, int homeScore, int awayScore) {
}
