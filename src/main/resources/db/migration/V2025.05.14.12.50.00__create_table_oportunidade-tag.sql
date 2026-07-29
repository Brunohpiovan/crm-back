CREATE TABLE oportunidade_tag (
    oportunidade_id BIGINT NOT NULL,
    tag_id BIGINT NOT NULL,
    PRIMARY KEY (oportunidade_id, tag_id),
    FOREIGN KEY (oportunidade_id) REFERENCES oportunidade(id),
    FOREIGN KEY (tag_id) REFERENCES tag(id)
);