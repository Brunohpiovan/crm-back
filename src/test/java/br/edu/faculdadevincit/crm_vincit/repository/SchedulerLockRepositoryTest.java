package br.edu.faculdadevincit.crm_vincit.repository;

import br.edu.faculdadevincit.crm_vincit.model.SchedulerLock;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.TestPropertySource;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verifica a semântica do lock de banco usado para impedir que duas instâncias da aplicação
 * processem o mesmo job de scheduler ao mesmo tempo (ver CadenciaFunilService.processarCadenciasAtivas):
 * o UPDATE condicional só adquire o lock se ele estiver livre ou obsoleto (instância anterior
 * travou sem liberar).
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:scheduler_lock_repo_test;DB_CLOSE_DELAY=-1;MODE=MySQL",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.flyway.enabled=false",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
class SchedulerLockRepositoryTest {

    private static final String NOME = "movimentacao_cadencia";

    @Autowired
    private SchedulerLockRepository schedulerLockRepository;

    @Test
    void tentarAdquirir_comLockLivre_adquireEMarcaComoRunning() {
        schedulerLockRepository.save(new SchedulerLock(NOME, false, null));

        LocalDateTime agora = LocalDateTime.now();
        int linhasAfetadas = schedulerLockRepository.tentarAdquirir(NOME, agora, agora.minusMinutes(5));

        assertThat(linhasAfetadas).isEqualTo(1);
        assertThat(schedulerLockRepository.findById(NOME).orElseThrow().isRunning()).isTrue();
    }

    @Test
    void tentarAdquirir_comLockJaEmExecucaoERecente_naoAdquire() {
        LocalDateTime agora = LocalDateTime.now();
        schedulerLockRepository.save(new SchedulerLock(NOME, true, agora));

        int linhasAfetadas = schedulerLockRepository.tentarAdquirir(NOME, agora, agora.minusMinutes(5));

        assertThat(linhasAfetadas).isEqualTo(0);
    }

    @Test
    void tentarAdquirir_comLockObsoleto_reclamaOLock() {
        LocalDateTime agora = LocalDateTime.now();
        // Lock "preso" há 10 minutos (ex.: instância anterior caiu no meio do processamento).
        schedulerLockRepository.save(new SchedulerLock(NOME, true, agora.minusMinutes(10)));

        int linhasAfetadas = schedulerLockRepository.tentarAdquirir(NOME, agora, agora.minusMinutes(5));

        assertThat(linhasAfetadas).isEqualTo(1);
    }

    @Test
    void liberar_marcaComoNaoRunning() {
        schedulerLockRepository.save(new SchedulerLock(NOME, true, LocalDateTime.now()));

        schedulerLockRepository.liberar(NOME);

        assertThat(schedulerLockRepository.findById(NOME).orElseThrow().isRunning()).isFalse();
    }
}
