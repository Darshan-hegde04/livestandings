package com.phinixlabs.livestandings.repository;

import com.phinixlabs.livestandings.entity.Standing;
import com.phinixlabs.livestandings.entity.StandingId;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface StandingRepository extends JpaRepository<Standing, StandingId> {

    List<Standing> findByIdLeagueIdOrderByPositionAsc(String leagueId);

    Page<Standing> findByIdLeagueId(String leagueId, Pageable pageable);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("delete from Standing s where s.id.leagueId = :leagueId")
    void deleteByLeagueId(@Param("leagueId") String leagueId);
}
