package com.juridiqsystem.crm.service;

import com.juridiqsystem.crm.config.CacheConfig;
import com.juridiqsystem.crm.model.Tag;
import com.juridiqsystem.crm.model.dtos.TagRequestDTO;
import com.juridiqsystem.crm.model.enums.Cor;
import com.juridiqsystem.crm.model.enums.Pertence;
import com.juridiqsystem.crm.model.enums.Situacao;
import com.juridiqsystem.crm.repository.TagRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.CacheManager;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Verifica o cache adicionado em TagService.findAllAtivas (CacheConfig.TAGS_CACHE): precisa de um
 * ApplicationContext real porque @Cacheable/@CacheEvict só funcionam através do proxy do Spring —
 * um teste puro com Mockito (@InjectMocks) não exercitaria o AOP de cache, só o método em si.
 */
@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = {CacheConfig.class, TagService.class})
@Import(TagServiceCacheTest.TestConfig.class)
class TagServiceCacheTest {

    @Autowired
    private TagService tagService;

    @Autowired
    private CacheManager cacheManager;

    @MockitoBean
    private TagRepository tagRepository;

    static class TestConfig {
    }

    /**
     * O CacheManager é um bean singleton reaproveitado entre os métodos de teste desta classe
     * (o Spring cacheia o ApplicationContext pela assinatura de configuração); sem limpar o
     * cache aqui, o resultado de um teste vaza para o próximo independente da ordem de execução.
     */
    @BeforeEach
    void limparCache() {
        cacheManager.getCache(CacheConfig.TAGS_CACHE).clear();
    }

    private Tag tag(String nome) {
        Tag tag = new Tag();
        tag.setNome(nome);
        tag.setSituacao(Situacao.ATIVA);
        tag.setCor(Cor.AZUL);
        tag.setPertence(Pertence.OPORTUNIDADES);
        return tag;
    }

    @Test
    void findAllAtivas_chamadasRepetidas_soConsultaRepositorioUmaVez() {
        when(tagRepository.findBySituacaoOrderByNomeAsc(Situacao.ATIVA)).thenReturn(List.of(tag("VIP")));

        tagService.findAllAtivas();
        tagService.findAllAtivas();
        List<com.juridiqsystem.crm.model.dtos.TagOportunidadeDTO> terceira = tagService.findAllAtivas();

        verify(tagRepository, times(1)).findBySituacaoOrderByNomeAsc(Situacao.ATIVA);
        assertThat(terceira).extracting("nome").containsExactly("VIP");
    }

    @Test
    void create_invalidaCache_proximaChamadaConsultaRepositorioDeNovo() {
        when(tagRepository.findBySituacaoOrderByNomeAsc(Situacao.ATIVA)).thenReturn(List.of(tag("VIP")));
        tagService.findAllAtivas();
        verify(tagRepository, times(1)).findBySituacaoOrderByNomeAsc(Situacao.ATIVA);

        when(tagRepository.existsByNome(anyString())).thenReturn(false);
        when(tagRepository.save(any(Tag.class))).thenReturn(tag("Nova"));
        TagRequestDTO novaTag = new TagRequestDTO();
        novaTag.setNome("Nova");
        novaTag.setSituacao(Situacao.ATIVA);
        novaTag.setCor(Cor.AZUL);
        novaTag.setPertence(Pertence.OPORTUNIDADES);
        tagService.create(novaTag);

        tagService.findAllAtivas();

        verify(tagRepository, times(2)).findBySituacaoOrderByNomeAsc(Situacao.ATIVA);
    }
}
