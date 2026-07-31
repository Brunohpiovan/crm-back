package br.edu.faculdadevincit.crm_vincit.controller;


import br.edu.faculdadevincit.crm_vincit.model.TemplateEmail;
import br.edu.faculdadevincit.crm_vincit.model.dtos.EmailTemplateRequestDto;
import br.edu.faculdadevincit.crm_vincit.model.dtos.TemplateAllDTO;
import br.edu.faculdadevincit.crm_vincit.model.dtos.TemplateEmailIdDTO;
import br.edu.faculdadevincit.crm_vincit.service.TemplateEmailService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;

@Tag(name = "Template de E-mail", description = "CRUD de templates de e-mail reutilizáveis (nome, assunto, mensagem, situação e anexos).")
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping("/template")
public class TemplateEmailController {

    @Autowired
    private TemplateEmailService templateEmailService;

    @Operation(summary = "Listar templates de e-mail", description = "Retorna todos os templates de e-mail cadastrados, em formato resumido (sem mensagem e anexos).")
    @ApiResponse(responseCode = "200", description = "Lista de templates",
            content = @Content(array = @ArraySchema(schema = @Schema(implementation = TemplateAllDTO.class))))
    @GetMapping
    public List<?> findAll() {
        return templateEmailService.findAll();
    }

    @Operation(summary = "Buscar template por id", description = "Retorna os dados completos de um template de e-mail, incluindo mensagem e URLs dos anexos.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Template encontrado",
                    content = @Content(schema = @Schema(implementation = TemplateEmailIdDTO.class))),
            @ApiResponse(responseCode = "400", description = "Template não encontrado para o id informado (resposta em texto puro, não JSON estruturado)",
                    content = @Content(mediaType = "text/plain", schema = @Schema(type = "string", example = "Template não encontrada")))
    })
    @GetMapping(value = "/{id}")
    public ResponseEntity<?> findById(@Parameter(description = "Id do template", required = true) @PathVariable Long id){
        return ResponseEntity.ok(templateEmailService.findById(id));
    }

    @Operation(
            summary = "Criar template de e-mail",
            description = """
                    Cria um novo template de e-mail. Requisição multipart/form-data com campos \
                    individuais: `nome`, `assunto`, `mensagem` (até 1000 caracteres), `situacao` (deve \
                    corresponder a um valor do enum `Situacao`: `ATIVA` ou `INATIVA`, case-insensitive, \
                    espaços viram '_') e `urlAnexo` (lista de URLs de anexos já existentes — campo \
                    obrigatório na requisição, mesmo que vazio na criação). `anexos` é opcional: os \
                    arquivos enviados são enviados ao S3 (pasta `anexos/`) e suas URLs são adicionadas ao \
                    template.
                    """
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Template criado com sucesso"),
            @ApiResponse(responseCode = "400", description = "Valor de 'situacao' inválido, ou falha no upload de algum anexo para o S3 (resposta em texto puro, não JSON estruturado; mensagem varia conforme a causa, ex.: \"Valor inválido para situação: ...\" ou \"A transferência falhou: ...\")",
                    content = @Content(mediaType = "text/plain", schema = @Schema(type = "string", example = "Valor inválido para situação: Foo")))
    })
    @PostMapping(value = "/create", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> create(
            @Parameter(description = "Nome do template", required = true) @RequestParam("nome") String nome,
            @Parameter(description = "Assunto do e-mail do template", required = true) @RequestParam("assunto") String assunto,
            @Parameter(description = "Corpo/mensagem do template (até 1000 caracteres)", required = true) @RequestParam("mensagem") String mensagem,
            @Parameter(description = "Situação do template: ATIVA ou INATIVA", required = true) @RequestParam("situacao") String situacao,
            @Parameter(description = "URLs de anexos já existentes a manter associados ao template (lista, pode ser vazia)", required = true) @RequestParam("urlAnexo") List<String> urlAnexo,
            @Parameter(description = "Novos arquivos de anexo a serem enviados ao S3 e associados ao template (multipart, opcional)") @RequestParam(value = "anexos", required = false) List<MultipartFile> anexos) {
        EmailTemplateRequestDto dto = new EmailTemplateRequestDto(nome,assunto,mensagem,situacao,urlAnexo,anexos);
        templateEmailService.create(dto);
        return ResponseEntity.ok().build();
    }

    @Operation(summary = "Excluir template de e-mail", description = "Remove definitivamente o template identificado por `id`. Não remove os anexos já enviados ao S3.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Template apagado com sucesso"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Template não encontrado para o id informado (resposta em texto puro, não JSON estruturado)",
                    content = @Content(mediaType = "text/plain", schema = @Schema(type = "string", example = "Template nao encontrada")))
    })
    @DeleteMapping(value = "/{id}")
    public ResponseEntity<?> delete(@Parameter(description = "Id do template a ser excluído", required = true) @PathVariable Long id) {
        templateEmailService.delete(id);
        return ResponseEntity.ok(Map.of("message", "Template apagado com sucesso"));
    }

    @Operation(
            summary = "Atualizar template de e-mail",
            description = """
                    Atualiza um template existente. Mesma estrutura multipart/form-data do endpoint de \
                    criação. Qualquer URL presente nos anexos atuais do template que não esteja em \
                    `urlAnexo` é removida do S3 e desassociada do template; novos arquivos enviados em \
                    `anexos` são enviados ao S3 e adicionados à lista de anexos.
                    """
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Template atualizado com sucesso"),
            @ApiResponse(responseCode = "400", description = "Template não encontrado, valor de 'situacao' inválido, ou falha no upload/remoção de algum anexo no S3 (resposta em texto puro, não JSON estruturado; mensagem varia conforme a causa, ex.: \"Template não encontrado\", \"Valor inválido para situação: ...\" ou \"Falha na deleção do objeto: ...\")",
                    content = @Content(mediaType = "text/plain", schema = @Schema(type = "string", example = "Template não encontrado")))
    })
    @PutMapping(value = "/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> update(@Parameter(description = "Id do template a ser atualizado", required = true) @PathVariable Long id,
                                    @Parameter(description = "Nome do template", required = true) @RequestParam("nome") String nome,
                                    @Parameter(description = "Assunto do e-mail do template", required = true) @RequestParam("assunto") String assunto,
                                    @Parameter(description = "Corpo/mensagem do template (até 1000 caracteres)", required = true) @RequestParam("mensagem") String mensagem,
                                    @Parameter(description = "Situação do template: ATIVA ou INATIVA", required = true) @RequestParam("situacao") String situacao,
                                    @Parameter(description = "URLs de anexos que devem permanecer associados ao template; URLs existentes ausentes desta lista são removidas do S3", required = true) @RequestParam("urlAnexo") List<String> urlAnexo,
                                    @Parameter(description = "Novos arquivos de anexo a serem enviados ao S3 e adicionados ao template (multipart, opcional)") @RequestParam(value = "anexos", required = false) List<MultipartFile> anexos) {
        EmailTemplateRequestDto dto = new EmailTemplateRequestDto(nome,assunto,mensagem,situacao,urlAnexo,anexos);
        templateEmailService.update(id, dto);
        return ResponseEntity.ok().build();
    }
}
