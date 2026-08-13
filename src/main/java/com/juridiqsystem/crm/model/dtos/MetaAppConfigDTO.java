package com.juridiqsystem.crm.model.dtos;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Dados públicos necessários para o frontend inicializar o Facebook JS SDK e disparar o Embedded Signup. Nunca inclui o App Secret.")
public record MetaAppConfigDTO(String appId, String configId, String graphApiVersion) {
}
