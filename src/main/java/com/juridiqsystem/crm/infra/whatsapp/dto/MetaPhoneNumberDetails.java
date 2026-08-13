package com.juridiqsystem.crm.infra.whatsapp.dto;

/** Resultado de GET /{phone-number-id}?fields=display_phone_number,verified_name. */
public record MetaPhoneNumberDetails(String displayPhoneNumber, String verifiedName) {
}
