package com.juridiqsystem.crm.model;

import com.juridiqsystem.crm.model.enums.TipoEventoOportunidade;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.TenantId;

import java.time.LocalDateTime;

@Schema(description = "Um evento do histórico de uma oportunidade (criação, edição, movimentação entre etapas, envio para lixeira, restauração). Somente leitura pelo frontend — nunca editado ou apagado depois de criado.")
@Getter
@Setter
@Entity
@Table(name = "oportunidade_historico")
@NoArgsConstructor
public class OportunidadeHistorico {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @TenantId
    @Column(name = "empresa_id", nullable = false)
    private Long empresaId;

    @Column(name = "oportunidade_id", nullable = false)
    private Long oportunidadeId;

    @Schema(description = "Nome do usuário que realizou a ação, capturado no momento do evento (não é uma FK — permanece correto mesmo que o usuário seja renomeado ou removido depois).")
    @Column(name = "autor_nome", nullable = false, length = 150)
    private String autorNome;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private TipoEventoOportunidade tipo;

    @Column(nullable = false, length = 255)
    private String descricao;

    @Column(name = "criado_em", nullable = false)
    private LocalDateTime criadoEm;

    public OportunidadeHistorico(Long oportunidadeId, String autorNome, TipoEventoOportunidade tipo, String descricao) {
        this.oportunidadeId = oportunidadeId;
        this.autorNome = autorNome;
        this.tipo = tipo;
        this.descricao = descricao;
        this.criadoEm = LocalDateTime.now();
    }
}
