package br.edu.faculdadevincit.crm_vincit.model.dtos;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
public class MensagemInternoDto {

    private Long id;
    private Long id_group;
    private Long id_sender;
    private Long id_reciver;
    private String conteudo;
    private LocalDateTime data_envio;
}
