package com.juridiqsystem.crm.model;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Lob;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.TenantId;
import org.hibernate.annotations.UuidGenerator;

import java.time.LocalDateTime;

@Schema(description = "Um comentário de uma oportunidade, exibido no modal de edição ('Comentários'). Ao contrário do histórico, guarda uma referência real ao autor (não um snapshot do nome) para exibir avatar atualizado e permitir checar quem pode excluir o comentário.")
@Getter
@Setter
@Entity
@Table(name = "oportunidade_comentario")
@NoArgsConstructor
public class OportunidadeComentario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @UuidGenerator
    @Column(name = "public_id", nullable = false, unique = true, updatable = false, length = 36)
    private String publicId;

    @TenantId
    @Column(name = "empresa_id", nullable = false)
    private Long empresaId;

    @Column(name = "oportunidade_id", nullable = false)
    private Long oportunidadeId;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "autor_id")
    private Usuario autor;

    @Lob
    @Column(columnDefinition = "TEXT", nullable = false)
    private String conteudo;

    @Column(name = "url_anexo", length = 500)
    private String urlAnexo;

    @Schema(description = "Nome original do arquivo, para exibir e baixar. Nunca é usado para montar a key no S3 — essa é sempre um UUID com a extensão derivada do tipo confirmado pelo servidor.")
    @Column(name = "nome_anexo", length = 255)
    private String nomeAnexo;

    @Schema(description = "Content-type confirmado por assinatura de bytes (não o declarado pelo cliente). Decide se o anexo abre no navegador ou desce como download.")
    @Column(name = "tipo_anexo", length = 120)
    private String tipoAnexo;

    @Column(name = "tamanho_anexo")
    private Long tamanhoAnexo;

    @Column(name = "criado_em", nullable = false)
    private LocalDateTime criadoEm;

    public OportunidadeComentario(Long oportunidadeId, Usuario autor, String conteudo) {
        this.oportunidadeId = oportunidadeId;
        this.autor = autor;
        this.conteudo = conteudo;
        this.criadoEm = LocalDateTime.now();
    }

    public void anexar(String urlAnexo, String nomeAnexo, String tipoAnexo, long tamanhoAnexo) {
        this.urlAnexo = urlAnexo;
        this.nomeAnexo = nomeAnexo;
        this.tipoAnexo = tipoAnexo;
        this.tamanhoAnexo = tamanhoAnexo;
    }
}
