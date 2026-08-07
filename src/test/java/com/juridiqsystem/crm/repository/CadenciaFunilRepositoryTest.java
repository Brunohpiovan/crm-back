package com.juridiqsystem.crm.repository;

import com.juridiqsystem.crm.model.CadenciaFunil;
import com.juridiqsystem.crm.model.Etapa;
import com.juridiqsystem.crm.model.Funil;
import com.juridiqsystem.crm.model.enums.Situacao;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.TestPropertySource;

import java.time.LocalTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verifica que a troca de EAGER implícito por LAZY + JOIN FETCH/@EntityGraph em
 * CadenciaFunilRepository continua entregando os mesmos dados que o código anterior
 * (EAGER) entregava, sem LazyInitializationException e sem duplicar linhas.
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:cadencia_funil_repo_test;DB_CLOSE_DELAY=-1;MODE=MySQL",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.flyway.enabled=false",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
class CadenciaFunilRepositoryTest {

    @Autowired
    private CadenciaFunilRepository cadenciaFunilRepository;

    @Autowired
    private FunilRepository funilRepository;

    @Autowired
    private EtapaRepository etapaRepository;

    @Test
    void findAllWithDetailsCarregaAssociacoesCorretamente() {
        Funil funilOrigem = funilRepository.save(criaFunil("Funil Origem"));
        Funil funilDestino = funilRepository.save(criaFunil("Funil Destino"));
        Etapa etapaOrigem = etapaRepository.save(criaEtapa("Etapa Origem", funilOrigem));
        Etapa etapaDestino = etapaRepository.save(criaEtapa("Etapa Destino", funilDestino));
        cadenciaFunilRepository.save(criaCadencia("Cadencia Teste", funilOrigem, etapaOrigem, funilDestino, etapaDestino));

        List<CadenciaFunil> resultado = cadenciaFunilRepository.findAllWithDetails();

        assertThat(resultado).hasSize(1);
        CadenciaFunil carregada = resultado.get(0);
        assertThat(carregada.getFunilOrigem().getNome()).isEqualTo("Funil Origem");
        assertThat(carregada.getEtapaOrigem().getNome()).isEqualTo("Etapa Origem");
        assertThat(carregada.getFunilDestino().getNome()).isEqualTo("Funil Destino");
        assertThat(carregada.getEtapaDestino().getNome()).isEqualTo("Etapa Destino");
    }

    @Test
    void findByIdCarregaAssociacoesViaEntityGraph() {
        Funil funilOrigem = funilRepository.save(criaFunil("Funil A"));
        Funil funilDestino = funilRepository.save(criaFunil("Funil B"));
        Etapa etapaOrigem = etapaRepository.save(criaEtapa("Etapa A", funilOrigem));
        Etapa etapaDestino = etapaRepository.save(criaEtapa("Etapa B", funilDestino));
        Long id = cadenciaFunilRepository
                .save(criaCadencia("Cadencia X", funilOrigem, etapaOrigem, funilDestino, etapaDestino))
                .getId();

        CadenciaFunil carregada = cadenciaFunilRepository.findById(id).orElseThrow();

        assertThat(carregada.getFunilOrigem().getNome()).isEqualTo("Funil A");
        assertThat(carregada.getEtapaDestino().getNome()).isEqualTo("Etapa B");
    }

    private CadenciaFunil criaCadencia(String nome, Funil funilOrigem, Etapa etapaOrigem,
                                        Funil funilDestino, Etapa etapaDestino) {
        CadenciaFunil cadencia = new CadenciaFunil();
        cadencia.setNome(nome);
        cadencia.setFunilOrigem(funilOrigem);
        cadencia.setEtapaOrigem(etapaOrigem);
        cadencia.setFunilDestino(funilDestino);
        cadencia.setEtapaDestino(etapaDestino);
        cadencia.setDiasNaEtapa(3);
        cadencia.setHorarioMovimentacao(LocalTime.of(8, 0));
        cadencia.setSituacao(Situacao.ATIVA);
        return cadencia;
    }

    private Funil criaFunil(String nome) {
        Funil funil = new Funil();
        funil.setNome(nome);
        return funil;
    }

    private Etapa criaEtapa(String nome, Funil funil) {
        Etapa etapa = new Etapa();
        etapa.setNome(nome);
        etapa.setFunil(funil);
        return etapa;
    }
}
