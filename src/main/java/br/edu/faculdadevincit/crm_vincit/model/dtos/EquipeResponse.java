package br.edu.faculdadevincit.crm_vincit.model.dtos;

import br.edu.faculdadevincit.crm_vincit.model.Equipe;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "Equipe com seus membros.")
public record EquipeResponse(String id, String nome, List<UsuarioContatoDto> membros) {

    public EquipeResponse(Equipe equipe) {
        this(equipe.getPublicId(), equipe.getNome(),
                equipe.getMembros().stream()
                        .map(usuario -> new UsuarioContatoDto(usuario.getPublicId(), usuario.getNome(), usuario.getUrlPicture()))
                        .toList());
    }
}
