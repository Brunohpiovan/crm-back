package com.juridiqsystem.crm.service;

import com.juridiqsystem.crm.model.Etapa;
import com.juridiqsystem.crm.model.Funil;
import com.juridiqsystem.crm.model.Tag;
import com.juridiqsystem.crm.model.Usuario;
import com.juridiqsystem.crm.model.dtos.*;
import com.juridiqsystem.crm.model.enums.SituacaoOportunidade;
import com.juridiqsystem.crm.model.enums.UserRole;
import com.juridiqsystem.crm.repository.EtapaRepository;
import com.juridiqsystem.crm.repository.FunilRepository;
import com.juridiqsystem.crm.repository.OportunidadeRepository;
import com.juridiqsystem.crm.repository.TagRepository;
import com.juridiqsystem.crm.repository.UsuarioRepository;
import com.juridiqsystem.crm.service.exceptions.ConflictException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class FunilService {

    @Autowired
    private FunilRepository funilRepository;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private EtapaService etapaService;

    @Autowired
    private EtapaRepository etapaRepository;

    @Autowired
    private OportunidadeRepository oportunidadeRepository;

    @Autowired
    private TagRepository tagRepository;

    public List<FunilAllDTO> findAll() {
        Usuario usuario = getUsuarioAutenticado();

        List<Funil> funis;
        if (usuario.getCargo() == UserRole.ADMINISTRADOR) {
            funis = funilRepository.findAll();
        }else {
            funis = funilRepository.findByFuncionariosContains(usuario);
        }
        return funis.stream().map(FunilAllDTO::new).collect(Collectors.toList());
    }

    private Usuario getUsuarioAutenticado() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return (Usuario) authentication.getPrincipal();
    }


    public List<UsuarioContatoDto> findFuncionariosFunil(String funilId) {
        Funil funil = funilRepository.findByPublicId(funilId)
                .orElseThrow(() -> new RuntimeException("Funil não encontrado"));

        List<UsuarioContatoDto> usuariosNaoNoFunil =
                usuarioRepository.findDisponiveisParaFunil(funil.getId(), UserRole.ADMINISTRADOR);

        return usuariosNaoNoFunil;
    }


    public void adicionarFuncionarioFunil(String funcionarioId,String funilId){
        Funil funil = funilRepository.findByPublicId(funilId)
                .orElseThrow(() -> new RuntimeException("Funil não encontrado"));
        Usuario funcionario = usuarioRepository.findByPublicId(funcionarioId).orElseThrow(() -> new RuntimeException("Funcionario não encontrado"));
        if (!funil.getFuncionarios().contains(funcionario)) {
            funil.getFuncionarios().add(funcionario);
        }
        if (!funcionario.getFunisPermitidos().contains(funil)) {
            funcionario.getFunisPermitidos().add(funil);
        }
        funilRepository.save(funil);
        usuarioRepository.save(funcionario);
    }


    public FunilDto findById(String id) {
        Funil funil = funilRepository.findByPublicIdWithEtapas(id)
                .orElseThrow(() -> new RuntimeException("Funil com id " + id + " não encontrado"));

        List<Long> etapaIds = funil.getEtapas().stream().map(Etapa::getId).toList();
        Map<Long, List<OportunidadeDTO>> oportunidadesPorEtapa = etapaService.carregarOportunidadesPorEtapa(etapaIds);

        List<EtapaDto> etapaDtos = funil.getEtapas().stream()
                .map(etapa -> new EtapaDto(etapa, oportunidadesPorEtapa.getOrDefault(etapa.getId(), List.of())))
                .collect(Collectors.toList());

        return new FunilDto(funil.getPublicId(), funil.getNome(), etapaDtos);
    }

    public FunilDto findByIdAndSituacao(String id, List<SituacaoOportunidade> situacoes, List<TagOportunidadeDTO> tags) {
        Funil funil = funilRepository.findByPublicIdWithEtapas(id).orElse(null);
        if (funil == null) return null;

        List<Long> tagIds = tags != null && !tags.isEmpty()
                ? tagRepository.findAllByPublicIdIn(tags.stream().map(TagOportunidadeDTO::getId).toList())
                        .stream().map(Tag::getId).toList()
                : List.of();
        List<Long> etapaIds = funil.getEtapas().stream().map(Etapa::getId).toList();

        Map<Long, List<OportunidadeDTO>> oportunidadesPorEtapa =
                etapaService.carregarOportunidadesPorEtapa(etapaIds, situacoes, tagIds);

        List<EtapaDto> etapaDtos = funil.getEtapas().stream()
                .map(etapa -> new EtapaDto(etapa, oportunidadesPorEtapa.getOrDefault(etapa.getId(), List.of())))
                .collect(Collectors.toList());

        return new FunilDto(funil.getPublicId(), funil.getNome(), etapaDtos);
    }


    public FunilDto create(FunilCreateRequest funilCreateRequest) {
        Funil funil = new Funil();
        funil.setNome(funilCreateRequest.nome());
        funil.setCriadoEm(LocalDateTime.now());
        Funil savedFunil = funilRepository.save(funil);
        return new FunilDto(savedFunil);
    }

    public FunilAllDTO update(String id, FunilAllDTO funil) {
        Funil funilBanco = funilRepository.findByPublicId(id)
                .orElseThrow(() -> new RuntimeException("Funil com id " + id + " não encontrado"));
        funilBanco.setNome(funil.getNome());
        Funil savedFunil = funilRepository.save(funilBanco);
        savedFunil.setAtualizadoEm(LocalDateTime.now());
        return new FunilAllDTO(savedFunil);
    }

    public void delete(String id) {
        Funil funilBanco = funilRepository.findByPublicId(id)
                .orElseThrow(() -> new RuntimeException("Funil com id " + id + " não encontrado"));
        List<Long> etapaIds = etapaRepository.findByFunilId(funilBanco.getId()).stream().map(Etapa::getId).toList();
        long oportunidadesAtivas = etapaIds.isEmpty() ? 0 : oportunidadeRepository.countPorEtapaIdIn(etapaIds);
        if (oportunidadesAtivas > 0) {
            throw new ConflictException("Não é possível excluir o funil: existem " + oportunidadesAtivas
                    + " oportunidade(s) em suas etapas. Mova ou envie para a lixeira antes de excluir.");
        }
        funilRepository.delete(funilBanco);
    }


}
