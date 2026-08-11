package com.juridiqsystem.crm.service;

import com.juridiqsystem.crm.model.Mensagem;
import com.juridiqsystem.crm.model.Participante;
import com.juridiqsystem.crm.model.dtos.ParticipanteCreateRequest;
import com.juridiqsystem.crm.model.dtos.ParticipanteDTO;
import com.juridiqsystem.crm.model.dtos.ParticipanteUpdateRequest;
import com.juridiqsystem.crm.model.enums.StatusProtocolo;
import com.juridiqsystem.crm.model.enums.TipoParticipante;
import com.juridiqsystem.crm.model.Usuario;
import com.juridiqsystem.crm.repository.MensagemRepository;
import com.juridiqsystem.crm.repository.ParticipanteRepository;
import com.juridiqsystem.crm.repository.ProtocoloRepository;
import com.juridiqsystem.crm.repository.UsuarioRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class ParticipanteService {

    @Autowired
    private ParticipanteRepository participanteRepository;

    @Autowired
    private ProtocoloRepository protocoloRepository;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private MensagemRepository mensagemRepository;

    public List<ParticipanteDTO> findAllFilter(String id){
        Usuario admin = usuarioRepository.findByPublicId(id)
                .orElseThrow(() -> new UsernameNotFoundException("Usuário não encontrado"));
        List<Participante> participantes = participanteRepository.findAllWithoutOpenProtocoloFromOtherAdmins(admin.getId());
        Set<Long> comProtocoloAbertoComigo = new HashSet<>(protocoloRepository.findParticipanteIdsComProtocoloAbertoPorAdmin(admin.getId()));

        // Preview de lastMessage vem de fontes diferentes dependendo do estado do participante:
        // com protocolo ABERTO comigo, mostra a última mensagem desse atendimento; sem protocolo
        // aberto, mostra a última mensagem "pública" (ainda sem protocolo) se houver uma — nunca
        // mensagem de um protocolo já FECHADO, senão a conversa parece "aberta" sem estar.
        List<Participante> comProtocoloAberto = participantes.stream()
                .filter(p -> comProtocoloAbertoComigo.contains(p.getId()))
                .toList();
        List<Participante> semProtocoloAberto = participantes.stream()
                .filter(p -> !comProtocoloAbertoComigo.contains(p.getId()))
                .toList();
        Map<Long, Mensagem> ultimaMensagemDeProtocoloAberto = buscarUltimasMensagensPorParticipante(comProtocoloAberto);
        Map<Long, Mensagem> ultimaMensagemPublica = buscarUltimasMensagensPublicas(semProtocoloAberto);

        return participantes.stream()
                .map(p -> {
                    boolean openProtocol = comProtocoloAbertoComigo.contains(p.getId());
                    ParticipanteDTO dto = new ParticipanteDTO(p, openProtocol);
                    Mensagem ultimaMensagem = openProtocol
                            ? ultimaMensagemDeProtocoloAberto.get(p.getId())
                            : ultimaMensagemPublica.get(p.getId());
                    if (ultimaMensagem != null) {
                        dto.setLastMessage(ultimaMensagem.getConteudo());
                        dto.setLastMessageAt(ultimaMensagem.getData_envio());
                    }
                    return dto;
                })
                .toList();
    }

    /**
     * Resolve a última mensagem do protocolo ABERTO de cada participante em 2 queries batched
     * (não N+1): primeiro o protocolo mais recente de cada um (que, para quem está nesta lista,
     * é sempre o aberto), depois a última mensagem desse protocolo.
     */
    private Map<Long, Mensagem> buscarUltimasMensagensPorParticipante(List<Participante> participantes) {
        if (participantes.isEmpty()) return Collections.emptyMap();

        List<Long> participanteIds = participantes.stream().map(Participante::getId).toList();
        Map<Long, Long> ultimoProtocoloIdPorParticipante = protocoloRepository
                .findUltimosProtocoloIdsPorParticipantes(participanteIds).stream()
                .collect(Collectors.toMap(
                        ProtocoloRepository.UltimoProtocoloProjection::getParticipanteId,
                        ProtocoloRepository.UltimoProtocoloProjection::getProtocoloId));

        if (ultimoProtocoloIdPorParticipante.isEmpty()) return Collections.emptyMap();

        List<Long> protocoloIds = List.copyOf(ultimoProtocoloIdPorParticipante.values());
        Map<Long, Mensagem> ultimaMensagemPorProtocolo = mensagemRepository
                .findUltimasMensagensPorProtocolos(protocoloIds).stream()
                .collect(Collectors.toMap(m -> m.getProtocolo().getId(), m -> m));

        Map<Long, Mensagem> resultado = new HashMap<>();
        ultimoProtocoloIdPorParticipante.forEach((participanteId, protocoloId) -> {
            Mensagem mensagem = ultimaMensagemPorProtocolo.get(protocoloId);
            if (mensagem != null) resultado.put(participanteId, mensagem);
        });
        return resultado;
    }

    /**
     * Última mensagem "pública" (sem protocolo ainda) de cada participante, batched (não N+1).
     * Participantes sem nenhuma mensagem pública ficam de fora do mapa retornado.
     */
    private Map<Long, Mensagem> buscarUltimasMensagensPublicas(List<Participante> participantes) {
        if (participantes.isEmpty()) return Collections.emptyMap();

        List<Long> participanteIds = participantes.stream().map(Participante::getId).toList();
        return mensagemRepository.findUltimasMensagensPublicasPorSenders(participanteIds).stream()
                .collect(Collectors.toMap(m -> m.getSender().getId(), m -> m));
    }


    public List<ParticipanteDTO> findAll() {
        return participanteRepository.findAll()
                .stream()
                .map(ParticipanteDTO::new)
                .toList();
    }

    /**
     * Versão paginada de {@link #findAll()}, que continua sem limite (mantida por
     * compatibilidade com o frontend atual). Usar esta para telas/listagens novas, dado que a
     * tabela de participantes cresce com todo contato recebido via WhatsApp.
     */
    public Page<ParticipanteDTO> findAllPaginado(Pageable pageable) {
        return participanteRepository.findAll(pageable).map(ParticipanteDTO::new);
    }


    public void create(ParticipanteCreateRequest request){
        Participante participante = new Participante();
        participante.setNome(request.nome());
        participante.setLogin(request.login());
        participante.setRg(request.rg());
        participante.setCpf(request.cpf());
        participante.setDataNascimento(request.dataNascimento());
        participante.setCelular(request.celular());
        participante.setEndereco(request.endereco());
        participante.setNumeroResidencial(request.numeroResidencial());
        participante.setComplemento(request.complemento());
        participante.setBairro(request.bairro());
        participante.setUf(request.uf());
        participante.setCidade(request.cidade());
        participante.setObservacoes(request.observacoes());
        participante.setTipoParticipante(request.tipoParticipante());
        create(participante);
    }

    /**
     * Usado internamente (ex.: espelhamento de Usuario/Oportunidade.cliente como Participante),
     * fora do fluxo de POST /participante — que usa {@link #create(ParticipanteCreateRequest)}.
     */
    public void create(Participante participante){
        participante.setUrlPicture("assets/img/avatar/padrao.jpeg");
        participante.setId(null);
        participante.setLogin(participante.getLogin().toLowerCase());
        participanteRepository.save(participante);
    }

    public ParticipanteDTO findById(String id) {
        Participante participante = participanteRepository.findByPublicId(id)
                .orElseThrow(() -> new UsernameNotFoundException("Participante não encontrado"));

        return new ParticipanteDTO(participante);
    }


    public Participante update(ParticipanteUpdateRequest request, String id){
        Participante participante = new Participante();
        participante.setUrlPicture(request.urlPicture());
        participante.setNome(request.nome());
        participante.setLogin(request.login());
        participante.setRg(request.rg());
        participante.setCpf(request.cpf());
        participante.setDataNascimento(request.dataNascimento());
        participante.setCelular(request.celular());
        participante.setEndereco(request.endereco());
        participante.setNumeroResidencial(request.numeroResidencial());
        participante.setComplemento(request.complemento());
        participante.setBairro(request.bairro());
        participante.setUf(request.uf());
        participante.setCidade(request.cidade());
        participante.setObservacoes(request.observacoes());
        participante.setTipoParticipante(request.tipoParticipante());
        Participante existente = participanteRepository.findByPublicId(id)
                .orElseThrow(() -> new UsernameNotFoundException("Participante não encontrado"));
        return update(participante, existente.getId());
    }

    /**
     * Usado internamente (ex.: sincronização de Participante ao atualizar um Usuario),
     * fora do fluxo de PUT /participante/{id} — que usa {@link #update(ParticipanteUpdateRequest, Long)}.
     */
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

    public void delete(String id){
        Participante optionalParticipante = participanteRepository.findByPublicId(id).orElseThrow(() ->
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
