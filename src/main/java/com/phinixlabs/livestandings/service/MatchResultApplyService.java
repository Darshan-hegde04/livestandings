package com.phinixlabs.livestandings.service;

import com.phinixlabs.livestandings.kafka.ResultEvent;
import java.time.Instant;
import java.util.List;

/**
 * Kafka apply path (D1/D3): offset-guarded upsert, per-league recompute, version bump
 * in one transaction. Caller pushes SSE on this instance after commit, then acks.
 */
public interface MatchResultApplyService {

    List<LeagueUpdate> applyBatch(List<ConsumedResult> batch);

    record ConsumedResult(ResultEvent event, int partition, long offset) {
    }

    record LeagueUpdate(String leagueId, long version, Instant updatedAt) {
    }
}
