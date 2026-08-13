-- Integração WhatsApp (Meta Cloud API) por empresa — substitui empresa_twilio_config (migração
-- Twilio -> Meta). Cada empresa é dona da própria WABA/número, conectados via Embedded Signup
-- (Tech Provider); nunca existe uma WABA compartilhada entre empresas. access_token_encrypted é
-- gravado cifrado pela aplicação (EncryptedStringConverter) — nunca texto puro no banco.
-- UNIQUE(phone_number_id) é como o webhook da Meta resolve o tenant a partir de
-- value.metadata.phone_number_id. UNIQUE(empresa_id) porque é uma configuração 1:1 por empresa.
-- phone_number_id/waba_id ficam NULL até a empresa concluir o Embedded Signup (status NOT_CONNECTED).
CREATE TABLE whatsapp_integration (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    empresa_id BIGINT NOT NULL,
    waba_id VARCHAR(64) NULL,
    phone_number_id VARCHAR(64) NULL,
    business_id VARCHAR(64) NULL,
    display_phone_number VARCHAR(32) NULL,
    verified_name VARCHAR(150) NULL,
    access_token_encrypted TEXT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'NOT_CONNECTED',
    connected_at DATETIME NULL,
    disconnected_at DATETIME NULL,
    criado_em DATETIME,
    atualizado_em DATETIME,
    CONSTRAINT uk_whatsapp_integration_empresa UNIQUE (empresa_id),
    CONSTRAINT uk_whatsapp_integration_phone_number_id UNIQUE (phone_number_id),
    CONSTRAINT fk_whatsapp_integration_empresa FOREIGN KEY (empresa_id) REFERENCES empresa(id)
);
