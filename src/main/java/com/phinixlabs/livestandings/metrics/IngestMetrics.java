package com.phinixlabs.livestandings.metrics;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import java.time.Duration;
import java.time.Instant;
import org.springframework.stereotype.Component;

/**
 * D4 metrics: apply latency timer and stale/duplicate rejection counter.
 */
@Component
public class IngestMetrics {

    private final MeterRegistry meterRegistry;
    private final Counter staleOrDuplicateRejections;

    public IngestMetrics(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
        this.staleOrDuplicateRejections = meterRegistry.counter("standings_stale_or_duplicate_rejections");
    }

    public void recordApplyLatency(String leagueId, Instant submittedAt) {
        if (submittedAt == null) {
            return;
        }
        Timer.builder("standings_apply_latency")
                .tag("league", leagueId)
                .publishPercentileHistogram()
                .register(meterRegistry)
                .record(Duration.between(submittedAt, Instant.now()));
    }

    public void recordApplied(String leagueId) {
        meterRegistry.counter("standings_results_applied", "league", leagueId).increment();
    }

    public void recordStaleOrDuplicateRejection() {
        staleOrDuplicateRejections.increment();
    }
}
