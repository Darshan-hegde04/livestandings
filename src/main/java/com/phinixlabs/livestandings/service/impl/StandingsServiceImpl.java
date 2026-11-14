package com.phinixlabs.livestandings.service.impl;

import com.phinixlabs.livestandings.common.PageResponse;
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
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class StandingsServiceImpl implements StandingsService {

    private static final Set<String> ALLOWED_SORTS = Set.of(
            "position", "points", "team", "goalDiff", "goalsFor", "played"
    );

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

    @Override
    @Transactional(readOnly = true)
    public PageResponse<StandingRowDto> list(String leagueId, Pageable pageable) {
        requireLeagueId(leagueId);
        Pageable safe = sanitize(pageable);
        Page<StandingRowDto> page = standingRepository
                .findByIdLeagueId(leagueId.trim(), safe)
                .map(this::toDto);
        return PageResponse.from(page);
    }

    private Pageable sanitize(Pageable pageable) {
        int size = Math.min(Math.max(pageable.getPageSize(), 1), 100);
        int page = Math.max(pageable.getPageNumber(), 0);

        Sort sort = pageable.getSort();
        if (sort.isUnsorted()) {
            return PageRequest.of(page, size, Sort.by(Sort.Direction.ASC, "position"));
        }

        List<Sort.Order> orders = sort.stream()
                .filter(order -> ALLOWED_SORTS.contains(order.getProperty()))
                .map(order -> {
                    String property = order.getProperty();
                    if ("team".equals(property)) {
                        return new Sort.Order(order.getDirection(), "id.team");
                    }
                    return order;
                })
                .toList();

        if (orders.isEmpty()) {
            throw new ValidationException("sort field not allowed");
        }
        return PageRequest.of(page, size, Sort.by(orders));
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
