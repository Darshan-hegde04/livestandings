package com.phinixlabs.livestandings.service.impl;

import com.phinixlabs.livestandings.common.exception.ValidationException;
import com.phinixlabs.livestandings.dto.CorrectResultRequest;
import com.phinixlabs.livestandings.dto.SubmitResultRequest;
import com.phinixlabs.livestandings.kafka.ResultEvent;
import com.phinixlabs.livestandings.kafka.ResultsProducer;
import com.phinixlabs.livestandings.service.ResultsService;
import java.time.Instant;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * REST → Kafka only. Standings are written exclusively by the Kafka apply service.
 */
@Service
@RequiredArgsConstructor
public class ResultsServiceImpl implements ResultsService {

    private final ResultsProducer resultsProducer;

    @Override
    public void submit(String leagueId, SubmitResultRequest request) {
        validateLeagueId(leagueId);
        validateTeams(request.homeTeam(), request.awayTeam());

        Instant submittedAt = Instant.now();
        Instant reportedAt = request.reportedAt() != null ? request.reportedAt() : submittedAt;

        resultsProducer.publish(new ResultEvent(
                request.matchId().trim(),
                leagueId.trim(),
                request.homeTeam().trim(),
                request.awayTeam().trim(),
                request.homeScore(),
                request.awayScore(),
                reportedAt,
                submittedAt,
                false
        ));
    }

    @Override
    public void correct(String leagueId, String matchId, CorrectResultRequest request) {
        validateLeagueId(leagueId);
        if (!StringUtils.hasText(matchId)) {
            throw new ValidationException("matchId is required");
        }
        validateTeams(request.homeTeam(), request.awayTeam());

        Instant submittedAt = Instant.now();
        Instant reportedAt = request.reportedAt() != null ? request.reportedAt() : submittedAt;

        resultsProducer.publish(new ResultEvent(
                matchId.trim(),
                leagueId.trim(),
                request.homeTeam().trim(),
                request.awayTeam().trim(),
                request.homeScore(),
                request.awayScore(),
                reportedAt,
                submittedAt,
                true
        ));
    }

    private static void validateLeagueId(String leagueId) {
        if (!StringUtils.hasText(leagueId)) {
            throw new ValidationException("leagueId is required");
        }
    }

    private static void validateTeams(String homeTeam, String awayTeam) {
        if (!StringUtils.hasText(homeTeam) || !StringUtils.hasText(awayTeam)) {
            throw new ValidationException("homeTeam and awayTeam are required");
        }
        if (homeTeam.trim().equalsIgnoreCase(awayTeam.trim())) {
            throw new ValidationException("homeTeam and awayTeam must differ");
        }
    }
}
