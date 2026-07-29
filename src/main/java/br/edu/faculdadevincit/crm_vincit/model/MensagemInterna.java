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
@Table(name = "mensagem_interna")
@NoArgsConstructor
@AllArgsConstructor
public class MensagemInterna {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "chat_grupo_id")
    private ChatGrupo chatGrupo;

    @ManyToOne(optional = false)
    @JoinColumn(name = "sender_id")
    private Usuario sender;

    @Lob
    @Column(columnDefinition = "TEXT", nullable = false)
    private String conteudo;

    @Column(nullable = false)
    private LocalDateTime dataEnvio;

}
