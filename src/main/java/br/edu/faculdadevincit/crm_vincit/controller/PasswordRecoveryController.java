package br.edu.faculdadevincit.crm_vincit.controller;

import br.edu.faculdadevincit.crm_vincit.model.dtos.ApiResponse;
import br.edu.faculdadevincit.crm_vincit.model.dtos.EmailDTO;
import br.edu.faculdadevincit.crm_vincit.service.auth.PasswordSendService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class PasswordRecoveryController {

    @Autowired
    private PasswordSendService service;

    @PostMapping("/recover-password")
    public ResponseEntity<ApiResponse> recoverPassword(@RequestBody EmailDTO email) {
        return ResponseEntity.ok(service.SendEmailRecovery(email));
    }
}