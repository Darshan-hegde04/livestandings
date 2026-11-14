package com.phinixlabs.livestandings.controller;

import com.phinixlabs.livestandings.common.ApiResponse;
import com.phinixlabs.livestandings.dto.CorrectResultRequest;
import com.phinixlabs.livestandings.dto.SubmitResultRequest;
import com.phinixlabs.livestandings.service.ResultsService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Organiser write path — REST → Kafka only (never writes standings).
 */
@RestController
@RequestMapping("/api/leagues/{leagueId}/results")
@RequiredArgsConstructor
public class ResultsController {

    private final ResultsService resultsService;

    @PostMapping
    public ResponseEntity<ApiResponse<String>> submit(
            @PathVariable String leagueId,
            @Valid @RequestBody SubmitResultRequest request
    ) {
        resultsService.submit(leagueId, request);
        return ResponseEntity
                .status(HttpStatus.ACCEPTED)
                .body(ApiResponse.ok(
                        "Result accepted",
                        "Standings are applied asynchronously — GET /api/leagues/"
                                + leagueId
                                + "/standings (or /standings/stream) to see the result"
                ));
    }

    @PutMapping("/{matchId}")
    public ResponseEntity<ApiResponse<String>> correct(
            @PathVariable String leagueId,
            @PathVariable String matchId,
            @Valid @RequestBody CorrectResultRequest request
    ) {
        resultsService.correct(leagueId, matchId, request);
        return ResponseEntity
                .status(HttpStatus.ACCEPTED)
                .body(ApiResponse.ok(
                        "Correction accepted",
                        "Standings are applied asynchronously — GET /api/leagues/"
                                + leagueId
                                + "/standings (or /standings/stream) to see the result"
                ));
    }
}
