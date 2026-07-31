package br.edu.faculdadevincit.crm_vincit.service;

import br.edu.faculdadevincit.crm_vincit.model.Etapa;
import br.edu.faculdadevincit.crm_vincit.model.Funil;
import br.edu.faculdadevincit.crm_vincit.model.Usuario;
import br.edu.faculdadevincit.crm_vincit.model.dtos.*;
import br.edu.faculdadevincit.crm_vincit.model.enums.SituacaoOportunidade;
import br.edu.faculdadevincit.crm_vincit.model.enums.UserRole;
import br.edu.faculdadevincit.crm_vincit.repository.FunilRepository;
import br.edu.faculdadevincit.crm_vincit.repository.UsuarioRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
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
        String email = authentication.getName();
        return (Usuario) usuarioRepository.findByLogin(email)
                .orElseThrow(() -> new UsernameNotFoundException("Usuário não encontrado"));
    }


    public List<UsuarioContatoDto> findFuncionariosFunil(Long funilId) {
        if (!funilRepository.existsById(funilId)) {
            throw new RuntimeException("Funil não encontrado");
        }

        List<UsuarioContatoDto> usuariosNaoNoFunil =
                usuarioRepository.findDisponiveisParaFunil(funilId, UserRole.ADMINISTRADOR);

        return usuariosNaoNoFunil;
    }


    public void adicionarFuncionarioFunil(Long funcionarioId,Long funilId){
        Funil funil = funilRepository.findById(funilId)
                .orElseThrow(() -> new RuntimeException("Funil não encontrado"));
        Usuario funcionario = usuarioRepository.findById(funcionarioId).orElseThrow(() -> new RuntimeException("Funcionario não encontrado"));
        if (!funil.getFuncionarios().contains(funcionario)) {
            funil.getFuncionarios().add(funcionario);
        }
        if (!funcionario.getFunisPermitidos().contains(funil)) {
            funcionario.getFunisPermitidos().add(funil);
        }
        funilRepository.save(funil);
        usuarioRepository.save(funcionario);
    }


    public FunilDto findById(Long id) {
        Funil funil = funilRepository.findByIdWithEtapas(id)
                .orElseThrow(() -> new RuntimeException("Funil com id " + id + " não encontrado"));

        List<Long> etapaIds = funil.getEtapas().stream().map(Etapa::getId).toList();
        Map<Long, List<OportunidadeDTO>> oportunidadesPorEtapa = etapaService.carregarOportunidadesPorEtapa(etapaIds);

        List<EtapaDto> etapaDtos = funil.getEtapas().stream()
                .map(etapa -> new EtapaDto(etapa, oportunidadesPorEtapa.getOrDefault(etapa.getId(), List.of())))
                .collect(Collectors.toList());

        return new FunilDto(funil.getId(), funil.getNome(), etapaDtos);
    }

    public FunilDto findByIdAndSituacao(Long id, List<SituacaoOportunidade> situacoes, List<TagOportunidadeDTO> tags) {
        Funil funil = funilRepository.findByIdWithEtapas(id).orElse(null);
        if (funil == null) return null;

        List<Long> tagIds = tags != null
                ? tags.stream().map(TagOportunidadeDTO::getId).toList()
                : List.of();
        List<Long> etapaIds = funil.getEtapas().stream().map(Etapa::getId).toList();

        Map<Long, List<OportunidadeDTO>> oportunidadesPorEtapa =
                etapaService.carregarOportunidadesPorEtapa(etapaIds, situacoes, tagIds);

        List<EtapaDto> etapaDtos = funil.getEtapas().stream()
                .map(etapa -> new EtapaDto(etapa, oportunidadesPorEtapa.getOrDefault(etapa.getId(), List.of())))
                .collect(Collectors.toList());

        return new FunilDto(funil.getId(), funil.getNome(), etapaDtos);
    }


    public FunilDto create(FunilCreateRequest funilCreateRequest) {
        Funil funil = new Funil();
        funil.setNome(funilCreateRequest.nome());
        funil.setCriadoEm(LocalDateTime.now());
        Funil savedFunil = funilRepository.save(funil);
        return new FunilDto(savedFunil);
    }

    public FunilAllDTO update(Long id, FunilAllDTO funil) {
        Funil funilBanco = funilRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Funil com id " + id + " não encontrado"));
        funilBanco.setNome(funil.getNome());
        Funil savedFunil = funilRepository.save(funilBanco);
        savedFunil.setAtualizadoEm(LocalDateTime.now());
        return new FunilAllDTO(savedFunil);
    }

    public void delete(Long id) {
        Funil funilBanco = funilRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Funil com id " + id + " não encontrado"));
        funilRepository.delete(funilBanco);
    }


}
