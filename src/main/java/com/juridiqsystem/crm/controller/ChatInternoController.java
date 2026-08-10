package com.juridiqsystem.crm.controller;
import com.juridiqsystem.crm.infra.security.TenantContext;
import com.juridiqsystem.crm.model.MensagemInterna;
import com.juridiqsystem.crm.model.Usuario;
import com.juridiqsystem.crm.model.dtos.MensagemInternaResponseDTO;
import com.juridiqsystem.crm.model.dtos.MensagemInternoDto;
import com.juridiqsystem.crm.service.ChatInternoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.RestController;

import java.security.Principal;

@RestController
public class ChatInternoController {

    @Autowired
    private ChatInternoService service;

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    @MessageMapping("/sendInterno")
    public void sendMensagem(@Payload MensagemInternoDto mensagem, Principal principal){
        Usuario usuarioAutenticado = (Usuario) ((Authentication) principal).getPrincipal();
        // id_sender vem do payload do client, mas nao confiamos nele: o remetente real e sempre
        // o dono da sessao WebSocket autenticada (ver StompAuthChannelInterceptor), senao
        // qualquer client poderia mandar mensagem se passando por outro usuario.
        mensagem.setId_sender(usuarioAutenticado.getPublicId());

        MensagemInternaResponseDTO dto = TenantContext.runAs(usuarioAutenticado.getEmpresaId(),
                () -> new MensagemInternaResponseDTO(service.recieveRequest(mensagem)));
        String chatId = String.valueOf(dto.getGrupoId());
        messagingTemplate.convertAndSend("/topic/messages-interna/" + chatId, dto);
    }
}
