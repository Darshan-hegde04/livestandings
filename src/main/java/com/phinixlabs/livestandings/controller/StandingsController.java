package com.phinixlabs.livestandings.controller;

import com.phinixlabs.livestandings.common.ApiResponse;
import com.phinixlabs.livestandings.common.PageResponse;
import com.phinixlabs.livestandings.dto.StandingRowDto;
import com.phinixlabs.livestandings.dto.StandingsSnapshotDto;
import com.phinixlabs.livestandings.service.StandingsService;
import com.phinixlabs.livestandings.sse.StandingsSseHub;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
 * Captain read path — public, no organiser token. Live updates via SSE.
 */
@RestController
@RequestMapping("/api/leagues/{leagueId}/standings")
@RequiredArgsConstructor
public class StandingsController {

    private final StandingsService standingsService;
    private final StandingsSseHub standingsSseHub;

    @GetMapping
    public ResponseEntity<ApiResponse<StandingsSnapshotDto>> snapshot(@PathVariable String leagueId) {
        return ResponseEntity.ok(ApiResponse.ok(standingsService.getSnapshot(leagueId)));
    }

    @GetMapping("/rows")
    public ResponseEntity<ApiResponse<PageResponse<StandingRowDto>>> list(
            @PathVariable String leagueId,
            @PageableDefault(size = 20, sort = "position", direction = Sort.Direction.ASC) Pageable pageable
    ) {
        return ResponseEntity.ok(ApiResponse.ok(standingsService.list(leagueId, pageable)));
    }

    /**
     * SSE stream: immediate {@code standings} snapshot, then live pushes after apply.
     */
    @GetMapping(path = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter stream(@PathVariable String leagueId) {
        SseEmitter emitter = standingsSseHub.subscribe(leagueId);
        // Same JSON path as fan-out pushes (safe from Kafka listener thread too).
        standingsSseHub.sendSnapshot(leagueId, standingsService.getSnapshot(leagueId));
        return emitter;
    }
}
