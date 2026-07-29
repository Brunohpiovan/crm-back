package br.edu.faculdadevincit.crm_vincit.service;

import br.edu.faculdadevincit.crm_vincit.model.*;
import br.edu.faculdadevincit.crm_vincit.model.dtos.MensagemDto;
import br.edu.faculdadevincit.crm_vincit.repository.ParticipanteRepository;
import br.edu.faculdadevincit.crm_vincit.repository.ProtocoloRepository;
import br.edu.faculdadevincit.crm_vincit.repository.UsuarioRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.List;

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

    @Autowired
    private CloudFrontService cloudFrontService;

    public List<Mensagem> sendMessage(MensagemDto mensagem) {
        Protocolo protocol = protocoloRepository.findById(mensagem.getId_protocolo()).orElseThrow(()-> new RuntimeException("Protocolo nao encontrado"));
        Usuario usuario = usuarioRepository.findById(mensagem.getId_sender()).orElseThrow(()->new RuntimeException("Usuario nao encontrado"));
        Participante sender = participanteRepository.findByLogin(usuario.getLogin()).orElseThrow(() ->
                new UsernameNotFoundException("Usuário não encontrado"));
        MensagemRequest mensagemRequest = new MensagemRequest();
        mensagemRequest.setTo(protocol.getParticipante().getCelular());
        mensagemRequest.setMessage(mensagem.getConteudo());
        mensagemRequest.setMedia(mensagem.getMedia());
        whatsAppService.sendWhatsAppMessage(mensagemRequest);
        if(mensagem.getMedia()!=null && !mensagem.getMedia().isEmpty() && mensagem.getConteudo() != null &&
        !mensagem.getConteudo().isEmpty()){
            return mensagemService.sendMessage(protocol, sender, mensagem.getConteudo(),mensagem.getMedia());
        }else if(mensagem.getMedia()!=null && !mensagem.getMedia().isEmpty()){
            return mensagemService.sendMessage(protocol, sender,null, mensagem.getMedia());
        }else{
            return mensagemService.sendMessage(protocol, sender, mensagem.getConteudo(),null);
        }

    }

}
