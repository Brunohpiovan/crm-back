package br.edu.faculdadevincit.crm_vincit.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Getter
@Setter
@Entity
@Table(name = "mensagem")
@NoArgsConstructor
@AllArgsConstructor
public class Mensagem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne
    @JoinColumn(name = "protocolo_id")
    private Protocolo protocolo;
    @ManyToOne
    @JoinColumn(name = "sender_id")
    private Participante sender;
    @Lob
    @Column(columnDefinition = "TEXT")
    private String conteudo;
    private LocalDateTime data_envio;

    public String getDataEnvio() {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
        return this.data_envio.format(formatter);
    }
}
