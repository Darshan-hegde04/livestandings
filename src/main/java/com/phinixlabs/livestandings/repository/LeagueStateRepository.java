package com.phinixlabs.livestandings.repository;

import com.phinixlabs.livestandings.entity.LeagueState;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface LeagueStateRepository extends JpaRepository<LeagueState, String> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select l from LeagueState l where l.leagueId = :leagueId")
    Optional<LeagueState> findByIdForUpdate(@Param("leagueId") String leagueId);
}
