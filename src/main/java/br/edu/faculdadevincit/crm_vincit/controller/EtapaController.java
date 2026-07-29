package br.edu.faculdadevincit.crm_vincit.controller;

import br.edu.faculdadevincit.crm_vincit.model.Etapa;
import br.edu.faculdadevincit.crm_vincit.model.dtos.EtapaDto;
import br.edu.faculdadevincit.crm_vincit.model.dtos.EtapaFunilDTO;
import br.edu.faculdadevincit.crm_vincit.model.dtos.EtapaUpdate2DTO;
import br.edu.faculdadevincit.crm_vincit.model.dtos.EtapaUpdateDTO;
import br.edu.faculdadevincit.crm_vincit.service.EtapaService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/etapa")
public class EtapaController {


    @Autowired
    private EtapaService etapaService;

    @GetMapping
    public List<EtapaDto> findAll() {
        return etapaService.findAll();
    }

    @GetMapping(value = "funil/{id}")
    public List<EtapaFunilDTO> findByFunilId(@PathVariable Long id){
        return etapaService.findByFunilId(id);
    }

    @GetMapping(value = "/{id}")
    public ResponseEntity<?> findById(@PathVariable Long id) {
        EtapaDto etapa = etapaService.findById(id);
        return ResponseEntity.ok(etapa);
    }

    @PostMapping
    public ResponseEntity<?> create(@RequestBody Etapa etapa) {
        EtapaDto newEtapa = etapaService.create(etapa);
        return ResponseEntity.ok(newEtapa);
    }

    @PutMapping(value = "/{id}")
    public ResponseEntity<?> update(@PathVariable Long id,@RequestBody EtapaUpdateDTO etapaRequest) {
        etapaService.update(id, etapaRequest);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping(value = "/{id}")
    public ResponseEntity<?> delete(@PathVariable Long id) {
        etapaService.delete(id);
        return ResponseEntity.ok().build();
    }
}
