package br.edu.faculdadevincit.crm_vincit.controller;

import br.edu.faculdadevincit.crm_vincit.model.Mensagem;
import br.edu.faculdadevincit.crm_vincit.model.dtos.MensagemResponseDTO;
import br.edu.faculdadevincit.crm_vincit.service.MensagemService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
public class MensagemController {

    @Autowired
    private MensagemService mensagemService;

    @GetMapping("/messages/{protocoloId}")
    public List<MensagemResponseDTO> getMessages(@PathVariable Long protocoloId) {
        List<MensagemResponseDTO> messages = mensagemService.getMessagesForProtocol(protocoloId);
        return messages;
    }

    @GetMapping("/api/messages/{protocoloId}")
    public List<MensagemResponseDTO> getMessagesLimit(@PathVariable Long protocoloId,
                                                      @RequestParam(defaultValue = "0") int offset,
                                                      @RequestParam(defaultValue = "10") int limit) {
        return mensagemService.getMessagesForProtocolLimit(protocoloId, offset, limit);
    }

    @GetMapping("/messages/public/{userId}")
    public List<Mensagem> getMessagesPublic(@PathVariable Long userId) {
        List<Mensagem> messages = mensagemService.getMessagesPublic(userId);
        return messages;
    }

    @GetMapping("messages/public/sender-ids")
    public ResponseEntity<List<Long>> getSenderIdsPublic() {
        List<Long> senderIds = mensagemService.getSenderIdsWithoutProtocol();
        return ResponseEntity.ok(senderIds);
    }


    @DeleteMapping("/messages/public/{userId}")
    public void deletePublicMessage(@PathVariable Long userId){
        mensagemService.deleteMessagesWithoutProtocol(userId);
    }
}
