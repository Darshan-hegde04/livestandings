package com.phinixlabs.livestandings.kafka;

import com.phinixlabs.livestandings.service.MatchResultApplyService.LeagueUpdate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
public class StandingsUpdatedProducer {

    private final KafkaTemplate<String, StandingsUpdatedEvent> kafkaTemplate;
    private final String topic;

    public StandingsUpdatedProducer(
            KafkaTemplate<String, StandingsUpdatedEvent> kafkaTemplate,
            @Value("${livestandings.kafka.topics.standings-updated:standings-updated}") String topic
    ) {
        this.kafkaTemplate = kafkaTemplate;
        this.topic = topic;
    }

    public void publish(LeagueUpdate update) {
        kafkaTemplate.send(
                topic,
                update.leagueId(),
                new StandingsUpdatedEvent(update.leagueId(), update.version(), update.updatedAt())
        );
    }
}
