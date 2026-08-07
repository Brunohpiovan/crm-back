package com.juridiqsystem.crm.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.TenantId;
import org.hibernate.annotations.UuidGenerator;

import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "acesso")
public class Acesso {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @UuidGenerator
    @Column(name = "public_id", nullable = false, unique = true, updatable = false, length = 36)
    private String publicId;

    @TenantId
    @Column(name = "empresa_id", nullable = false)
    private Long empresaId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private Usuario usuario;

    @Column(nullable = false, length = 45)
    private String enderecoIp;

    @Column(length = 255)
    private String localizacao;

    @Column(nullable = false)
    private LocalDateTime data_acesso;

    private LocalDateTime data_saida;

    @Column(length = 100)
    private String provedorInternet;

    @Column(length = 100)
    private String sistemaOperacional;

    @Column(length = 50)
    private String tipoDispositivo;

    @Column(length = 50)
    private String fusoHorario;

    @Column(length = 100)
    private String navegador;

    @Column(length = 20)
    private String versaoNavegador;

    @Column(length = 50)
    private String idiomaNavegador;


}
