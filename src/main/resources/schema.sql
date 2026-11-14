-- Reference schema (Hibernate ddl-auto=update for local; prefer validate in prod).
CREATE TABLE IF NOT EXISTS match_results (
    match_id         VARCHAR(64)  NOT NULL PRIMARY KEY,
    league_id        VARCHAR(64)  NOT NULL,
    home_team        VARCHAR(128) NOT NULL,
    away_team        VARCHAR(128) NOT NULL,
    home_score       INT          NOT NULL,
    away_score       INT          NOT NULL,
    reported_at      TIMESTAMP(6) NULL,
    submitted_at     TIMESTAMP(6) NOT NULL,
    source_partition INT          NOT NULL,
    source_offset    BIGINT       NOT NULL,
    applied_at       TIMESTAMP(6) NOT NULL,
    INDEX idx_match_results_league (league_id)
);

CREATE TABLE IF NOT EXISTS standings (
    league_id     VARCHAR(64)  NOT NULL,
    team          VARCHAR(128) NOT NULL,
    played        INT          NOT NULL,
    won           INT          NOT NULL,
    drawn         INT          NOT NULL,
    lost          INT          NOT NULL,
    goals_for     INT          NOT NULL,
    goals_against INT          NOT NULL,
    goal_diff     INT          NOT NULL,
    points        INT          NOT NULL,
    position      INT          NOT NULL,
    PRIMARY KEY (league_id, team)
);

CREATE TABLE IF NOT EXISTS league_state (
    league_id  VARCHAR(64)  NOT NULL PRIMARY KEY,
    version    BIGINT       NOT NULL,
    updated_at TIMESTAMP(6) NOT NULL
);
