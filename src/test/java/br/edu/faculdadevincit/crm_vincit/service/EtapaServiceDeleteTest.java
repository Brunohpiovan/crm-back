package br.edu.faculdadevincit.crm_vincit.service;

import br.edu.faculdadevincit.crm_vincit.model.Etapa;
import br.edu.faculdadevincit.crm_vincit.repository.EtapaRepository;
import br.edu.faculdadevincit.crm_vincit.repository.FunilRepository;
import br.edu.faculdadevincit.crm_vincit.repository.OportunidadeRepository;
import br.edu.faculdadevincit.crm_vincit.service.exceptions.ConflictException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Cobre o item crítico C3 da auditoria: EtapaService.delete usava
 * cascade=ALL + orphanRemoval=true sem checagem, apagando fisicamente oportunidades (inclusive
 * GANHO) junto com a etapa. Agora o delete é bloqueado se a etapa tiver qualquer oportunidade
 * fora da lixeira.
 */
@ExtendWith(MockitoExtension.class)
class EtapaServiceDeleteTest {

    @Mock
    private EtapaRepository etapaRepository;

    @Mock
    private FunilRepository funilRepository;

    @Mock
    private OportunidadeRepository oportunidadeRepository;

    @Mock
    private SimpMessagingTemplate messagingTemplate;

    @InjectMocks
    private EtapaService etapaService;

    @Test
    void delete_etapaComOportunidades_lancaConflictExceptionENaoApaga() {
        Etapa etapa = new Etapa();
        etapa.setId(1L);
        when(etapaRepository.findById(1L)).thenReturn(Optional.of(etapa));
        when(oportunidadeRepository.countPorEtapaIdIn(List.of(1L))).thenReturn(3L);

        assertThatThrownBy(() -> etapaService.delete(1L))
                .isInstanceOf(ConflictException.class);

        verify(etapaRepository, never()).delete(any());
        verify(messagingTemplate, never()).convertAndSend(anyString(), any(Object.class));
    }

    @Test
    void delete_etapaSemOportunidades_apagaNormalmente() {
        Etapa etapa = new Etapa();
        etapa.setId(1L);
        when(etapaRepository.findById(1L)).thenReturn(Optional.of(etapa));
        when(oportunidadeRepository.countPorEtapaIdIn(List.of(1L))).thenReturn(0L);

        etapaService.delete(1L);

        verify(etapaRepository).delete(etapa);
    }
}
