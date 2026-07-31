-- Lock de banco para impedir que duas instancias da aplicacao processem o mesmo job de
-- scheduler ao mesmo tempo (ver SchedulerLockRepository). A linha 'movimentacao_cadencia'
-- protege MovimentacaoScheduler/CadenciaFunilService.processarCadenciasAtivas.
CREATE TABLE scheduler_lock (
    nome VARCHAR(100) NOT NULL PRIMARY KEY,
    running BOOLEAN NOT NULL DEFAULT FALSE,
    iniciado_em DATETIME NULL
);

INSERT INTO scheduler_lock (nome, running, iniciado_em) VALUES ('movimentacao_cadencia', FALSE, NULL);
