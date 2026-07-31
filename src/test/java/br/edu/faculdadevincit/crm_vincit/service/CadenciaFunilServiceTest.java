package br.edu.faculdadevincit.crm_vincit.service;

import br.edu.faculdadevincit.crm_vincit.model.CadenciaFunil;
import br.edu.faculdadevincit.crm_vincit.model.Etapa;
import br.edu.faculdadevincit.crm_vincit.model.Oportunidade;
import br.edu.faculdadevincit.crm_vincit.model.enums.Situacao;
import br.edu.faculdadevincit.crm_vincit.repository.CadenciaFunilRepository;
import br.edu.faculdadevincit.crm_vincit.repository.EtapaRepository;
import br.edu.faculdadevincit.crm_vincit.repository.FunilRepository;
import br.edu.faculdadevincit.crm_vincit.repository.OportunidadeRepository;
import br.edu.faculdadevincit.crm_vincit.repository.SchedulerLockRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Cobre a correção do item "isole falha por item" da auditoria: processarCadenciasAtivas não pode
 * mais rodar em uma única transação gigante, então cada oportunidade tem que ser movida (e poder
 * falhar) de forma independente, o lock precisa impedir execução concorrente entre instâncias, e
 * uma oportunidade já movida não pode ser reprocessada.
 */
@ExtendWith(MockitoExtension.class)
class CadenciaFunilServiceTest {

    @Mock private CadenciaFunilRepository cadenciaFunilRepository;
    @Mock private FunilRepository funilRepository;
    @Mock private EtapaRepository etapaRepository;
    @Mock private OportunidadeService oportunidadeService;
    @Mock private OportunidadeRepository oportunidadeRepository;
    @Mock private SchedulerLockRepository schedulerLockRepository;

    private CadenciaFunilService cadenciaFunilService;

    @BeforeEach
    void setUp() {
        cadenciaFunilService = new CadenciaFunilService();
        setField("cadenciaFunilRepository", cadenciaFunilRepository);
        setField("funilRepository", funilRepository);
        setField("etapaRepository", etapaRepository);
        setField("oportunidadeService", oportunidadeService);
        setField("oportunidadeRepository", oportunidadeRepository);
        setField("schedulerLockRepository", schedulerLockRepository);
    }

    private void setField(String nome, Object valor) {
        org.springframework.test.util.ReflectionTestUtils.setField(cadenciaFunilService, nome, valor);
    }

    private Etapa etapa(Long id) {
        Etapa etapa = new Etapa();
        etapa.setId(id);
        return etapa;
    }

    private Oportunidade oportunidade(Long id, Etapa etapaAtual) {
        Oportunidade oportunidade = new Oportunidade();
        oportunidade.setId(id);
        oportunidade.setEtapa(etapaAtual);
        return oportunidade;
    }

    private CadenciaFunil cadenciaAgora(String nome, Etapa origem, Etapa destino) {
        CadenciaFunil cadencia = new CadenciaFunil();
        cadencia.setNome(nome);
        cadencia.setEtapaOrigem(origem);
        cadencia.setEtapaDestino(destino);
        cadencia.setDiasNaEtapa(3);
        cadencia.setSituacao(Situacao.ATIVA);
        cadencia.setHorarioMovimentacao(LocalTime.now().truncatedTo(ChronoUnit.MINUTES));
        return cadencia;
    }

    @Test
    void semLockAdquirido_naoProcessaNemLibera() {
        when(schedulerLockRepository.tentarAdquirir(anyString(), any(), any())).thenReturn(0);

        cadenciaFunilService.processarCadenciasAtivas();

        verify(cadenciaFunilRepository, never()).findAllBySituacaoWithDetails(any());
        verify(schedulerLockRepository, never()).liberar(anyString());
    }

    @Test
    void comLockAdquirido_processaELiberaOLockAoFinal() {
        when(schedulerLockRepository.tentarAdquirir(anyString(), any(), any())).thenReturn(1);
        when(cadenciaFunilRepository.findAllBySituacaoWithDetails(Situacao.ATIVA)).thenReturn(List.of());

        cadenciaFunilService.processarCadenciasAtivas();

        verify(schedulerLockRepository, times(1)).liberar("movimentacao_cadencia");
    }

    @Test
    void falhaEmUmaOportunidade_naoImpedeProcessamentoDasDemais() {
        Etapa origem = etapa(1L);
        Etapa destino = etapa(2L);
        CadenciaFunil cadencia = cadenciaAgora("Cadencia Teste", origem, destino);

        Oportunidade comFalha = oportunidade(10L, origem);
        Oportunidade comSucesso = oportunidade(11L, origem);

        when(schedulerLockRepository.tentarAdquirir(anyString(), any(), any())).thenReturn(1);
        when(cadenciaFunilRepository.findAllBySituacaoWithDetails(Situacao.ATIVA)).thenReturn(List.of(cadencia));
        when(oportunidadeRepository.findElegiveisParaMovimentacao(eq(1L), any(LocalDateTime.class)))
                .thenReturn(List.of(comFalha, comSucesso));

        // A oportunidade 10 "some" entre a consulta de elegíveis e a movimentação em si (ex.: foi
        // excluída concorrentemente) — simula falha real de um item do lote.
        when(oportunidadeRepository.findByIdWithDetails(10L)).thenReturn(Optional.empty());
        when(oportunidadeRepository.findByIdWithDetails(11L)).thenReturn(Optional.of(comSucesso));
        when(etapaRepository.findById(2L)).thenReturn(Optional.of(destino));
        when(oportunidadeRepository.countByEtapa(destino)).thenReturn(0L);

        assertThatCode(() -> cadenciaFunilService.processarCadenciasAtivas()).doesNotThrowAnyException();

        verify(oportunidadeService, never()).movimentarOportunidadeCarregada(eq(comFalha), anyLong(), ArgumentMatchers.anyInt());
        verify(oportunidadeService, times(1)).movimentarOportunidadeCarregada(eq(comSucesso), eq(2L), ArgumentMatchers.anyInt());
        verify(schedulerLockRepository, times(1)).liberar("movimentacao_cadencia");
    }

    @Test
    void oportunidadeJaNaEtapaDestino_naoEReprocessada() {
        Etapa origem = etapa(1L);
        Etapa destino = etapa(2L);
        CadenciaFunil cadencia = cadenciaAgora("Cadencia Teste", origem, destino);

        // findElegiveisParaMovimentacao já filtra por etapa de origem, mas simulamos o caso em que,
        // entre a consulta e o processamento, a oportunidade foi movida (ex.: pelo usuário, na UI)
        // para a própria etapa destino — não deve ser movida de novo.
        Oportunidade jaMovida = oportunidade(20L, destino);

        when(schedulerLockRepository.tentarAdquirir(anyString(), any(), any())).thenReturn(1);
        when(cadenciaFunilRepository.findAllBySituacaoWithDetails(Situacao.ATIVA)).thenReturn(List.of(cadencia));
        when(oportunidadeRepository.findElegiveisParaMovimentacao(eq(1L), any(LocalDateTime.class)))
                .thenReturn(List.of(jaMovida));
        when(oportunidadeRepository.findByIdWithDetails(20L)).thenReturn(Optional.of(jaMovida));

        cadenciaFunilService.processarCadenciasAtivas();

        verify(oportunidadeService, never()).movimentarOportunidadeCarregada(any(), anyLong(), ArgumentMatchers.anyInt());
    }
}
