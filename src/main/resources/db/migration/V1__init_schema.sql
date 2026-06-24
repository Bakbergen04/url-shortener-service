CREATE TABLE short_links (
    id UUID PRIMARY KEY,
    original_url VARCHAR(2048) NOT NULL,
    short_code VARCHAR(32) NOT NULL,
    title VARCHAR(255),
    active BOOLEAN NOT NULL DEFAULT TRUE,
    click_count BIGINT NOT NULL DEFAULT 0 CHECK (click_count >= 0),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    expires_at TIMESTAMP WITH TIME ZONE,
    last_accessed_at TIMESTAMP WITH TIME ZONE
);

CREATE UNIQUE INDEX ux_short_links_short_code ON short_links (short_code);
CREATE INDEX ix_short_links_created_at ON short_links (created_at);
CREATE INDEX ix_short_links_expires_at ON short_links (expires_at);

CREATE TABLE click_events (
    id UUID PRIMARY KEY,
    short_link_id UUID NOT NULL,
    clicked_at TIMESTAMP WITH TIME ZONE NOT NULL,
    user_agent VARCHAR(1024),
    referer VARCHAR(2048),
    ip_hash VARCHAR(64),
    CONSTRAINT fk_click_events_short_link
        FOREIGN KEY (short_link_id) REFERENCES short_links (id) ON DELETE CASCADE
);

CREATE INDEX ix_click_events_short_link_id ON click_events (short_link_id);
CREATE INDEX ix_click_events_clicked_at ON click_events (clicked_at);
