package com.phinixlabs.livestandings.kafka;

import com.phinixlabs.livestandings.service.MatchResultApplyService;
import com.phinixlabs.livestandings.service.MatchResultApplyService.ConsumedResult;
import com.phinixlabs.livestandings.service.MatchResultApplyService.LeagueUpdate;
import com.phinixlabs.livestandings.service.StandingsService;
import com.phinixlabs.livestandings.sse.StandingsSseHub;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

/**
 * Consumes {@code match-results}, applies via {@link MatchResultApplyService},
 * pushes SSE on this instance, then manual ack (D2).
 *
 * <p>In-process SSE (main): no {@code standings-updated} fan-out topic.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ResultsConsumer {

    private final MatchResultApplyService matchResultApplyService;
    private final StandingsService standingsService;
    private final StandingsSseHub standingsSseHub;

    @KafkaListener(
            topics = "${livestandings.kafka.topics.match-results:match-results}",
            groupId = "${livestandings.kafka.consumer-group:livestandings-results-applier}",
            containerFactory = "resultsKafkaListenerContainerFactory"
    )
    public void onBatch(List<ConsumerRecord<String, ResultEvent>> records, Acknowledgment ack) {
        if (records == null || records.isEmpty()) {
            return;
        }

        List<ConsumedResult> batch = new ArrayList<>(records.size());
        for (ConsumerRecord<String, ResultEvent> record : records) {
            ResultEvent event = record.value();
            if (event == null) {
                log.warn("Skipping null ResultEvent partition={} offset={}", record.partition(), record.offset());
                continue;
            }
            batch.add(new ConsumedResult(event, record.partition(), record.offset()));
        }

        List<LeagueUpdate> updates = matchResultApplyService.applyBatch(batch);
        for (LeagueUpdate update : updates) {
            standingsSseHub.sendSnapshot(update.leagueId(), standingsService.getSnapshot(update.leagueId()));
        }
        ack.acknowledge();

        if (!updates.isEmpty()) {
            log.info("Applied standings updates: {}", updates);
        }
    }
}
