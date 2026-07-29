package br.edu.faculdadevincit.crm_vincit.controller;


import br.edu.faculdadevincit.crm_vincit.model.Tag;
import br.edu.faculdadevincit.crm_vincit.model.dtos.TagDTO;
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
@RequestMapping("/tag")
public class TagController {

    @Autowired
    private TagService tagService;

    @GetMapping
    public List<?> findAll() {
        return tagService.findAll();
    }

    @GetMapping("/ativas")
    public List<?> findAllAtivas() {
        return tagService.findAllAtivas();
    }

    @GetMapping(value = "/{id}")
    public ResponseEntity<?> findById(@PathVariable Long id){
        return ResponseEntity.ok(tagService.findById(id));
    }

    @PostMapping
    public ResponseEntity<?> create(@RequestBody Tag tag) {
        tagService.create(tag);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping(value = "/{id}")
    public ResponseEntity<?> delete(@PathVariable Long id) {
        tagService.delete(id);
        return ResponseEntity.ok(Map.of("message", "Tag apagada com sucesso"));
    }

    @PutMapping(value = "/{id}")
    public ResponseEntity<?> update(@PathVariable Long id, @RequestBody @Valid Tag tag) {
        tagService.update(id, tag);
        return ResponseEntity.ok().build();
    }




}
