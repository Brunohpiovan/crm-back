package br.edu.faculdadevincit.crm_vincit.model.dtos;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.web.multipart.MultipartFile;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
public class MensagemDto{

    private String id;
    private String id_protocolo;
    private String id_sender;
    private String conteudo;
    private String media;
    private LocalDateTime data_envio;

}
