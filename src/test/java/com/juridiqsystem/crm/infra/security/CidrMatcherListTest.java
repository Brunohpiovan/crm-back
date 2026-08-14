package com.juridiqsystem.crm.infra.security;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class CidrMatcherListTest {

    @Test
    void vazio_naoConsideraNenhumIpConfiavel() {
        CidrMatcherList lista = CidrMatcherList.parse("", "TEST");

        assertThat(lista.isEmpty()).isTrue();
        assertThat(lista.matches("127.0.0.1")).isFalse();
        assertThat(lista.matches("10.0.0.5")).isFalse();
    }

    @Test
    void ipExato_bateSomenteComEleMesmo() {
        CidrMatcherList lista = CidrMatcherList.parse("192.168.1.10", "TEST");

        assertThat(lista.matches("192.168.1.10")).isTrue();
        assertThat(lista.matches("192.168.1.11")).isFalse();
    }

    @Test
    void cidr_bateComQualquerIpDaFaixa() {
        CidrMatcherList lista = CidrMatcherList.parse("10.0.0.0/8", "TEST");

        assertThat(lista.matches("10.1.2.3")).isTrue();
        assertThat(lista.matches("10.255.255.255")).isTrue();
        assertThat(lista.matches("11.0.0.1")).isFalse();
    }

    @Test
    void multiplasEntradas_csvComEspacos() {
        CidrMatcherList lista = CidrMatcherList.parse(" 10.0.0.0/8 , 172.16.0.5 ", "TEST");

        assertThat(lista.matches("10.5.5.5")).isTrue();
        assertThat(lista.matches("172.16.0.5")).isTrue();
        assertThat(lista.matches("172.16.0.6")).isFalse();
    }

    @Test
    void entradaInvalida_eIgnoradaSemDerrubarAsDemais() {
        CidrMatcherList lista = CidrMatcherList.parse("nao-e-um-ip, 10.0.0.0/8", "TEST");

        assertThat(lista.matches("10.1.1.1")).isTrue();
    }

    @Test
    void ipv6_suportado() {
        CidrMatcherList lista = CidrMatcherList.parse("::1/128", "TEST");

        assertThat(lista.matches("0:0:0:0:0:0:0:1")).isTrue();
        assertThat(lista.matches("2001:db8::1")).isFalse();
    }
}
