package br.edu.faculdadevincit.crm_vincit.controller;


import br.edu.faculdadevincit.crm_vincit.model.dtos.TagDTO;
import br.edu.faculdadevincit.crm_vincit.model.dtos.TagOportunidadeDTO;
import br.edu.faculdadevincit.crm_vincit.model.dtos.TagRequestDTO;
import br.edu.faculdadevincit.crm_vincit.service.TagService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/tag")
public class TagController {

    @Autowired
    private TagService tagService;

    @GetMapping
    public List<TagDTO> findAll() {
        return tagService.findAll();
    }

    @GetMapping("/ativas")
    public List<TagOportunidadeDTO> findAllAtivas() {
        return tagService.findAllAtivas();
    }

    @GetMapping(value = "/{id}")
    public ResponseEntity<TagDTO> findById(@PathVariable Long id){
        return ResponseEntity.ok(tagService.findById(id));
    }

    @PostMapping
    public ResponseEntity<?> create(@Valid @RequestBody TagRequestDTO tag) {
        tagService.create(tag);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping(value = "/{id}")
    public ResponseEntity<?> delete(@PathVariable Long id) {
        tagService.delete(id);
        return ResponseEntity.ok(Map.of("message", "Tag apagada com sucesso"));
    }

    @PutMapping(value = "/{id}")
    public ResponseEntity<?> update(@PathVariable Long id, @RequestBody @Valid TagRequestDTO tag) {
        tagService.update(id, tag);
        return ResponseEntity.ok().build();
    }




}
