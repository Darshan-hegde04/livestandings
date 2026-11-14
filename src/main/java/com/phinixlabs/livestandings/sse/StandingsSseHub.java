package com.phinixlabs.livestandings.sse;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.phinixlabs.livestandings.dto.StandingsSnapshotDto;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
 * Per-instance SSE emitter registry. ResultsConsumer pushes snapshots here after apply.
 */
@Slf4j
@Component
public class StandingsSseHub {

    private final Map<String, CopyOnWriteArrayList<SseEmitter>> emittersByLeague = new ConcurrentHashMap<>();
    private final ObjectMapper objectMapper;

    public StandingsSseHub() {
        this.objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());
    }

    public SseEmitter subscribe(String leagueId) {
        SseEmitter emitter = new SseEmitter(0L);
        emittersByLeague.computeIfAbsent(leagueId, id -> new CopyOnWriteArrayList<>()).add(emitter);
        log.info("SSE subscribe leagueId={} emitters={}", leagueId, emittersByLeague.get(leagueId).size());

        Runnable remove = () -> removeEmitter(leagueId, emitter);
        emitter.onCompletion(remove);
        emitter.onTimeout(remove);
        emitter.onError(ex -> remove.run());
        return emitter;
    }

    public void sendSnapshot(String leagueId, StandingsSnapshotDto snapshot) {
        List<SseEmitter> emitters = emittersByLeague.get(leagueId);
        if (emitters == null || emitters.isEmpty()) {
            log.debug("SSE skip leagueId={} — no subscribers", leagueId);
            return;
        }

        final String json;
        try {
            json = objectMapper.writeValueAsString(snapshot);
        } catch (Exception ex) {
            log.warn("SSE serialize failed leagueId={}: {}", leagueId, ex.toString());
            return;
        }

        for (SseEmitter emitter : List.copyOf(emitters)) {
            try {
                emitter.send(SseEmitter.event()
                        .name("standings")
                        .data(json, MediaType.APPLICATION_JSON));
            } catch (Exception ex) {
                // Broken pipe / client gone — never let this escape onto the Kafka listener thread.
                log.debug("SSE dead emitter leagueId={}: {}", leagueId, ex.toString());
                removeEmitter(leagueId, emitter);
                try {
                    emitter.complete();
                } catch (Exception ignored) {
                    // already closed
                }
            }
        }
    }

    private void removeEmitter(String leagueId, SseEmitter emitter) {
        List<SseEmitter> emitters = emittersByLeague.get(leagueId);
        if (emitters != null) {
            emitters.remove(emitter);
        }
    }
}
