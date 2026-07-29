package br.edu.faculdadevincit.crm_vincit.controller;

import br.edu.faculdadevincit.crm_vincit.model.Protocolo;
import br.edu.faculdadevincit.crm_vincit.model.dtos.ProtocoloDto;
import br.edu.faculdadevincit.crm_vincit.model.dtos.ProtocoloMoveDTO;
import br.edu.faculdadevincit.crm_vincit.service.ProtocoloService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;

@RestController
@RequestMapping("/protocolos")
public class ProtocoloController {
    @Autowired
    private ProtocoloService protocoloService;

    @PostMapping
    public ResponseEntity<?> createProtocol(@RequestBody ProtocoloDto protocoloDto) {
        Protocolo protocolo = protocoloService.createProtocolo(protocoloDto);
        return ResponseEntity.ok(protocolo);
    }

    @DeleteMapping(value = "/{id}")
    public ResponseEntity<?> closeProtocol(@PathVariable Long id) {
        protocoloService.closeProtocolo(id);
        return ResponseEntity.ok(Map.of("message", "Protocolo fechado com sucesso"));
    }

    @GetMapping("/{idUsuario1}/{idUsuario2}")
    public ResponseEntity<?> getProtocoloByUsuario(@PathVariable Long idUsuario1, @PathVariable Long idUsuario2) {
        return protocoloService.getProtocoloByUsuario(idUsuario1, idUsuario2)
                .map(protocolo -> ResponseEntity.ok(new ProtocoloMoveDTO(protocolo)))
                .orElse(ResponseEntity.noContent().build());
    }



    @GetMapping("/get/{idUsuario}")
    public ResponseEntity<?> getProtocolo(@PathVariable Long idUsuario){
        return ResponseEntity.ok(protocoloService.getProtocols(idUsuario));
    }

    @GetMapping(value = "/{id}")
    public ResponseEntity<?> findById(@PathVariable Long id){
        return ResponseEntity.ok(protocoloService.findById(id));
    }

    @PutMapping("/encaminhar")
    public ResponseEntity<ProtocoloMoveDTO> encaminharProtocolo(@RequestParam Long id_admin, @RequestParam Long id_Protocolo) {
        ProtocoloMoveDTO protocolo = protocoloService.encaminha(id_admin, id_Protocolo);
            return new ResponseEntity<>(protocolo, HttpStatus.OK);
    }


}
