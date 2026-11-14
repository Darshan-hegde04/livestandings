package com.phinixlabs.livestandings.kafka;

import com.phinixlabs.livestandings.dto.StandingsSnapshotDto;
import com.phinixlabs.livestandings.service.StandingsService;
import com.phinixlabs.livestandings.sse.StandingsSseHub;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

/**
 * Unique consumer group per instance — every node receives standings-updated and pushes SSE.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class StandingsUpdatedFanoutConsumer {

    private final StandingsService standingsService;
    private final StandingsSseHub standingsSseHub;

    @KafkaListener(
            topics = "${livestandings.kafka.topics.standings-updated:standings-updated}",
            groupId = "${livestandings.kafka.fanout-group}",
            containerFactory = "standingsUpdatedKafkaListenerContainerFactory"
    )
    public void onUpdate(StandingsUpdatedEvent event, Acknowledgment ack) {
        try {
            if (event != null && event.leagueId() != null) {
                StandingsSnapshotDto snapshot = standingsService.getSnapshot(event.leagueId());
                standingsSseHub.sendSnapshot(event.leagueId(), snapshot);
                log.debug("SSE fan-out leagueId={} version={}", event.leagueId(), snapshot.version());
            }
        } finally {
            ack.acknowledge();
        }
    }
}
