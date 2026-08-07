package com.juridiqsystem.crm.tools;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

/**
 * Utilitário standalone (não é endpoint, não sobe o Spring) pra gerar o hash BCrypt de uma senha
 * em texto puro, usado no script de provisionamento manual de empresa nova (ver
 * scripts/provisionar-empresa.sql). Usa o mesmo BCryptPasswordEncoder (força padrão, 10) do bean
 * declarado em SecurityConfiguration — o hash gerado aqui é aceito normalmente pelo login.
 * <p>
 * Rodar (depois de {@code mvn compile}):
 * <pre>
 * java -cp target/classes com.juridiqsystem.crm.tools.GeradorHashSenha "senha-em-texto-puro"
 * </pre>
 */
public final class GeradorHashSenha {

    private GeradorHashSenha() {
    }

    public static void main(String[] args) {
        if (args.length != 1 || args[0].isBlank()) {
            System.err.println("Uso: GeradorHashSenha \"senha-em-texto-puro\"");
            System.exit(1);
        }
        System.out.println(new BCryptPasswordEncoder().encode(args[0]));
    }
}
