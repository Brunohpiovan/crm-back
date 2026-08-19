package com.juridiqsystem.crm.controller;

import com.juridiqsystem.crm.model.Processo;
import com.juridiqsystem.crm.model.dtos.ProcessoBuscaResultResponse;
import com.juridiqsystem.crm.model.dtos.ProcessoDetailResponse;
import com.juridiqsystem.crm.model.dtos.ProcessoResumoIaResponse;
import com.juridiqsystem.crm.model.dtos.ProcessoSummaryResponse;
import com.juridiqsystem.crm.service.ProcessoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "Processo", description = "Consulta e acompanhamento de processos judiciais via Escavador Business API (v2).")
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping("/processos")
public class ProcessoController {

    private final ProcessoService processoService;

    public ProcessoController(ProcessoService processoService) {
        this.processoService = processoService;
    }

    @Operation(summary = "Listar processos vinculados a alguma oportunidade da empresa",
            description = "Requer JWT. Filtro livre opcional por número CNJ ou nome do cliente.")
    @GetMapping
    public List<ProcessoSummaryResponse> listar(@RequestParam(required = false) String search) {
        return processoService.listarVinculados(search);
    }

    @Operation(summary = "Buscar processo por número CNJ",
            description = "Requer JWT. Consulta capa+envolvidos+movimentações na Escavador e faz upsert local. Não vincula a nenhuma oportunidade.")
    @GetMapping("/buscar-por-cnj")
    public ProcessoDetailResponse buscarPorCnj(@RequestParam String numeroCnj) {
        Processo processo = processoService.consultarEUpsertPorCnj(numeroCnj);
        return processoService.buscarDetalheLocalPorPublicId(processo.getPublicId());
    }

    @Operation(summary = "Busca reversa por envolvido (nome/CPF/CNPJ)",
            description = "Requer JWT. Não faz upsert local — apenas lista resultados da Escavador para o usuário escolher qual vincular.")
    @GetMapping("/buscar-por-envolvido")
    public List<ProcessoBuscaResultResponse> buscarPorEnvolvido(
            @RequestParam(required = false) String documento,
            @RequestParam(required = false) String nome) {
        return processoService.buscarPorEnvolvido(documento, nome);
    }

    @Operation(summary = "Busca reversa por OAB de advogado",
            description = "Requer JWT. Formato do OAB: \"123123/SP\".")
    @GetMapping("/buscar-por-oab")
    public List<ProcessoBuscaResultResponse> buscarPorOab(@RequestParam String oab) {
        return processoService.buscarPorOab(oab);
    }

    @Operation(summary = "Detalhe local de um processo", description = "Requer JWT. Não consulta a Escavador novamente — só dados já salvos.")
    @GetMapping("/{publicId}")
    public ProcessoDetailResponse buscarDetalhe(@PathVariable String publicId) {
        return processoService.buscarDetalheLocalPorPublicId(publicId);
    }

    @Operation(summary = "Atualizar processo no tribunal", description = "Requer JWT. Força nova consulta na Escavador e reconsolida movimentações sem duplicar.")
    @PostMapping("/{publicId}/atualizar")
    public ProcessoDetailResponse atualizar(@PathVariable String publicId) {
        Processo atualizado = processoService.atualizarNoTribunal(publicId);
        return processoService.buscarDetalheLocalPorPublicId(atualizado.getPublicId());
    }

    @Operation(summary = "Solicitar/obter resumo do processo por IA",
            description = "Requer JWT. Fluxo assíncrono: a primeira chamada dispara a geração (status PENDENTE); chamadas seguintes verificam se já concluiu (status CONCLUIDO).")
    @PostMapping("/{publicId}/resumo-ia")
    public ProcessoResumoIaResponse resumoIa(@PathVariable String publicId) {
        return processoService.gerarOuObterResumoIa(publicId);
    }
}
