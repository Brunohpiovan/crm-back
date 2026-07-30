package br.edu.faculdadevincit.crm_vincit.model;
import br.edu.faculdadevincit.crm_vincit.model.enums.Origem;
import br.edu.faculdadevincit.crm_vincit.model.enums.SituacaoOportunidade;
import jakarta.persistence.*;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@Table(name = "oportunidade")
@AllArgsConstructor
@Entity
public class Oportunidade {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "titulo")
    @Size(max = 150, message = "O titulo deve ter no maximo 150 caracteres")
    private String titulo;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "card_id")
    private Etapa etapa;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "criador_id")
    private Usuario criador;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cliente_id")
    private Participante cliente;

    private BigDecimal valor;

    private LocalDateTime data_criacao;

    @Enumerated(EnumType.STRING)
    private Origem origem;

    @Column(name = "descricao")
    @Size(max = 150, message = "A descricao deve ter no maximo 150 caracteres")
    private String descricao;

    @Size(max = 100, message = "O campo interesse deve ter no maximo 100 caracteres")
    private String interesse;

    private String url_anexo;

    private Integer indice;

    @Size(max = 255, message = "A observacao deve ter no maximo 255 caracteres")
    private String observacoes;

    @Column(name = "data_entrada_etapa")
    private LocalDateTime dataEntradaEtapa;

    @Column(name = "atualizado_em")
    private LocalDateTime atualizadoEm;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private SituacaoOportunidade situacao;

    @ManyToMany
    @JoinTable(
            name = "oportunidade_tag",
            joinColumns = @JoinColumn(name = "oportunidade_id"),
            inverseJoinColumns = @JoinColumn(name = "tag_id")
    )
    private List<Tag> tags;

    @Version
    @Column(name = "version", nullable = false)
    private Long version;

}
