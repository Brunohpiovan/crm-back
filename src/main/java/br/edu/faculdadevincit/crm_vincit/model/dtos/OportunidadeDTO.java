package br.edu.faculdadevincit.crm_vincit.model.dtos;

import br.edu.faculdadevincit.crm_vincit.model.Oportunidade;
import br.edu.faculdadevincit.crm_vincit.model.Participante;
import br.edu.faculdadevincit.crm_vincit.model.enums.Origem;
import br.edu.faculdadevincit.crm_vincit.model.enums.SituacaoOportunidade;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Schema(description = "Oportunidade de venda (card do funil), retornada por todos os endpoints de leitura/escrita de /oportunidade e publicada nos tópicos WebSocket de atualização em tempo real.")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class OportunidadeDTO {
    private Long id;
    private String titulo;
    @Schema(description = "Id da etapa em que a oportunidade se encontra atualmente")
    private Long etapa;
    @Schema(description = "Usuário responsável (vendedor/dono) pela oportunidade")
    private CriadorOportunidadeDto criador;
    @Schema(description = "Cliente/participante associado à oportunidade")
    private ParticipanteDTO cliente;
    private BigDecimal valor;
    private LocalDateTime data_criacao;
    private String interesse;
    @Schema(description = "URL do arquivo anexado à oportunidade (imagem), ou null se não houver anexo")
    private String url_anexo;
    @Schema(description = "Origem/canal pelo qual a oportunidade chegou")
    private Origem origem;
    @Schema(description = "Detalhamento adicional, normalmente preenchido apenas quando origem = OUTRO")
    private String descricao;
    private String observacoes;
    @Schema(description = "Situação atual da oportunidade")
    private SituacaoOportunidade situacao;
    private List<TagOportunidadeDTO> tags;
    @Schema(description = "Posição (0-based) da oportunidade dentro da lista de oportunidades da sua etapa atual, usada para ordenação no Kanban")
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
