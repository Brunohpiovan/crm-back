-- Migração definitiva Twilio -> Meta WhatsApp Cloud API (sistema ainda não está em produção,
-- sem necessidade de manter compatibilidade/dados da Twilio). Toda integração WhatsApp passa a
-- viver em whatsapp_integration (ver migration anterior). Nenhum histórico de chat é afetado —
-- esta tabela só guardava credenciais da conta Twilio, nunca mensagens.
DROP TABLE IF EXISTS empresa_twilio_config;
