package com.phinixlabs.livestandings.service;

import com.phinixlabs.livestandings.dto.StandingsSnapshotDto;

/**
 * Captain read path: full standings snapshot from MySQL.
 */
public interface StandingsService {

    StandingsSnapshotDto getSnapshot(String leagueId);
}
