package com.juridiqsystem.crm.service;

import com.juridiqsystem.crm.infra.security.TenantContext;
import com.juridiqsystem.crm.model.*;
import com.juridiqsystem.crm.model.dtos.CadenciaAllDTO;
import com.juridiqsystem.crm.model.dtos.CadenciaFunilDto;
import com.juridiqsystem.crm.model.dtos.CadenciaFunilRequestDto;
import com.juridiqsystem.crm.model.enums.Situacao;
import com.juridiqsystem.crm.repository.CadenciaFunilRepository;
import com.juridiqsystem.crm.repository.EmpresaRepository;
import com.juridiqsystem.crm.repository.EtapaRepository;
import com.juridiqsystem.crm.repository.FunilRepository;
import com.juridiqsystem.crm.repository.LogMovimentacaoCadenciaRepository;
import com.juridiqsystem.crm.repository.OportunidadeRepository;
import com.juridiqsystem.crm.repository.SchedulerLockRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Slf4j
@Service
public class CadenciaFunilService {

    private static final String SCHEDULER_LOCK_NOME = "movimentacao_cadencia";
    private static final long SCHEDULER_LOCK_OBSOLESCENCIA_MINUTOS = 5;

    // horarioMovimentacao é configurado pelo usuário pensando em horário de Brasília. Comparado
    // explicitamente nesse fuso (em vez do default da JVM) porque em produção (Railway) o
    // container roda em UTC - sem isso, uma cadência configurada para "14:00" só disparava às
    // 17:00 (UTC), 3h depois do esperado.
    private static final ZoneId FUSO_HORARIO_MOVIMENTACAO = ZoneId.of("America/Sao_Paulo");

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

    @Autowired
    private LogMovimentacaoCadenciaRepository logMovimentacaoCadenciaRepository;

    @Autowired
    private EmpresaRepository empresaRepository;

    // Auto-injeção do proxy do Spring: moverOportunidadeIsolada só roda numa transação própria
    // (ver Javadoc do método) se for chamado através do proxy. Chamar via "this" dentro da própria
    // classe pula o proxy e o @Transactional não tem nenhum efeito - era exatamente por isso que
    // toda movimentação vinha falhando com LazyInitializationException.
    @Lazy
    @Autowired
    private CadenciaFunilService self;

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
            // Job roda fora de qualquer requisição HTTP (sem Usuario autenticado), então
            // TenantContext não vem populado por nenhum filtro — precisa iterar cada Empresa e
            // setar o tenant manualmente por rodada. Empresa em si não é @TenantId (é o próprio
            // tenant), então findAll() aqui já é global de propósito.
            for (Empresa empresa : empresaRepository.findAll()) {
                TenantContext.set(empresa.getId());
                try {
                    executarCadenciasDoMinuto();
                } catch (Exception e) {
                    log.error("Falha ao processar cadencias da empresa {} ({}).", empresa.getId(), empresa.getCodigo(), e);
                } finally {
                    TenantContext.clear();
                }
            }
        } finally {
            schedulerLockRepository.liberar(SCHEDULER_LOCK_NOME);
        }
    }

    private void executarCadenciasDoMinuto() {
        List<CadenciaFunil> cadencias = cadenciaFunilRepository.findAllBySituacaoWithDetails(Situacao.ATIVA);
        LocalTime agora = LocalTime.now(FUSO_HORARIO_MOVIMENTACAO).truncatedTo(ChronoUnit.MINUTES);

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
                boolean movida = self.moverOportunidadeIsolada(oportunidade.getId(), etapaDestinoId);
                if (movida) {
                    logMovimentacaoCadenciaRepository.save(new LogMovimentacaoCadencia(
                            cadencia.getId(), oportunidade.getId(), etapaOrigemId, etapaDestinoId));
                }
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
     *
     * @return true se uma movimentação real aconteceu; false se a oportunidade já estava na etapa
     * destino (reprocessamento ignorado) — usado por {@link #moverOportunidades} para não gravar
     * uma entrada de log de auditoria para um "no-op".
     */
    @Transactional
    boolean moverOportunidadeIsolada(Long oportunidadeId, Long etapaDestinoId) {
        Oportunidade oportunidade = oportunidadeRepository.findByIdWithDetails(oportunidadeId)
                .orElseThrow(() -> new RuntimeException("Oportunidade " + oportunidadeId + " não encontrada"));

        if (oportunidade.getEtapa() != null && oportunidade.getEtapa().getId().equals(etapaDestinoId)) {
            log.debug("Oportunidade {} já está na etapa destino {}, reprocessamento ignorado.", oportunidadeId, etapaDestinoId);
            return false;
        }

        Etapa etapaDestino = etapaRepository.findById(etapaDestinoId)
                .orElseThrow(() -> new RuntimeException("Etapa " + etapaDestinoId + " não encontrada"));
        int novoIndice = calcularNovoIndice(etapaDestino);
        oportunidadeService.movimentarOportunidadeCarregada(oportunidade, etapaDestinoId, novoIndice);
        return true;
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

    public CadenciaFunilDto findById(String id){
        return new CadenciaFunilDto(cadenciaFunilRepository.findByPublicId(id).orElseThrow(()->new RuntimeException("Cadencia de Funil nao encontrada")));
    }

    public void create(CadenciaFunilRequestDto cadenciaFunil){
        CadenciaFunil cadencia = criaCadencia(cadenciaFunil);
        cadencia.setCriadoEm(LocalDateTime.now());
        cadenciaFunilRepository.save(cadencia);
    }

    private CadenciaFunil criaCadencia(CadenciaFunilRequestDto cadenciaFunil){
        CadenciaFunil cadencia = new CadenciaFunil();
        cadencia.setFunilOrigem(funilRepository.findByPublicId(cadenciaFunil.getFunilOrigem()).orElseThrow(()->new RuntimeException("Funil de origem nao encontrado")));
        cadencia.setFunilDestino(funilRepository.findByPublicId(cadenciaFunil.getFunilDestino()).orElseThrow(()->new RuntimeException("Funil de destino nao encontrado")));
        cadencia.setEtapaOrigem(etapaRepository.findByPublicId(cadenciaFunil.getEtapaOrigem()).orElseThrow(()->new RuntimeException("Etapa de origem nao encontrada")));
        cadencia.setEtapaDestino(etapaRepository.findByPublicId(cadenciaFunil.getEtapaDestino()).orElseThrow(()->new RuntimeException("Etapa de destino nao encontrada")));
        cadencia.setNome(cadenciaFunil.getNome());
        cadencia.setDescricao(cadenciaFunil.getDescricao());
        cadencia.setDiasNaEtapa(cadenciaFunil.getDiasNaEtapa());
        cadencia.setSituacao(cadenciaFunil.getSituacao());
        cadencia.setHorarioMovimentacao(cadenciaFunil.getHorarioMovimentacao());
        return cadencia;
    }

    public void delete(String id){
        CadenciaFunil cadenciaFunil = cadenciaFunilRepository.findByPublicId(id).orElseThrow(()-> new RuntimeException("Tag nao encontrada"));
        cadenciaFunilRepository.delete(cadenciaFunil);
    }

    public void update(String id, CadenciaFunilRequestDto cadenciaFunil) {
        CadenciaFunil cadenciaBanco = cadenciaFunilRepository.findByPublicId(id)
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
