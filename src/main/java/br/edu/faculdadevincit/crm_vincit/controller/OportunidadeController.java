package br.edu.faculdadevincit.crm_vincit.controller;

import br.edu.faculdadevincit.crm_vincit.model.dtos.MoveOportunidadeDTO;
import br.edu.faculdadevincit.crm_vincit.model.dtos.OportunidadeCreateRequest;
import br.edu.faculdadevincit.crm_vincit.model.dtos.OportunidadeDTO;
import br.edu.faculdadevincit.crm_vincit.model.dtos.OportunidadeUpdateRequest;
import br.edu.faculdadevincit.crm_vincit.service.OportunidadeService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/oportunidade")
public class OportunidadeController {

    @Autowired
    private OportunidadeService oportunidadeService;

    @GetMapping
    public Page<OportunidadeDTO> findAll(@PageableDefault(size = 20) Pageable pageable) {
        return oportunidadeService.findAll(pageable);
    }

    @GetMapping(value = "/{id}")
    public ResponseEntity<OportunidadeDTO> findById(@PathVariable Long id) {
        return ResponseEntity.ok(oportunidadeService.findById(id));
    }

    @GetMapping(value = "/cliente/{id}")
    public ResponseEntity<OportunidadeDTO> findByClienteAndCriadorNull(@PathVariable Long id) {
        return ResponseEntity.ok(oportunidadeService.findByClienteAndCriadorNull(id));
    }

    @PostMapping(consumes = { MediaType.MULTIPART_FORM_DATA_VALUE })
    public ResponseEntity<OportunidadeDTO> create(
            @RequestPart("oportunidade") @Valid OportunidadeCreateRequest oportunidade,
            @RequestPart(value = "file", required = false) MultipartFile file) {

        OportunidadeDTO dto = oportunidadeService.create(oportunidade, file);
        return ResponseEntity.ok(dto);
    }

    @PutMapping(value = "/{id}", consumes = { MediaType.MULTIPART_FORM_DATA_VALUE })
    public ResponseEntity<OportunidadeDTO> update(
            @PathVariable Long id,
            @RequestPart("oportunidade") @Valid OportunidadeUpdateRequest oportunidade,
            @RequestPart(value = "file", required = false) MultipartFile file) {

        OportunidadeDTO dto = oportunidadeService.update(id, oportunidade, file);
        return ResponseEntity.ok(dto);
    }

    @PutMapping("/move")
    public ResponseEntity<?> move(@RequestBody @Valid MoveOportunidadeDTO dto) {
        oportunidadeService.movimentoOportunidade(dto.getOportunidadeId(), dto.getEtapaId(), dto.getIndice());
        return ResponseEntity.ok().build();
    }

    @DeleteMapping(value = "/{id}")
    public ResponseEntity<?> delete(@PathVariable Long id) {
        oportunidadeService.delete(id);
        return ResponseEntity.ok().build();
    }
}
