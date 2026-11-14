package com.phinixlabs.livestandings.service;

import com.phinixlabs.livestandings.records.MatchScore;
import com.phinixlabs.livestandings.records.TeamStanding;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Pure standings fold. Points: win 3, draw 1, loss 0.
 * Rank: points → goal difference → goals for → team name.
 */
public final class StandingsCalculator {

    public static final int POINTS_WIN = 3;
    public static final int POINTS_DRAW = 1;
    public static final int POINTS_LOSS = 0;

    private StandingsCalculator() {
    }

    public static List<TeamStanding> calculate(Collection<MatchScore> matches) {
        Map<String, MutableStats> byTeam = new HashMap<>();
        for (MatchScore match : matches) {
            applyMatch(byTeam, match);
        }
        return rank(byTeam);
    }

    private static void applyMatch(Map<String, MutableStats> byTeam, MatchScore match) {
        if (match.homeTeam() == null || match.awayTeam() == null) {
            throw new IllegalArgumentException("Teams must be non-null");
        }
        if (match.homeTeam().equals(match.awayTeam())) {
            throw new IllegalArgumentException("Home and away teams must differ");
        }

        MutableStats home = byTeam.computeIfAbsent(match.homeTeam(), t -> new MutableStats());
        MutableStats away = byTeam.computeIfAbsent(match.awayTeam(), t -> new MutableStats());

        home.played++;
        away.played++;
        home.goalsFor += match.homeScore();
        home.goalsAgainst += match.awayScore();
        away.goalsFor += match.awayScore();
        away.goalsAgainst += match.homeScore();

        if (match.homeScore() > match.awayScore()) {
            home.won++;
            home.points += POINTS_WIN;
            away.lost++;
            away.points += POINTS_LOSS;
        } else if (match.homeScore() < match.awayScore()) {
            away.won++;
            away.points += POINTS_WIN;
            home.lost++;
            home.points += POINTS_LOSS;
        } else {
            home.drawn++;
            away.drawn++;
            home.points += POINTS_DRAW;
            away.points += POINTS_DRAW;
        }
    }

    private static List<TeamStanding> rank(Map<String, MutableStats> byTeam) {
        List<TeamStanding> ranked = new ArrayList<>(byTeam.size());
        byTeam.forEach((team, stats) -> ranked.add(stats.toStanding(team, 0)));

        ranked.sort(Comparator
                .comparingInt(TeamStanding::points).reversed()
                .thenComparing(Comparator.comparingInt(TeamStanding::goalDiff).reversed())
                .thenComparing(Comparator.comparingInt(TeamStanding::goalsFor).reversed())
                .thenComparing(TeamStanding::team));

        List<TeamStanding> withPosition = new ArrayList<>(ranked.size());
        for (int i = 0; i < ranked.size(); i++) {
            TeamStanding row = ranked.get(i);
            withPosition.add(new TeamStanding(
                    row.team(),
                    row.played(),
                    row.won(),
                    row.drawn(),
                    row.lost(),
                    row.goalsFor(),
                    row.goalsAgainst(),
                    row.goalDiff(),
                    row.points(),
                    i + 1
            ));
        }
        return withPosition;
    }

    private static final class MutableStats {
        private int played;
        private int won;
        private int drawn;
        private int lost;
        private int goalsFor;
        private int goalsAgainst;
        private int points;

        private TeamStanding toStanding(String team, int position) {
            return new TeamStanding(
                    team,
                    played,
                    won,
                    drawn,
                    lost,
                    goalsFor,
                    goalsAgainst,
                    goalsFor - goalsAgainst,
                    points,
                    position
            );
        }
    }
}
