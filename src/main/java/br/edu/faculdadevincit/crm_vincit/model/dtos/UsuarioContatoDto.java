package br.edu.faculdadevincit.crm_vincit.model.dtos;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Schema(description = "Resumo mínimo (id, nome, foto) de um contato/participante, publicado via WebSocket no tópico /topic/usuarios quando um novo participante de WhatsApp é criado.")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class UsuarioContatoDto {

    private String id;
    private String nome;
    private String urlPicture;


    @Override
    public String toString() {
        return "UsuarioContatoDto{" +
                "id=" + id +
                ", nome='" + nome + '\'' +
                ", urlPicture='" + urlPicture + '\'' +
                '}';
    }
}
