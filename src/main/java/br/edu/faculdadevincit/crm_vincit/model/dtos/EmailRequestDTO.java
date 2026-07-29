package br.edu.faculdadevincit.crm_vincit.model.dtos;

import jakarta.persistence.JoinColumn;
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
public class EmailRequestDTO {
    private String destinatario;
    private String assunto;
    private String corpo;
    @JoinColumn(name = "remetente_id")
    private Long id_remetente;
    private List<MultipartFile> anexos;
}
