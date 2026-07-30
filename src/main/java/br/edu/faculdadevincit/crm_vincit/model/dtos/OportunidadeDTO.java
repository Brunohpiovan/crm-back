package br.edu.faculdadevincit.crm_vincit.model.dtos;

import br.edu.faculdadevincit.crm_vincit.model.Oportunidade;
import br.edu.faculdadevincit.crm_vincit.model.Participante;
import br.edu.faculdadevincit.crm_vincit.model.enums.Origem;
import br.edu.faculdadevincit.crm_vincit.model.enums.SituacaoOportunidade;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class OportunidadeDTO {
    private Long id;
    private String titulo;
    private Long etapa;
    private CriadorOportunidadeDto criador;
    private ParticipanteDTO cliente;
    private BigDecimal valor;
    private LocalDateTime data_criacao;
    private String interesse;
    private String url_anexo;
    private Origem origem;
    private String descricao;
    private String observacoes;
    private SituacaoOportunidade situacao;
    private List<TagOportunidadeDTO> tags;
    private int indice;

    public OportunidadeDTO(Oportunidade oportunidade) {
        this.id = oportunidade.getId();
        this.titulo = oportunidade.getTitulo();
        this.etapa = oportunidade.getEtapa() != null ? oportunidade.getEtapa().getId() : null;
        this.criador = oportunidade.getCriador() != null ? new CriadorOportunidadeDto(oportunidade.getCriador()) : null;
        this.cliente = oportunidade.getCliente() != null ? new ParticipanteDTO(oportunidade.getCliente()) : null;
        this.valor = oportunidade.getValor();
        this.data_criacao = oportunidade.getData_criacao();
        this.url_anexo = oportunidade.getUrl_anexo();
        this.indice = oportunidade.getIndice();
        this.origem = oportunidade.getOrigem();
        this.interesse = oportunidade.getInteresse();
        this.descricao = oportunidade.getDescricao();
        this.observacoes = oportunidade.getObservacoes();
        this.situacao = oportunidade.getSituacao();
        if (oportunidade.getTags() != null) {
            this.tags = oportunidade.getTags().stream()
                    .map(tag -> new TagOportunidadeDTO(tag))
                    .collect(Collectors.toList());
        }
    }

}
