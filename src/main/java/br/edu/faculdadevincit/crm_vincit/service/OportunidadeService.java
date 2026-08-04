package br.edu.faculdadevincit.crm_vincit.service;

import br.edu.faculdadevincit.crm_vincit.model.Etapa;
import br.edu.faculdadevincit.crm_vincit.model.Oportunidade;
import br.edu.faculdadevincit.crm_vincit.model.Participante;
import br.edu.faculdadevincit.crm_vincit.model.Tag;
import br.edu.faculdadevincit.crm_vincit.model.Usuario;
import br.edu.faculdadevincit.crm_vincit.model.dtos.OportunidadeClienteRequest;
import br.edu.faculdadevincit.crm_vincit.model.dtos.OportunidadeCreateRequest;
import br.edu.faculdadevincit.crm_vincit.model.dtos.OportunidadeDTO;
import br.edu.faculdadevincit.crm_vincit.model.dtos.OportunidadeMovimentoDTO;
import br.edu.faculdadevincit.crm_vincit.model.dtos.OportunidadeUpdateRequest;
import br.edu.faculdadevincit.crm_vincit.model.dtos.UsuarioContatoDto;
import br.edu.faculdadevincit.crm_vincit.model.enums.Origem;
import br.edu.faculdadevincit.crm_vincit.model.enums.SituacaoOportunidade;
import br.edu.faculdadevincit.crm_vincit.model.enums.TipoParticipante;
import br.edu.faculdadevincit.crm_vincit.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.messaging.simp.SimpMessagingTemplate;
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

    public OportunidadeDTO findByClienteAndCriadorNull(Long clienteId) {
        return oportunidadeRepository.findByClienteIdAndCriadorIsNull(clienteId)
                .map(this::toDto)
                .orElse(null);
    }

    public Page<OportunidadeDTO> findAll(Pageable pageable) {
        return oportunidadeRepository.findAllWithDetails(pageable).map(this::toDto);
    }

    public OportunidadeDTO findById(Long id) {
        Oportunidade oportunidade = oportunidadeRepository.findByIdWithDetails(id)
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
        return criarComAnexoResolvido(request, urlAnexo);
    }

    @Transactional
    OportunidadeDTO criarComAnexoResolvido(OportunidadeCreateRequest request, String urlAnexo) {
        Etapa etapa = etapaRepository.findById(request.etapaId())
                .orElseThrow(() -> new RuntimeException("Etapa não encontrado"));
        Usuario criador = usuarioRepository.findById(request.criadorId())
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
        UsuarioContatoDto contatoDto = new UsuarioContatoDto(salvo.getId(), salvo.getNome(), salvo.getUrlPicture());
        afterCommit(() -> messagingTemplate.convertAndSend("/topic/usuarios", contatoDto));
        return salvo;
    }

    private List<Tag> resolveTags(List<Long> tagIds) {
        if (tagIds == null || tagIds.isEmpty()) {
            return new ArrayList<>();
        }
        List<Tag> tags = tagRepository.findAllById(tagIds);
        if (tags.size() != new HashSet<>(tagIds).size()) {
            throw new RuntimeException("Uma ou mais tags informadas não foram encontradas");
        }
        return tags;
    }

    public OportunidadeDTO update(Long id, OportunidadeUpdateRequest request, MultipartFile file) {
        if (!oportunidadeRepository.existsById(id)) {
            throw new RuntimeException("Oportunidade com id " + id + " nao encontrada");
        }
        String urlAnexoAtual = oportunidadeRepository.findUrlAnexoById(id).orElse(null);
        String urlAnexoFinal = resolveAnexo(urlAnexoAtual, request.urlAnexo(), file);
        return atualizarComAnexoResolvido(id, request, urlAnexoFinal);
    }

    @Transactional
    OportunidadeDTO atualizarComAnexoResolvido(Long id, OportunidadeUpdateRequest request, String urlAnexoFinal) {
        Oportunidade oportunidadeBanco = oportunidadeRepository.findByIdWithDetails(id)
                .orElseThrow(() -> new RuntimeException("Oportunidade com id " + id + " nao encontrada"));
        Etapa novaEtapa = etapaRepository.findById(request.etapaId())
                .orElseThrow(() -> new RuntimeException("Etapa não encontrado"));
        Usuario criador = usuarioRepository.findById(request.criadorId())
                .orElseThrow(() -> new RuntimeException("Dono não encontrado"));
        Participante clienteBanco = participanteRepository.findById(request.cliente().id())
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
    public void movimentoOportunidade(Long oportunidadeId, Long etapaId, int novoIndice) {
        Oportunidade oportunidade = oportunidadeRepository.findByIdWithDetails(oportunidadeId)
                .orElseThrow(() -> new RuntimeException("Oportunidade não encontrada"));
        movimentarOportunidadeCarregada(oportunidade, etapaId, novoIndice);
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
        }

        OportunidadeMovimentoDTO movimentoDto = new OportunidadeMovimentoDTO(oportunidade.getId(), etapaId);
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
    public void delete(Long id) {
        Oportunidade oportunidadeBanco = oportunidadeRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Oportunidade com id " + id + " nao encontrada"));
        Etapa etapa = oportunidadeBanco.getEtapa();
        BigDecimal valor = oportunidadeBanco.getValor();
        oportunidadeRepository.delete(oportunidadeBanco);
        if (etapa != null) {
            etapaService.updateSubValor(etapa.getId(), valor);
        }
        afterCommit(() -> messagingTemplate.convertAndSend("/topic/deletedoportunidade", id));
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
