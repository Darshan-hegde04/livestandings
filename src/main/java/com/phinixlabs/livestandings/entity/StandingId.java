package com.phinixlabs.livestandings.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.io.Serializable;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Embeddable
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class StandingId implements Serializable {

    @Column(name = "league_id", nullable = false, length = 64)
    private String leagueId;

    @Column(name = "team", nullable = false, length = 128)
    private String team;
}
