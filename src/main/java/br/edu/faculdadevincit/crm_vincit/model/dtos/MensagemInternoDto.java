package br.edu.faculdadevincit.crm_vincit.model.dtos;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
public class MensagemInternoDto {

    private String id;
    private String id_group;
    private String id_sender;
    private String id_reciver;
    private String conteudo;
    private LocalDateTime data_envio;
}
