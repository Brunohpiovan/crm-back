package br.edu.faculdadevincit.crm_vincit.scheduler;

import br.edu.faculdadevincit.crm_vincit.service.CadenciaFunilService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class MovimentacaoScheduler {

    @Autowired
    private CadenciaFunilService cadenciaService;

    @Scheduled(cron = "0 * * * * *") // Roda a cada minuto
    public void executar() {
        cadenciaService.processarCadenciasAtivas();
    }
}
