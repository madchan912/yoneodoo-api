-- watched_youtubers: 크롤링 대상 유튜버 목록
CREATE TABLE IF NOT EXISTS watched_youtubers (
    id            BIGSERIAL    PRIMARY KEY,
    channel_url   VARCHAR(500) NOT NULL,
    youtuber_name VARCHAR(100) NOT NULL,
    is_active     BOOLEAN      NOT NULL DEFAULT true,
    last_crawled_at TIMESTAMP,
    created_at    TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- crawl_history: 크롤링 실행 이력
CREATE TABLE IF NOT EXISTS crawl_history (
    id             BIGSERIAL   PRIMARY KEY,
    youtuber_name  VARCHAR(100),
    channel_url    VARCHAR(500),
    job_id         VARCHAR(100),
    start_idx      INTEGER,
    end_idx        INTEGER,
    status         VARCHAR(20),
    result_summary TEXT,
    triggered_by   VARCHAR(20),
    started_at     TIMESTAMP,
    finished_at    TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_crawl_history_job_id ON crawl_history(job_id);
