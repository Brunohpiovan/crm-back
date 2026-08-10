package com.juridiqsystem.crm.infra.security;

import com.auth0.jwt.interfaces.DecodedJWT;
import com.juridiqsystem.crm.model.Usuario;
import com.juridiqsystem.crm.repository.UsuarioRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.lang.NonNull;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessagingException;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Autentica o handshake STOMP (frame CONNECT) do WebSocket. Sem isso, as conexoes de chat nunca
 * passam pelo SecurityFilter (que so roda na FilterChain de servlet), entao o TenantContext nunca
 * era populado e toda busca por Usuario/@TenantId feita a partir de uma mensagem STOMP voltava
 * vazia (ex.: "Usuario Sender nao encontrado" no chat interno), mesmo com o remetente existindo.
 *
 * accessor.setUser(...) so vale para a mensagem atual - o Spring NAO reanexa esse Principal
 * automaticamente aos frames SEND seguintes da mesma sessao (confirmado: sem isso, o Principal
 * chegava null no @MessageMapping e estourava NPE). Por isso guardamos o Usuario resolvido no
 * CONNECT nos sessionAttributes (esses sim persistem por toda a sessao WebSocket) e reanexamos
 * o Principal em toda mensagem subsequente a partir dali.
 */
@Component
public class StompAuthChannelInterceptor implements ChannelInterceptor {

    private static final String SESSION_ATTR_USUARIO = "usuarioAutenticado";

    @Autowired
    private TokenService tokenService;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Override
    public Message<?> preSend(@NonNull Message<?> message, @NonNull MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
        if (accessor == null || accessor.getCommand() == null) {
            return message;
        }

        Map<String, Object> sessionAttributes = accessor.getSessionAttributes();

        if (accessor.getCommand() == StompCommand.CONNECT) {
            Usuario usuario = authenticate(accessor);
            if (sessionAttributes != null) {
                sessionAttributes.put(SESSION_ATTR_USUARIO, usuario);
            }
            accessor.setUser(toPrincipal(usuario));
            return message;
        }

        Usuario usuario = sessionAttributes != null ? (Usuario) sessionAttributes.get(SESSION_ATTR_USUARIO) : null;
        if (usuario == null) {
            throw new MessagingException("Sessao WebSocket nao autenticada");
        }
        if (accessor.getCommand() == StompCommand.SUBSCRIBE) {
            validarDestinoDaMesmaEmpresa(accessor.getDestination(), usuario);
        }
        accessor.setUser(toPrincipal(usuario));
        return message;
    }

    /**
     * Tópicos do chat de atendimento (WhatsApp/Twilio) são escopados por empresa no formato
     * "/topic/empresa/{empresaId}/...". Não basta o frontend montar a URL certa — sem essa
     * checagem, qualquer usuário autenticado (de qualquer empresa) poderia assinar o tópico de
     * outra empresa só sabendo o empresaId dela (é um Long sequencial, fácil de adivinhar).
     * Tópicos fora desse padrão (chat interno, funil etc.) não são afetados por este método.
     */
    private void validarDestinoDaMesmaEmpresa(String destino, Usuario usuario) {
        if (destino == null) {
            return;
        }
        String prefixo = "/topic/empresa/";
        if (!destino.startsWith(prefixo)) {
            return;
        }
        String resto = destino.substring(prefixo.length());
        String empresaIdDoTopico = resto.contains("/") ? resto.substring(0, resto.indexOf('/')) : resto;
        if (!String.valueOf(usuario.getEmpresaId()).equals(empresaIdDoTopico)) {
            throw new MessagingException("Nao autorizado a assinar topico de outra empresa");
        }
    }

    private Usuario authenticate(StompHeaderAccessor accessor) {
        String token = extractToken(accessor);
        if (token == null) {
            throw new MessagingException("Token ausente na conexao WebSocket");
        }

        DecodedJWT decoded = tokenService.validateToken(token);
        if (decoded == null) {
            throw new MessagingException("Token invalido ou expirado");
        }

        Long empresaId = decoded.getClaim("empresaId").asLong();
        String userPublicId = decoded.getSubject();
        Usuario usuario = TenantContext.runAs(empresaId,
                () -> usuarioRepository.findByPublicId(userPublicId).orElse(null));
        if (usuario == null) {
            throw new MessagingException("Usuario do token nao encontrado");
        }
        return usuario;
    }

    private UsernamePasswordAuthenticationToken toPrincipal(Usuario usuario) {
        return new UsernamePasswordAuthenticationToken(usuario, null, usuario.getAuthorities());
    }

    private String extractToken(StompHeaderAccessor accessor) {
        String header = accessor.getFirstNativeHeader("Authorization");
        if (header == null || !header.startsWith("Bearer ")) {
            return null;
        }
        return header.substring("Bearer ".length());
    }
}
