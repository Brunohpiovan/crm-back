package br.edu.faculdadevincit.crm_vincit.model;

import br.edu.faculdadevincit.crm_vincit.model.enums.StatusProtocolo;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@Entity
@Table(name = "protocolo")
@NoArgsConstructor
@AllArgsConstructor
public class Protocolo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "admin_id", nullable = false)
    private Usuario admin;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "admin_anterior_id")
    private Usuario adminAnterior;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private Participante participante;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private StatusProtocolo status;

    @Column(name = "data_criacao")
    private LocalDateTime dataCriacao;
    @Column(name = "data_encerramento")
    private LocalDateTime dataEncerramento;

    @Override
    public String toString() {
        return "Protocolo{" +
                "id=" + id +
                ", status='" + status + '\'' +
                ", dataCriacao=" + dataCriacao +
                '}';
    }

}
