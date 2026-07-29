package br.edu.faculdadevincit.crm_vincit.service;

import br.edu.faculdadevincit.crm_vincit.model.Etapa;
import br.edu.faculdadevincit.crm_vincit.model.Funil;
import br.edu.faculdadevincit.crm_vincit.model.dtos.EtapaDto;
import br.edu.faculdadevincit.crm_vincit.model.dtos.EtapaFunilDTO;
import br.edu.faculdadevincit.crm_vincit.model.dtos.EtapaUpdate2DTO;
import br.edu.faculdadevincit.crm_vincit.model.dtos.EtapaUpdateDTO;
import br.edu.faculdadevincit.crm_vincit.repository.EtapaRepository;
import br.edu.faculdadevincit.crm_vincit.repository.FunilRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Duration;
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
    private SimpMessagingTemplate messagingTemplate;

    @Autowired
    private CloudFrontService cloudFrontService;

    public List<EtapaDto> findAll() {
        List<Etapa> etapas = etapaRepository.findAll();

        List<EtapaDto> etapaDtos = etapas.stream()
                .map(etapa -> {
                    EtapaDto dto = new EtapaDto(etapa);

                    if (dto.getOportunidades() != null) {
                        dto.getOportunidades().forEach(oportunidadeDTO -> {
                            if (oportunidadeDTO.getUrl_anexo() != null && oportunidadeDTO.getUrl_anexo().contains(cloudFrontService.getBaseUrl())) {
                                String signedUrl = cloudFrontService.generateSignedUrl(oportunidadeDTO.getUrl_anexo(), Duration.ofMinutes(60));
                                oportunidadeDTO.setUrl_anexo(signedUrl);
                            }
                        });
                    }

                    return dto;
                })
                .collect(Collectors.toList());

        return etapaDtos;
    }


    public EtapaDto findById(Long id) {
        Etapa etapa = etapaRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Etapa com id " + id + " não encontrado"));

        EtapaDto dto = new EtapaDto(etapa);

        if (dto.getOportunidades() != null) {
            dto.getOportunidades().forEach(oportunidadeDTO -> {
                if (oportunidadeDTO.getUrl_anexo() != null && oportunidadeDTO.getUrl_anexo().contains(cloudFrontService.getBaseUrl())) {
                    String signedUrl = cloudFrontService.generateSignedUrl(oportunidadeDTO.getUrl_anexo(), Duration.ofMinutes(60));
                    oportunidadeDTO.setUrl_anexo(signedUrl);
                }
            });
        }

        return dto;
    }


    public EtapaDto create(Etapa etapa) {
        boolean exists = etapaRepository.existsByNomeAndFunilId(etapa.getNome(), etapa.getFunil().getId());
        if (exists) {
            throw new RuntimeException("Já existe um etapa com o mesmo nome neste funil.");
        }
        Funil funil = funilRepository.findById(etapa.getFunil().getId())
                .orElseThrow(() -> new RuntimeException("Funil não encontrado"));
        etapa.setFunil(funil);
        etapa.setValor_total(BigDecimal.ZERO);
        Etapa savedEtapa = etapaRepository.save(etapa);
        messagingTemplate.convertAndSend("/topic/newetapa", savedEtapa);
        savedEtapa.setCriadoEm(LocalDateTime.now());
        return new EtapaDto(savedEtapa);
    }

    public EtapaDto updateAddValor(Long id, BigDecimal valor){
        Etapa etapaBanco = etapaRepository.findById(id).orElseThrow(()-> new RuntimeException("Etapa com id "+id+" nao encontrada"));
        etapaBanco.setValor_total(etapaBanco.getValor_total().add(valor));
        Etapa savedEtapa = etapaRepository.save(etapaBanco);
        EtapaDto dto = new EtapaDto(savedEtapa);
        Map<String, Object> payload = new HashMap<>();
        payload.put("etapaId", etapaBanco.getId());
        payload.put("valor", valor);
        messagingTemplate.convertAndSend("/topic/addvalue", payload);
        return dto;
    }

    public EtapaDto updateSubValor(Long id, BigDecimal valor){
        Etapa etapaBanco = etapaRepository.findById(id).orElseThrow(()-> new RuntimeException("Etapa com id "+id+" nao encontrada"));
        etapaBanco.setValor_total(etapaBanco.getValor_total().subtract(valor));
        Etapa savedEtapa = etapaRepository.save(etapaBanco);
        EtapaDto dto = new EtapaDto(savedEtapa);
        Map<String, Object> payload = new HashMap<>();
        payload.put("etapaId", etapaBanco.getId());
        payload.put("valor", valor);
        messagingTemplate.convertAndSend("/topic/subvalue", payload);
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

}
