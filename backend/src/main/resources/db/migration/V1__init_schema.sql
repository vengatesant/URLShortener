CREATE TABLE urls (
    id            BIGSERIAL PRIMARY KEY,
    short_code    VARCHAR(16)  NOT NULL,
    long_url      TEXT         NOT NULL,
    created_by    VARCHAR(64),
    created_at    TIMESTAMPTZ  NOT NULL DEFAULT now(),
    expires_at    TIMESTAMPTZ,
    is_active     BOOLEAN      NOT NULL DEFAULT true
);

-- Enforced only while a code is active, so a deactivated code can be superseded
-- by a fresh row without a global unique constraint fighting soft deletes.
CREATE UNIQUE INDEX uq_urls_short_code_active ON urls (short_code) WHERE is_active;
CREATE INDEX idx_urls_short_code ON urls (short_code);

CREATE TABLE click_events (
    id           BIGSERIAL PRIMARY KEY,
    url_id       BIGINT NOT NULL REFERENCES urls (id) ON DELETE CASCADE,
    clicked_at   TIMESTAMPTZ NOT NULL DEFAULT now(),
    referrer     TEXT,
    user_agent   TEXT,
    country      VARCHAR(2)
);

CREATE INDEX idx_click_events_url_id_clicked_at ON click_events (url_id, clicked_at DESC);
