package com.juridiqsystem.crm.service;

import com.juridiqsystem.crm.infra.security.TenantContext;
import com.juridiqsystem.crm.infra.security.logging.SecurityLogger;
import com.juridiqsystem.crm.model.Cargo;
import com.juridiqsystem.crm.model.dtos.CargoCreateRequest;
import com.juridiqsystem.crm.model.dtos.CargoResponse;
import com.juridiqsystem.crm.model.dtos.CargoUpdateRequest;
import com.juridiqsystem.crm.model.enums.Permissao;
import com.juridiqsystem.crm.repository.CargoRepository;
import com.juridiqsystem.crm.repository.UsuarioRepository;
import com.juridiqsystem.crm.service.exceptions.AccessDeniedException;
import com.juridiqsystem.crm.service.exceptions.ConflictException;
import com.juridiqsystem.crm.service.exceptions.ResourceNotFoundException;
import com.juridiqsystem.crm.testsupport.TestCargoFactory;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Trava as regras de proteção do cargo administrador no backend — a UI as espelha, mas quem
 * chamar a API direto (contornando a tela) precisa ser rejeitado do mesmo jeito — e as demais
 * regras de negócio de cargo: nome único por empresa, cargo novo nunca nasce administrador e
 * cargo com usuários vinculados não pode ser excluído.
 */
@ExtendWith(MockitoExtension.class)
class CargoServiceTest {

    private static final Long EMPRESA_ID = 7L;

    @Mock
    private CargoRepository cargoRepository;

    @Mock
    private UsuarioRepository usuarioRepository;

    @Mock
    private SecurityLogger securityLogger;

    @InjectMocks
    private CargoService cargoService;

    @BeforeEach
    void definirTenant() {
        TenantContext.set(EMPRESA_ID);
    }

    @AfterEach
    void limparTenant() {
        TenantContext.clear();
    }

    @Test
    void criar_cargoNovoNasceSemPrivilegioDeAdministrador() {
        when(cargoRepository.existsByEmpresaIdAndNomeIgnoreCase(EMPRESA_ID, "Advogado")).thenReturn(false);
        when(cargoRepository.save(any(Cargo.class))).thenAnswer(invocacao -> invocacao.getArgument(0));

        CargoResponse resposta = cargoService.criar(
                new CargoCreateRequest("Advogado", List.of(Permissao.GERENCIAR_TEMPLATE_EMAIL)));

        ArgumentCaptor<Cargo> captor = ArgumentCaptor.forClass(Cargo.class);
        verify(cargoRepository).save(captor.capture());
        assertThat(captor.getValue().isAdministrador()).isFalse();
        assertThat(captor.getValue().getPermissoes()).containsExactly(Permissao.GERENCIAR_TEMPLATE_EMAIL);
        assertThat(resposta.administrador()).isFalse();
    }

    @Test
    void criar_nomeJaUsadoNaEmpresa_ehRejeitado() {
        when(cargoRepository.existsByEmpresaIdAndNomeIgnoreCase(EMPRESA_ID, "Advogado")).thenReturn(true);

        assertThatThrownBy(() -> cargoService.criar(new CargoCreateRequest("Advogado", List.of())))
                .isInstanceOf(ConflictException.class);

        verify(cargoRepository, never()).save(any(Cargo.class));
    }

    @Test
    void atualizar_cargoAdministrador_ehRejeitadoMesmoContornandoAUI() {
        Cargo administrador = TestCargoFactory.administrador();
        when(cargoRepository.findByEmpresaIdAndPublicId(EMPRESA_ID, administrador.getPublicId()))
                .thenReturn(Optional.of(administrador));

        assertThatThrownBy(() -> cargoService.atualizar(administrador.getPublicId(),
                new CargoUpdateRequest("Administrador Turbinado", List.of(Permissao.GERENCIAR_TAG))))
                .isInstanceOf(AccessDeniedException.class);

        verify(cargoRepository, never()).save(any(Cargo.class));
    }

    @Test
    void atualizar_cargoCustomizado_substituiOConjuntoInteiroDePermissoes() {
        Cargo cargo = TestCargoFactory.comum("Advogado", Permissao.GERENCIAR_FUNIL, Permissao.GERENCIAR_TAG);
        when(cargoRepository.findByEmpresaIdAndPublicId(EMPRESA_ID, cargo.getPublicId()))
                .thenReturn(Optional.of(cargo));
        when(cargoRepository.existsByEmpresaIdAndNomeIgnoreCaseAndIdNot(anyLong(), anyString(), any()))
                .thenReturn(false);
        when(cargoRepository.save(any(Cargo.class))).thenAnswer(invocacao -> invocacao.getArgument(0));

        CargoResponse resposta = cargoService.atualizar(cargo.getPublicId(),
                new CargoUpdateRequest("Advogado", List.of(Permissao.GERENCIAR_TAG)));

        assertThat(resposta.permissoes()).containsExactly(Permissao.GERENCIAR_TAG);
    }

    @Test
    void excluir_cargoAdministrador_ehRejeitado() {
        Cargo administrador = TestCargoFactory.administrador();
        when(cargoRepository.findByEmpresaIdAndPublicId(EMPRESA_ID, administrador.getPublicId()))
                .thenReturn(Optional.of(administrador));

        assertThatThrownBy(() -> cargoService.excluir(administrador.getPublicId()))
                .isInstanceOf(AccessDeniedException.class);

        verify(cargoRepository, never()).delete(any(Cargo.class));
    }

    @Test
    void excluir_cargoComUsuariosVinculados_ehRejeitado() {
        Cargo cargo = TestCargoFactory.comum();
        cargo.setId(10L);
        when(cargoRepository.findByEmpresaIdAndPublicId(EMPRESA_ID, cargo.getPublicId()))
                .thenReturn(Optional.of(cargo));
        when(usuarioRepository.existsByCargoId(10L)).thenReturn(true);

        assertThatThrownBy(() -> cargoService.excluir(cargo.getPublicId()))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("Realoque");

        verify(cargoRepository, never()).delete(any(Cargo.class));
    }

    @Test
    void excluir_cargoDeOutraEmpresa_naoEhEncontrado() {
        when(cargoRepository.findByEmpresaIdAndPublicId(EMPRESA_ID, "cargo-de-outra-empresa"))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> cargoService.excluir("cargo-de-outra-empresa"))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void permissoesDisponiveis_exponhaTodoOCatalogoComRotulo() {
        assertThat(cargoService.permissoesDisponiveis())
                .hasSize(Permissao.values().length)
                .allSatisfy(permissao -> assertThat(permissao.rotulo()).isNotBlank());
    }
}
