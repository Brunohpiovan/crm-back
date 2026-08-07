package com.juridiqsystem.crm.service;

import com.juridiqsystem.crm.model.*;
import com.juridiqsystem.crm.model.dtos.MensagemDto;
import com.juridiqsystem.crm.model.dtos.MensagemInternaResponseDTO;
import com.juridiqsystem.crm.model.dtos.MensagemInternoDto;
import com.juridiqsystem.crm.model.dtos.UsuarioAllContactsDTO;
import com.juridiqsystem.crm.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

@Service
public class ChatInternoService {
    @Autowired
    private MensagemInternaRepository mensagemService;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private ChatGrupoRepository chatGrupoRepository;

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    public MensagemInterna recieveRequest(MensagemInternoDto mensagem){
        MensagemInterna savedMessage;
        if(Objects.equals(mensagem.getId_reciver(), mensagem.getId_group())){
            savedMessage = sendMessageGroup(mensagem);
        }else{
            savedMessage = sendMessage(mensagem);
        }
        return savedMessage;
    }

    public MensagemInterna sendMessage(MensagemInternoDto mensagem) {
        Usuario sender = usuarioRepository.findByPublicId(mensagem.getId_sender()).orElseThrow(()->new RuntimeException("Usuario Sender nao encontrado"));
        Usuario reciver = usuarioRepository.findByPublicId(mensagem.getId_reciver()).orElseThrow(()->new RuntimeException("Usuario Reciver nao encontrado"));
        MensagemInterna mensagemNew = new MensagemInterna();
        if(mensagem.getId_group() == null){
            Optional<ChatGrupo> grupoExistente = chatGrupoRepository
                    .findGrupoPrivadoByUsuarios(sender.getId(), reciver.getId())
                    .stream()
                    .min(Comparator.comparing(ChatGrupo::getCriadoEm));

            if (grupoExistente.isPresent()) {
                mensagemNew.setChatGrupo(grupoExistente.get());
            } else {
                mensagemNew.setChatGrupo(criaGroup(sender, reciver));
                messagingTemplate.convertAndSend("/topic/newGroup/" + sender.getPublicId(), criaDto(reciver));
                messagingTemplate.convertAndSend("/topic/newGroup/" + reciver.getPublicId(), criaDto(sender));
            }
        }else{
            mensagemNew.setChatGrupo(chatGrupoRepository.findByPublicId(mensagem.getId_group()).orElseThrow(()->new RuntimeException("chatGrupo nao encontrado")));
        }
        mensagemNew.setConteudo(mensagem.getConteudo());
        mensagemNew.setSender(sender);
        mensagemNew.setDataEnvio(LocalDateTime.now());
        return mensagemService.save(mensagemNew);
    }

    public MensagemInterna sendMessageGroup(MensagemInternoDto mensagem){
        Usuario sender = usuarioRepository.findByPublicId(mensagem.getId_sender()).orElseThrow(()->new RuntimeException("Usuario Sender nao encontrado"));
        MensagemInterna mensagemNew = new MensagemInterna();
        mensagemNew.setChatGrupo(chatGrupoRepository.findByPublicId(mensagem.getId_group()).orElseThrow(()->new RuntimeException("chatGrupo nao encontrado")));
        mensagemNew.setConteudo(mensagem.getConteudo());
        mensagemNew.setSender(sender);
        mensagemNew.setDataEnvio(LocalDateTime.now());
        return mensagemService.save(mensagemNew);
    }

    private UsuarioAllContactsDTO criaDto(Usuario usuario){
        return new UsuarioAllContactsDTO(usuario);
    }

    public ChatGrupo criaGroup(Usuario sender,Usuario reciver){
        return instaciaGroup(sender,reciver);
    }

    private ChatGrupo instaciaGroup(Usuario sender,Usuario reciver){
        ChatGrupo grupo = new ChatGrupo();
        grupo.setAvatarUrl(null);
        grupo.setImagemFundoUrl(null);
        grupo.setNome(null);
        grupo.setPrivado(true);
        List<Usuario> list = new ArrayList<>();
        list.add(sender);
        list.add(reciver);
        grupo.setUsuarios(list);
        grupo.setCriadoEm(LocalDateTime.now());
        return chatGrupoRepository.save(grupo);

    }
}
