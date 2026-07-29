package br.edu.faculdadevincit.crm_vincit.service;

import br.edu.faculdadevincit.crm_vincit.model.*;
import br.edu.faculdadevincit.crm_vincit.model.dtos.CadenciaAllDTO;
import br.edu.faculdadevincit.crm_vincit.model.dtos.CadenciaFunilDto;
import br.edu.faculdadevincit.crm_vincit.model.dtos.CadenciaFunilRequestDto;
import br.edu.faculdadevincit.crm_vincit.model.enums.Situacao;
import br.edu.faculdadevincit.crm_vincit.repository.CadenciaFunilRepository;
import br.edu.faculdadevincit.crm_vincit.repository.EtapaRepository;
import br.edu.faculdadevincit.crm_vincit.repository.FunilRepository;
import br.edu.faculdadevincit.crm_vincit.repository.OportunidadeRepository;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Service
public class CadenciaFunilService {

    @Autowired
    private CadenciaFunilRepository cadenciaFunilRepository;

    @Autowired
    private FunilRepository funilRepository;

    @Autowired
    private EtapaRepository etapaRepository;

    @Autowired
    private OportunidadeService oportunidadeService;

    @Autowired
    private OportunidadeRepository oportunidadeRepository;

    @Transactional
    public void processarCadenciasAtivas() {
        List<CadenciaFunil> cadencias = cadenciaFunilRepository.findAllBySituacao(Situacao.ATIVA);
        LocalTime agora = LocalTime.now().truncatedTo(ChronoUnit.MINUTES);

        for (CadenciaFunil cadencia : cadencias) {
            if (cadencia.getHorarioMovimentacao().equals(agora)) {
                moverOportunidades(cadencia);
            }
        }
    }

    private void moverOportunidades(CadenciaFunil cadencia) {
        LocalDateTime limite = LocalDateTime.now().minusDays(cadencia.getDiasNaEtapa());

        List<Oportunidade> oportunidades = oportunidadeRepository.findElegiveisParaMovimentacao(
                cadencia.getEtapaOrigem().getId(),
                limite
        );

        for (Oportunidade oportunidade : oportunidades) {
            int novoIndice = calcularNovoIndice(cadencia.getEtapaDestino());
            oportunidadeService.movimentoOportunidade(oportunidade.getId(), cadencia.getEtapaDestino().getId(), novoIndice);
        }
    }
    private int calcularNovoIndice(Etapa etapaDestino) {
        return (int)oportunidadeRepository.countByEtapa(etapaDestino);
    }


    public List<CadenciaAllDTO> findAll() {
        return cadenciaFunilRepository.findAll()
                .stream()
                .map(CadenciaAllDTO::new)
                .toList();
    }

    public CadenciaFunilDto findById(Long id){
        return new CadenciaFunilDto(cadenciaFunilRepository.findById(id).orElseThrow(()->new RuntimeException("Cadencia de Funil nao encontrada")));
    }

    public void create(CadenciaFunilRequestDto cadenciaFunil){
        CadenciaFunil cadencia = criaCadencia(cadenciaFunil);
        cadencia.setCriadoEm(LocalDateTime.now());
        cadenciaFunilRepository.save(cadencia);
    }

    private CadenciaFunil criaCadencia(CadenciaFunilRequestDto cadenciaFunil){
        CadenciaFunil cadencia = new CadenciaFunil();
        cadencia.setFunilOrigem(funilRepository.findById(cadenciaFunil.getFunilOrigem()).orElseThrow(()->new RuntimeException("Funil de origem nao encontrado")));
        cadencia.setFunilDestino(funilRepository.findById(cadenciaFunil.getFunilDestino()).orElseThrow(()->new RuntimeException("Funil de destino nao encontrado")));
        cadencia.setEtapaOrigem(etapaRepository.findById(cadenciaFunil.getEtapaOrigem()).orElseThrow(()->new RuntimeException("Etapa de origem nao encontrada")));
        cadencia.setEtapaDestino(etapaRepository.findById(cadenciaFunil.getEtapaDestino()).orElseThrow(()->new RuntimeException("Etapa de destino nao encontrada")));
        cadencia.setNome(cadenciaFunil.getNome());
        cadencia.setDescricao(cadenciaFunil.getDescricao());
        cadencia.setDiasNaEtapa(cadenciaFunil.getDiasNaEtapa());
        cadencia.setSituacao(cadenciaFunil.getSituacao());
        cadencia.setHorarioMovimentacao(cadenciaFunil.getHorarioMovimentacao());
        return cadencia;
    }

    public void delete(Long id){
        CadenciaFunil cadenciaFunil = cadenciaFunilRepository.findById(id).orElseThrow(()-> new RuntimeException("Tag nao encontrada"));
        cadenciaFunilRepository.delete(cadenciaFunil);
    }

    public void update(Long id, CadenciaFunilRequestDto cadenciaFunil) {
        CadenciaFunil cadenciaBanco = cadenciaFunilRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Cadencia de Funil não encontrada"));
        CadenciaFunil updateCadencia = criaCadencia(cadenciaFunil);
        CadenciaFunil newCadencia = preencheTag(cadenciaBanco, updateCadencia);
        newCadencia.setAtualizadoEm(LocalDateTime.now());
        cadenciaFunilRepository.save(newCadencia);
    }

    private CadenciaFunil preencheTag(CadenciaFunil cadenciaBanco,CadenciaFunil newCadencia){
        cadenciaBanco.setNome(newCadencia.getNome());
        cadenciaBanco.setFunilOrigem(newCadencia.getFunilOrigem());
        cadenciaBanco.setFunilDestino(newCadencia.getFunilDestino());
        cadenciaBanco.setEtapaOrigem(newCadencia.getEtapaOrigem());
        cadenciaBanco.setEtapaDestino(newCadencia.getEtapaDestino());
        cadenciaBanco.setDiasNaEtapa(newCadencia.getDiasNaEtapa());
        cadenciaBanco.setHorarioMovimentacao(newCadencia.getHorarioMovimentacao());
        cadenciaBanco.setSituacao(newCadencia.getSituacao());
        cadenciaBanco.setDescricao(newCadencia.getDescricao());
        return cadenciaBanco;
    }
}
