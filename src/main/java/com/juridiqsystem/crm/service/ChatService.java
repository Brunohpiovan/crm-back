package com.juridiqsystem.crm.service;

import com.juridiqsystem.crm.model.*;
import com.juridiqsystem.crm.model.dtos.MensagemDto;
import com.juridiqsystem.crm.model.dtos.WhatsAppSendResultDTO;
import com.juridiqsystem.crm.repository.ParticipanteRepository;
import com.juridiqsystem.crm.repository.ProtocoloRepository;
import com.juridiqsystem.crm.repository.UsuarioRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
public class ChatService {

    @Autowired
    private MensagemService mensagemService;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private ProtocoloRepository protocoloRepository;

    @Autowired
    private WhatsAppService whatsAppService;

    @Autowired
    private ParticipanteRepository participanteRepository;

    public List<Mensagem> sendMessage(MensagemDto mensagem) {
        Protocolo protocol = protocoloRepository.findByPublicId(mensagem.getId_protocolo()).orElseThrow(()-> new RuntimeException("Protocolo nao encontrado"));
        Usuario usuario = usuarioRepository.findByPublicId(mensagem.getId_sender()).orElseThrow(()->new RuntimeException("Usuario nao encontrado"));
        Participante sender = participanteRepository.findByLogin(usuario.getLogin()).orElseThrow(() ->
                new UsernameNotFoundException("Usuário não encontrado"));
        MensagemRequest mensagemRequest = new MensagemRequest();
        mensagemRequest.setTo(protocol.getParticipante().getCelular());
        mensagemRequest.setMessage(mensagem.getConteudo());
        mensagemRequest.setMedia(mensagem.getMedia());
        WhatsAppSendResultDTO resultado = whatsAppService.sendWhatsAppMessage(mensagemRequest);
        // A partir daqui a mensagem já foi enviada ao WhatsApp do cliente. Se a persistência abaixo
        // falhar, a mensagem existe no WhatsApp mas não no histórico do CRM — logamos com contexto
        // suficiente para permitir reconciliação manual, sem esconder a exceção original.
        try {
            if(mensagem.getMedia()!=null && !mensagem.getMedia().isEmpty() && mensagem.getConteudo() != null &&
            !mensagem.getConteudo().isEmpty()){
                return mensagemService.sendMessage(protocol, sender, mensagem.getConteudo(), mensagem.getMedia(), resultado.externalMessageId(), resultado.status());
            }else if(mensagem.getMedia()!=null && !mensagem.getMedia().isEmpty()){
                return mensagemService.sendMessage(protocol, sender, null, mensagem.getMedia(), resultado.externalMessageId(), resultado.status());
            }else{
                return mensagemService.sendMessage(protocol, sender, mensagem.getConteudo(), null, resultado.externalMessageId(), resultado.status());
            }
        } catch (RuntimeException e) {
            log.error("Mensagem enviada via WhatsApp para o protocolo {} (participante {}), mas a persistência no CRM falhou.",
                    protocol.getId(), protocol.getParticipante().getId(), e);
            throw e;
        }
    }

}
