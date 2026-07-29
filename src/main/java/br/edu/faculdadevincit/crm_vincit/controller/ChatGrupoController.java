package br.edu.faculdadevincit.crm_vincit.controller;

import br.edu.faculdadevincit.crm_vincit.model.Oportunidade;
import br.edu.faculdadevincit.crm_vincit.model.dtos.ChatGrupoResponseById;
import br.edu.faculdadevincit.crm_vincit.model.dtos.GrupoCreateDTO;
import br.edu.faculdadevincit.crm_vincit.model.dtos.OportunidadeDTO;
import br.edu.faculdadevincit.crm_vincit.service.ChatGrupoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/grupos")
public class ChatGrupoController {

    @Autowired
    private ChatGrupoService chatGrupoService;

    @GetMapping("/{idUsuario1}/{idUsuario2}")
    public ResponseEntity<?> getProtocoloByUsuario(@PathVariable Long idUsuario1, @PathVariable Long idUsuario2) {
        return chatGrupoService.getGrupoByUsuario(idUsuario1, idUsuario2)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.noContent().build());
    }


    @GetMapping("/{idUsuario}")
    public ResponseEntity<?> getProtocoloByUsuarioAndPublic(@PathVariable Long idUsuario) {
        return chatGrupoService.getGrupoByUsuarioAndPublic(idUsuario)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.noContent().build());
    }


    @GetMapping("/id/{idUsuario}")
    public ResponseEntity<?> findById(@PathVariable Long idUsuario){
        ChatGrupoResponseById grupo = chatGrupoService.findById(idUsuario);
        return ResponseEntity.ok(grupo);
    }


    @PostMapping
    public ResponseEntity<?> create(
            @RequestPart("grupo") GrupoCreateDTO grupoCreateDTO,
            @RequestPart(value = "foto", required = false) MultipartFile foto,
            @RequestPart(value = "imagemFundo", required = false) MultipartFile imagemFundo) {
            chatGrupoService.create(grupoCreateDTO, foto, imagemFundo);
            return ResponseEntity.ok().build();
    }


    @PutMapping(value = "/{id}", consumes = { MediaType.MULTIPART_FORM_DATA_VALUE })
    public ResponseEntity<?> update(
            @PathVariable Long id,
            @RequestPart("grupo") GrupoCreateDTO grupo,
            @RequestPart(value = "foto", required = false) MultipartFile file,
            @RequestPart(value = "imagemFundo", required = false) MultipartFile imagemFundo) {

        chatGrupoService.update(id, grupo, file,imagemFundo);
        return ResponseEntity.ok().build();
    }
}
