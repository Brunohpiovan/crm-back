package com.juridiqsystem.crm.model.dtos;

import com.juridiqsystem.crm.model.OportunidadeComentario;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

@Schema(description = "Um comentário de uma oportunidade, para exibição em ordem cronológica (mais recente primeiro). podeExcluir já vem calculado pelo backend (autor do comentário ou administrador), para o frontend não precisar duplicar essa regra.")
public record OportunidadeComentarioDTO(
        String publicId,
        String autorId,
        String autorNome,
        String autorAvatarUrl,
        String conteudo,
        String urlAnexo,
        @Schema(description = "Nome original do arquivo anexado, para exibição e download.")
        String nomeAnexo,
        @Schema(description = "Content-type validado do anexo. O frontend usa para decidir entre miniatura (imagem) e item de download (documento).")
        String tipoAnexo,
        @Schema(description = "Tamanho do anexo em bytes.")
        Long tamanhoAnexo,
        LocalDateTime criadoEm,
        boolean podeExcluir
) {
    public OportunidadeComentarioDTO(OportunidadeComentario comentario, boolean podeExcluir) {
        this(
                comentario.getPublicId(),
                comentario.getAutor().getPublicId(),
                comentario.getAutor().getNome(),
                comentario.getAutor().getUrlPicture(),
                comentario.getConteudo(),
                comentario.getUrlAnexo(),
                comentario.getNomeAnexo(),
                comentario.getTipoAnexo(),
                comentario.getTamanhoAnexo(),
                comentario.getCriadoEm(),
                podeExcluir
        );
    }
}
