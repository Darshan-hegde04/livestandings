package com.phinixlabs.livestandings.repository;

import com.phinixlabs.livestandings.entity.MatchResult;
import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface MatchResultRepository extends JpaRepository<MatchResult, String> {

    List<MatchResult> findByLeagueId(String leagueId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select m from MatchResult m where m.matchId = :matchId")
    Optional<MatchResult> findByIdForUpdate(@Param("matchId") String matchId);
}
