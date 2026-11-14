package com.phinixlabs.livestandings.service;

import com.phinixlabs.livestandings.common.PageResponse;
import com.phinixlabs.livestandings.dto.StandingRowDto;
import com.phinixlabs.livestandings.dto.StandingsSnapshotDto;
import org.springframework.data.domain.Pageable;

/**
 * Captain read path: standings snapshot and paginated list from MySQL.
 */
public interface StandingsService {

    StandingsSnapshotDto getSnapshot(String leagueId);

    PageResponse<StandingRowDto> list(String leagueId, Pageable pageable);
}
