package com.phinixlabs.livestandings.service.impl;

import com.phinixlabs.livestandings.entity.LeagueState;
import com.phinixlabs.livestandings.entity.MatchResult;
import com.phinixlabs.livestandings.entity.Standing;
import com.phinixlabs.livestandings.entity.StandingId;
import com.phinixlabs.livestandings.kafka.ResultEvent;
import com.phinixlabs.livestandings.metrics.IngestMetrics;
import com.phinixlabs.livestandings.repository.LeagueStateRepository;
import com.phinixlabs.livestandings.repository.MatchResultRepository;
import com.phinixlabs.livestandings.repository.StandingRepository;
import com.phinixlabs.livestandings.records.MatchScore;
import com.phinixlabs.livestandings.records.TeamStanding;
import com.phinixlabs.livestandings.service.MatchResultApplyService;
import com.phinixlabs.livestandings.service.StandingsCalculator;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * D1 offset guard + D3 full-league recompute in one DB transaction.
 * One recompute per league per batch.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MatchResultApplyServiceImpl implements MatchResultApplyService {

    private final MatchResultRepository matchResultRepository;
    private final StandingRepository standingRepository;
    private final LeagueStateRepository leagueStateRepository;
    private final IngestMetrics ingestMetrics;

    @Override
    @Transactional
    public List<LeagueUpdate> applyBatch(List<ConsumedResult> batch) {
        if (batch == null || batch.isEmpty()) {
            return List.of();
        }

        Map<String, List<ConsumedResult>> byLeague = new LinkedHashMap<>();
        for (ConsumedResult item : batch) {
            byLeague
                    .computeIfAbsent(item.event().leagueId(), k -> new ArrayList<>())
                    .add(item);
        }

        Set<String> leaguesToRecompute = new LinkedHashSet<>();
        Map<String, Instant> latestSubmittedByLeague = new LinkedHashMap<>();

        for (Map.Entry<String, List<ConsumedResult>> entry : byLeague.entrySet()) {
            String leagueId = entry.getKey();
            List<ConsumedResult> leagueBatch = entry.getValue().stream()
                    .sorted(Comparator.comparingLong(ConsumedResult::offset))
                    .toList();

            for (ConsumedResult item : leagueBatch) {
                if (tryUpsert(item)) {
                    leaguesToRecompute.add(leagueId);
                    latestSubmittedByLeague.put(leagueId, item.event().submittedAt());
                    ingestMetrics.recordApplied(leagueId);
                }
            }
        }

        List<LeagueUpdate> updates = new ArrayList<>();
        Instant now = Instant.now();
        for (String leagueId : leaguesToRecompute) {
            recomputeStandings(leagueId);
            LeagueState state = bumpVersion(leagueId, now);
            updates.add(new LeagueUpdate(leagueId, state.getVersion(), state.getUpdatedAt()));

            Instant submittedAt = latestSubmittedByLeague.get(leagueId);
            ingestMetrics.recordApplyLatency(leagueId, submittedAt);
        }

        return updates;
    }

    /**
     * @return true if the row was inserted or updated (newer offset)
     */
    private boolean tryUpsert(ConsumedResult item) {
        ResultEvent event = item.event();
        int partition = item.partition();
        long offset = item.offset();

        MatchResult existing = matchResultRepository.findByIdForUpdate(event.matchId()).orElse(null);
        if (existing != null) {
            if (!existing.getLeagueId().equals(event.leagueId())) {
                log.warn(
                        "Rejecting result for matchId={} — league mismatch stored={} incoming={}",
                        event.matchId(), existing.getLeagueId(), event.leagueId()
                );
                ingestMetrics.recordStaleOrDuplicateRejection();
                return false;
            }
            // D1: authority is Kafka offset order on the league partition
            if (existing.getSourcePartition() != partition || offset <= existing.getSourceOffset()) {
                log.debug(
                        "Rejecting stale/duplicate matchId={} partition={} offset={} storedOffset={}",
                        event.matchId(), partition, offset, existing.getSourceOffset()
                );
                ingestMetrics.recordStaleOrDuplicateRejection();
                return false;
            }
            applyFields(existing, event, partition, offset);
            matchResultRepository.save(existing);
            return true;
        }

        MatchResult created = MatchResult.builder()
                .matchId(event.matchId())
                .leagueId(event.leagueId())
                .homeTeam(event.homeTeam())
                .awayTeam(event.awayTeam())
                .homeScore(event.homeScore())
                .awayScore(event.awayScore())
                .reportedAt(event.reportedAt())
                .submittedAt(event.submittedAt())
                .sourcePartition(partition)
                .sourceOffset(offset)
                .appliedAt(Instant.now())
                .build();
        matchResultRepository.save(created);
        return true;
    }

    private static void applyFields(MatchResult row, ResultEvent event, int partition, long offset) {
        row.setHomeTeam(event.homeTeam());
        row.setAwayTeam(event.awayTeam());
        row.setHomeScore(event.homeScore());
        row.setAwayScore(event.awayScore());
        row.setReportedAt(event.reportedAt());
        row.setSubmittedAt(event.submittedAt());
        row.setSourcePartition(partition);
        row.setSourceOffset(offset);
        row.setAppliedAt(Instant.now());
    }

    private void recomputeStandings(String leagueId) {
        List<MatchScore> scores = matchResultRepository.findByLeagueId(leagueId).stream()
                .map(m -> new MatchScore(m.getHomeTeam(), m.getAwayTeam(), m.getHomeScore(), m.getAwayScore()))
                .toList();

        List<TeamStanding> ranked = StandingsCalculator.calculate(scores);

        standingRepository.deleteByLeagueId(leagueId);
        List<Standing> rows = ranked.stream()
                .map(ts -> Standing.builder()
                        .id(new StandingId(leagueId, ts.team()))
                        .played(ts.played())
                        .won(ts.won())
                        .drawn(ts.drawn())
                        .lost(ts.lost())
                        .goalsFor(ts.goalsFor())
                        .goalsAgainst(ts.goalsAgainst())
                        .goalDiff(ts.goalDiff())
                        .points(ts.points())
                        .position(ts.position())
                        .build())
                .toList();
        standingRepository.saveAll(rows);
    }

    private LeagueState bumpVersion(String leagueId, Instant updatedAt) {
        LeagueState state = leagueStateRepository.findByIdForUpdate(leagueId).orElse(null);
        if (state == null) {
            state = LeagueState.builder()
                    .leagueId(leagueId)
                    .version(1L)
                    .updatedAt(updatedAt)
                    .build();
        } else {
            state.setVersion(state.getVersion() + 1);
            state.setUpdatedAt(updatedAt);
        }
        return leagueStateRepository.save(state);
    }
}
