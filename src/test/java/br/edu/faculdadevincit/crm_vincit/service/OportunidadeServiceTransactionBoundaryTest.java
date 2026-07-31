package br.edu.faculdadevincit.crm_vincit.service;

import br.edu.faculdadevincit.crm_vincit.model.dtos.OportunidadeClienteRequest;
import br.edu.faculdadevincit.crm_vincit.model.dtos.OportunidadeCreateRequest;
import br.edu.faculdadevincit.crm_vincit.model.dtos.OportunidadeUpdateRequest;
import br.edu.faculdadevincit.crm_vincit.repository.EtapaRepository;
import br.edu.faculdadevincit.crm_vincit.repository.OportunidadeRepository;
import br.edu.faculdadevincit.crm_vincit.repository.ParticipanteRepository;
import br.edu.faculdadevincit.crm_vincit.repository.TagRepository;
import br.edu.faculdadevincit.crm_vincit.repository.UsuarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * O upload/delete de anexo no S3 não pode mais rodar dentro da transação @Transactional de
 * create/update (auditoria: "não mantenha transação aberta durante AWS S3"). Este teste garante
 * que a chamada ao S3 acontece antes de qualquer leitura/escrita no banco, delegando depois para
 * o método transacional só com a URL já resolvida.
 */
@ExtendWith(MockitoExtension.class)
class OportunidadeServiceTransactionBoundaryTest {

    @Mock private OportunidadeRepository oportunidadeRepository;
    @Mock private UsuarioRepository usuarioRepository;
    @Mock private ParticipanteRepository participanteRepository;
    @Mock private SimpMessagingTemplate messagingTemplate;
    @Mock private EtapaRepository etapaRepository;
    @Mock private EtapaService etapaService;
    @Mock private ParticipanteService participanteService;
    @Mock private S3Service s3Service;
    @Mock private TagRepository tagRepository;
    @Mock private MultipartFile file;

    private OportunidadeService oportunidadeService;

    @BeforeEach
    void setUp() {
        oportunidadeService = spy(new OportunidadeService());
        ReflectionTestUtils.setField(oportunidadeService, "oportunidadeRepository", oportunidadeRepository);
        ReflectionTestUtils.setField(oportunidadeService, "usuarioRepository", usuarioRepository);
        ReflectionTestUtils.setField(oportunidadeService, "participanteRepository", participanteRepository);
        ReflectionTestUtils.setField(oportunidadeService, "messagingTemplate", messagingTemplate);
        ReflectionTestUtils.setField(oportunidadeService, "etapaRepository", etapaRepository);
        ReflectionTestUtils.setField(oportunidadeService, "etapaService", etapaService);
        ReflectionTestUtils.setField(oportunidadeService, "participanteService", participanteService);
        ReflectionTestUtils.setField(oportunidadeService, "s3Service", s3Service);
        ReflectionTestUtils.setField(oportunidadeService, "tagRepository", tagRepository);
    }

    private OportunidadeCreateRequest createRequest() {
        return new OportunidadeCreateRequest(
                "Titulo", 1L, 2L,
                new OportunidadeClienteRequest(null, "Cliente", null, "11999999999"),
                BigDecimal.TEN, null, null, null, null, null, null, null);
    }

    private OportunidadeUpdateRequest updateRequest() {
        return new OportunidadeUpdateRequest(
                "Titulo", 1L, 2L,
                new OportunidadeClienteRequest(3L, "Cliente", null, "11999999999"),
                BigDecimal.TEN, null, null, null, null, null, null, null, null);
    }

    @Test
    void create_comArquivo_fazUploadNoS3AntesDeDelegarParaOMetodoTransacional() {
        when(file.isEmpty()).thenReturn(false);
        when(file.getSize()).thenReturn(100L);
        when(file.getContentType()).thenReturn("image/png");
        when(file.getOriginalFilename()).thenReturn("foto.png");
        when(s3Service.uploadFile(eq(file), anyString())).thenReturn("https://cdn.exemplo.com/imgetapa/x.png");
        doReturn(null).when(oportunidadeService).criarComAnexoResolvido(any(), anyString());

        oportunidadeService.create(createRequest(), file);

        InOrder ordem = inOrder(s3Service, oportunidadeService);
        ordem.verify(s3Service).uploadFile(eq(file), anyString());
        ordem.verify(oportunidadeService).criarComAnexoResolvido(createRequest(), "https://cdn.exemplo.com/imgetapa/x.png");

        // create() em si não deve ler/escrever no banco: isso é responsabilidade exclusiva do
        // método transacional (criarComAnexoResolvido), para o qual o upload já foi resolvido antes.
        verifyNoInteractions(etapaRepository, oportunidadeRepository, usuarioRepository);
    }

    @Test
    void create_semArquivo_naoChamaS3() {
        when(file.isEmpty()).thenReturn(true);
        doReturn(null).when(oportunidadeService).criarComAnexoResolvido(any(), any());

        oportunidadeService.create(createRequest(), file);

        verify(s3Service, never()).uploadFile(any(), anyString());
        verify(oportunidadeService).criarComAnexoResolvido(createRequest(), null);
    }

    @Test
    void update_resolveAnexoAntesDeAbrirATransacao() {
        when(oportunidadeRepository.findUrlAnexoById(5L)).thenReturn(Optional.of("https://cdn.exemplo.com/imgetapa/antigo.png"));
        when(file.isEmpty()).thenReturn(false);
        when(file.getSize()).thenReturn(100L);
        when(file.getContentType()).thenReturn("image/png");
        when(file.getOriginalFilename()).thenReturn("nova.png");
        when(s3Service.uploadFile(eq(file), anyString())).thenReturn("https://cdn.exemplo.com/imgetapa/nova.png");
        doReturn(null).when(oportunidadeService).atualizarComAnexoResolvido(eq(5L), any(), anyString());

        oportunidadeService.update(5L, updateRequest(), file);

        InOrder ordem = inOrder(oportunidadeRepository, s3Service, oportunidadeService);
        ordem.verify(oportunidadeRepository).findUrlAnexoById(5L);
        ordem.verify(s3Service).deleteFile(anyString());
        ordem.verify(s3Service).uploadFile(eq(file), anyString());
        ordem.verify(oportunidadeService).atualizarComAnexoResolvido(5L, updateRequest(), "https://cdn.exemplo.com/imgetapa/nova.png");

        // A leitura completa da entidade (findByIdWithDetails) só acontece dentro do método
        // transacional, depois que o S3 já terminou.
        verify(oportunidadeRepository, never()).findByIdWithDetails(any());
    }
}
