package br.edu.faculdadevincit.crm_vincit.model.dtos;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.annotation.Nullable;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Schema(description = "Dados de um grupo de chat, enviados como a parte multipart 'grupo' (JSON) nos endpoints de criação/atualização de grupo.")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class GrupoCreateDTO {

    @Schema(description = "Id do grupo. Não deve ser enviado ao criar um novo grupo; usado apenas ao atualizar um grupo existente (deve corresponder ao {id} da URL).")
    private String id;

    @Nullable
    @Schema(description = "URL da imagem de avatar do grupo já existente no S3. Ignorado quando um novo arquivo é enviado na parte multipart 'foto'.")
    private String urlPicture;

    @Nullable
    @Schema(description = "URL da imagem de fundo do grupo já existente no S3. Ignorado quando um novo arquivo é enviado na parte multipart 'imagemFundo'.")
    private String backgroundImageUrl;

    @NotNull(message = "O campo NOME é requerido.")
    @Size(max = 150, message = "O nome deve ter no maximo 150 caracteres")
    @Schema(description = "Nome do grupo (máximo 150 caracteres).", requiredMode = Schema.RequiredMode.REQUIRED)
    private String nome;

    @Schema(description = "Ids dos usuários participantes do grupo.")
    private List<String> usuarios;
}
