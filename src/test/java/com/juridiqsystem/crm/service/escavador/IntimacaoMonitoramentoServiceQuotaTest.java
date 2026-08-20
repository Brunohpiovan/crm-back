package com.juridiqsystem.crm.service.escavador;

import com.juridiqsystem.crm.infra.escavador.EscavadorMonitoramentoDiarioApi;
import com.juridiqsystem.crm.infra.escavador.dto.EscavadorMonitoramentoDiarioResponse;
import com.juridiqsystem.crm.infra.escavador.dto.EscavadorOrigemDiario;
import com.juridiqsystem.crm.infra.escavador.dto.EscavadorOrigemGrupo;
import com.juridiqsystem.crm.infra.security.TenantContext;
import com.juridiqsystem.crm.model.Empresa;
import com.juridiqsystem.crm.model.Usuario;
import com.juridiqsystem.crm.model.dtos.IntimacaoMonitoramentoResponse;
import com.juridiqsystem.crm.model.dtos.MonitoramentoQuotaResponse;
import com.juridiqsystem.crm.model.enums.Uf;
import com.juridiqsystem.crm.repository.EmpresaRepository;
import com.juridiqsystem.crm.repository.IntimacaoMonitoramentoRepository;
import com.juridiqsystem.crm.repository.UsuarioRepository;
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
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Mirror de ProcessoMonitoramentoServiceQuotaTest, mas para a cota de OABs monitoradas
 * (Empresa.intimacoesMonitoradasLimite): item 3 do checklist do prompt — "com
 * intimacoesMonitoradasLimite = 1, ativar duas OABs diferentes deve rejeitar a segunda com
 * mensagem clara".
 */
@ExtendWith(MockitoExtension.class)
class IntimacaoMonitoramentoServiceQuotaTest {

    private static final Long EMPRESA_ID = 7L;

    @Mock
    private IntimacaoMonitoramentoRepository intimacaoMonitoramentoRepository;

    @Mock
    private UsuarioRepository usuarioRepository;

    @Mock
    private EmpresaRepository empresaRepository;

    @Mock
    private EscavadorMonitoramentoDiarioApi escavadorMonitoramentoDiarioApi;

    @InjectMocks
    private IntimacaoMonitoramentoService intimacaoMonitoramentoService;

    @BeforeEach
    void autenticarUsuarioEDefinirTenant() {
        Usuario usuario = new Usuario();
        usuario.setId(1L);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(usuario, null, List.of()));
        TenantContext.set(EMPRESA_ID);
    }

    @AfterEach
    void limparContexto() {
        SecurityContextHolder.clearContext();
        TenantContext.clear();
    }

    @Test
    void ativar_comDuasOabsELimiteDeUma_rejeitaASegundaComMensagemClara() {
        prepararEmpresaComLimite(1);
        when(intimacaoMonitoramentoRepository.findByOabNumeroAndOabUf(anyString(), any())).thenReturn(Optional.empty());
        when(intimacaoMonitoramentoRepository.countByEmpresaIdAndAtivoTrue(EMPRESA_ID)).thenReturn(1L);

        assertThatThrownBy(() -> intimacaoMonitoramentoService.ativar("222222", Uf.RJ, null))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("Limite de 1 OABs monitoradas");

        verify(escavadorMonitoramentoDiarioApi, never()).criar(anyString(), any());
        verify(intimacaoMonitoramentoRepository, never()).save(any());
    }

    @Test
    void ativar_comVagaDisponivel_criaAssinaturaEGuardaOIdDoEscavador() {
        prepararEmpresaComLimite(5);
        prepararOrigens();
        when(intimacaoMonitoramentoRepository.findByOabNumeroAndOabUf("111111", Uf.SP)).thenReturn(Optional.empty());
        when(intimacaoMonitoramentoRepository.countByEmpresaIdAndAtivoTrue(EMPRESA_ID)).thenReturn(0L);
        when(escavadorMonitoramentoDiarioApi.criar(eq("111111"), any()))
                .thenReturn(new EscavadorMonitoramentoDiarioResponse(1567024L, "111111", "TERMO", 0, 1, null));

        IntimacaoMonitoramentoResponse resposta = intimacaoMonitoramentoService.ativar("111111", Uf.SP, null);

        assertThat(resposta.ativo()).isTrue();
        assertThat(resposta.confirmadoNaEscavador()).isTrue();
        verify(intimacaoMonitoramentoRepository).save(any());
    }

    /** Limite nulo = plano sem teto: nem consulta a contagem de ativos. */
    @Test
    void ativar_comLimiteNulo_naoBloqueiaEIgnoraContagem() {
        prepararEmpresaComLimite(null);
        prepararOrigens();
        when(intimacaoMonitoramentoRepository.findByOabNumeroAndOabUf(anyString(), any())).thenReturn(Optional.empty());
        when(escavadorMonitoramentoDiarioApi.criar(anyString(), any()))
                .thenReturn(new EscavadorMonitoramentoDiarioResponse(99L, "x", "TERMO", 0, 0, null));

        intimacaoMonitoramentoService.ativar("333333", Uf.MG, null);

        verify(intimacaoMonitoramentoRepository, never()).countByEmpresaIdAndAtivoTrue(any());
    }

    @Test
    void obterCotaAtual_devolveLimiteDoPlanoEQuantidadeEmUso() {
        prepararEmpresaComLimite(50);
        when(intimacaoMonitoramentoRepository.countByEmpresaIdAndAtivoTrue(EMPRESA_ID)).thenReturn(12L);

        MonitoramentoQuotaResponse quota = intimacaoMonitoramentoService.obterCotaAtual(EMPRESA_ID);

        assertThat(quota.limite()).isEqualTo(50);
        assertThat(quota.utilizados()).isEqualTo(12L);
    }

    private void prepararOrigens() {
        when(escavadorMonitoramentoDiarioApi.listarOrigens()).thenReturn(List.of(
                new EscavadorOrigemGrupo("São Paulo", List.of(
                        new EscavadorOrigemDiario(5, "TRT da 2ª Região", "TRT-2", "SP", "Tribunais Regionais do Trabalho")))));
    }

    private void prepararEmpresaComLimite(Integer limite) {
        Empresa empresa = new Empresa();
        empresa.setId(EMPRESA_ID);
        empresa.setIntimacoesMonitoradasLimite(limite);
        when(empresaRepository.findById(EMPRESA_ID)).thenReturn(Optional.of(empresa));
    }
}
