package com.juridiqsystem.crm.service;

import com.juridiqsystem.crm.model.Etapa;
import com.juridiqsystem.crm.model.Funil;
import com.juridiqsystem.crm.model.Oportunidade;
import com.juridiqsystem.crm.model.dtos.EtapaCreateRequest;
import com.juridiqsystem.crm.model.dtos.EtapaDto;
import com.juridiqsystem.crm.model.dtos.EtapaFunilDTO;
import com.juridiqsystem.crm.model.dtos.EtapaUpdate2DTO;
import com.juridiqsystem.crm.model.dtos.EtapaUpdateDTO;
import com.juridiqsystem.crm.model.dtos.OportunidadeDTO;
import com.juridiqsystem.crm.model.enums.SituacaoOportunidade;
import com.juridiqsystem.crm.repository.EtapaRepository;
import com.juridiqsystem.crm.repository.FunilRepository;
import com.juridiqsystem.crm.repository.OportunidadeProcessoRepository;
import com.juridiqsystem.crm.repository.OportunidadeRepository;
import com.juridiqsystem.crm.service.exceptions.ConflictException;
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
    private OportunidadeProcessoRepository oportunidadeProcessoRepository;

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


    public EtapaDto findById(String id) {
        Etapa etapa = etapaRepository.findByPublicId(id)
                .orElseThrow(() -> new RuntimeException("Etapa com id " + id + " não encontrado"));

        List<OportunidadeDTO> oportunidades =
                carregarOportunidadesPorEtapa(List.of(etapa.getId())).getOrDefault(etapa.getId(), List.of());

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
        return agruparPorEtapaComTotalProcessos(oportunidades);
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
        return agruparPorEtapaComTotalProcessos(oportunidades);
    }

    /**
     * Monta os OportunidadeDTO com totalProcessos já preenchido, numa única consulta de contagem
     * agrupada (evita N+1 -- mesmo racional do JOIN FETCH de tags já usado nas queries de cards).
     */
    private Map<Long, List<OportunidadeDTO>> agruparPorEtapaComTotalProcessos(List<Oportunidade> oportunidades) {
        Map<Long, Long> totalProcessosPorOportunidade = new HashMap<>();
        if (!oportunidades.isEmpty()) {
            List<Long> oportunidadeIds = oportunidades.stream().map(Oportunidade::getId).collect(Collectors.toList());
            for (Object[] linha : oportunidadeProcessoRepository.countByOportunidadeIdIn(oportunidadeIds)) {
                totalProcessosPorOportunidade.put((Long) linha[0], (Long) linha[1]);
            }
        }
        Map<Long, List<OportunidadeDTO>> resultado = new HashMap<>();
        for (Oportunidade oportunidade : oportunidades) {
            OportunidadeDTO dto = new OportunidadeDTO(oportunidade);
            dto.setTotalProcessos(totalProcessosPorOportunidade.getOrDefault(oportunidade.getId(), 0L));
            resultado.computeIfAbsent(oportunidade.getEtapa().getId(), k -> new ArrayList<>()).add(dto);
        }
        return resultado;
    }


    public EtapaDto create(EtapaCreateRequest etapaCreateRequest) {
        Funil funil = funilRepository.findByPublicId(etapaCreateRequest.funil().getId())
                .orElseThrow(() -> new RuntimeException("Funil não encontrado"));
        boolean exists = etapaRepository.existsByNomeAndFunilId(etapaCreateRequest.nome(), funil.getId());
        if (exists) {
            throw new RuntimeException("Já existe um etapa com o mesmo nome neste funil.");
        }
        int proximaPosicao = etapaRepository.findByFunilIdOrderByPosicaoAscIdAsc(funil.getId()).stream()
                .map(Etapa::getPosicao)
                .filter(Objects::nonNull)
                .mapToInt(Integer::intValue)
                .max()
                .orElse(-1) + 1;
        Etapa etapa = new Etapa();
        etapa.setNome(etapaCreateRequest.nome());
        etapa.setFunil(funil);
        etapa.setPosicao(proximaPosicao);
        etapa.setValor_total(BigDecimal.ZERO);
        etapa.setCriadoEm(LocalDateTime.now());
        Etapa savedEtapa = etapaRepository.save(etapa);
        // Publica um EtapaDto (mesmo padrão do update(), que já usa EtapaUpdate2DTO),
        // nunca a entidade JPA crua: savedEtapa.funil.etapas cria uma referência
        // circular (Etapa -> Funil -> etapas -> Etapa -> ...) que o Jackson não
        // consegue serializar e derruba a publicação no /topic/newetapa em
        // silêncio (a etapa fica salva no banco, mas ninguém recebe o evento
        // em tempo real).
        EtapaDto dto = new EtapaDto(savedEtapa, List.of());
        messagingTemplate.convertAndSend("/topic/newetapa", dto);
        return dto;
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


    public void update(String id, EtapaUpdateDTO etapa) {
        Funil funilFiltro = funilRepository.findByPublicId(etapa.getFunil().getId())
                .orElseThrow(() -> new RuntimeException("Funil nao encontrado"));
        boolean exists = etapaRepository.existsByNomeAndFunilId(etapa.getNome(), funilFiltro.getId());
        if (exists) {
            throw new RuntimeException("Já existe um etapa com o mesmo nome neste funil.");
        }
        Etapa etapaBanco = etapaRepository.findByPublicId(id).orElseThrow(()-> new RuntimeException("Etapa com id "+id+" nao encontrada"));
        Etapa savedEtapa = etapaRepository.save(preencheEtapa(etapaBanco, etapa));
        EtapaUpdate2DTO dto = new EtapaUpdate2DTO(savedEtapa);
        messagingTemplate.convertAndSend("/topic/updateEtapa", dto);
    }

    public void delete(String id) {
        Etapa etapaBanco = etapaRepository.findByPublicId(id).orElseThrow(()-> new RuntimeException("Etapa com id "+id+" nao encontrada"));
        long oportunidadesAtivas = oportunidadeRepository.countPorEtapaIdIn(List.of(etapaBanco.getId()));
        if (oportunidadesAtivas > 0) {
            throw new ConflictException("Não é possível excluir a etapa: existem " + oportunidadesAtivas
                    + " oportunidade(s) nela. Mova ou envie para a lixeira antes de excluir.");
        }
        etapaRepository.delete(etapaBanco);
        messagingTemplate.convertAndSend("/topic/deleteetapa", etapaBanco.getPublicId());
    }

    public List<EtapaFunilDTO> findByFunilId(String funilId) {
        return funilRepository.findByPublicId(funilId)
                .map(funil -> etapaRepository.findByFunilIdOrderByPosicaoAscIdAsc(funil.getId()))
                .orElseGet(List::of)
                .stream().map(EtapaFunilDTO::new).collect(Collectors.toList());
    }

    public Etapa preencheEtapa(Etapa etapaBanco, EtapaUpdateDTO newEtapa){
        etapaBanco.setNome(newEtapa.getNome());
        if (newEtapa.getFunil() != null) {
            Funil funil = funilRepository.findByPublicId(newEtapa.getFunil().getId()).orElseThrow(()->new RuntimeException("Funil nao encontrado"));
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
