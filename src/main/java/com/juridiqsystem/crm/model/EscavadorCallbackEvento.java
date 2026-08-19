package com.juridiqsystem.crm.model;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * Log bruto de auditoria de cada callback recebido da Escavador. Deliberadamente SEM @TenantId:
 * o webhook é público e o tenant só é conhecido depois de resolver o processo pelo número CNJ —
 * um callback que não casa com nenhum processo desta instalação também precisa ficar registrado,
 * e uma entidade com @TenantId nunca seria gravada (nem lida) fora de um tenant resolvido.
 */
@Schema(description = "Registro bruto de um callback recebido da Escavador, gravado sempre (inclusive quando o processamento falha), para auditoria e reprocessamento manual.")
@Getter
@Setter
@Entity
@Table(name = "escavador_callback_evento")
@NoArgsConstructor
public class EscavadorCallbackEvento {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "recebido_em", nullable = false)
    private LocalDateTime recebidoEm;

    @Column(name = "payload_json", nullable = false, columnDefinition = "TEXT")
    private String payloadJson;

    @Column(name = "processado", nullable = false)
    private Boolean processado;

    @Schema(description = "Mensagem do erro que impediu o processamento. Null quando processado = true.")
    @Column(name = "erro", columnDefinition = "TEXT")
    private String erro;

    @Column(name = "numero_cnj_resolvido", length = 25)
    private String numeroCnjResolvido;

    public EscavadorCallbackEvento(String payloadJson) {
        this.payloadJson = payloadJson;
        this.recebidoEm = LocalDateTime.now();
        this.processado = false;
    }

    public void marcarProcessado(String numeroCnjResolvido) {
        this.numeroCnjResolvido = numeroCnjResolvido;
        this.processado = true;
        this.erro = null;
    }

    public void marcarErro(String numeroCnjResolvido, String erro) {
        this.numeroCnjResolvido = numeroCnjResolvido;
        this.processado = false;
        this.erro = erro;
    }
}
