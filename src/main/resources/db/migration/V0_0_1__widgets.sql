CREATE TABLE WIDGETS
(
    id               UUID PRIMARY KEY,
    height           INTEGER   NOT NULL,
    width            INTEGER   NOT NULL,
    x                INTEGER   NOT NULL,
    y                INTEGER   NOT NULL,
    z                INTEGER   NOT NULL,
    created_at       TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP(),
    last_modified_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP(),
    polygon          GEOMETRY
);

CREATE INDEX "Z_INDEX" ON WIDGETS (Z);