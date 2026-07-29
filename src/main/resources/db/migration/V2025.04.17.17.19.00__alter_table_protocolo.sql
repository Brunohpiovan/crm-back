ALTER TABLE protocolo
ADD COLUMN admin_anterior_id BIGINT NULL;

ALTER TABLE protocolo
ADD CONSTRAINT fk_protocolo_admin_anterior_id
FOREIGN KEY (admin_anterior_id) REFERENCES usuario(id);
