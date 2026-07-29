package br.edu.faculdadevincit.crm_vincit.model.dtos;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class UsuarioContatoDto {

    private Long id;
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
