package br.edu.faculdadevincit.crm_vincit.controller;

import br.edu.faculdadevincit.crm_vincit.model.CadenciaFunil;
import br.edu.faculdadevincit.crm_vincit.model.Tag;
import br.edu.faculdadevincit.crm_vincit.model.dtos.CadenciaAllDTO;
import br.edu.faculdadevincit.crm_vincit.model.dtos.CadenciaFunilDto;
import br.edu.faculdadevincit.crm_vincit.model.dtos.CadenciaFunilRequestDto;
import br.edu.faculdadevincit.crm_vincit.service.CadenciaFunilService;
import br.edu.faculdadevincit.crm_vincit.service.TagService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/cadencia")
public class CadenciaFunilController {

    @Autowired
    private CadenciaFunilService cadenciaFunilService;

    @GetMapping
    public List<?> findAll() {
        return cadenciaFunilService.findAll();
    }

    @GetMapping(value = "/{id}")
    public ResponseEntity<?> findById(@PathVariable Long id){
        return ResponseEntity.ok(cadenciaFunilService.findById(id));
    }

    @PostMapping
    public ResponseEntity<?> create(@RequestBody CadenciaFunilRequestDto cadenciaFunil) {
        cadenciaFunilService.create(cadenciaFunil);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping(value = "/{id}")
    public ResponseEntity<?> delete(@PathVariable Long id) {
        cadenciaFunilService.delete(id);
        return ResponseEntity.ok(Map.of("message", "Cadencia apagada com sucesso"));
    }

    @PutMapping(value = "/{id}")
    public ResponseEntity<?> update(@PathVariable Long id, @RequestBody @Valid CadenciaFunilRequestDto cadenciaFunil) {
        cadenciaFunilService.update(id, cadenciaFunil);
        return ResponseEntity.ok().build();
    }
}
