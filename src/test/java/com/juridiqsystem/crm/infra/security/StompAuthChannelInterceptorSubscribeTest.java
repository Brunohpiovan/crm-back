package com.juridiqsystem.crm.infra.security;

import com.juridiqsystem.crm.model.Usuario;
import com.juridiqsystem.crm.testsupport.TestCargoFactory;
import com.juridiqsystem.crm.repository.UsuarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessagingException;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Os tópicos WebSocket do chat de atendimento são escopados por empresa
 * ("/topic/empresa/{empresaId}/..."). Não basta o frontend montar a URL certa: o backend precisa
 * validar, no SUBSCRIBE, que o usuário autenticado realmente pertence àquela empresa — sem isso,
 * qualquer usuário autenticado poderia assinar o tópico de outra empresa só sabendo o empresaId
 * dela (um Long sequencial, fácil de adivinhar).
 */
@ExtendWith(MockitoExtension.class)
class StompAuthChannelInterceptorSubscribeTest {

    private static final String SECRET = "segredo-de-teste-nao-usado-em-producao";

    @Mock
    private UsuarioRepository usuarioRepository;

    private TokenService tokenService;
    private StompAuthChannelInterceptor interceptor;

    @BeforeEach
    void setUp() {
        tokenService = new TokenService();
        ReflectionTestUtils.setField(tokenService, "secret", SECRET);

        interceptor = new StompAuthChannelInterceptor();
        ReflectionTestUtils.setField(interceptor, "tokenService", tokenService);
        ReflectionTestUtils.setField(interceptor, "usuarioRepository", usuarioRepository);
    }

    private Usuario usuario(Long empresaId, String publicId) {
        Usuario usuario = new Usuario();
        usuario.setId(1L);
        usuario.setPublicId(publicId);
        usuario.setEmpresaId(empresaId);
        usuario.setLogin("user@empresa" + empresaId + ".com");
        usuario.setNome("Usuário Teste");
        usuario.setCargo(TestCargoFactory.comum());
        return usuario;
    }

    private Map<String, Object> conecta(Long empresaId, String publicId) {
        String token = tokenService.generateToken(usuario(empresaId, publicId));
        when(usuarioRepository.findByPublicId(publicId)).thenReturn(Optional.of(usuario(empresaId, publicId)));

        Map<String, Object> sessionAttributes = new HashMap<>();
        StompHeaderAccessor connectAccessor = StompHeaderAccessor.create(StompCommand.CONNECT);
        connectAccessor.setSessionAttributes(sessionAttributes);
        connectAccessor.setNativeHeader("Authorization", "Bearer " + token);
        // Sem isso, o accessor recuperado de dentro de preSend (via MessageHeaderAccessor.getAccessor)
        // fica "congelado" e qualquer setter (ex.: accessor.setUser(...) dentro do interceptor) lança
        // "IllegalState: Already immutable".
        connectAccessor.setLeaveMutable(true);
        Message<byte[]> connectMessage = MessageBuilder.createMessage(new byte[0], connectAccessor.getMessageHeaders());

        interceptor.preSend(connectMessage, mock(MessageChannel.class));
        return sessionAttributes;
    }

    private void subscreve(Map<String, Object> sessionAttributes, String destino) {
        StompHeaderAccessor subscribeAccessor = StompHeaderAccessor.create(StompCommand.SUBSCRIBE);
        subscribeAccessor.setSessionAttributes(sessionAttributes);
        subscribeAccessor.setDestination(destino);
        subscribeAccessor.setLeaveMutable(true);
        Message<byte[]> subscribeMessage = MessageBuilder.createMessage(new byte[0], subscribeAccessor.getMessageHeaders());

        interceptor.preSend(subscribeMessage, mock(MessageChannel.class));
    }

    @Test
    void subscribe_topicoDaPropriaEmpresa_permitido() {
        Map<String, Object> sessao = conecta(1L, "public-id-empresa-1");

        assertThatCode(() -> subscreve(sessao, "/topic/empresa/1/messages/public")).doesNotThrowAnyException();
    }

    @Test
    void subscribe_topicoDeOutraEmpresa_rejeitado() {
        Map<String, Object> sessao = conecta(1L, "public-id-empresa-1");

        assertThatThrownBy(() -> subscreve(sessao, "/topic/empresa/2/messages/public"))
                .isInstanceOf(MessagingException.class);
    }

    @Test
    void subscribe_topicoDeOutraEmpresaComProtocoloEspecifico_rejeitado() {
        Map<String, Object> sessao = conecta(1L, "public-id-empresa-1");

        assertThatThrownBy(() -> subscreve(sessao, "/topic/empresa/2/protocolo/aberto/algum-user-id"))
                .isInstanceOf(MessagingException.class);
    }

    @Test
    void subscribe_topicoForaDoPadraoPorEmpresa_naoEhAfetado() {
        Map<String, Object> sessao = conecta(1L, "public-id-empresa-1");

        assertThatCode(() -> subscreve(sessao, "/topic/messages-interna/5")).doesNotThrowAnyException();
    }
}
