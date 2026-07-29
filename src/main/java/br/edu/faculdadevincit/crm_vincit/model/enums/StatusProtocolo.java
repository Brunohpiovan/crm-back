package br.edu.faculdadevincit.crm_vincit.model.enums;

import lombok.Getter;

@Getter
public enum StatusProtocolo {

    ABERTO(0),
    FECHADO(1);

    private final int value;

    StatusProtocolo(int value) {
        this.value = value;
    }

    public static StatusProtocolo fromValue(int value) {
        for (StatusProtocolo status : StatusProtocolo.values()) {
            if (status.getValue() == value) {
                return status;
            }
        }
        throw new IllegalArgumentException("Invalid value: " + value);
    }
}
