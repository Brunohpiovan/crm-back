package br.edu.faculdadevincit.crm_vincit.service;

import br.edu.faculdadevincit.crm_vincit.model.Participante;
import br.edu.faculdadevincit.crm_vincit.model.dtos.ParticipanteDTO;
import br.edu.faculdadevincit.crm_vincit.model.enums.StatusProtocolo;
import br.edu.faculdadevincit.crm_vincit.model.enums.TipoParticipante;
import br.edu.faculdadevincit.crm_vincit.repository.ParticipanteRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.List;

@Service
public class ParticipanteService {

    @Autowired
    private ParticipanteRepository participanteRepository;

    @Autowired
    private CloudFrontService cloudFrontService;

    public List<ParticipanteDTO> findAllFilter(Long id){
        List<Participante> participantes = participanteRepository.findAllWithoutOpenProtocoloFromOtherAdmins(id);

        return participantes.stream()
                .map(participante -> {
                    ParticipanteDTO dto = new ParticipanteDTO(participante);

                    String urlPicture = dto.getUrlPicture();
                    if (urlPicture != null && urlPicture.contains(cloudFrontService.getBaseUrl())) {
                        String urlAssinada = cloudFrontService.generateSignedUrl(urlPicture, Duration.ofMinutes(30));
                        dto.setUrlPicture(urlAssinada);
                    }

                    return dto;
                })
                .toList();
    }


    public List<ParticipanteDTO> findAll() {
        return participanteRepository.findAll()
                .stream()
                .map(participante -> {
                    ParticipanteDTO dto = new ParticipanteDTO(participante);

                    String urlPicture = dto.getUrlPicture();
                    if (urlPicture != null && urlPicture.contains(cloudFrontService.getBaseUrl())) {
                        String urlAssinada = cloudFrontService.generateSignedUrl(urlPicture, Duration.ofMinutes(30));
                        dto.setUrlPicture(urlAssinada);
                    }

                    return dto;
                })
                .toList();
    }


    public void create(Participante participante){
        participante.setUrlPicture("assets/img/avatar/padrao.jpeg");
        participante.setId(null);
        participante.setLogin(participante.getLogin().toLowerCase());
        participanteRepository.save(participante);
    }

    public ParticipanteDTO findById(Long id) {
        Participante participante = participanteRepository.findById(id)
                .orElseThrow(() -> new UsernameNotFoundException("Participante não encontrado"));

        ParticipanteDTO dto = new ParticipanteDTO(participante);

        String urlPicture = dto.getUrlPicture();
        if (urlPicture != null && urlPicture.contains(cloudFrontService.getBaseUrl())) {
            String urlAssinada = cloudFrontService.generateSignedUrl(urlPicture, Duration.ofMinutes(30));
            dto.setUrlPicture(urlAssinada);
        }

        return dto;
    }


    public Participante update(Participante participante,Long id){
        Participante optionalParticipante = participanteRepository.findById(id).orElseThrow(() ->
                new UsernameNotFoundException("Participante não encontrado"));
        Participante newPartipante = preencheParticipante(participante,optionalParticipante);
        return participanteRepository.save(newPartipante);

    }

    public Participante preencheParticipante(Participante newParticipante, Participante participanteBanco) {
        participanteBanco.setNome(newParticipante.getNome());
        participanteBanco.setLogin(newParticipante.getLogin().toLowerCase());
        participanteBanco.setUrlPicture(newParticipante.getUrlPicture());
        participanteBanco.setRg(newParticipante.getRg());
        participanteBanco.setCpf(newParticipante.getCpf());
        participanteBanco.setDataNascimento(newParticipante.getDataNascimento());
        participanteBanco.setCelular(newParticipante.getCelular());
        participanteBanco.setEndereco(newParticipante.getEndereco());
        participanteBanco.setNumeroResidencial(newParticipante.getNumeroResidencial());
        participanteBanco.setComplemento(newParticipante.getComplemento());
        participanteBanco.setBairro(newParticipante.getBairro());
        participanteBanco.setUf(newParticipante.getUf());
        participanteBanco.setCidade(newParticipante.getCidade());
        participanteBanco.setObservacoes(newParticipante.getObservacoes());
        participanteBanco.setTipoParticipante(newParticipante.getTipoParticipante());
        return participanteBanco;
    }

    public void delete(Long id){
        Participante optionalParticipante = participanteRepository.findById(id).orElseThrow(() ->
                new UsernameNotFoundException("Participante não encontrado"));
        participanteRepository.delete(optionalParticipante);
    }

    public Participante findByCelular(String celular){
        return participanteRepository.findByCelular(celular).orElseThrow(()->new RuntimeException("Nao existe participante com esse celular cadastrado"));
    }

    public ParticipanteDTO findByLogin(String login){
        return new ParticipanteDTO(participanteRepository.findByLogin(login).orElseThrow(()->new RuntimeException("Nao existe participante com esse login cadastrado")));
    }

    public Participante findByLoginSystem(String login){
        return participanteRepository.findByLogin(login).orElseThrow(()->new RuntimeException("Nao existe participante com esse login cadastrado"));
    }
}
