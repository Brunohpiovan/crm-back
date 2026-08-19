package com.juridiqsystem.crm.model.dtos;

import com.juridiqsystem.crm.model.ProcessoEnvolvido;
import com.juridiqsystem.crm.model.enums.TipoEnvolvido;

public record ProcessoEnvolvidoResponse(String nome, TipoEnvolvido tipo, String documento, String oab) {

    public ProcessoEnvolvidoResponse(ProcessoEnvolvido envolvido) {
        this(envolvido.getNome(), envolvido.getTipo(), envolvido.getDocumento(), envolvido.getOab());
    }
}
