package com.juridiqsystem.crm.controller;

import com.juridiqsystem.crm.infra.security.TenantContext;
import com.juridiqsystem.crm.model.Mensagem;
import com.juridiqsystem.crm.model.Usuario;
import com.juridiqsystem.crm.model.dtos.MensagemDto;
import com.juridiqsystem.crm.model.dtos.MensagemResponseDTO;
import com.juridiqsystem.crm.service.ChatService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.RestController;

import java.security.Principal;
import java.util.List;

@RestController
public class ChatController {

    @Autowired
    private ChatService service;

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    @MessageMapping("/send")
    public void sendMensagem(@Payload MensagemDto mensagem, Principal principal){
        Usuario usuarioAutenticado = (Usuario) ((Authentication) principal).getPrincipal();
        // Mesmo motivo do ChatInternoController: id_sender do payload nao e confiavel, o
        // remetente real e sempre o dono da sessao WebSocket autenticada.
        mensagem.setId_sender(usuarioAutenticado.getPublicId());

        List<Mensagem> savedMessage = TenantContext.runAs(usuarioAutenticado.getEmpresaId(),
                () -> service.sendMessage(mensagem));
        String protocoloId = String.valueOf(mensagem.getId_protocolo());
        savedMessage.forEach(mensagemNew -> {
            messagingTemplate.convertAndSend("/topic/messages/" + protocoloId, new MensagemResponseDTO(mensagemNew));
        });
    }

}
