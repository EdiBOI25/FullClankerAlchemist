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

CREATE TABLE IF NOT EXISTS recipe (
    id         INTEGER PRIMARY KEY,
    item_a_id  INTEGER NOT NULL REFERENCES item (id),
    item_b_id  INTEGER NOT NULL REFERENCES item (id),
    result_id  INTEGER NOT NULL REFERENCES item (id),
    UNIQUE (item_a_id, item_b_id),
    CHECK (item_a_id <= item_b_id)
);
