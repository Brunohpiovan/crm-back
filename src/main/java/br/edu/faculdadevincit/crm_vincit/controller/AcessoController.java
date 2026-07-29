package br.edu.faculdadevincit.crm_vincit.controller;

import br.edu.faculdadevincit.crm_vincit.model.dtos.AcessoResponseDto;
import br.edu.faculdadevincit.crm_vincit.service.AcessoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/log")
public class AcessoController {

    @Autowired
    private AcessoService acessoService;

    /*
    @PostMapping("/acesso")
    public ResponseEntity<Long> logAccess(@RequestBody LogAcessoDTO log) {
        Long logId = acessoService.save(log);
        return ResponseEntity.status(HttpStatus.CREATED).body(logId);
    }
    */


    @PutMapping("/finalizar/{id}")
    public ResponseEntity<Void> updateLogExit(@PathVariable Long id) {
        acessoService.updateExitTime(id);
        return ResponseEntity.ok().build();
    }

    @GetMapping
    public List<?> findAll() {
        return acessoService.findAllLogAcessos();
    }

    @GetMapping("/{id}")
    public List<AcessoResponseDto> findAllByUser(@PathVariable Long id) {
        return acessoService.findAllByUser(id);
    }

}
