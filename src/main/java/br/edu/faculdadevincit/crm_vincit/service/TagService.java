package br.edu.faculdadevincit.crm_vincit.service;


import br.edu.faculdadevincit.crm_vincit.model.Tag;
import br.edu.faculdadevincit.crm_vincit.model.dtos.TagDTO;
import br.edu.faculdadevincit.crm_vincit.model.dtos.TagOportunidadeDTO;
import br.edu.faculdadevincit.crm_vincit.model.dtos.TagRequestDTO;
import br.edu.faculdadevincit.crm_vincit.model.enums.Situacao;
import br.edu.faculdadevincit.crm_vincit.repository.TagRepository;
import org.springframework.beans.factory.annotation.Autowired;
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

    public List<TagOportunidadeDTO> findAllAtivas(){
        return tagRepository.findBySituacaoOrderByNomeAsc(Situacao.ATIVA)
                .stream()
                .map(TagOportunidadeDTO::new)
                .collect(Collectors.toList());
    }


    public TagDTO findById(Long id){
        return new TagDTO(tagRepository.findById(id).orElseThrow(()->new RuntimeException("Tag nao encontrada")));
    }

    public void create(TagRequestDTO tag){
        if (tagRepository.existsByNome(tag.getNome())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Já existe uma tag com este nome.");
        }
        Tag newTag = preencheTag(new Tag(), tag);
        newTag.setCriadoEm(LocalDateTime.now());
        tagRepository.save(newTag);
    }

    public void delete(Long id){
        Tag tag = tagRepository.findById(id).orElseThrow(()-> new RuntimeException("Tag nao encontrada"));
        tagRepository.delete(tag);
    }

    public void update(Long id, TagRequestDTO tag) {
        Tag tagBanco = tagRepository.findById(id)
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
