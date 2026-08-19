package com.juridiqsystem.crm.service;

import com.juridiqsystem.crm.model.Etapa;
import com.juridiqsystem.crm.model.Funil;
import com.juridiqsystem.crm.repository.EtapaRepository;
import com.juridiqsystem.crm.repository.FunilRepository;
import com.juridiqsystem.crm.repository.OportunidadeRepository;
import com.juridiqsystem.crm.repository.UsuarioRepository;
import com.juridiqsystem.crm.service.exceptions.ConflictException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Cobre o item crítico C3 da auditoria (mesma causa raiz de EtapaServiceDeleteTest): excluir um
 * funil não pode mais apagar em cascata as oportunidades de suas etapas. O delete é bloqueado se
 * qualquer etapa do funil tiver oportunidade fora da lixeira.
 */
@ExtendWith(MockitoExtension.class)
class FunilServiceDeleteTest {

    @Mock
    private FunilRepository funilRepository;

    @Mock
    private UsuarioRepository usuarioRepository;

    @Mock
    private EtapaService etapaService;

    @Mock
    private EtapaRepository etapaRepository;

    @Mock
    private OportunidadeRepository oportunidadeRepository;

    @InjectMocks
    private FunilService funilService;

    @Test
    void delete_funilComOportunidadesEmSuasEtapas_lancaConflictExceptionENaoApaga() {
        Funil funil = new Funil();
        funil.setId(1L);
        funil.setPublicId("funil-1");
        Etapa etapa = new Etapa();
        etapa.setId(10L);
        when(funilRepository.findByPublicId("funil-1")).thenReturn(Optional.of(funil));
        when(etapaRepository.findByFunilIdOrderByPosicaoAscIdAsc(1L)).thenReturn(List.of(etapa));
        when(oportunidadeRepository.countPorEtapaIdIn(List.of(10L))).thenReturn(2L);

        assertThatThrownBy(() -> funilService.delete("funil-1"))
                .isInstanceOf(ConflictException.class);

        verify(funilRepository, never()).delete(funil);
    }

    @Test
    void delete_funilSemOportunidades_apagaNormalmente() {
        Funil funil = new Funil();
        funil.setId(1L);
        funil.setPublicId("funil-1");
        when(funilRepository.findByPublicId("funil-1")).thenReturn(Optional.of(funil));
        when(etapaRepository.findByFunilIdOrderByPosicaoAscIdAsc(1L)).thenReturn(List.of());

        funilService.delete("funil-1");

        verify(funilRepository).delete(funil);
    }
}
