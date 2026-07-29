package br.edu.faculdadevincit.crm_vincit.controller;


import br.edu.faculdadevincit.crm_vincit.model.TemplateEmail;
import br.edu.faculdadevincit.crm_vincit.model.dtos.EmailTemplateRequestDto;
import br.edu.faculdadevincit.crm_vincit.model.dtos.TemplateAllDTO;
import br.edu.faculdadevincit.crm_vincit.service.TemplateEmailService;
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

@RestController
@RequestMapping("/template")
public class TemplateEmailController {

    @Autowired
    private TemplateEmailService templateEmailService;

    @GetMapping
    public List<?> findAll() {
        return templateEmailService.findAll();
    }

    @GetMapping(value = "/{id}")
    public ResponseEntity<?> findById(@PathVariable Long id){
        return ResponseEntity.ok(templateEmailService.findById(id));
    }

    @PostMapping(value = "/create", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> create(
            @RequestParam("nome") String nome,
            @RequestParam("assunto") String assunto,
            @RequestParam("mensagem") String mensagem,
            @RequestParam("situacao") String situacao,
            @RequestParam("urlAnexo") List<String> urlAnexo,
            @RequestParam(value = "anexos", required = false) List<MultipartFile> anexos) {
        EmailTemplateRequestDto dto = new EmailTemplateRequestDto(nome,assunto,mensagem,situacao,urlAnexo,anexos);
        templateEmailService.create(dto);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping(value = "/{id}")
    public ResponseEntity<?> delete(@PathVariable Long id) {
        templateEmailService.delete(id);
        return ResponseEntity.ok(Map.of("message", "Template apagado com sucesso"));
    }

    @PutMapping(value = "/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> update(@PathVariable Long id,
                                    @RequestParam("nome") String nome,
                                    @RequestParam("assunto") String assunto,
                                    @RequestParam("mensagem") String mensagem,
                                    @RequestParam("situacao") String situacao,
                                    @RequestParam("urlAnexo") List<String> urlAnexo,
                                    @RequestParam(value = "anexos", required = false) List<MultipartFile> anexos) {
        EmailTemplateRequestDto dto = new EmailTemplateRequestDto(nome,assunto,mensagem,situacao,urlAnexo,anexos);
        templateEmailService.update(id, dto);
        return ResponseEntity.ok().build();
    }
}
