package br.edu.faculdadevincit.crm_vincit.controller;

import br.edu.faculdadevincit.crm_vincit.model.Oportunidade;
import br.edu.faculdadevincit.crm_vincit.model.dtos.MoveOportunidadeDTO;
import br.edu.faculdadevincit.crm_vincit.model.dtos.OportunidadeDTO;
import br.edu.faculdadevincit.crm_vincit.service.OportunidadeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/oportunidade")
public class OportunidadeController {

    @Autowired
    private OportunidadeService oportunidadeService;

    @GetMapping
    public List<OportunidadeDTO> findAll() {
        return oportunidadeService.findAll();
    }

    @GetMapping(value = "/{id}")
    public ResponseEntity<?> findById(@PathVariable Long id) {
        OportunidadeDTO oportunidade = oportunidadeService.findById(id);
        return ResponseEntity.ok(oportunidade);
    }

    @GetMapping(value = "/cliente/{id}")
    public ResponseEntity<?> findByClienteAndCriadorNull(@PathVariable Long id) {
        OportunidadeDTO oportunidade = oportunidadeService.findByClienteAndCriadorNull(id);
        return ResponseEntity.ok(oportunidade);
    }

    @PostMapping(consumes = { MediaType.MULTIPART_FORM_DATA_VALUE })
    public ResponseEntity<OportunidadeDTO> create(
            @RequestPart("oportunidade") Oportunidade oportunidade,
            @RequestPart(value = "file", required = false) MultipartFile file) {

        oportunidadeService.create(oportunidade, file);
        return ResponseEntity.ok().build();
    }


    @PutMapping(value = "/{id}", consumes = { MediaType.MULTIPART_FORM_DATA_VALUE })
    public ResponseEntity<?> update(
            @PathVariable Long id,
            @RequestPart("oportunidade") Oportunidade oportunidadeRequest,
            @RequestPart(value = "file", required = false) MultipartFile file) {

        oportunidadeService.update(id, oportunidadeRequest, file);
        return ResponseEntity.ok().build();
    }


    @PutMapping("/move")
    public ResponseEntity<?> move(@RequestBody MoveOportunidadeDTO dto) {
        oportunidadeService.movimentoOportunidade(dto.getOportunidadeId(), dto.getEtapaId(), dto.getIndice());
        return ResponseEntity.ok().build();
    }

    @DeleteMapping(value = "/{id}")
    public ResponseEntity<?> delete(@PathVariable Long id) {
        oportunidadeService.delete(id);
        return ResponseEntity.ok().build();
    }
}
