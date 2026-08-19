package com.juridiqsystem.crm.model.dtos;

import com.juridiqsystem.crm.model.Usuario;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Schema(description = "Representação resumida do usuário responsável (vendedor/dono) por uma oportunidade, aninhada dentro de OportunidadeDTO.")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CriadorOportunidadeDto {
    private String id;
    private String nome;
    private String urlPicture;

    public CriadorOportunidadeDto(Usuario usuario){
        this.id = usuario.getPublicId();
        this.nome=usuario.getNome();
        this.urlPicture = usuario.getUrlPicture();
    }
}
