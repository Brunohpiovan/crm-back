package com.juridiqsystem.crm.model.enums;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

/**
 * Persiste FusoHorarioEmpresa como o id IANA (`America/Sao_Paulo`) na coluna `empresa.timezone`,
 * em vez do nome da constante Java (`AMERICA_SAO_PAULO`) que @Enumerated(STRING) usaria — mantém
 * a coluna (VARCHAR(60), já populada com esses valores antes do campo virar enum) sem precisar
 * de migração. `autoApply = true` dispensa @Convert no campo da entidade.
 */
@Converter(autoApply = true)
public class FusoHorarioEmpresaConverter implements AttributeConverter<FusoHorarioEmpresa, String> {

    @Override
    public String convertToDatabaseColumn(FusoHorarioEmpresa attribute) {
        return attribute == null ? null : attribute.getZoneId();
    }

    @Override
    public FusoHorarioEmpresa convertToEntityAttribute(String dbData) {
        return dbData == null ? null : FusoHorarioEmpresa.fromZoneId(dbData);
    }
}
