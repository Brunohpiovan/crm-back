package br.edu.faculdadevincit.crm_vincit.service;

import br.edu.faculdadevincit.crm_vincit.model.Etapa;
import br.edu.faculdadevincit.crm_vincit.model.Oportunidade;
import br.edu.faculdadevincit.crm_vincit.model.Participante;
import br.edu.faculdadevincit.crm_vincit.model.Tag;
import br.edu.faculdadevincit.crm_vincit.model.dtos.CriadorOportunidadeDto;
import br.edu.faculdadevincit.crm_vincit.model.dtos.OportunidadeDTO;
import br.edu.faculdadevincit.crm_vincit.model.dtos.UsuarioContatoDto;
import br.edu.faculdadevincit.crm_vincit.model.enums.Origem;
import br.edu.faculdadevincit.crm_vincit.model.enums.TipoParticipante;
import br.edu.faculdadevincit.crm_vincit.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class OportunidadeService {

    @Autowired
    private OportunidadeRepository oportunidadeRepository;

    @Autowired
    private FunilRepository funilRepository;

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
    private CloudFrontService cloudFrontService;

    @Autowired
    private TagRepository tagRepository;


    long maxSizeBytes = 100 * 1024 * 1024;

    public Oportunidade findByClienteAndCriadorNull(Long clienteId) {
        Oportunidade oportunidade = oportunidadeRepository.findByClienteIdAndCriadorIsNull(clienteId).orElse(null);
        return oportunidade;
    }


    public List<OportunidadeDTO> findAll() {
        List<Oportunidade> oportunidades = oportunidadeRepository.findAll();

        return oportunidades.stream()
                .map(oportunidade -> {
                    OportunidadeDTO dto = new OportunidadeDTO(oportunidade);

                    if (dto.getUrl_anexo() != null && dto.getUrl_anexo().contains(cloudFrontService.getBaseUrl())) {
                        String signedUrl = cloudFrontService.generateSignedUrl(dto.getUrl_anexo(), Duration.ofMinutes(60));
                        dto.setUrl_anexo(signedUrl);
                    }

                    return dto;
                })
                .collect(Collectors.toList());
    }


    public OportunidadeDTO findById(Long id) {
        Oportunidade oportunidade = oportunidadeRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Oportunidade com id " + id + " nao encontrada"));

        OportunidadeDTO dto = new OportunidadeDTO(oportunidade);

        if (dto.getUrl_anexo() != null && dto.getUrl_anexo().contains(cloudFrontService.getBaseUrl())) {
            String signedUrl = cloudFrontService.generateSignedUrl(dto.getUrl_anexo(), Duration.ofMinutes(60));
            dto.setUrl_anexo(signedUrl);
        }

        return dto;
    }


    public OportunidadeDTO create(Oportunidade oportunidade, MultipartFile file) {
        Etapa etapa = etapaRepository.findById(oportunidade.getEtapa().getId())
                .orElseThrow(() -> new RuntimeException("Etapa não encontrado"));
        funilRepository.findById(etapa.getFunil().getId())
                .orElseThrow(() -> new RuntimeException("Funil não encontrado"));
        usuarioRepository.findById(oportunidade.getCriador().getId())
                .orElseThrow(() -> new RuntimeException("Dono não encontrado"));
        List<Tag> tags = new ArrayList<>();

        if (oportunidade.getTags() != null && !oportunidade.getTags().isEmpty()) {
            for (Tag tag : oportunidade.getTags()) {
                Tag foundTag = tagRepository.findById(tag.getId())
                        .orElseThrow(() -> new RuntimeException("Tag com ID " + tag.getId() + " não encontrada"));
                tags.add(foundTag);
            }
        }

        oportunidade.setTags(tags);

        Optional<Participante> cliente = participanteRepository.findByCelular(oportunidade.getCliente().getCelular());
        if (cliente.isPresent()) {
            cliente.get().setTipoParticipante(TipoParticipante.PARTICIPANTE);
            if(cliente.get().getLogin() != null){
                oportunidade.setCliente(cliente.get());
            }else{
                Participante updateClient = participanteService.preencheParticipante(oportunidade.getCliente(),cliente.get());
                oportunidade.setCliente(updateClient);
            }

        } else {
            Participante newCliente = createParticipante(oportunidade.getCliente());
            oportunidade.setCliente(newCliente);
        }
        if (oportunidade.getData_criacao() == null) {
            oportunidade.setData_criacao(LocalDateTime.now());
        }
        oportunidade.setIndice(0);
        oportunidade.setDataEntradaEtapa(LocalDateTime.now());

        List<Oportunidade> oportunidadesNoEtapa = oportunidadeRepository.findByEtapaId(etapa.getId());
        for (Oportunidade op : oportunidadesNoEtapa) {
            if (op.getIndice() >= 0) {
                op.setIndice(op.getIndice() + 1);
                oportunidadeRepository.save(op);
            }
        }

        if(file != null){
            if (file.getSize() > maxSizeBytes) {
                throw new RuntimeException("O arquivo selecionado excede o limite de 100 MB.");
            }
            String key = "imgetapa/" + file.getOriginalFilename();
            oportunidade.setUrl_anexo(s3Service.uploadFile(file,key));
        }
        etapaService.updateAddValor(etapa.getId(),oportunidade.getValor());
        Oportunidade newOportunidade = oportunidadeRepository.save(oportunidade);
        OportunidadeDTO dto = new OportunidadeDTO(newOportunidade);
        if (dto.getUrl_anexo() != null && dto.getUrl_anexo().contains(cloudFrontService.getBaseUrl())) {
            String signedUrl = cloudFrontService.generateSignedUrl(dto.getUrl_anexo(), Duration.ofMinutes(60));
            dto.setUrl_anexo(signedUrl);
        }
        messagingTemplate.convertAndSend("/topic/newoportunidade", newOportunidade);
        return dto;
    }

    public Participante createParticipante(Participante participante){
        Participante newparticipante = new Participante();
        newparticipante.setNome(participante.getNome());
        newparticipante.setLogin(participante.getLogin());
        newparticipante.setRg(null);
        newparticipante.setCpf(null);
        newparticipante.setUrlPicture("assets/img/avatar/padrao.jpeg");
        newparticipante.setDataNascimento(null);
        newparticipante.setCelular(participante.getCelular());
        newparticipante.setEndereco(null);
        newparticipante.setNumeroResidencial(null);
        newparticipante.setComplemento(null);
        newparticipante.setBairro(null);
        newparticipante.setUf(null);
        newparticipante.setCidade(null);
        newparticipante.setObservacoes(null);
        newparticipante.setTipoParticipante(TipoParticipante.PARTICIPANTE);
        participanteRepository.save(newparticipante);
        UsuarioContatoDto contatoDto = new UsuarioContatoDto(newparticipante.getId(), newparticipante.getNome(), newparticipante.getUrlPicture());
        messagingTemplate.convertAndSend("/topic/usuarios", contatoDto);
        return newparticipante;
    }

    public void update(Long id, Oportunidade oportunidade,MultipartFile file) {
        Oportunidade oportunidadeBanco = oportunidadeRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Oportunidade com id " + id + " nao encontrada"));
        Participante cliente = participanteRepository.findById(oportunidade.getCliente().getId())
                .orElseThrow(() -> new RuntimeException("Cliente encontrado"));

        if(!oportunidade.getOrigem().equals(Origem.OUTRO)){
            oportunidade.setDescricao(null);
        }

        BigDecimal diferenca = oportunidade.getValor().subtract(oportunidadeBanco.getValor());
        boolean etapaChanged = false;
        if (oportunidade.getEtapa() != null && oportunidadeBanco.getEtapa() != null) {
            etapaChanged = !Objects.equals(oportunidade.getEtapa().getId(), oportunidadeBanco.getEtapa().getId());
        }
        
        if (etapaChanged) {
            oportunidade.setDataEntradaEtapa(LocalDateTime.now());
            etapaService.updateAddValor(oportunidade.getEtapa().getId(), oportunidade.getValor());
            etapaService.updateSubValor(oportunidadeBanco.getEtapa().getId(), oportunidadeBanco.getValor());
        } else if (diferenca.compareTo(BigDecimal.ZERO) != 0) {
            if (diferenca.compareTo(BigDecimal.ZERO) > 0) {
                etapaService.updateAddValor(oportunidade.getEtapa().getId(), diferenca);
            } else {
                etapaService.updateSubValor(oportunidade.getEtapa().getId(), diferenca.abs());
            }
        }
        if (oportunidadeBanco.getUrl_anexo() != null && !oportunidadeBanco.getUrl_anexo().isEmpty()) {
            if (oportunidade.getUrl_anexo() == null && file == null) {
                String chaveArquivo = getFileKeyFromUrl(oportunidadeBanco.getUrl_anexo());
                s3Service.deleteFile(chaveArquivo);
            }
            else if (file != null) {
                String chaveArquivo = getFileKeyFromUrl(oportunidadeBanco.getUrl_anexo());
                s3Service.deleteFile(chaveArquivo);
                String key = "imgetapa/" + file.getOriginalFilename();
                oportunidade.setUrl_anexo(s3Service.uploadFile(file, key));
            }
        } else {
            if (file != null) {
                if (file.getSize() > maxSizeBytes) {
                    throw new RuntimeException("O arquivo selecionado excede o limite de 100 MB.");
                }
                String key = "imgetapa/" + file.getOriginalFilename();
                oportunidade.setUrl_anexo(s3Service.uploadFile(file, key));
            }
        }
        if (oportunidade.getTags() != null && !oportunidade.getTags().isEmpty()) {
            List<Tag> tagsAtualizadas = new ArrayList<>();
            for (Tag tagRecebida : oportunidade.getTags()) {
                Tag tag = tagRepository.findById(tagRecebida.getId())
                        .orElseThrow(() -> new RuntimeException("Tag com id " + tagRecebida.getId() + " não encontrada"));
                tagsAtualizadas.add(tag);
            }

            oportunidade.setTags(tagsAtualizadas);
        }

        Oportunidade newOportunidade = preencheOportunidade(oportunidadeBanco, oportunidade);
        Participante updateClient = preencheCliente(cliente, oportunidade.getCliente());
        participanteRepository.save(updateClient);

        if (newOportunidade.getData_criacao() == null) {
            newOportunidade.setData_criacao(LocalDateTime.now());
        }
        newOportunidade.setAtualizadoEm(LocalDateTime.now());
        OportunidadeDTO dto = new OportunidadeDTO(oportunidadeRepository.save(newOportunidade));
        if (dto.getUrl_anexo() != null && dto.getUrl_anexo().contains(cloudFrontService.getBaseUrl())) {
            String signedUrl = cloudFrontService.generateSignedUrl(dto.getUrl_anexo(), Duration.ofMinutes(60));
            dto.setUrl_anexo(signedUrl);
        }
        String topic = etapaChanged ? "/topic/newoportunidade" : "/topic/updateOportunidade";
        messagingTemplate.convertAndSend(topic, dto);
    }

    private String getFileKeyFromUrl(String url) {
        return url.substring(url.indexOf("imgetapa/"));
    }

    public void movimentoOportunidade(Long oportunidadeId, Long etapaId, int novoIndice) {
        Oportunidade oportunidade = oportunidadeRepository.findById(oportunidadeId)
                .orElseThrow(() -> new RuntimeException("Oportunidade não encontrada"));

        Etapa etapaAtual = oportunidade.getEtapa();

        if (etapaAtual.getId().equals(etapaId)) {
            reorganizarIndicesOportunidades(etapaAtual, oportunidade, novoIndice);
        } else {
            Etapa newEtapa = etapaRepository.findById(etapaId)
                    .orElseThrow(() -> new RuntimeException("Etapa não encontrado"));

            etapaService.updateAddValor(newEtapa.getId(), oportunidade.getValor());
            etapaService.updateSubValor(etapaAtual.getId(), oportunidade.getValor());

            reorganizarIndicesOportunidades(etapaAtual, oportunidade, -1);

            oportunidade.setDataEntradaEtapa(LocalDateTime.now());
            oportunidade.setEtapa(newEtapa);
            reorganizarIndicesOportunidades(newEtapa, oportunidade, novoIndice);
        }
        OportunidadeDTO dto = new OportunidadeDTO(oportunidade);
        if (dto.getUrl_anexo() != null && dto.getUrl_anexo().contains(cloudFrontService.getBaseUrl())) {
            String signedUrl = cloudFrontService.generateSignedUrl(dto.getUrl_anexo(), Duration.ofMinutes(60));
            dto.setUrl_anexo(signedUrl);
        }
        CriadorOportunidadeDto criador = dto.getCriador();
        if (criador != null && criador.getUrlPicture() != null &&
                criador.getUrlPicture().contains(cloudFrontService.getBaseUrl())) {

            String signedPictureUrl = cloudFrontService.generateSignedUrl(criador.getUrlPicture(), Duration.ofMinutes(60));
            criador.setUrlPicture(signedPictureUrl);
        }
        messagingTemplate.convertAndSend("/topic/movimentoOportunidade",dto);
        oportunidadeRepository.save(oportunidade);
    }


    private void reorganizarIndicesOportunidades(Etapa etapa, Oportunidade oportunidadeMovida, int novoIndice) {
        List<Oportunidade> oportunidades = oportunidadeRepository.findByEtapaId(etapa.getId());

        oportunidades.sort(Comparator.comparingInt(Oportunidade::getIndice));

        oportunidades.removeIf(o -> o.getId().equals(oportunidadeMovida.getId()));

        if (novoIndice != -1) {
            oportunidades.add(novoIndice, oportunidadeMovida);
        }
        for (int i = 0; i < oportunidades.size(); i++) {
            oportunidades.get(i).setIndice(i);
        }
        oportunidadeRepository.saveAll(oportunidades);
        List<OportunidadeDTO> dtoList = oportunidades.stream()
                .map(OportunidadeDTO::new)
                .peek(dto -> {
                    if (dto.getUrl_anexo() != null && dto.getUrl_anexo().contains(cloudFrontService.getBaseUrl())) {
                        String signedUrl = cloudFrontService.generateSignedUrl(dto.getUrl_anexo(), Duration.ofMinutes(60));
                        dto.setUrl_anexo(signedUrl);
                    }

                    CriadorOportunidadeDto criador = dto.getCriador();
                    if (criador != null && criador.getUrlPicture() != null &&
                            criador.getUrlPicture().contains(cloudFrontService.getBaseUrl())) {

                        String signedPictureUrl = cloudFrontService.generateSignedUrl(criador.getUrlPicture(), Duration.ofMinutes(60));
                        criador.setUrlPicture(signedPictureUrl);
                    }
                })
                .collect(Collectors.toList());


        messagingTemplate.convertAndSend("/topic/attoportunidades", dtoList);
    }



    public void delete(Long id) {
        Oportunidade oportunidadeBanco = oportunidadeRepository.findById(id).orElseThrow(()-> new RuntimeException("Oportunidade com id "+id+" nao encontrada"));
        Etapa etapa = etapaRepository.findById(oportunidadeBanco.getEtapa().getId()).orElseThrow(()-> new RuntimeException("Etapa nao encontrado"));
        BigDecimal valor = oportunidadeBanco.getValor();
        oportunidadeRepository.delete(oportunidadeBanco);
        oportunidadeRepository.flush();
        etapaService.updateSubValor(etapa.getId(),valor);
        messagingTemplate.convertAndSend("/topic/deletedoportunidade",id);
    }

    public Oportunidade preencheOportunidade(Oportunidade oportunidadeBanco,Oportunidade newOportunidade){
        oportunidadeBanco.setTitulo(newOportunidade.getTitulo());
        oportunidadeBanco.setEtapa(newOportunidade.getEtapa());
        oportunidadeBanco.setCriador(newOportunidade.getCriador());
        oportunidadeBanco.setCliente(newOportunidade.getCliente());
        oportunidadeBanco.setValor(newOportunidade.getValor());
        oportunidadeBanco.setData_criacao(newOportunidade.getData_criacao());
        oportunidadeBanco.setUrl_anexo(newOportunidade.getUrl_anexo());
        oportunidadeBanco.setOrigem(newOportunidade.getOrigem());
        oportunidadeBanco.setInteresse(newOportunidade.getInteresse());
        oportunidadeBanco.setDescricao(newOportunidade.getDescricao());
        oportunidadeBanco.setObservacoes(newOportunidade.getObservacoes());
        oportunidadeBanco.setDataEntradaEtapa(newOportunidade.getDataEntradaEtapa());
        oportunidadeBanco.setSituacao(newOportunidade.getSituacao());
        oportunidadeBanco.setTags(newOportunidade.getTags());
        return oportunidadeBanco;
    }

    public Participante preencheCliente(Participante participanteBanco,Participante newParticipante){
        participanteBanco.setNome(newParticipante.getNome());
        participanteBanco.setLogin(newParticipante.getLogin());
        participanteBanco.setCelular(newParticipante.getCelular());
        return participanteBanco;
    }


}
