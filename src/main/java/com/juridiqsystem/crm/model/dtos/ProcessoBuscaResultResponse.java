package com.juridiqsystem.crm.model.dtos;

import com.juridiqsystem.crm.infra.escavador.dto.EscavadorProcesso;

/** Item de resultado de busca reversa (por envolvido/OAB) — dado cru da Escavador, sem upsert local. */
public record ProcessoBuscaResultResponse(String numeroCnj, String tituloPoloAtivo, String tituloPoloPassivo, String estadoOrigem, String dataInicio) {

    public ProcessoBuscaResultResponse(EscavadorProcesso remoto) {
        this(
                remoto.numeroCnj(),
                remoto.tituloPoloAtivo(),
                remoto.tituloPoloPassivo(),
                remoto.estadoOrigem() != null ? remoto.estadoOrigem().sigla() : null,
                remoto.dataInicio()
        );
    }
}
