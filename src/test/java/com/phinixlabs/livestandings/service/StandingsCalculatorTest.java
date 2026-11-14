package com.phinixlabs.livestandings.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.phinixlabs.livestandings.records.MatchScore;
import com.phinixlabs.livestandings.records.TeamStanding;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;

class StandingsCalculatorTest {

    @Test
    void awardsPointsForWinDrawLoss() {
        List<TeamStanding> table = StandingsCalculator.calculate(List.of(
                new MatchScore("Alpha", "Beta", 2, 0),
                new MatchScore("Gamma", "Alpha", 1, 1),
                new MatchScore("Beta", "Gamma", 0, 3)
        ));

        Map<String, TeamStanding> byTeam = index(table);

        TeamStanding alpha = byTeam.get("Alpha");
        assertEquals(1, alpha.won());
        assertEquals(1, alpha.drawn());
        assertEquals(0, alpha.lost());
        assertEquals(4, alpha.points());
        assertEquals(3, alpha.goalsFor());
        assertEquals(1, alpha.goalsAgainst());
        assertEquals(2, alpha.goalDiff());

        TeamStanding beta = byTeam.get("Beta");
        assertEquals(0, beta.won());
        assertEquals(0, beta.drawn());
        assertEquals(2, beta.lost());
        assertEquals(0, beta.points());

        TeamStanding gamma = byTeam.get("Gamma");
        assertEquals(1, gamma.won());
        assertEquals(1, gamma.drawn());
        assertEquals(0, gamma.lost());
        assertEquals(4, gamma.points());
    }

    @Test
    void ranksByPointsThenGoalDiffThenGoalsForThenName() {
        // Alpha and Beta both 3 points; Alpha better GD
        // Gamma and Delta both 1 point, same GD and GF → name order
        List<TeamStanding> table = StandingsCalculator.calculate(List.of(
                new MatchScore("Alpha", "Zeta", 3, 0),
                new MatchScore("Beta", "Zeta", 1, 0),
                new MatchScore("Gamma", "Delta", 1, 1)
        ));

        assertEquals(List.of("Alpha", "Beta", "Delta", "Gamma", "Zeta"),
                table.stream().map(TeamStanding::team).toList());
        assertEquals(List.of(1, 2, 3, 4, 5),
                table.stream().map(TeamStanding::position).toList());
    }

    @Test
    void correctionReplacingScoreChangesTable() {
        List<MatchScore> original = List.of(
                new MatchScore("Alpha", "Beta", 0, 1)
        );
        List<TeamStanding> before = StandingsCalculator.calculate(original);
        assertEquals("Beta", before.getFirst().team());
        assertEquals(3, before.getFirst().points());

        // Correction: Alpha actually won 2-1 (recompute from latest row per match)
        List<TeamStanding> after = StandingsCalculator.calculate(List.of(
                new MatchScore("Alpha", "Beta", 2, 1)
        ));
        assertEquals("Alpha", after.getFirst().team());
        assertEquals(3, after.getFirst().points());
        assertEquals(0, after.get(1).points());
        assertEquals("Beta", after.get(1).team());
    }

    @Test
    void rejectsIdenticalTeams() {
        assertThrows(IllegalArgumentException.class, () ->
                StandingsCalculator.calculate(List.of(new MatchScore("Alpha", "Alpha", 1, 0)))
        );
    }

    private static Map<String, TeamStanding> index(List<TeamStanding> table) {
        return table.stream().collect(Collectors.toMap(TeamStanding::team, Function.identity()));
    }
}
