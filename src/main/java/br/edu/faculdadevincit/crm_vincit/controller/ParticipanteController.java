package br.edu.faculdadevincit.crm_vincit.controller;

import br.edu.faculdadevincit.crm_vincit.model.Participante;
import br.edu.faculdadevincit.crm_vincit.model.Usuario;
import br.edu.faculdadevincit.crm_vincit.model.dtos.ParticipanteDTO;
import br.edu.faculdadevincit.crm_vincit.service.ParticipanteService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/participante")
public class ParticipanteController {
    @Autowired
    ParticipanteService participanteService;


    @GetMapping
    public List<ParticipanteDTO> findAll() {
        return participanteService.findAll();
    }

    @GetMapping(value = "/filter/{id}")
    public List<ParticipanteDTO> findAllFilter(@PathVariable Long id) {

        return participanteService.findAllFilter(id);
    }

    @GetMapping(value = "/{id}")
    public ResponseEntity<?> findById(@PathVariable Long id) {
        ParticipanteDTO resposta = participanteService.findById(id);
        return ResponseEntity.ok(resposta);
    }

    @PostMapping
    public ResponseEntity<?> post(@RequestBody Participante participante) {
        participanteService.create(participante);
        return ResponseEntity.ok().build();
    }

    @PutMapping(value = "/{id}")
    public ResponseEntity<?> update(@PathVariable Long id,@RequestBody Participante participante ) {
        Participante participanteResposta = participanteService.update(participante,id);
        return ResponseEntity.ok(participanteResposta);
    }

    @DeleteMapping(value = "/{id}")
    public ResponseEntity<?> delete(@PathVariable Long id) {
        participanteService.delete(id);
        return ResponseEntity.ok().build();
    }

    @GetMapping(value = "/celular/{celular}")
    public ResponseEntity<?> findByCelular(@PathVariable String celular) {
        return ResponseEntity.ok(participanteService.findByCelular(celular));
    }

    @GetMapping(value = "/login/{login}")
    public ResponseEntity<?> findByLogin(@PathVariable String login) {
        return ResponseEntity.ok(participanteService.findByLogin(login));
    }


}
