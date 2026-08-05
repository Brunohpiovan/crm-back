package br.edu.faculdadevincit.crm_vincit.service;

import br.edu.faculdadevincit.crm_vincit.model.Etapa;
import br.edu.faculdadevincit.crm_vincit.model.Funil;
import br.edu.faculdadevincit.crm_vincit.repository.EtapaRepository;
import br.edu.faculdadevincit.crm_vincit.repository.FunilRepository;
import br.edu.faculdadevincit.crm_vincit.repository.OportunidadeRepository;
import br.edu.faculdadevincit.crm_vincit.repository.UsuarioRepository;
import br.edu.faculdadevincit.crm_vincit.service.exceptions.ConflictException;
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
        Etapa etapa = new Etapa();
        etapa.setId(10L);
        when(funilRepository.findById(1L)).thenReturn(Optional.of(funil));
        when(etapaRepository.findByFunilId(1L)).thenReturn(List.of(etapa));
        when(oportunidadeRepository.countPorEtapaIdIn(List.of(10L))).thenReturn(2L);

        assertThatThrownBy(() -> funilService.delete(1L))
                .isInstanceOf(ConflictException.class);

        verify(funilRepository, never()).delete(funil);
    }

    @Test
    void delete_funilSemOportunidades_apagaNormalmente() {
        Funil funil = new Funil();
        funil.setId(1L);
        when(funilRepository.findById(1L)).thenReturn(Optional.of(funil));
        when(etapaRepository.findByFunilId(1L)).thenReturn(List.of());

        funilService.delete(1L);

        verify(funilRepository).delete(funil);
    }
}
