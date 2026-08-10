-- Histórico/log de eventos de uma oportunidade (criação, edição, movimentação entre etapas,
-- envio para lixeira, restauração), exibido no modal de edição da oportunidade ("Detalhes da
-- oportunidade"). autor_nome é um snapshot do nome do usuário no momento do evento (não uma FK),
-- pra permanecer correto mesmo que o usuário seja renomeado ou removido depois. ON DELETE CASCADE
-- porque a exclusão física de uma oportunidade (DELETE /oportunidade/{id}) não tem mais nenhuma
-- tela que exiba esse histórico depois — sem motivo pra manter linhas órfãs.
CREATE TABLE oportunidade_historico (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    empresa_id BIGINT NOT NULL,
    oportunidade_id BIGINT NOT NULL,
    autor_nome VARCHAR(150) NOT NULL,
    tipo VARCHAR(30) NOT NULL,
    descricao VARCHAR(255) NOT NULL,
    criado_em DATETIME NOT NULL,
    CONSTRAINT fk_oportunidade_historico_oportunidade FOREIGN KEY (oportunidade_id) REFERENCES oportunidade(id) ON DELETE CASCADE
);

CREATE INDEX idx_oportunidade_historico_oportunidade ON oportunidade_historico(oportunidade_id, criado_em);
