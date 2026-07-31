package br.edu.faculdadevincit.crm_vincit.service;

import br.edu.faculdadevincit.crm_vincit.model.Etapa;
import br.edu.faculdadevincit.crm_vincit.model.Funil;
import br.edu.faculdadevincit.crm_vincit.model.Oportunidade;
import br.edu.faculdadevincit.crm_vincit.model.dtos.EtapaCreateRequest;
import br.edu.faculdadevincit.crm_vincit.model.dtos.EtapaDto;
import br.edu.faculdadevincit.crm_vincit.model.dtos.EtapaFunilDTO;
import br.edu.faculdadevincit.crm_vincit.model.dtos.EtapaUpdate2DTO;
import br.edu.faculdadevincit.crm_vincit.model.dtos.EtapaUpdateDTO;
import br.edu.faculdadevincit.crm_vincit.model.dtos.OportunidadeDTO;
import br.edu.faculdadevincit.crm_vincit.model.enums.SituacaoOportunidade;
import br.edu.faculdadevincit.crm_vincit.repository.EtapaRepository;
import br.edu.faculdadevincit.crm_vincit.repository.FunilRepository;
import br.edu.faculdadevincit.crm_vincit.repository.OportunidadeRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class EtapaService {

    @Autowired
    private EtapaRepository etapaRepository;

    @Autowired
    private FunilRepository funilRepository;

    @Autowired
    private OportunidadeRepository oportunidadeRepository;

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    public List<EtapaDto> findAll() {
        List<Etapa> etapas = etapaRepository.findAll();
        Map<Long, List<OportunidadeDTO>> oportunidadesPorEtapa =
                carregarOportunidadesPorEtapa(etapas.stream().map(Etapa::getId).collect(Collectors.toList()));

        return etapas.stream()
                .map(etapa -> new EtapaDto(etapa, oportunidadesPorEtapa.getOrDefault(etapa.getId(), List.of())))
                .collect(Collectors.toList());
    }


    public EtapaDto findById(Long id) {
        Etapa etapa = etapaRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Etapa com id " + id + " não encontrado"));

        List<OportunidadeDTO> oportunidades =
                carregarOportunidadesPorEtapa(List.of(id)).getOrDefault(id, List.of());

        return new EtapaDto(etapa, oportunidades);
    }

    /**
     * Carrega, em uma única consulta, os cards de todas as etapas informadas (evita N+1 por etapa).
     */
    Map<Long, List<OportunidadeDTO>> carregarOportunidadesPorEtapa(List<Long> etapaIds) {
        if (etapaIds == null || etapaIds.isEmpty()) {
            return Map.of();
        }
        List<Oportunidade> oportunidades = oportunidadeRepository.findCardsByEtapaIdIn(etapaIds);
        Map<Long, List<OportunidadeDTO>> resultado = new HashMap<>();
        for (Oportunidade oportunidade : oportunidades) {
            OportunidadeDTO dto = new OportunidadeDTO(oportunidade);
            resultado.computeIfAbsent(oportunidade.getEtapa().getId(), k -> new ArrayList<>()).add(dto);
        }
        return resultado;
    }

    Map<Long, List<OportunidadeDTO>> carregarOportunidadesPorEtapa(List<Long> etapaIds,
                                                                     List<SituacaoOportunidade> situacoes,
                                                                     List<Long> tagIds) {
        if (etapaIds == null || etapaIds.isEmpty()) {
            return Map.of();
        }
        List<Oportunidade> oportunidades = (tagIds != null && !tagIds.isEmpty())
                ? oportunidadeRepository.findCardsByEtapaIdInAndSituacaoInAndTagIdsIn(etapaIds, situacoes, tagIds)
                : oportunidadeRepository.findCardsByEtapaIdInAndSituacaoIn(etapaIds, situacoes);
        Map<Long, List<OportunidadeDTO>> resultado = new HashMap<>();
        for (Oportunidade oportunidade : oportunidades) {
            OportunidadeDTO dto = new OportunidadeDTO(oportunidade);
            resultado.computeIfAbsent(oportunidade.getEtapa().getId(), k -> new ArrayList<>()).add(dto);
        }
        return resultado;
    }


    public EtapaDto create(EtapaCreateRequest etapaCreateRequest) {
        Long funilId = etapaCreateRequest.funil().getId();
        boolean exists = etapaRepository.existsByNomeAndFunilId(etapaCreateRequest.nome(), funilId);
        if (exists) {
            throw new RuntimeException("Já existe um etapa com o mesmo nome neste funil.");
        }
        Funil funil = funilRepository.findById(funilId)
                .orElseThrow(() -> new RuntimeException("Funil não encontrado"));
        Etapa etapa = new Etapa();
        etapa.setNome(etapaCreateRequest.nome());
        etapa.setFunil(funil);
        etapa.setValor_total(BigDecimal.ZERO);
        etapa.setCriadoEm(LocalDateTime.now());
        Etapa savedEtapa = etapaRepository.save(etapa);
        messagingTemplate.convertAndSend("/topic/newetapa", savedEtapa);
        return new EtapaDto(savedEtapa, List.of());
    }

    public EtapaDto updateAddValor(Long id, BigDecimal valor){
        Etapa etapaBanco = etapaRepository.findById(id).orElseThrow(()-> new RuntimeException("Etapa com id "+id+" nao encontrada"));
        etapaBanco.setValor_total(etapaBanco.getValor_total().add(valor));
        Etapa savedEtapa = etapaRepository.save(etapaBanco);
        EtapaDto dto = new EtapaDto(savedEtapa, List.of());
        Map<String, Object> payload = new HashMap<>();
        payload.put("etapaId", etapaBanco.getId());
        payload.put("valor", valor);
        afterCommit(() -> messagingTemplate.convertAndSend("/topic/addvalue", payload));
        return dto;
    }

    public EtapaDto updateSubValor(Long id, BigDecimal valor){
        Etapa etapaBanco = etapaRepository.findById(id).orElseThrow(()-> new RuntimeException("Etapa com id "+id+" nao encontrada"));
        etapaBanco.setValor_total(etapaBanco.getValor_total().subtract(valor));
        Etapa savedEtapa = etapaRepository.save(etapaBanco);
        EtapaDto dto = new EtapaDto(savedEtapa, List.of());
        Map<String, Object> payload = new HashMap<>();
        payload.put("etapaId", etapaBanco.getId());
        payload.put("valor", valor);
        afterCommit(() -> messagingTemplate.convertAndSend("/topic/subvalue", payload));
        return dto;
    }


    public void update(Long id, EtapaUpdateDTO etapa) {
        boolean exists = etapaRepository.existsByNomeAndFunilId(etapa.getNome(), etapa.getFunil().getId());
        if (exists) {
            throw new RuntimeException("Já existe um etapa com o mesmo nome neste funil.");
        }
        Etapa etapaBanco = etapaRepository.findById(id).orElseThrow(()-> new RuntimeException("Etapa com id "+id+" nao encontrada"));
        Etapa savedEtapa = etapaRepository.save(preencheEtapa(etapaBanco, etapa));
        EtapaUpdate2DTO dto = new EtapaUpdate2DTO(savedEtapa);
        messagingTemplate.convertAndSend("/topic/updateEtapa", dto);
    }

    public void delete(Long id) {
        Etapa etapaBanco = etapaRepository.findById(id).orElseThrow(()-> new RuntimeException("Etapa com id "+id+" nao encontrada"));
        etapaRepository.delete(etapaBanco);
        messagingTemplate.convertAndSend("/topic/deleteetapa", etapaBanco.getId());
    }

    public List<EtapaFunilDTO> findByFunilId(Long funilId) {
        List<Etapa> etapas = etapaRepository.findByFunilId(funilId);
        return etapas.stream().map(EtapaFunilDTO::new).collect(Collectors.toList());
    }

    public Etapa preencheEtapa(Etapa etapaBanco, EtapaUpdateDTO newEtapa){
        etapaBanco.setNome(newEtapa.getNome());
        if (newEtapa.getFunil() != null) {
            Funil funil = funilRepository.findById(newEtapa.getFunil().getId()).orElseThrow(()->new RuntimeException("Funil nao encontrado"));
            etapaBanco.setFunil(funil);
        }
        etapaBanco.setAtualizadoEm(LocalDateTime.now());
        return etapaBanco;
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
