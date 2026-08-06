package br.edu.faculdadevincit.crm_vincit.service;

import br.edu.faculdadevincit.crm_vincit.config.CacheConfig;
import br.edu.faculdadevincit.crm_vincit.model.Equipe;
import br.edu.faculdadevincit.crm_vincit.model.Usuario;
import br.edu.faculdadevincit.crm_vincit.model.dtos.EquipeCreateRequest;
import br.edu.faculdadevincit.crm_vincit.model.dtos.EquipeResponse;
import br.edu.faculdadevincit.crm_vincit.repository.EquipeRepository;
import br.edu.faculdadevincit.crm_vincit.repository.UsuarioRepository;
import br.edu.faculdadevincit.crm_vincit.service.exceptions.ResourceNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class EquipeService {

    @Autowired
    private EquipeRepository equipeRepository;

    @Autowired
    private UsuarioRepository usuarioRepository;

    /**
     * Cacheado (CacheConfig.EQUIPES_CACHE, TTL 5min): equipes mudam raramente e são usadas para
     * popular o filtro do dashboard. A lista retornada é compartilhada entre requisições enquanto
     * o cache estiver quente — não deve ser mutada pelo chamador.
     */
    @Cacheable(CacheConfig.EQUIPES_CACHE)
    public List<EquipeResponse> findAll() {
        return equipeRepository.findAll().stream().map(EquipeResponse::new).toList();
    }

    @CacheEvict(value = CacheConfig.EQUIPES_CACHE, allEntries = true)
    public EquipeResponse create(EquipeCreateRequest request) {
        Equipe equipe = new Equipe();
        equipe.setNome(request.nome());
        equipe.setCriadoEm(LocalDateTime.now());
        Equipe salva = equipeRepository.save(equipe);
        return new EquipeResponse(salva);
    }

    @CacheEvict(value = CacheConfig.EQUIPES_CACHE, allEntries = true)
    public EquipeResponse rename(String id, EquipeCreateRequest request) {
        Equipe equipe = buscarOuFalhar(id);
        equipe.setNome(request.nome());
        equipe.setAtualizadoEm(LocalDateTime.now());
        return new EquipeResponse(equipeRepository.save(equipe));
    }

    @CacheEvict(value = CacheConfig.EQUIPES_CACHE, allEntries = true)
    public EquipeResponse addMembro(String equipeId, String usuarioId) {
        Equipe equipe = buscarOuFalhar(equipeId);
        Usuario usuario = usuarioRepository.findByPublicId(usuarioId)
                .orElseThrow(() -> new ResourceNotFoundException("Usuário com id " + usuarioId + " não encontrado"));
        if (!equipe.getMembros().contains(usuario)) {
            equipe.getMembros().add(usuario);
        }
        equipe.setAtualizadoEm(LocalDateTime.now());
        return new EquipeResponse(equipeRepository.save(equipe));
    }

    @CacheEvict(value = CacheConfig.EQUIPES_CACHE, allEntries = true)
    public EquipeResponse removeMembro(String equipeId, String usuarioId) {
        Equipe equipe = buscarOuFalhar(equipeId);
        equipe.getMembros().removeIf(membro -> membro.getPublicId().equals(usuarioId));
        equipe.setAtualizadoEm(LocalDateTime.now());
        return new EquipeResponse(equipeRepository.save(equipe));
    }

    @CacheEvict(value = CacheConfig.EQUIPES_CACHE, allEntries = true)
    public void delete(String id) {
        Equipe equipe = buscarOuFalhar(id);
        equipeRepository.delete(equipe);
    }

    private Equipe buscarOuFalhar(String id) {
        return equipeRepository.findByPublicId(id)
                .orElseThrow(() -> new ResourceNotFoundException("Equipe com id " + id + " não encontrada"));
    }
}
