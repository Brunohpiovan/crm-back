-- Integração Twilio por empresa (multi-tenant do Chat de Atendimento): cada Empresa passa a ter
-- sua própria conta Twilio (Account SID, Auth Token, número WhatsApp) em vez da conta global única
-- usada até aqui (TwilioConfig/@Value). auth_token é gravado cifrado pela aplicação
-- (EncryptedStringConverter) — nunca texto puro no banco. UNIQUE(whatsapp_number) impede o mesmo
-- número Twilio ser cadastrado em duas empresas (é assim que o webhook resolve o tenant a partir do
-- "To"). UNIQUE(empresa_id) porque é uma configuração 1:1 por empresa.
CREATE TABLE empresa_twilio_config (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    empresa_id BIGINT NOT NULL,
    account_sid VARCHAR(64) NOT NULL,
    auth_token VARCHAR(255) NOT NULL,
    whatsapp_number VARCHAR(32) NOT NULL,
    enabled BOOLEAN NOT NULL DEFAULT FALSE,
    criado_em DATETIME,
    atualizado_em DATETIME,
    CONSTRAINT uk_empresa_twilio_config_empresa UNIQUE (empresa_id),
    CONSTRAINT uk_empresa_twilio_config_whatsapp_number UNIQUE (whatsapp_number),
    CONSTRAINT fk_empresa_twilio_config_empresa FOREIGN KEY (empresa_id) REFERENCES empresa(id)
);
