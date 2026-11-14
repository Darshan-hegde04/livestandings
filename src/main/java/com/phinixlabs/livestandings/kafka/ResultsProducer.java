package com.phinixlabs.livestandings.kafka;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

/**
 * Publishes match results keyed by {@code leagueId} (D2 — never null / round-robin).
 */
@Component
public class ResultsProducer {

    private final KafkaTemplate<String, ResultEvent> kafkaTemplate;
    private final String matchResultsTopic;

    public ResultsProducer(
            KafkaTemplate<String, ResultEvent> kafkaTemplate,
            @Value("${livestandings.kafka.topics.match-results:match-results}") 
            String matchResultsTopic
    ) {
        this.kafkaTemplate = kafkaTemplate;
        this.matchResultsTopic = matchResultsTopic;
    }

    public void publish(ResultEvent event) {
        if (event.leagueId() == null || event.leagueId().isBlank()) {
            throw new IllegalArgumentException("ResultEvent.leagueId is required as the Kafka key");
        }
        kafkaTemplate.send(matchResultsTopic, event.leagueId(), event);
    }
}
