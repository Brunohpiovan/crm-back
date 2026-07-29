package br.edu.faculdadevincit.crm_vincit.service;

import br.edu.faculdadevincit.crm_vincit.model.Funil;
import br.edu.faculdadevincit.crm_vincit.model.Oportunidade;
import br.edu.faculdadevincit.crm_vincit.model.Usuario;
import br.edu.faculdadevincit.crm_vincit.model.dtos.*;
import br.edu.faculdadevincit.crm_vincit.model.enums.SituacaoOportunidade;
import br.edu.faculdadevincit.crm_vincit.model.enums.UserRole;
import br.edu.faculdadevincit.crm_vincit.repository.FunilRepository;
import br.edu.faculdadevincit.crm_vincit.repository.OportunidadeRepository;
import br.edu.faculdadevincit.crm_vincit.repository.UsuarioRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class FunilService {

    @Autowired
    private FunilRepository funilRepository;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private CloudFrontService cloudFrontService;

    @Autowired
    private OportunidadeRepository oportunidadeRepository;

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
        Funil funil = funilRepository.findById(funilId)
                .orElseThrow(() -> new RuntimeException("Funil não encontrado"));

        List<Usuario> todosUsuarios = usuarioRepository.findByNotCargo(UserRole.ADMINISTRADOR);
        List<Usuario> usuariosNaoNoFunil = todosUsuarios.stream()
                .filter(usuario -> !funil.getFuncionarios().contains(usuario))
                .toList();

        List<UsuarioContatoDto> usuariosNaoNoFunilDto = usuariosNaoNoFunil.stream()
                .map(usuario -> {
                    String urlPicture = usuario.getUrlPicture();
                    if (urlPicture != null && urlPicture.contains(cloudFrontService.getBaseUrl())) {
                        urlPicture = cloudFrontService.generateSignedUrl(urlPicture, Duration.ofMinutes(60));
                    }
                    return new UsuarioContatoDto(usuario.getId(), usuario.getNome(), urlPicture);
                })
                .collect(Collectors.toList());

        return usuariosNaoNoFunilDto;
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
        Funil funil = funilRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Funil com id " + id + " não encontrado"));
        FunilDto dto = new FunilDto(funil);

        if (dto.getEtapas() != null) {
            dto.getEtapas().forEach(etapaDto -> {
                if (etapaDto.getOportunidades() != null) {
                    etapaDto.getOportunidades().forEach(oportunidadeDTO -> {
                        // Assina url_anexo se necessário
                        if (oportunidadeDTO.getUrl_anexo() != null &&
                                oportunidadeDTO.getUrl_anexo().contains(cloudFrontService.getBaseUrl())) {

                            String signedUrl = cloudFrontService.generateSignedUrl(
                                    oportunidadeDTO.getUrl_anexo(),
                                    Duration.ofMinutes(60)
                            );
                            oportunidadeDTO.setUrl_anexo(signedUrl);
                        }
                        
                        CriadorOportunidadeDto criador = oportunidadeDTO.getCriador();
                        if (criador != null &&
                                criador.getUrlPicture() != null &&
                                criador.getUrlPicture().contains(cloudFrontService.getBaseUrl())) {

                            String signedPictureUrl = cloudFrontService.generateSignedUrl(
                                    criador.getUrlPicture(),
                                    Duration.ofMinutes(60)
                            );
                            criador.setUrlPicture(signedPictureUrl);
                        }
                    });
                }
            });
        }

        return dto;
    }

    public FunilDto findByIdAndSituacao(Long id, List<SituacaoOportunidade> situacoes, List<TagOportunidadeDTO> tags) {
        Funil funil = funilRepository.findByIdWithEtapas(id).orElse(null);
        if (funil == null) return null;

        List<Long> tagIds = tags != null
                ? tags.stream().map(TagOportunidadeDTO::getId).toList()
                : List.of();

        funil.getEtapas().forEach(etapa -> {
            List<Oportunidade> filtradas;

            if (!tagIds.isEmpty()) {
                filtradas = oportunidadeRepository
                        .findByEtapaIdAndSituacaoInAndTagIdsIn(etapa.getId(), situacoes, tagIds);
            } else {
                filtradas = oportunidadeRepository
                        .findByEtapaIdAndSituacaoIn(etapa.getId(), situacoes);
            }

            etapa.setOportunidades(filtradas);
        });

        FunilDto dto = new FunilDto(funil);

        if (dto.getEtapas() != null) {
            dto.getEtapas().forEach(etapaDto -> {
                if (etapaDto.getOportunidades() != null) {
                    etapaDto.getOportunidades().forEach(oportunidadeDTO -> {
                        if (oportunidadeDTO.getUrl_anexo() != null &&
                                oportunidadeDTO.getUrl_anexo().contains(cloudFrontService.getBaseUrl())) {
                            String signedUrl = cloudFrontService.generateSignedUrl(
                                    oportunidadeDTO.getUrl_anexo(),
                                    Duration.ofMinutes(60)
                            );
                            oportunidadeDTO.setUrl_anexo(signedUrl);
                        }

                        CriadorOportunidadeDto criador = oportunidadeDTO.getCriador();
                        if (criador != null &&
                                criador.getUrlPicture() != null &&
                                criador.getUrlPicture().contains(cloudFrontService.getBaseUrl())) {
                            String signedPictureUrl = cloudFrontService.generateSignedUrl(
                                    criador.getUrlPicture(),
                                    Duration.ofMinutes(60)
                            );
                            criador.setUrlPicture(signedPictureUrl);
                        }
                    });
                }
            });
        }

        return dto;
    }


    public FunilDto create(Funil funil) {
        Funil savedFunil = funilRepository.save(funil);
        savedFunil.setCriadoEm(LocalDateTime.now());
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
