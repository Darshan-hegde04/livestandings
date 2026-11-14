package com.phinixlabs.livestandings.service;

import com.phinixlabs.livestandings.dto.CorrectResultRequest;
import com.phinixlabs.livestandings.dto.SubmitResultRequest;

/**
 * Organiser write path: validate and publish to Kafka. Never writes standings.
 */
public interface ResultsService {

    void submit(String leagueId, SubmitResultRequest request);

    void correct(String leagueId, String matchId, CorrectResultRequest request);
}
