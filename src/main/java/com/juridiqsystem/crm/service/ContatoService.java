package com.juridiqsystem.crm.service;

import com.juridiqsystem.crm.infra.security.logging.SecurityEventType;
import com.juridiqsystem.crm.infra.security.logging.SecurityLogger;
import com.juridiqsystem.crm.model.Etapa;
import com.juridiqsystem.crm.model.Usuario;
import com.juridiqsystem.crm.model.dtos.ContatoDTO;
import com.juridiqsystem.crm.model.dtos.OportunidadeClienteRequest;
import com.juridiqsystem.crm.model.dtos.OportunidadeCreateRequest;
import com.juridiqsystem.crm.model.enums.Origem;
import com.juridiqsystem.crm.model.enums.SituacaoOportunidade;
import com.juridiqsystem.crm.repository.EtapaRepository;
import com.juridiqsystem.crm.repository.UsuarioRepository;
import com.juridiqsystem.crm.service.auth.ClientInfoService;
import com.juridiqsystem.crm.service.auth.SlidingWindowRateLimiter;
import com.juridiqsystem.crm.service.exceptions.TooManyRequestsException;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Duration;

@Service
public class ContatoService {

    // Formulário público sem login: limite generoso o bastante para uso legítimo (alguém
    // preenchendo de novo após corrigir um campo) mas que impede um bot de inundar o funil de
    // vendas com leads falsos a partir do mesmo IP.
    private static final int MAX_SUBMISSOES_POR_JANELA = 5;
    private static final Duration JANELA_RATE_LIMIT = Duration.ofMinutes(10);

    @Value("${etapa.entrada.id}")
    private String etapaId;

    @Autowired
    private EtapaRepository etapaRepository;

    @Autowired
    private OportunidadeService oportunidadeService;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private ClientInfoService clientInfoService;

    @Autowired
    private SlidingWindowRateLimiter rateLimiter;

    @Autowired
    private SecurityLogger securityLogger;

    public void create(ContatoDTO contatoDTO, HttpServletRequest httpRequest) {
        String clientIp = clientInfoService.getClientIp(httpRequest);

        if (!rateLimiter.tryAcquire("contato:" + clientIp, MAX_SUBMISSOES_POR_JANELA, JANELA_RATE_LIMIT)) {
            throw new TooManyRequestsException("Muitas solicitações. Tente novamente mais tarde.");
        }

        // Honeypot: campo invisível pro usuário real, que nenhum humano preenche. Bot que
        // preenche todo campo do formulário cai aqui. Nunca revela a detecção ao chamador —
        // simplesmente não persiste nada e retorna como se tivesse dado certo (ver
        // ContatoController), pra não ensinar o bot a identificar e evitar esse mecanismo.
        if (contatoDTO.getWebsite() != null && !contatoDTO.getWebsite().isBlank()) {
            securityLogger.log(SecurityEventType.SUSPICIOUS_REQUEST,
                    "Honeypot preenchido no formulário de contato (provável bot)", null, clientIp, "/api/contato");
            return;
        }

        Etapa etapa = findEtapaEntrada();
        Usuario usuario = findUsuarioDisponivel();

        OportunidadeClienteRequest cliente = new OportunidadeClienteRequest(
                null,
                contatoDTO.getNome(),
                contatoDTO.getEmail(),
                contatoDTO.getCelular()
        );

        OportunidadeCreateRequest request = new OportunidadeCreateRequest(
                contatoDTO.getNome(),
                etapa.getPublicId(),
                usuario.getPublicId(),
                cliente,
                BigDecimal.ZERO,
                null,
                Origem.SITE,
                contatoDTO.getInteresse(),
                null,
                null,
                SituacaoOportunidade.ABERTO,
                null
        );

        oportunidadeService.create(request, null);
    }

    private Etapa findEtapaEntrada() {
        return etapaRepository.findById(Long.parseLong(etapaId))
                .orElseThrow(() -> new RuntimeException("A etapa com id " + etapaId + " não existe no funil"));
    }

    private Usuario findUsuarioDisponivel() {
        return usuarioRepository.findRandomUsuarioDisponivel()
                .orElseThrow(() -> new RuntimeException("Nenhum usuário disponível encontrado"));
    }

}
