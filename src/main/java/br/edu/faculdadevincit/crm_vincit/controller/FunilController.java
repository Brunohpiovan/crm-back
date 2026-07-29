package br.edu.faculdadevincit.crm_vincit.controller;

import br.edu.faculdadevincit.crm_vincit.model.Funil;
import br.edu.faculdadevincit.crm_vincit.model.dtos.FiltroFunilDto;
import br.edu.faculdadevincit.crm_vincit.model.dtos.FunilAllDTO;
import br.edu.faculdadevincit.crm_vincit.model.dtos.FunilDto;
import br.edu.faculdadevincit.crm_vincit.model.dtos.UsuarioContatoDto;
import br.edu.faculdadevincit.crm_vincit.service.FunilService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/funil")
public class FunilController {

    @Autowired
    private FunilService funilService;

    @GetMapping
    public List<FunilAllDTO> findAll() {
        return funilService.findAll();
    }

    @GetMapping("/no-funil/{funilId}")
    public ResponseEntity<List<UsuarioContatoDto>> findFuncionarios(@PathVariable Long funilId) {
        List<UsuarioContatoDto> funcionarios = funilService.findFuncionariosFunil(funilId);
        return ResponseEntity.ok(funcionarios);
    }

    @GetMapping(value = "/{id}")
    public ResponseEntity<?> findById(@PathVariable Long id) {
        FunilDto funil = funilService.findById(id);
        return ResponseEntity.ok(funil);
    }

    @PostMapping("/filtro")
    public ResponseEntity<?> findByIdAndSituacao(@RequestBody FiltroFunilDto filtro) {
        FunilDto funil = funilService.findByIdAndSituacao(filtro.getId(), filtro.getSituacoes(),filtro.getTags());
        return funil != null ? ResponseEntity.ok(funil) : ResponseEntity.notFound().build();
    }


    @PostMapping
    public ResponseEntity<FunilDto> create(@RequestBody Funil funil) {
        FunilDto dto = funilService.create(funil);
        return ResponseEntity.ok(dto);
    }


    @PostMapping("/add-funcionario/{funilId}/{funcionarioId}")
    public ResponseEntity<FunilDto> addFuncionario(@PathVariable Long funilId,@PathVariable Long funcionarioId) {
        funilService.adicionarFuncionarioFunil(funcionarioId,funilId);
        return ResponseEntity.ok().build();
    }

    @PutMapping(value = "/{id}")
    public ResponseEntity<?> update(@PathVariable Long id,@RequestBody FunilAllDTO funilRequest ) {
        FunilAllDTO funil = funilService.update(id, funilRequest);
        return ResponseEntity.ok(funil);
    }

    @DeleteMapping(value = "/{id}")
    public ResponseEntity<?> delete(@PathVariable Long id) {
        funilService.delete(id);
        return ResponseEntity.ok().build();
    }
}
