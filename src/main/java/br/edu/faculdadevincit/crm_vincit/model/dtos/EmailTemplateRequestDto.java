package br.edu.faculdadevincit.crm_vincit.model.dtos;

import br.edu.faculdadevincit.crm_vincit.model.enums.Situacao;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.JoinColumn;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Schema(description = "Dados para criação/atualização de um template de e-mail. Montado internamente a partir dos campos multipart/form-data recebidos pelo controller (não é vinculado diretamente como corpo de requisição JSON).")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class EmailTemplateRequestDto {
    private String nome;
    private String assunto;
    @Size(max = 1000, message = "A Mensagem deve ter no maximo 1000 caracteres")
    @Schema(description = "Corpo/mensagem do template (máximo 1000 caracteres).")
    private String mensagem;
    @Schema(description = "Situação do template como texto (deve corresponder a um valor do enum Situacao: ATIVA ou INATIVA, case-insensitive).")
    private String situacao;
    @Schema(description = "URLs de anexos já existentes a manter associados ao template.")
    private List<String> urlAnexo;
    @Schema(description = "Novos arquivos de anexo enviados via multipart, a serem enviados ao S3.")
    private List<MultipartFile> anexos;

}
