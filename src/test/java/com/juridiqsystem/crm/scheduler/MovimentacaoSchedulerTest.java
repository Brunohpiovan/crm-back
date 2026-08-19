package com.juridiqsystem.crm.scheduler;

import com.juridiqsystem.crm.service.CadenciaFunilService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Mockito.doThrow;

/**
 * Uma falha estrutural (ex.: lock de banco indisponível) não pode derrubar o processo do
 * @Scheduled: precisa ser logada e o próximo tick (1 minuto depois) precisa continuar rodando
 * normalmente, então o método do scheduler nunca pode propagar exceção.
 */
@ExtendWith(MockitoExtension.class)
class MovimentacaoSchedulerTest {

    @Mock
    private CadenciaFunilService cadenciaFunilService;

    private MovimentacaoScheduler scheduler;

    @BeforeEach
    void setUp() {
        scheduler = new MovimentacaoScheduler();
        ReflectionTestUtils.setField(scheduler, "cadenciaService", cadenciaFunilService);
    }

    @Test
    void falhaInesperadaNoProcessamento_naoPropagaExcecao() {
        doThrow(new RuntimeException("falha estrutural simulada")).when(cadenciaFunilService).processarCadenciasAtivas();

        assertThatCode(() -> scheduler.executar()).doesNotThrowAnyException();
    }
}
