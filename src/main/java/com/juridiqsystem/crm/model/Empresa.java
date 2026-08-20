package com.juridiqsystem.crm.model;

import com.juridiqsystem.crm.model.enums.FusoHorarioEmpresa;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.UuidGenerator;

import java.time.LocalDateTime;

@Schema(description = "Empresa (tenant). Todo dado do sistema pertence a exatamente uma Empresa — ver TenantContext/TenantIdentifierResolver para o mecanismo de isolamento entre empresas.")
@Getter
@Setter
@Entity
@Table(name = "empresa")
@NoArgsConstructor
@AllArgsConstructor
public class Empresa {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @UuidGenerator
    @Column(name = "public_id", nullable = false, unique = true, updatable = false, length = 36)
    private String publicId;

    @NotBlank(message = "Informe um código para a empresa")
    @Size(max = 60, message = "O código deve ter no máximo 60 caracteres")
    @Column(name = "codigo", nullable = false, unique = true, length = 60)
    private String codigo;

    @NotBlank(message = "Informe um nome para a empresa")
    @Size(max = 150, message = "O nome deve ter no máximo 150 caracteres")
    @Column(name = "nome", nullable = false, length = 150)
    private String nome;

    @Column(name = "logo_url")
    private String logoUrl;

    @NotNull(message = "Informe o timezone da empresa")
    @Column(name = "timezone", nullable = false, length = 60)
    private FusoHorarioEmpresa timezone;

    @Column(name = "protocolo_risco_horas", nullable = false)
    private Integer protocoloRiscoHoras;

    @Schema(description = "Quantos processos esta empresa pode manter monitorados simultaneamente na Escavador, conforme o plano contratado. Null = ilimitado.")
    @Column(name = "processos_monitorados_limite")
    private Integer processosMonitoradosLimite;

    @Schema(description = "Quantas OABs esta empresa pode manter monitoradas simultaneamente nos Diários Oficiais (Escavador), conforme o plano contratado. Null = ilimitado.")
    @Column(name = "intimacoes_monitoradas_limite")
    private Integer intimacoesMonitoradasLimite;

    @Column(name = "notificacao_visual_habilitada", nullable = false)
    private Boolean notificacaoVisualHabilitada;

    @Column(name = "notificacao_sonora_habilitada", nullable = false)
    private Boolean notificacaoSonoraHabilitada;

    @Schema(description = "Empresa interna reservada para o usuário master (não é uma empresa-cliente): fica de fora das listagens/CRUD de empresas em /master/empresas.")
    @Column(name = "interna", nullable = false)
    private Boolean interna = false;

    @Column(name = "criado_em")
    private LocalDateTime criadoEm;

    @Column(name = "atualizado_em")
    private LocalDateTime atualizadoEm;
}
