package br.edu.faculdadevincit.crm_vincit.controller;

import br.edu.faculdadevincit.crm_vincit.model.Mensagem;
import br.edu.faculdadevincit.crm_vincit.model.MensagemInterna;
import br.edu.faculdadevincit.crm_vincit.model.dtos.MensagemInternaResponseDTO;
import br.edu.faculdadevincit.crm_vincit.service.MensagemInternaService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
public class MensagemInternaController {

    @Autowired
    private MensagemInternaService mensagemInternaService;

    @GetMapping("/messagesInterna/{grupoId}")
    public List<MensagemInternaResponseDTO> getMessagesLimit(@PathVariable Long grupoId,
                                                             @RequestParam(defaultValue = "0") int offset,
                                                             @RequestParam(defaultValue = "10") int limit) {
        return mensagemInternaService.getMessagesForProtocolLimit(grupoId, offset, limit);
    }
}
