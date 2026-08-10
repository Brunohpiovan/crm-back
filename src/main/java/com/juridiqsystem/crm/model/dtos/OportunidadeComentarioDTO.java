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
                comentario.getCriadoEm(),
                podeExcluir
        );
    }
}
