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
import br.edu.faculdadevincit.crm_vincit.repository.SchedulerLockRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Slf4j
@Service
public class CadenciaFunilService {

    private static final String SCHEDULER_LOCK_NOME = "movimentacao_cadencia";
    private static final long SCHEDULER_LOCK_OBSOLESCENCIA_MINUTOS = 5;

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

    @Autowired
    private SchedulerLockRepository schedulerLockRepository;

    /**
     * Ponto de entrada chamado pelo scheduler. Não é transacional: o lock é adquirido/liberado em
     * transações próprias (curtas) e cada oportunidade movida tem sua própria transação isolada
     * (ver {@link #moverOportunidadeIsolada}), para que a falha de um item não desfaça os demais.
     */
    public void processarCadenciasAtivas() {
        LocalDateTime agora = LocalDateTime.now();
        boolean lockAdquirido = schedulerLockRepository.tentarAdquirir(
                SCHEDULER_LOCK_NOME, agora, agora.minusMinutes(SCHEDULER_LOCK_OBSOLESCENCIA_MINUTOS)) > 0;

        if (!lockAdquirido) {
            log.debug("Execução de movimentação de cadência ignorada: outra instância já está processando.");
            return;
        }

        try {
            executarCadenciasDoMinuto();
        } finally {
            schedulerLockRepository.liberar(SCHEDULER_LOCK_NOME);
        }
    }

    private void executarCadenciasDoMinuto() {
        List<CadenciaFunil> cadencias = cadenciaFunilRepository.findAllBySituacaoWithDetails(Situacao.ATIVA);
        LocalTime agora = LocalTime.now().truncatedTo(ChronoUnit.MINUTES);

        for (CadenciaFunil cadencia : cadencias) {
            if (cadencia.getHorarioMovimentacao().equals(agora)) {
                moverOportunidades(cadencia);
            }
        }
    }

    private void moverOportunidades(CadenciaFunil cadencia) {
        Long etapaOrigemId = cadencia.getEtapaOrigem().getId();
        Long etapaDestinoId = cadencia.getEtapaDestino().getId();
        LocalDateTime limite = LocalDateTime.now().minusDays(cadencia.getDiasNaEtapa());

        List<Oportunidade> oportunidades = oportunidadeRepository.findElegiveisParaMovimentacao(etapaOrigemId, limite);
        log.info("Cadência '{}': {} oportunidade(s) elegível(is) para mover da etapa {} para {}.",
                cadencia.getNome(), oportunidades.size(), etapaOrigemId, etapaDestinoId);

        int movidas = 0;
        int falhas = 0;
        for (Oportunidade oportunidade : oportunidades) {
            try {
                moverOportunidadeIsolada(oportunidade.getId(), etapaDestinoId);
                movidas++;
            } catch (Exception e) {
                falhas++;
                log.error("Falha ao mover oportunidade {} (cadência '{}', etapa destino {}). Os demais itens continuam sendo processados.",
                        oportunidade.getId(), cadencia.getNome(), etapaDestinoId, e);
            }
        }
        log.info("Cadência '{}' processada: {} movida(s), {} falha(s).", cadencia.getNome(), movidas, falhas);
    }

    /**
     * Transação própria por oportunidade: se esta movimentação falhar (ex.: OptimisticLockException
     * por conflito com uma edição manual concorrente), só ela é desfeita — as demais oportunidades
     * do lote continuam sendo processadas normalmente por {@link #moverOportunidades}.
     */
    @Transactional
    void moverOportunidadeIsolada(Long oportunidadeId, Long etapaDestinoId) {
        Oportunidade oportunidade = oportunidadeRepository.findByIdWithDetails(oportunidadeId)
                .orElseThrow(() -> new RuntimeException("Oportunidade " + oportunidadeId + " não encontrada"));

        if (oportunidade.getEtapa() != null && oportunidade.getEtapa().getId().equals(etapaDestinoId)) {
            log.debug("Oportunidade {} já está na etapa destino {}, reprocessamento ignorado.", oportunidadeId, etapaDestinoId);
            return;
        }

        Etapa etapaDestino = etapaRepository.findById(etapaDestinoId)
                .orElseThrow(() -> new RuntimeException("Etapa " + etapaDestinoId + " não encontrada"));
        int novoIndice = calcularNovoIndice(etapaDestino);
        oportunidadeService.movimentarOportunidadeCarregada(oportunidade, etapaDestinoId, novoIndice);
    }

    private int calcularNovoIndice(Etapa etapaDestino) {
        return (int)oportunidadeRepository.countByEtapa(etapaDestino);
    }


    public List<CadenciaAllDTO> findAll() {
        return cadenciaFunilRepository.findAllWithDetails()
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
