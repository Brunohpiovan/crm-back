package com.juridiqsystem.crm.model.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

/**
 * Fusos horários que uma Empresa pode escolher na aba Configurações/master. Fechado num enum (em
 * vez de String livre) pra garantir que só um fuso suportado pelo front (ver Select de timezone
 * em SettingsPage/EmpresaForm) chegue a ser persistido. O valor serializado em JSON e persistido
 * no banco (coluna `empresa.timezone`, via FusoHorarioEmpresaConverter) é o id IANA (`zoneId`),
 * não o nome da constante — o contrato da API não muda.
 */
public enum FusoHorarioEmpresa {

    AMERICA_SAO_PAULO("America/Sao_Paulo"),
    AMERICA_MANAUS("America/Manaus"),
    AMERICA_NORONHA("America/Noronha");

    private final String zoneId;

    FusoHorarioEmpresa(String zoneId) {
        this.zoneId = zoneId;
    }

    @JsonValue
    public String getZoneId() {
        return zoneId;
    }

    @JsonCreator
    public static FusoHorarioEmpresa fromZoneId(String zoneId) {
        for (FusoHorarioEmpresa fuso : values()) {
            if (fuso.zoneId.equalsIgnoreCase(zoneId)) {
                return fuso;
            }
        }
        throw new IllegalArgumentException("Timezone não suportado: " + zoneId);
    }
}
