package com.phinixlabs.livestandings.entity;

import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "standings")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Standing {

    @EmbeddedId
    private StandingId id;

    @Column(name = "played", nullable = false)
    private int played;

    @Column(name = "won", nullable = false)
    private int won;

    @Column(name = "drawn", nullable = false)
    private int drawn;

    @Column(name = "lost", nullable = false)
    private int lost;

    @Column(name = "goals_for", nullable = false)
    private int goalsFor;

    @Column(name = "goals_against", nullable = false)
    private int goalsAgainst;

    @Column(name = "goal_diff", nullable = false)
    private int goalDiff;

    @Column(name = "points", nullable = false)
    private int points;

    @Column(name = "position", nullable = false)
    private int position;
}
