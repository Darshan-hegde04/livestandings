package com.phinixlabs.livestandings.service.impl;

import com.phinixlabs.livestandings.common.exception.ValidationException;
import com.phinixlabs.livestandings.dto.StandingRowDto;
import com.phinixlabs.livestandings.dto.StandingsSnapshotDto;
import com.phinixlabs.livestandings.entity.LeagueState;
import com.phinixlabs.livestandings.entity.Standing;
import com.phinixlabs.livestandings.repository.LeagueStateRepository;
import com.phinixlabs.livestandings.repository.StandingRepository;
import com.phinixlabs.livestandings.service.StandingsService;
import java.time.Instant;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class StandingsServiceImpl implements StandingsService {

    private final StandingRepository standingRepository;
    private final LeagueStateRepository leagueStateRepository;

    @Override
    @Transactional(readOnly = true)
    public StandingsSnapshotDto getSnapshot(String leagueId) {
        requireLeagueId(leagueId);
        String id = leagueId.trim();

        LeagueState state = leagueStateRepository.findById(id).orElse(null);
        List<StandingRowDto> rows = standingRepository.findByIdLeagueIdOrderByPositionAsc(id).stream()
                .map(this::toDto)
                .toList();

        if (state == null) {
            return new StandingsSnapshotDto(id, 0L, Instant.EPOCH, rows);
        }
        return new StandingsSnapshotDto(id, state.getVersion(), state.getUpdatedAt(), rows);
    }

    private StandingRowDto toDto(Standing standing) {
        return new StandingRowDto(
                standing.getPosition(),
                standing.getId().getTeam(),
                standing.getPlayed(),
                standing.getWon(),
                standing.getDrawn(),
                standing.getLost(),
                standing.getGoalsFor(),
                standing.getGoalsAgainst(),
                standing.getGoalDiff(),
                standing.getPoints()
        );
    }

    private static void requireLeagueId(String leagueId) {
        if (!StringUtils.hasText(leagueId)) {
            throw new ValidationException("leagueId is required");
        }
    }
}
