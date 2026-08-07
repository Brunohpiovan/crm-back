package com.juridiqsystem.crm.model.dtos;

import com.juridiqsystem.crm.model.Oportunidade;
import com.juridiqsystem.crm.model.Participante;
import com.juridiqsystem.crm.model.enums.TipoParticipante;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Schema(description = "Resumo de participante (id, nome, login, celular, foto, tipo), usado nas listagens e buscas de /participante.")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ParticipanteDTO {

    private String id;
    private String urlPicture;
    private String nome;
    private String login;
    private String celular;
    @Schema(description = "Tipo do participante: FUNCIONARIO (usuário interno do CRM) ou PARTICIPANTE (contato externo, ex.: cliente via WhatsApp)")
    private TipoParticipante tipoParticipante;
    @Schema(description = "true quando este participante tem um protocolo ABERTO com o administrador que fez a requisição")
    private boolean openProtocol;

    public ParticipanteDTO(Participante participante) {
        this(participante, false);
    }

    public ParticipanteDTO(Participante participante, boolean openProtocol) {
        this.id = participante.getPublicId();
        this.nome = participante.getNome();
        this.login = participante.getLogin();
        this.celular = participante.getCelular();
        this.urlPicture = participante.getUrlPicture();
        this.tipoParticipante = participante.getTipoParticipante();
        this.openProtocol = openProtocol;
    }
}
