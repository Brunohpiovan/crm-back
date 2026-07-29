package br.edu.faculdadevincit.crm_vincit.controller;

import br.edu.faculdadevincit.crm_vincit.model.dtos.ContatoDTO;
import br.edu.faculdadevincit.crm_vincit.service.ContatoService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class ContatoController {

    @Autowired
    private ContatoService contatoService;

    @PostMapping(value = "/contato")
    public ResponseEntity<?> create(@RequestBody @Valid ContatoDTO contatoDTO) {
        contatoService.create(contatoDTO);
        return ResponseEntity.ok().build();
    }
}
