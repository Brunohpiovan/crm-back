package br.edu.faculdadevincit.crm_vincit.controller;

import br.edu.faculdadevincit.crm_vincit.model.Mensagem;
import br.edu.faculdadevincit.crm_vincit.model.dtos.MensagemDto;
import br.edu.faculdadevincit.crm_vincit.service.ChatService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
public class ChatController {

    @Autowired
    private ChatService service;

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    @MessageMapping("/send")
    public void sendMensagem(@Payload MensagemDto mensagem){
        List<Mensagem> savedMessage = service.sendMessage(mensagem);
        String protocoloId = String.valueOf(mensagem.getId_protocolo());
        savedMessage.forEach(mensagemNew -> {
            messagingTemplate.convertAndSend("/topic/messages/" + protocoloId, mensagemNew);
        });
    }

}
