package br.edu.faculdadevincit.crm_vincit.repository;

import br.edu.faculdadevincit.crm_vincit.model.Etapa;
import br.edu.faculdadevincit.crm_vincit.model.Funil;
import br.edu.faculdadevincit.crm_vincit.model.Oportunidade;
import br.edu.faculdadevincit.crm_vincit.model.enums.SituacaoOportunidade;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.TestPropertySource;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * OportunidadeRepository.findElegiveisParaMovimentacao passou a usar LEFT JOIN FETCH
 * (etapa/criador/cliente/tags) para que o scheduler de cadência (CadenciaFunilService) reaproveite
 * a entidade já carregada em vez de buscá-la de novo via findByIdWithDetails. Este teste garante
 * que o filtro por etapa e data-limite continua correto após a mudança na query.
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:oportunidade_repo_test;DB_CLOSE_DELAY=-1;MODE=MySQL",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.flyway.enabled=false",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
class OportunidadeRepositoryTest {

    @Autowired
    private OportunidadeRepository oportunidadeRepository;

    @Autowired
    private EtapaRepository etapaRepository;

    @Autowired
    private FunilRepository funilRepository;

    @Test
    void findElegiveisParaMovimentacaoFiltraPorEtapaEDataLimite() {
        Funil funil = new Funil();
        funil.setNome("Funil Cadencia");
        funilRepository.save(funil);

        Etapa etapaOrigem = new Etapa();
        etapaOrigem.setNome("Origem");
        etapaOrigem.setFunil(funil);
        etapaRepository.save(etapaOrigem);

        Etapa outraEtapa = new Etapa();
        outraEtapa.setNome("Outra");
        outraEtapa.setFunil(funil);
        etapaRepository.save(outraEtapa);

        LocalDateTime limite = LocalDateTime.now().minusDays(3);

        Oportunidade elegivel = oportunidadeRepository.save(criaOportunidade(etapaOrigem, limite.minusDays(1)));
        oportunidadeRepository.save(criaOportunidade(etapaOrigem, limite.plusDays(1)));
        oportunidadeRepository.save(criaOportunidade(outraEtapa, limite.minusDays(1)));

        List<Oportunidade> resultado =
                oportunidadeRepository.findElegiveisParaMovimentacao(etapaOrigem.getId(), limite);

        assertThat(resultado)
                .extracting(Oportunidade::getId)
                .containsExactly(elegivel.getId());
        assertThat(resultado.get(0).getEtapa().getNome()).isEqualTo("Origem");
    }

    private Oportunidade criaOportunidade(Etapa etapa, LocalDateTime dataEntradaEtapa) {
        Oportunidade oportunidade = new Oportunidade();
        oportunidade.setTitulo("Card Teste");
        oportunidade.setEtapa(etapa);
        oportunidade.setSituacao(SituacaoOportunidade.ABERTO);
        oportunidade.setIndice(0);
        oportunidade.setDataEntradaEtapa(dataEntradaEtapa);
        return oportunidade;
    }
}
