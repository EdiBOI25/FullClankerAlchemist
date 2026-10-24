CREATE TABLE IF NOT EXISTS item (
    id       INTEGER PRIMARY KEY,
    name     TEXT NOT NULL UNIQUE COLLATE NOCASE,
    emoji    TEXT,
    icon_url TEXT
);

CREATE TABLE IF NOT EXISTS tag (
    id   INTEGER PRIMARY KEY,
    name TEXT NOT NULL UNIQUE CHECK (name = lower(name))
);

CREATE TABLE IF NOT EXISTS item_tag (
    item_id INTEGER NOT NULL REFERENCES item (id),
    tag_id  INTEGER NOT NULL REFERENCES tag (id),
    PRIMARY KEY (item_id, tag_id)
);
