package com.phinixlabs.livestandings.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "match_results")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MatchResult {

    @Id
    @Column(name = "match_id", nullable = false, length = 64)
    private String matchId;

    @Column(name = "league_id", nullable = false, length = 64)
    private String leagueId;

    @Column(name = "home_team", nullable = false, length = 128)
    private String homeTeam;

    @Column(name = "away_team", nullable = false, length = 128)
    private String awayTeam;

    @Column(name = "home_score", nullable = false)
    private int homeScore;

    @Column(name = "away_score", nullable = false)
    private int awayScore;

    @Column(name = "reported_at")
    private Instant reportedAt;

    @Column(name = "submitted_at", nullable = false)
    private Instant submittedAt;

    @Column(name = "source_partition", nullable = false)
    private int sourcePartition;

    @Column(name = "source_offset", nullable = false)
    private long sourceOffset;

    @Column(name = "applied_at", nullable = false)
    private Instant appliedAt;
}
