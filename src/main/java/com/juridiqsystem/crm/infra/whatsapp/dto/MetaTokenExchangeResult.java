package com.juridiqsystem.crm.infra.whatsapp.dto;

/** Resultado da troca do código do Embedded Signup por um token de acesso de negócio (business token). */
public record MetaTokenExchangeResult(String accessToken, Long expiresInSeconds) {
}
