package com.juridiqsystem.crm.model.whatsapp;

/** Tipo de conteúdo de uma mensagem WhatsApp recebida, já normalizado a partir do JSON da Meta. */
public enum MessageType {
    TEXT,
    IMAGE,
    AUDIO,
    DOCUMENT,
    VIDEO,
    UNKNOWN
}
