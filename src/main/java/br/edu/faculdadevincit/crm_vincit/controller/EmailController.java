package br.edu.faculdadevincit.crm_vincit.controller;

import br.edu.faculdadevincit.crm_vincit.model.dtos.EmailRequestDTO;
import br.edu.faculdadevincit.crm_vincit.service.auth.EmailService;
import jakarta.mail.MessagingException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.UnsupportedEncodingException;
import java.util.List;

@RestController
@RequestMapping("/email")
public class EmailController {

    @Autowired
    private EmailService emailService;

    @PostMapping(value = "/enviar", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<String> enviarEmail(
            @RequestParam("destinatario") String destinatario,
            @RequestParam("assunto") String assunto,
            @RequestParam("corpo") String corpo,
            @RequestParam("id_remetente") Long idRemetente,
            @RequestParam(value = "anexos", required = false) List<MultipartFile> anexos) throws MessagingException, UnsupportedEncodingException {
            EmailRequestDTO email = new EmailRequestDTO(destinatario.toLowerCase(), assunto, corpo, idRemetente, anexos);
            emailService.enviarEmail(email);
            return ResponseEntity.ok().build();
    }

}
