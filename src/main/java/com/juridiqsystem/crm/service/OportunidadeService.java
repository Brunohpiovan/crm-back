package com.juridiqsystem.crm.service;

import com.juridiqsystem.crm.infra.security.TenantContext;
import com.juridiqsystem.crm.model.Etapa;
import com.juridiqsystem.crm.model.Oportunidade;
import com.juridiqsystem.crm.model.OportunidadeHistorico;
import com.juridiqsystem.crm.model.Participante;
import com.juridiqsystem.crm.model.Tag;
import com.juridiqsystem.crm.model.Usuario;
import com.juridiqsystem.crm.model.dtos.OportunidadeClienteRequest;
import com.juridiqsystem.crm.model.dtos.OportunidadeCreateRequest;
import com.juridiqsystem.crm.model.dtos.OportunidadeDTO;
import com.juridiqsystem.crm.model.dtos.OportunidadeHistoricoDTO;
import com.juridiqsystem.crm.model.dtos.OportunidadeMovimentoDTO;
import com.juridiqsystem.crm.model.dtos.OportunidadeUpdateRequest;
import com.juridiqsystem.crm.model.dtos.UsuarioContatoDto;
import com.juridiqsystem.crm.model.enums.Origem;
import com.juridiqsystem.crm.model.enums.SituacaoOportunidade;
import com.juridiqsystem.crm.model.enums.TipoEventoOportunidade;
import com.juridiqsystem.crm.model.enums.TipoParticipante;
import com.juridiqsystem.crm.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class OportunidadeService {

    private static final long MAX_SIZE_BYTES = 100L * 1024 * 1024;
    private static final Set<String> ALLOWED_CONTENT_TYPES =
            Set.of("image/png", "image/jpeg", "image/jpg", "image/webp", "image/gif");

    @Autowired
    private OportunidadeRepository oportunidadeRepository;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private ParticipanteRepository participanteRepository;

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    @Autowired
    private EtapaRepository etapaRepository;

    @Autowired
    private EtapaService etapaService;

    @Autowired
    private ParticipanteService participanteService;

    @Autowired
    private S3Service s3Service;

    @Autowired
    private TagRepository tagRepository;

    @Autowired
    private OportunidadeHistoricoRepository oportunidadeHistoricoRepository;

    // Auto-injeção do proxy do Spring: criarComAnexoResolvido/atualizarComAnexoResolvido só rodam
    // numa transação de verdade se forem chamados através do proxy. Chamar via "this" dentro da
    // própria classe (create/update abaixo) pula o proxy e o @Transactional não tem nenhum efeito.
    @Lazy
    @Autowired
    private OportunidadeService self;

    public OportunidadeDTO findByClienteAndCriadorNull(String clienteId) {
        Participante cliente = participanteRepository.findByPublicId(clienteId)
                .orElseThrow(() -> new RuntimeException("Cliente não encontrado"));
        return oportunidadeRepository.findByClienteIdAndCriadorIsNull(cliente.getId())
                .map(this::toDto)
                .orElse(null);
    }

    public Page<OportunidadeDTO> findAll(Pageable pageable) {
        return oportunidadeRepository.findAllWithDetails(pageable).map(this::toDto);
    }

    public OportunidadeDTO findById(String id) {
        Oportunidade oportunidade = oportunidadeRepository.findByPublicIdWithDetails(id)
                .orElseThrow(() -> new RuntimeException("Oportunidade com id " + id + " nao encontrada"));
        return toDto(oportunidade);
    }

    /**
     * O upload ao S3 roda antes de abrir a transação (e o delete de anexo antigo, em {@link #update},
     * depois de fechá-la) para que a conexão/transação JPA nunca fique presa esperando uma chamada de rede.
     */
    public OportunidadeDTO create(OportunidadeCreateRequest request, MultipartFile file) {
        String urlAnexo = null;
        if (file != null && !file.isEmpty()) {
            validarArquivo(file);
            urlAnexo = uploadArquivo(file);
        }
        return self.criarComAnexoResolvido(request, urlAnexo);
    }

    @Transactional
    OportunidadeDTO criarComAnexoResolvido(OportunidadeCreateRequest request, String urlAnexo) {
        Etapa etapa = etapaRepository.findByPublicId(request.etapaId())
                .orElseThrow(() -> new RuntimeException("Etapa não encontrado"));
        Usuario criador = usuarioRepository.findByPublicId(request.criadorId())
                .orElseThrow(() -> new RuntimeException("Dono não encontrado"));

        List<Tag> tags = resolveTags(request.tagIds());
        Participante cliente = resolveCliente(request.cliente());

        Oportunidade oportunidade = new Oportunidade();
        oportunidade.setTitulo(request.titulo());
        oportunidade.setEtapa(etapa);
        oportunidade.setCriador(criador);
        oportunidade.setCliente(cliente);
        oportunidade.setValor(request.valor());
        oportunidade.setData_criacao(request.dataCriacao() != null ? request.dataCriacao() : LocalDateTime.now());
        oportunidade.setOrigem(request.origem());
        oportunidade.setInteresse(request.interesse());
        oportunidade.setDescricao(request.descricao());
        oportunidade.setObservacoes(request.observacoes());
        oportunidade.setSituacao(request.situacao() != null ? request.situacao() : SituacaoOportunidade.ABERTO);
        oportunidade.setTags(tags);
        oportunidade.setIndice(0);
        oportunidade.setDataEntradaEtapa(LocalDateTime.now());
        oportunidade.setUrl_anexo(urlAnexo);

        List<Oportunidade> oportunidadesNaEtapa = oportunidadeRepository.findCardsByEtapaId(etapa.getId());
        oportunidadesNaEtapa.forEach(op -> op.setIndice(op.getIndice() + 1));
        oportunidadeRepository.saveAll(oportunidadesNaEtapa);

        etapaService.updateAddValor(etapa.getId(), oportunidade.getValor());
        Oportunidade salva = oportunidadeRepository.save(oportunidade);
        registrarHistorico(salva.getId(), TipoEventoOportunidade.CRIACAO, "criou esta oportunidade");

        OportunidadeDTO dto = toDto(salva);
        afterCommit(() -> messagingTemplate.convertAndSend("/topic/newoportunidade", dto));
        return dto;
    }

    private Participante resolveCliente(OportunidadeClienteRequest clienteRequest) {
        Optional<Participante> existenteOpt = participanteRepository.findByCelular(clienteRequest.celular());
        if (existenteOpt.isPresent()) {
            Participante existente = existenteOpt.get();
            existente.setTipoParticipante(TipoParticipante.PARTICIPANTE);
            if (existente.getLogin() != null) {
                return existente;
            }
            Participante dadosRequest = new Participante();
            dadosRequest.setNome(clienteRequest.nome());
            dadosRequest.setLogin(clienteRequest.login());
            dadosRequest.setCelular(clienteRequest.celular());
            dadosRequest.setTipoParticipante(TipoParticipante.PARTICIPANTE);
            return participanteService.preencheParticipante(dadosRequest, existente);
        }
        Participante novo = new Participante();
        novo.setNome(clienteRequest.nome());
        novo.setLogin(clienteRequest.login());
        novo.setCelular(clienteRequest.celular());
        return createParticipante(novo);
    }

    private Participante createParticipante(Participante participante) {
        Participante novo = new Participante();
        novo.setNome(participante.getNome());
        novo.setLogin(participante.getLogin());
        novo.setUrlPicture("assets/img/avatar/padrao.jpeg");
        novo.setCelular(participante.getCelular());
        novo.setTipoParticipante(TipoParticipante.PARTICIPANTE);
        Participante salvo = participanteRepository.save(novo);
        UsuarioContatoDto contatoDto = new UsuarioContatoDto(salvo.getPublicId(), salvo.getNome(), salvo.getUrlPicture());
        Long empresaId = TenantContext.get();
        afterCommit(() -> messagingTemplate.convertAndSend("/topic/empresa/" + empresaId + "/usuarios", contatoDto));
        return salvo;
    }

    private List<Tag> resolveTags(List<String> tagIds) {
        if (tagIds == null || tagIds.isEmpty()) {
            return new ArrayList<>();
        }
        List<Tag> tags = tagRepository.findAllByPublicIdIn(tagIds);
        if (tags.size() != new HashSet<>(tagIds).size()) {
            throw new RuntimeException("Uma ou mais tags informadas não foram encontradas");
        }
        return tags;
    }

    public OportunidadeDTO update(String id, OportunidadeUpdateRequest request, MultipartFile file) {
        Oportunidade oportunidadeBanco = oportunidadeRepository.findByPublicId(id)
                .orElseThrow(() -> new RuntimeException("Oportunidade com id " + id + " nao encontrada"));
        String urlAnexoFinal = resolveAnexo(oportunidadeBanco.getUrl_anexo(), request.urlAnexo(), file);
        return self.atualizarComAnexoResolvido(oportunidadeBanco.getId(), request, urlAnexoFinal);
    }

    @Transactional
    OportunidadeDTO atualizarComAnexoResolvido(Long id, OportunidadeUpdateRequest request, String urlAnexoFinal) {
        Oportunidade oportunidadeBanco = oportunidadeRepository.findByIdWithDetails(id)
                .orElseThrow(() -> new RuntimeException("Oportunidade com id " + id + " nao encontrada"));
        Etapa novaEtapa = etapaRepository.findByPublicId(request.etapaId())
                .orElseThrow(() -> new RuntimeException("Etapa não encontrado"));
        Usuario criador = usuarioRepository.findByPublicId(request.criadorId())
                .orElseThrow(() -> new RuntimeException("Dono não encontrado"));
        Participante clienteBanco = participanteRepository.findByPublicId(request.cliente().id())
                .orElseThrow(() -> new RuntimeException("Cliente não encontrado"));

        Etapa etapaAntiga = oportunidadeBanco.getEtapa();
        BigDecimal valorAntigo = oportunidadeBanco.getValor();
        boolean etapaChanged = etapaAntiga == null || !etapaAntiga.getId().equals(novaEtapa.getId());
        BigDecimal diferenca = request.valor().subtract(valorAntigo);

        if (etapaChanged) {
            etapaService.updateAddValor(novaEtapa.getId(), request.valor());
            if (etapaAntiga != null) {
                etapaService.updateSubValor(etapaAntiga.getId(), valorAntigo);
            }
        } else if (diferenca.compareTo(BigDecimal.ZERO) != 0) {
            if (diferenca.compareTo(BigDecimal.ZERO) > 0) {
                etapaService.updateAddValor(novaEtapa.getId(), diferenca);
            } else {
                etapaService.updateSubValor(novaEtapa.getId(), diferenca.abs());
            }
        }

        List<Tag> tags = resolveTags(request.tagIds());
        Participante clienteAtualizado = preencheCliente(clienteBanco, request.cliente());
        participanteRepository.save(clienteAtualizado);

        oportunidadeBanco.setTitulo(request.titulo());
        oportunidadeBanco.setEtapa(novaEtapa);
        oportunidadeBanco.setCriador(criador);
        oportunidadeBanco.setCliente(clienteAtualizado);
        oportunidadeBanco.setValor(request.valor());
        if (oportunidadeBanco.getData_criacao() == null) {
            oportunidadeBanco.setData_criacao(request.dataCriacao() != null ? request.dataCriacao() : LocalDateTime.now());
        }
        oportunidadeBanco.setUrl_anexo(urlAnexoFinal);
        oportunidadeBanco.setOrigem(request.origem());
        oportunidadeBanco.setInteresse(request.interesse());
        oportunidadeBanco.setDescricao(Origem.OUTRO.equals(request.origem()) ? request.descricao() : null);
        oportunidadeBanco.setObservacoes(request.observacoes());
        oportunidadeBanco.setSituacao(request.situacao());
        oportunidadeBanco.setTags(tags);
        if (etapaChanged) {
            oportunidadeBanco.setDataEntradaEtapa(LocalDateTime.now());
        }
        oportunidadeBanco.setAtualizadoEm(LocalDateTime.now());

        Oportunidade salva = oportunidadeRepository.save(oportunidadeBanco);

        if (etapaChanged && etapaAntiga != null) {
            registrarHistorico(salva.getId(), TipoEventoOportunidade.MOVIMENTACAO,
                    "moveu de '" + etapaAntiga.getNome() + "' para '" + novaEtapa.getNome() + "'");
        } else {
            registrarHistorico(salva.getId(), TipoEventoOportunidade.EDICAO, "editou os dados desta oportunidade");
        }

        OportunidadeDTO dto = toDto(salva);
        String topic = etapaChanged ? "/topic/newoportunidade" : "/topic/updateOportunidade";
        afterCommit(() -> messagingTemplate.convertAndSend(topic, dto));
        return dto;
    }

    private String resolveAnexo(String urlAnexoAtual, String urlAnexoRequest, MultipartFile file) {
        boolean existiaAnexo = urlAnexoAtual != null && !urlAnexoAtual.isEmpty();
        if (existiaAnexo) {
            if (urlAnexoRequest == null && (file == null || file.isEmpty())) {
                s3Service.deleteFile(getFileKeyFromUrl(urlAnexoAtual));
                return null;
            }
            if (file != null && !file.isEmpty()) {
                validarArquivo(file);
                s3Service.deleteFile(getFileKeyFromUrl(urlAnexoAtual));
                return uploadArquivo(file);
            }
            return urlAnexoAtual;
        }
        if (file != null && !file.isEmpty()) {
            validarArquivo(file);
            return uploadArquivo(file);
        }
        return null;
    }

    private String getFileKeyFromUrl(String url) {
        int idx = url.indexOf("imgetapa/");
        if (idx < 0) {
            throw new RuntimeException("URL de anexo inválida: " + url);
        }
        return url.substring(idx);
    }

    private void validarArquivo(MultipartFile file) {
        if (file.getSize() > MAX_SIZE_BYTES) {
            throw new RuntimeException("O arquivo selecionado excede o limite de 100 MB.");
        }
        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_CONTENT_TYPES.contains(contentType.toLowerCase())) {
            throw new RuntimeException("Tipo de arquivo não permitido. Envie uma imagem (PNG, JPG, JPEG, WEBP ou GIF).");
        }
    }

    private String uploadArquivo(MultipartFile file) {
        String key = "imgetapa/" + UUID.randomUUID() + extrairExtensao(file.getOriginalFilename());
        return s3Service.uploadFile(file, key);
    }

    private String extrairExtensao(String nomeOriginal) {
        if (nomeOriginal == null) {
            return "";
        }
        int idx = nomeOriginal.lastIndexOf('.');
        return idx >= 0 ? nomeOriginal.substring(idx) : "";
    }

    @Transactional
    public void movimentoOportunidade(String oportunidadeId, String etapaId, int novoIndice) {
        Oportunidade oportunidade = oportunidadeRepository.findByPublicIdWithDetails(oportunidadeId)
                .orElseThrow(() -> new RuntimeException("Oportunidade não encontrada"));
        Etapa etapaDestino = etapaRepository.findByPublicId(etapaId)
                .orElseThrow(() -> new RuntimeException("Etapa não encontrado"));
        movimentarOportunidadeCarregada(oportunidade, etapaDestino.getId(), novoIndice);
    }

    /**
     * Mesma lógica de {@link #movimentoOportunidade}, mas recebe a entidade já carregada
     * (com os relacionamentos necessários via JOIN FETCH) para evitar um SELECT redundante
     * quando o chamador (ex.: scheduler de cadência) já obteve a oportunidade em outra consulta.
     */
    void movimentarOportunidadeCarregada(Oportunidade oportunidade, Long etapaId, int novoIndice) {
        Etapa etapaAtual = oportunidade.getEtapa();
        if (etapaAtual == null) {
            throw new RuntimeException("Oportunidade sem etapa definida");
        }

        if (etapaAtual.getId().equals(etapaId)) {
            reorganizarIndices(etapaAtual.getId(), oportunidade, novoIndice);
        } else {
            Etapa novaEtapa = etapaRepository.findById(etapaId)
                    .orElseThrow(() -> new RuntimeException("Etapa não encontrado"));

            etapaService.updateAddValor(novaEtapa.getId(), oportunidade.getValor());
            etapaService.updateSubValor(etapaAtual.getId(), oportunidade.getValor());

            reorganizarIndices(etapaAtual.getId(), oportunidade, -1);

            oportunidade.setDataEntradaEtapa(LocalDateTime.now());
            oportunidade.setEtapa(novaEtapa);
            reorganizarIndices(novaEtapa.getId(), oportunidade, novoIndice);

            registrarHistorico(oportunidade.getId(), TipoEventoOportunidade.MOVIMENTACAO,
                    "moveu de '" + etapaAtual.getNome() + "' para '" + novaEtapa.getNome() + "'");
        }

        OportunidadeMovimentoDTO movimentoDto = new OportunidadeMovimentoDTO(oportunidade.getPublicId(), oportunidade.getEtapa().getPublicId());
        afterCommit(() -> messagingTemplate.convertAndSend("/topic/movimentoOportunidade", movimentoDto));
    }

    private void reorganizarIndices(Long etapaId, Oportunidade oportunidadeMovida, int novoIndice) {
        List<Oportunidade> oportunidades = oportunidadeRepository.findCardsByEtapaId(etapaId);
        oportunidades.removeIf(o -> o.getId().equals(oportunidadeMovida.getId()));

        if (novoIndice != -1) {
            int indiceValido = Math.max(0, Math.min(novoIndice, oportunidades.size()));
            oportunidades.add(indiceValido, oportunidadeMovida);
        }
        for (int i = 0; i < oportunidades.size(); i++) {
            oportunidades.get(i).setIndice(i);
        }
        oportunidadeRepository.saveAll(oportunidades);

        List<OportunidadeDTO> dtoList = oportunidades.stream()
                .map(this::toDto)
                .collect(Collectors.toList());
        afterCommit(() -> messagingTemplate.convertAndSend("/topic/attoportunidades", dtoList));
    }

    @Transactional
    public void delete(String id) {
        Oportunidade oportunidadeBanco = oportunidadeRepository.findByPublicId(id)
                .orElseThrow(() -> new RuntimeException("Oportunidade com id " + id + " nao encontrada"));
        Etapa etapa = oportunidadeBanco.getEtapa();
        BigDecimal valor = oportunidadeBanco.getValor();
        oportunidadeRepository.delete(oportunidadeBanco);
        if (etapa != null) {
            etapaService.updateSubValor(etapa.getId(), valor);
        }
        afterCommit(() -> messagingTemplate.convertAndSend("/topic/deletedoportunidade", id));
    }

    /**
     * Soft delete: marca a oportunidade como LIXEIRA em vez de apagar a linha. O board (GET
     * /funil/filtro) já filtra por situação e nunca inclui LIXEIRA por padrão, então a
     * oportunidade some da tela como num delete de verdade - mas sem esbarrar na FK de
     * log_movimentacao_cadencia (a oportunidade continua existindo, só a exclusão física de
     * verdade colide com o histórico de movimentação por cadência).
     */
    @Transactional
    public void moverParaLixeira(String id) {
        Oportunidade oportunidadeBanco = oportunidadeRepository.findByPublicId(id)
                .orElseThrow(() -> new RuntimeException("Oportunidade com id " + id + " nao encontrada"));
        Etapa etapa = oportunidadeBanco.getEtapa();
        BigDecimal valor = oportunidadeBanco.getValor();
        oportunidadeBanco.setSituacao(SituacaoOportunidade.LIXEIRA);
        oportunidadeRepository.save(oportunidadeBanco);
        registrarHistorico(oportunidadeBanco.getId(), TipoEventoOportunidade.LIXEIRA, "moveu esta oportunidade para a lixeira");
        if (etapa != null) {
            etapaService.updateSubValor(etapa.getId(), valor);
        }
        afterCommit(() -> messagingTemplate.convertAndSend("/topic/deletedoportunidade", id));
    }

    /**
     * Desfaz {@link #moverParaLixeira}: volta a oportunidade para ABERTO e devolve seu valor ao
     * total da etapa. Publica em /topic/newoportunidade (não /topic/updateOportunidade) porque,
     * do ponto de vista de quem está com o board filtrado para as situações padrão, a
     * oportunidade está "aparecendo" de novo, não só sendo atualizada.
     */
    @Transactional
    public void restaurar(String id) {
        Oportunidade oportunidadeBanco = oportunidadeRepository.findByPublicIdWithDetails(id)
                .orElseThrow(() -> new RuntimeException("Oportunidade com id " + id + " nao encontrada"));
        Etapa etapa = oportunidadeBanco.getEtapa();
        oportunidadeBanco.setSituacao(SituacaoOportunidade.ABERTO);
        oportunidadeRepository.save(oportunidadeBanco);
        registrarHistorico(oportunidadeBanco.getId(), TipoEventoOportunidade.RESTAURACAO, "restaurou esta oportunidade da lixeira");
        if (etapa != null) {
            etapaService.updateAddValor(etapa.getId(), oportunidadeBanco.getValor());
        }
        OportunidadeDTO dto = toDto(oportunidadeBanco);
        afterCommit(() -> messagingTemplate.convertAndSend("/topic/newoportunidade", dto));
    }

    private Participante preencheCliente(Participante participanteBanco, OportunidadeClienteRequest cliente) {
        participanteBanco.setNome(cliente.nome());
        participanteBanco.setLogin(cliente.login());
        participanteBanco.setCelular(cliente.celular());
        return participanteBanco;
    }

    private OportunidadeDTO toDto(Oportunidade oportunidade) {
        return new OportunidadeDTO(oportunidade);
    }

    /**
     * Registra um evento no histórico/log da oportunidade (exibido no modal de edição, campo
     * "Detalhes da oportunidade"). Chamado de dentro dos métodos @Transactional acima, então a
     * linha só é persistida se a transação inteira for commitada com sucesso.
     */
    private void registrarHistorico(Long oportunidadeId, TipoEventoOportunidade tipo, String descricao) {
        oportunidadeHistoricoRepository.save(new OportunidadeHistorico(oportunidadeId, currentActorName(), tipo, descricao));
    }

    /**
     * Nome de quem está autenticado no momento (usado como autor do evento de histórico). Fora de
     * um request autenticado — ex.: o scheduler de cadência movendo uma oportunidade em segundo
     * plano — não há Authentication no contexto, e o evento é atribuído a "Cadência automática".
     */
    private String currentActorName() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof Usuario usuario) {
            return usuario.getNome();
        }
        return "Cadência automática";
    }

    public List<OportunidadeHistoricoDTO> getHistorico(String oportunidadeId) {
        Oportunidade oportunidade = oportunidadeRepository.findByPublicId(oportunidadeId)
                .orElseThrow(() -> new RuntimeException("Oportunidade com id " + oportunidadeId + " nao encontrada"));
        return oportunidadeHistoricoRepository.findByOportunidadeIdOrderByCriadoEmDesc(oportunidade.getId()).stream()
                .map(OportunidadeHistoricoDTO::new)
                .collect(Collectors.toList());
    }

    private void afterCommit(Runnable action) {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    action.run();
                }
            });
        } else {
            action.run();
        }
    }

}
