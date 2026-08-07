package com.juridiqsystem.crm.controller;

import com.juridiqsystem.crm.model.Mensagem;
import com.juridiqsystem.crm.model.dtos.MensagemDto;
import com.juridiqsystem.crm.model.dtos.MensagemResponseDTO;
import com.juridiqsystem.crm.service.ChatService;
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
            messagingTemplate.convertAndSend("/topic/messages/" + protocoloId, new MensagemResponseDTO(mensagemNew));
        });
    }

}
