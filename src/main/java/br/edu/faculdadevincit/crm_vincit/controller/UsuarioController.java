package br.edu.faculdadevincit.crm_vincit.controller;

import br.edu.faculdadevincit.crm_vincit.model.Usuario;
import br.edu.faculdadevincit.crm_vincit.model.dtos.*;
import br.edu.faculdadevincit.crm_vincit.service.UsuarioService;
import br.edu.faculdadevincit.crm_vincit.service.exceptions.DataIntegrityViolationException;
import br.edu.faculdadevincit.crm_vincit.service.exceptions.UserNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/usuario")
public class UsuarioController {

    @Autowired
    private UsuarioService usuarioService;

    @GetMapping
    public List<UsuarioAllDTO> findAll() {
        return usuarioService.findAll();
    }

    @GetMapping("/contacts/{userId}")
    public List<UsuarioAllContactsDTO> findAllContacts(@PathVariable Long userId) {
        return usuarioService.findAllContacts(userId);
    }

    @GetMapping("/contacts/dispo/{userId}")
    public List<UsuarioAllContactsDTO> findAllContactsDispo(@PathVariable Long userId) {
        return usuarioService.findAllContactsDispo(userId);
    }

    @GetMapping("/criador")
    public List<CriadorDto> findAllcriador() {
        return usuarioService.findAllCriador();
    }

    @GetMapping("/admin")
    public List<Usuario> findCargo() {
        return usuarioService.findByAdmin();
    }

    @GetMapping(value = "/noAuth/{id}")
    public ResponseEntity<?> findByIdNoAuth(@PathVariable Long id) {
        UsuarioResponseNoAuthDto resposta = usuarioService.findByIdNoAuth(id);
        return ResponseEntity.ok(resposta);
    }

    @GetMapping(value = "/{id}")
    public ResponseEntity<?> findById(@PathVariable Long id) {
        UsuarioResponseDto resposta = usuarioService.findById(id);
        return ResponseEntity.ok(resposta);
    }

    @PostMapping
    public ResponseEntity<?> post(@RequestPart("usuario") UsuarioDTO usuarioRequest,
                                        @RequestPart(value = "foto", required = false) MultipartFile foto) {
        UsuarioAllDTO dto = usuarioService.save(usuarioRequest,foto);
        return ResponseEntity.ok(dto);

    }

    @PutMapping(value = "/{id}")
    public ResponseEntity<?> update(@PathVariable Long id,
                                    @RequestPart("usuario") UsuarioDTO usuarioRequest,
                                    @RequestPart(value = "foto", required = false) MultipartFile foto) {
        LoginResponseDTO responseDTO = usuarioService.update(id, usuarioRequest, foto);
        return ResponseEntity.ok(responseDTO);

    }

    @PutMapping(value = "/all/{id}")
    public ResponseEntity<?> updateAll(@PathVariable Long id,
                                    @RequestPart("usuario") UsuarioDTO usuarioRequest,
                                    @RequestPart(value = "foto", required = false) MultipartFile foto) {
        UsuarioAllDTO responseDTO = usuarioService.updateAll(id, usuarioRequest, foto);
        return ResponseEntity.ok(responseDTO);

    }

    @DeleteMapping(value = "/{id}")
    public ResponseEntity<?> delete(@PathVariable Long id) {
        usuarioService.delete(id);
        return ResponseEntity.ok().build();
    }
}