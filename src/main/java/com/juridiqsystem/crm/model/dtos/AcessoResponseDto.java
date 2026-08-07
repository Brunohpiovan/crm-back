package com.juridiqsystem.crm.model.dtos;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class AcessoResponseDto {
    private String id;
    private String enderecoIp;
    private String localizacao;
    private LocalDateTime data_acesso;
    private LocalDateTime data_saida;
    private String provedorInternet;
    private String sistemaOperacional;
    private String tipoDispositivo;
    private String fusoHorario;
    private String navegador;
    private String versaoNavegador;
    private String idiomaNavegador;
}
