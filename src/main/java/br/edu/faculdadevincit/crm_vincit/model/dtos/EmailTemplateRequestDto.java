package br.edu.faculdadevincit.crm_vincit.model.dtos;

import br.edu.faculdadevincit.crm_vincit.model.enums.Situacao;
import jakarta.persistence.JoinColumn;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class EmailTemplateRequestDto {
    private String nome;
    private String assunto;
    @Size(max = 1000, message = "A Mensagem deve ter no maximo 1000 caracteres")
    private String mensagem;
    private String situacao;
    private List<String> urlAnexo;
    private List<MultipartFile> anexos;

}
