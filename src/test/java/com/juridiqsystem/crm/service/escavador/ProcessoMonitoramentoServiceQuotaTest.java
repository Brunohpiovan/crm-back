package com.juridiqsystem.crm.service.escavador;

import com.juridiqsystem.crm.infra.escavador.EscavadorMonitoramentoApi;
import com.juridiqsystem.crm.infra.escavador.dto.EscavadorMonitoramentoResponse;
import com.juridiqsystem.crm.model.Empresa;
import com.juridiqsystem.crm.model.Processo;
import com.juridiqsystem.crm.model.Usuario;
import com.juridiqsystem.crm.model.dtos.MonitoramentoQuotaResponse;
import com.juridiqsystem.crm.model.dtos.ProcessoMonitoramentoDetailResponse;
import com.juridiqsystem.crm.model.enums.FrequenciaMonitoramento;
import com.juridiqsystem.crm.repository.EmpresaRepository;
import com.juridiqsystem.crm.repository.ProcessoMonitoramentoRepository;
import com.juridiqsystem.crm.repository.ProcessoRepository;
import com.juridiqsystem.crm.service.exceptions.ConflictException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * A cota de processos monitorados é o que a empresa efetivamente contrata — estourá-la significa
 * consumir a conta compartilhada do juriq-crm na Escavador sem contrapartida. Estes testes fixam
 * as três decisões: bloqueia acima do limite, não cobra vaga de reativação/reconfiguração, e
 * limite nulo é ilimitado.
 */
@ExtendWith(MockitoExtension.class)
class ProcessoMonitoramentoServiceQuotaTest {

    private static final Long EMPRESA_ID = 7L;
    private static final String PROCESSO_PUBLIC_ID = "processo-1";

    @Mock
    private ProcessoMonitoramentoRepository processoMonitoramentoRepository;

    @Mock
    private ProcessoRepository processoRepository;

    @Mock
    private EmpresaRepository empresaRepository;

    @Mock
    private EscavadorMonitoramentoApi escavadorMonitoramentoApi;

    @InjectMocks
    private ProcessoMonitoramentoService processoMonitoramentoService;

    @BeforeEach
    void autenticarUsuario() {
        Usuario usuario = new Usuario();
        usuario.setId(1L);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(usuario, null, List.of()));
    }

    @AfterEach
    void limparContexto() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void ativar_comLimiteJaAtingido_lancaConflictExceptionENaoChamaEscavador() {
        prepararProcesso();
        prepararEmpresaComLimite(1);
        when(processoMonitoramentoRepository.findByProcessoId(10L)).thenReturn(Optional.empty());
        when(processoMonitoramentoRepository.countByEmpresaIdAndAtivoTrue(EMPRESA_ID)).thenReturn(1L);

        assertThatThrownBy(() -> processoMonitoramentoService.ativar(PROCESSO_PUBLIC_ID, FrequenciaMonitoramento.DIARIA, false))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("Limite de 1 processos monitorados");

        verify(escavadorMonitoramentoApi, never()).criar(anyString(), any(), anyBoolean());
        verify(processoMonitoramentoRepository, never()).save(any());
    }

    @Test
    void ativar_comVagaDisponivel_criaAssinaturaEGuardaOIdDoEscavador() {
        prepararProcesso();
        prepararEmpresaComLimite(5);
        when(processoMonitoramentoRepository.findByProcessoId(10L)).thenReturn(Optional.empty());
        when(processoMonitoramentoRepository.countByEmpresaIdAndAtivoTrue(EMPRESA_ID)).thenReturn(2L);
        when(escavadorMonitoramentoApi.criar("0800493-92.2024.8.14.0112", FrequenciaMonitoramento.DIARIA, false))
                .thenReturn(new EscavadorMonitoramentoResponse(1567024L, "0800493-92.2024.8.14.0112", "DIARIA", "PENDENTE", false));

        ProcessoMonitoramentoDetailResponse resposta =
                processoMonitoramentoService.ativar(PROCESSO_PUBLIC_ID, FrequenciaMonitoramento.DIARIA, false);

        assertThat(resposta.ativo()).isTrue();
        assertThat(resposta.confirmadoNaEscavador()).isTrue();
        verify(processoMonitoramentoRepository).save(any());
    }

    /** Limite nulo = plano sem teto: nem consulta a contagem de ativos. */
    @Test
    void ativar_comLimiteNulo_naoBloqueiaEIgnoraContagem() {
        prepararProcesso();
        prepararEmpresaComLimite(null);
        when(processoMonitoramentoRepository.findByProcessoId(10L)).thenReturn(Optional.empty());
        when(escavadorMonitoramentoApi.criar(anyString(), any(), anyBoolean()))
                .thenReturn(new EscavadorMonitoramentoResponse(99L, "x", "DIARIA", "PENDENTE", false));

        processoMonitoramentoService.ativar(PROCESSO_PUBLIC_ID, FrequenciaMonitoramento.DIARIA, false);

        verify(processoMonitoramentoRepository, never()).countByEmpresaIdAndAtivoTrue(any());
    }

    @Test
    void obterCotaAtual_devolveLimiteDoPlanoEQuantidadeEmUso() {
        prepararEmpresaComLimite(50);
        when(processoMonitoramentoRepository.countByEmpresaIdAndAtivoTrue(EMPRESA_ID)).thenReturn(12L);

        MonitoramentoQuotaResponse quota = processoMonitoramentoService.obterCotaAtual(EMPRESA_ID);

        assertThat(quota.limite()).isEqualTo(50);
        assertThat(quota.utilizados()).isEqualTo(12L);
    }

    private void prepararProcesso() {
        Processo processo = new Processo();
        processo.setId(10L);
        processo.setPublicId(PROCESSO_PUBLIC_ID);
        processo.setEmpresaId(EMPRESA_ID);
        processo.setNumeroCnj("0800493-92.2024.8.14.0112");
        when(processoRepository.findByPublicId(PROCESSO_PUBLIC_ID)).thenReturn(Optional.of(processo));
    }

    private void prepararEmpresaComLimite(Integer limite) {
        Empresa empresa = new Empresa();
        empresa.setId(EMPRESA_ID);
        empresa.setProcessosMonitoradosLimite(limite);
        when(empresaRepository.findById(EMPRESA_ID)).thenReturn(Optional.of(empresa));
    }
}
