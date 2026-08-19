package com.juridiqsystem.crm.scheduler;

import com.juridiqsystem.crm.service.CadenciaFunilService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class MovimentacaoScheduler {

    @Autowired
    private CadenciaFunilService cadenciaService;

    @Scheduled(cron = "0 * * * * *") // Roda a cada minuto
    public void executar() {
        try {
            cadenciaService.processarCadenciasAtivas();
        } catch (Exception e) {
            // Falha por item já é isolada dentro de CadenciaFunilService; chegar aqui indica um
            // erro estrutural (ex.: lock de banco indisponível). Logamos para não perder o rastro
            // e deixamos o próximo tick (1 minuto depois) tentar novamente.
            log.error("Execução do MovimentacaoScheduler falhou de forma inesperada.", e);
        }
    }
}
