package com.juridiqsystem.crm.controller;
import com.juridiqsystem.crm.model.MensagemInterna;
import com.juridiqsystem.crm.model.dtos.MensagemInternaResponseDTO;
import com.juridiqsystem.crm.model.dtos.MensagemInternoDto;
import com.juridiqsystem.crm.service.ChatInternoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.web.bind.annotation.RestController;

import java.util.Objects;

@RestController
public class ChatInternoController {

    @Autowired
    private ChatInternoService service;

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    @MessageMapping("/sendInterno")
    public void sendMensagem(@Payload MensagemInternoDto mensagem){
        MensagemInternaResponseDTO dto = new MensagemInternaResponseDTO(service.recieveRequest(mensagem));
        String chatId = String.valueOf(dto.getGrupoId());
        messagingTemplate.convertAndSend("/topic/messages-interna/" + chatId, dto);
    }
}
