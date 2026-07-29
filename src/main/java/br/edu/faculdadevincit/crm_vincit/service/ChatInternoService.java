package br.edu.faculdadevincit.crm_vincit.service;

import br.edu.faculdadevincit.crm_vincit.model.*;
import br.edu.faculdadevincit.crm_vincit.model.dtos.MensagemDto;
import br.edu.faculdadevincit.crm_vincit.model.dtos.MensagemInternaResponseDTO;
import br.edu.faculdadevincit.crm_vincit.model.dtos.MensagemInternoDto;
import br.edu.faculdadevincit.crm_vincit.model.dtos.UsuarioAllContactsDTO;
import br.edu.faculdadevincit.crm_vincit.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

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
        Usuario sender = usuarioRepository.findById(mensagem.getId_sender()).orElseThrow(()->new RuntimeException("Usuario Sender nao encontrado"));
        Usuario reciver = usuarioRepository.findById(mensagem.getId_reciver()).orElseThrow(()->new RuntimeException("Usuario Reciver nao encontrado"));
        MensagemInterna mensagemNew = new MensagemInterna();
        if(mensagem.getId_group() == null){
            mensagemNew.setChatGrupo(criaGroup(sender,reciver));
            messagingTemplate.convertAndSend("/topic/newGroup/" + sender.getId(), criaDto(reciver));
            messagingTemplate.convertAndSend("/topic/newGroup/" + reciver.getId(), criaDto(sender));
        }else{
            mensagemNew.setChatGrupo(chatGrupoRepository.findById(mensagem.getId_group()).orElseThrow(()->new RuntimeException("chatGrupo nao encontrado")));
        }
        mensagemNew.setConteudo(mensagem.getConteudo());
        mensagemNew.setSender(sender);
        mensagemNew.setDataEnvio(LocalDateTime.now());
        return mensagemService.save(mensagemNew);
    }

    public MensagemInterna sendMessageGroup(MensagemInternoDto mensagem){
        Usuario sender = usuarioRepository.findById(mensagem.getId_sender()).orElseThrow(()->new RuntimeException("Usuario Sender nao encontrado"));
        MensagemInterna mensagemNew = new MensagemInterna();
        mensagemNew.setChatGrupo(chatGrupoRepository.findById(mensagem.getId_group()).orElseThrow(()->new RuntimeException("chatGrupo nao encontrado")));
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
