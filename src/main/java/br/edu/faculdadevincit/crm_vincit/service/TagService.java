package br.edu.faculdadevincit.crm_vincit.service;


import br.edu.faculdadevincit.crm_vincit.config.CacheConfig;
import br.edu.faculdadevincit.crm_vincit.model.Tag;
import br.edu.faculdadevincit.crm_vincit.model.dtos.TagDTO;
import br.edu.faculdadevincit.crm_vincit.model.dtos.TagOportunidadeDTO;
import br.edu.faculdadevincit.crm_vincit.model.dtos.TagRequestDTO;
import br.edu.faculdadevincit.crm_vincit.model.enums.Situacao;
import br.edu.faculdadevincit.crm_vincit.repository.TagRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class TagService {

    @Autowired
    private TagRepository tagRepository;

    public List<TagDTO> findAll() {
        return tagRepository.findAllOrdered()
                .stream()
                .map(TagDTO::new)
                .collect(Collectors.toList());
    }

    /**
     * Cacheado (CacheConfig.TAGS_CACHE, TTL 5min): usado para popular o seletor de tags ao
     * criar/editar oportunidade, chamado com frequência e mudando raramente. A lista retornada é
     * compartilhada entre requisições enquanto o cache estiver quente — não deve ser mutada pelo
     * chamador.
     */
    @Cacheable(CacheConfig.TAGS_CACHE)
    public List<TagOportunidadeDTO> findAllAtivas(){
        return tagRepository.findBySituacaoOrderByNomeAsc(Situacao.ATIVA)
                .stream()
                .map(TagOportunidadeDTO::new)
                .collect(Collectors.toList());
    }


    public TagDTO findById(String id){
        return new TagDTO(tagRepository.findByPublicId(id).orElseThrow(()->new RuntimeException("Tag nao encontrada")));
    }

    @CacheEvict(value = CacheConfig.TAGS_CACHE, allEntries = true)
    public void create(TagRequestDTO tag){
        if (tagRepository.existsByNome(tag.getNome())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Já existe uma tag com este nome.");
        }
        Tag newTag = preencheTag(new Tag(), tag);
        newTag.setCriadoEm(LocalDateTime.now());
        tagRepository.save(newTag);
    }

    @CacheEvict(value = CacheConfig.TAGS_CACHE, allEntries = true)
    public void delete(String id){
        Tag tag = tagRepository.findByPublicId(id).orElseThrow(()-> new RuntimeException("Tag nao encontrada"));
        tagRepository.delete(tag);
    }

    @CacheEvict(value = CacheConfig.TAGS_CACHE, allEntries = true)
    public void update(String id, TagRequestDTO tag) {
        Tag tagBanco = tagRepository.findByPublicId(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Tag não encontrada"));

        if (!tag.getNome().equals(tagBanco.getNome())) {
            if (tagRepository.existsByNome(tag.getNome())) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Já existe uma tag com este nome.");
            }
        }

        Tag newTag = preencheTag(tagBanco, tag);
        newTag.setAtualizadoEm(LocalDateTime.now());
        tagRepository.save(newTag);
    }

    private Tag preencheTag(Tag tagBanco, TagRequestDTO newTag){
        tagBanco.setNome(newTag.getNome());
        tagBanco.setCor(newTag.getCor());
        tagBanco.setSituacao(newTag.getSituacao());
        tagBanco.setPertence(newTag.getPertence());
        return tagBanco;
    }
}
