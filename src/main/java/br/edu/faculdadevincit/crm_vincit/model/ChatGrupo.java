package br.edu.faculdadevincit.crm_vincit.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@Entity
@Table(name = "chat_grupo")
@NoArgsConstructor
@AllArgsConstructor
public class ChatGrupo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String nome;

    @Column(name = "avatar_url")
    private String avatarUrl;

    @Column(name = "imagem_fundo_url")
    private String imagemFundoUrl;

    private Boolean privado;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "chat_grupo_usuario",
            joinColumns = @JoinColumn(name = "chat_grupo_id"),
            inverseJoinColumns = @JoinColumn(name = "usuario_id")
    )
    private List<Usuario> usuarios;

    @Column(name = "criado_em")
    private LocalDateTime criadoEm;
    @Column(name = "atualizado_em")
    private LocalDateTime atualizadoEm;

}
