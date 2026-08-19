package com.juridiqsystem.crm.service;

import com.juridiqsystem.crm.infra.security.TenantContext;
import com.juridiqsystem.crm.infra.whatsapp.MetaWebhookMapper;
import com.juridiqsystem.crm.infra.whatsapp.MetaWebhookSignatureValidator;
import com.juridiqsystem.crm.infra.whatsapp.MetaWhatsAppClient;
import com.juridiqsystem.crm.infra.whatsapp.MetaWhatsAppProperties;
import com.juridiqsystem.crm.infra.whatsapp.dto.MetaMediaUrlResult;
import com.juridiqsystem.crm.infra.whatsapp.dto.MetaSendMessageResult;
import com.juridiqsystem.crm.model.*;
import com.juridiqsystem.crm.model.dtos.MensagemResponseDTO;
import com.juridiqsystem.crm.model.dtos.NovaMensagemNotificacaoDTO;
import com.juridiqsystem.crm.model.dtos.UsuarioContatoDto;
import com.juridiqsystem.crm.model.dtos.WhatsAppSendResultDTO;
import com.juridiqsystem.crm.model.dtos.WhatsAppSendTemplateRequestDTO;
import com.juridiqsystem.crm.model.enums.MessageStatus;
import com.juridiqsystem.crm.model.enums.StatusProtocolo;
import com.juridiqsystem.crm.model.enums.TipoParticipante;
import com.juridiqsystem.crm.model.enums.WhatsAppIntegrationStatus;
import com.juridiqsystem.crm.model.whatsapp.IncomingWhatsAppMessage;
import com.juridiqsystem.crm.model.whatsapp.WhatsAppStatusEvent;
import com.juridiqsystem.crm.model.whatsapp.WhatsAppWebhookPayload;
import com.juridiqsystem.crm.repository.MensagemRepository;
import com.juridiqsystem.crm.repository.OportunidadeRepository;
import com.juridiqsystem.crm.repository.ParticipanteRepository;
import com.juridiqsystem.crm.repository.ProtocoloRepository;
import com.juridiqsystem.crm.repository.WhatsAppIntegrationRepository;
import com.juridiqsystem.crm.repository.WhatsappWebhookEventoRepository;
import com.juridiqsystem.crm.service.auth.SlidingWindowRateLimiter;
import com.juridiqsystem.crm.service.exceptions.ConflictException;
import com.juridiqsystem.crm.service.exceptions.IntegrationException;
import com.juridiqsystem.crm.service.exceptions.MetaWebhookException;
import com.juridiqsystem.crm.service.exceptions.TooManyRequestsException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.security.MessageDigest;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

/**
 * Envio e recebimento de mensagens WhatsApp via Meta Cloud API. Arquitetura:
 * WhatsAppController/ChatService -&gt; WhatsAppService -&gt; MetaWhatsAppClient -&gt; Graph API (saída) e
 * Meta -&gt; webhook -&gt; WhatsAppService -&gt; MetaWebhookMapper -&gt; Business Service (entrada). Ver
 * docs/whatsapp/ARCHITECTURE.md.
 */
@Slf4j
@Service
public class WhatsAppService {

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    @Autowired
    private ProtocoloRepository protocoloRepository;

    @Autowired
    private MensagemService mensagemService;

    @Autowired
    private MensagemRepository mensagemRepository;

    @Autowired
    private ParticipanteRepository participanteRepository;

    @Autowired
    private S3Service s3Service;

    @Autowired
    private OportunidadeRepository oportunidadeRepository;

    @Autowired
    private WhatsappWebhookEventoRepository whatsappWebhookEventoRepository;

    @Autowired
    private WhatsAppIntegrationRepository whatsAppIntegrationRepository;

    @Autowired
    private MetaWhatsAppClient metaWhatsAppClient;

    @Autowired
    private MetaWebhookMapper metaWebhookMapper;

    @Autowired
    private MetaWebhookSignatureValidator signatureValidator;

    @Autowired
    private MetaWhatsAppProperties metaProperties;

    @Autowired
    private SlidingWindowRateLimiter rateLimiter;

    // Cada mensagem custa dinheiro (Meta cobra por conversa) e uma conta comprometida poderia
    // usar isso pra enviar spam em massa via WhatsApp — limite por usuário autenticado.
    private static final int MAX_MESSAGES_PER_WINDOW = 30;
    private static final Duration RATE_LIMIT_WINDOW = Duration.ofMinutes(1);

    // ---------------------------------------------------------------------------------------
    // Envio
    // ---------------------------------------------------------------------------------------

    public WhatsAppSendResultDTO sendWhatsAppMessage(MensagemRequest mensagemRequest) {
        // Chamado tanto por /whatsapp/send (REST, autenticado via SecurityFilter -> SecurityContextHolder
        // populado) quanto por ChatService.sendMessage (STOMP /app/send, onde o Principal fica só na
        // mensagem STOMP em si — StompAuthChannelInterceptor nunca popula o SecurityContextHolder global).
        // Sem esse fallback, todo envio pelo chat interno (não pela tela de disparo avulso) lançava NPE aqui.
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String actor = authentication != null ? authentication.getName() : "empresa:" + TenantContext.get();
        if (!rateLimiter.tryAcquire("whatsapp-send:" + actor, MAX_MESSAGES_PER_WINDOW, RATE_LIMIT_WINDOW)) {
            throw new TooManyRequestsException("Muitas mensagens enviadas em um curto intervalo. Tente novamente em instantes.");
        }

        WhatsAppIntegration integration = requireConnectedIntegration(TenantContext.get());
        String to = normalizeToE164Digits(mensagemRequest.getTo());
        String media = mensagemRequest.getMedia();
        String body = mensagemRequest.getMessage();

        log.info("Enviando mensagem WhatsApp via Meta. empresaId={} phoneNumberId={} to={}", TenantContext.get(), integration.getPhoneNumberId(), to);

        MetaSendMessageResult result;
        if (media != null && !media.isBlank()) {
            String mediaType = guessMediaType(media);
            result = metaWhatsAppClient.sendMedia(integration.getPhoneNumberId(), integration.getAccessToken(), to, mediaType, media, body);
        } else {
            result = metaWhatsAppClient.sendText(integration.getPhoneNumberId(), integration.getAccessToken(), to, body != null ? body : "");
        }
        return new WhatsAppSendResultDTO(result.messageId(), MessageStatus.SENT);
    }

    public WhatsAppSendResultDTO sendTemplateMessage(WhatsAppSendTemplateRequestDTO request) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String actor = authentication != null ? authentication.getName() : "empresa:" + TenantContext.get();
        if (!rateLimiter.tryAcquire("whatsapp-send:" + actor, MAX_MESSAGES_PER_WINDOW, RATE_LIMIT_WINDOW)) {
            throw new TooManyRequestsException("Muitas mensagens enviadas em um curto intervalo. Tente novamente em instantes.");
        }

        WhatsAppIntegration integration = requireConnectedIntegration(TenantContext.get());
        String to = normalizeToE164Digits(request.getTo());

        MetaSendMessageResult result = metaWhatsAppClient.sendTemplate(
                integration.getPhoneNumberId(), integration.getAccessToken(), to,
                request.getTemplateName(), request.getLanguageCode(), request.getBodyParams());
        return new WhatsAppSendResultDTO(result.messageId(), MessageStatus.SENT);
    }

    private WhatsAppIntegration requireConnectedIntegration(Long empresaId) {
        WhatsAppIntegration integration = whatsAppIntegrationRepository.findByEmpresaId(empresaId)
                .orElseThrow(() -> new ConflictException("Integração com o WhatsApp (Meta) não configurada para esta empresa."));
        if (integration.getStatus() != WhatsAppIntegrationStatus.CONNECTED) {
            throw new ConflictException("Integração com o WhatsApp desta empresa não está conectada.");
        }
        return integration;
    }

    /**
     * A Graph API espera o destinatário como dígitos puros (sem "+", sem prefixo "whatsapp:"),
     * incluindo o DDI 55 e o 9º dígito do celular — diferente do formato Twilio antigo, que não
     * usava o 9. Alguns números ficaram salvos no banco no formato pré-2012 (DDD + 8 dígitos, sem
     * o 9), o que a Meta rejeita com "(#131030) Recipient phone number not in allowed list" mesmo
     * para o número correto: 55+DDD+8 dígitos vira 12 dígitos ao todo, e sem o 9 a Meta não
     * reconhece o número. Corrigimos isso inserindo o 9 de volta quando detectamos esse formato.
     */
    private String normalizeToE164Digits(String to) {
        String digits = to.replaceAll("\\D", "");
        if (!digits.startsWith("55")) {
            digits = "55" + digits;
        }
        if (digits.length() == 12) {
            digits = digits.substring(0, 4) + "9" + digits.substring(4);
        }
        return digits;
    }

    private String guessMediaType(String mediaUrl) {
        String lower = mediaUrl.toLowerCase(Locale.ROOT);
        int query = lower.indexOf('?');
        if (query >= 0) {
            lower = lower.substring(0, query);
        }
        if (lower.matches(".*\\.(jpe?g|png|webp)$")) {
            return "image";
        }
        if (lower.matches(".*\\.(mp3|ogg|oga|opus|amr|aac|m4a|webm)$")) {
            return "audio";
        }
        if (lower.matches(".*\\.(mp4|3gp|mov)$")) {
            return "video";
        }
        return "document";
    }

    // ---------------------------------------------------------------------------------------
    // Webhook (recebimento)
    // ---------------------------------------------------------------------------------------

    public String verifyWebhook(String mode, String verifyToken, String challenge) {
        boolean tokenOk = verifyToken != null && metaProperties.getVerifyToken() != null
                && MessageDigest.isEqual(verifyToken.getBytes(StandardCharsets.UTF_8), metaProperties.getVerifyToken().getBytes(StandardCharsets.UTF_8));
        if (!"subscribe".equals(mode) || !tokenOk) {
            throw new MetaWebhookException("Verify token inválido.");
        }
        return challenge;
    }

    /**
     * Webhook público da Meta: diferente do modelo Twilio (Auth Token por empresa, exigindo
     * resolver o tenant ANTES de validar a assinatura), aqui a assinatura é validada com um único
     * App Secret do SaaS inteiro — então validamos primeiro, e só depois resolvemos a empresa a
     * partir do phone_number_id do payload.
     */
    public void receiveWebhookEvent(String rawBody, String signatureHeader) {
        if (!signatureValidator.isValid(rawBody, signatureHeader, metaProperties.getAppSecret())) {
            throw new MetaWebhookException("Assinatura do webhook Meta inválida.");
        }

        WhatsAppWebhookPayload payload = metaWebhookMapper.parse(rawBody);
        if (payload.phoneNumberId() == null) {
            return; // evento sem relação com mensagens (ex.: outro campo de conta) — nada a fazer
        }

        Optional<WhatsAppIntegration> integrationOpt = whatsAppIntegrationRepository
                .findByPhoneNumberIdIgnoringTenant(payload.phoneNumberId())
                .filter(i -> i.getStatus() == WhatsAppIntegrationStatus.CONNECTED);
        if (integrationOpt.isEmpty()) {
            log.warn("Webhook Meta recebido para phone_number_id nao cadastrado/desconectado. phoneNumberId={}", payload.phoneNumberId());
            return;
        }
        WhatsAppIntegration integration = integrationOpt.get();

        TenantContext.runAs(integration.getEmpresaId(), () -> {
            payload.messages().forEach(this::processarMensagemRecebida);
            payload.statuses().forEach(this::processarStatusEvento);
            return null;
        });
    }

    private void processarMensagemRecebida(IncomingWhatsAppMessage message) {
        if (!tentarRegistrarMensagemComoProcessada(message.externalMessageId())) {
            log.info("Webhook Meta ignorado: mensagem {} já foi processada ou está em processamento.", message.externalMessageId());
            return;
        }

        try {
            switch (message.type()) {
                case IMAGE -> receiveMedia(message, "imagem");
                case AUDIO -> receiveMedia(message, "audio");
                case DOCUMENT -> receiveMedia(message, "documentos");
                case VIDEO -> receiveMedia(message, "video");
                default -> receiveMessage(message);
            }
        } catch (RuntimeException e) {
            // Sem isso, uma falha real (ex.: S3 fora do ar) deixaria o id marcado como processado
            // para sempre, e um reenvio do webhook pela Meta nunca mais conseguiria reprocessar.
            desfazerRegistroDeProcessamento(message.externalMessageId());
            throw e;
        }
    }

    private void receiveMessage(IncomingWhatsAppMessage message) {
        Participante participante = resolveParticipante(message.from(), message.profileName());
        Optional<Protocolo> optionalProtocolo = protocoloRepository.findByCelularAndStatus(participante.getCelular(), StatusProtocolo.ABERTO);
        handleMessage(optionalProtocolo, participante, message.text(), null, message.externalMessageId());
    }

    private void receiveMedia(IncomingWhatsAppMessage message, String pastaS3) {
        Participante participante = resolveParticipante(message.from(), message.profileName());
        Optional<Protocolo> optionalProtocolo = protocoloRepository.findByCelularAndStatus(participante.getCelular(), StatusProtocolo.ABERTO);

        String s3Url;
        try {
            String accessToken = integrationAccessTokenAtual();
            MetaMediaUrlResult mediaUrlResult = metaWhatsAppClient.fetchMediaUrl(message.mediaId(), accessToken);
            byte[] mediaBytes = metaWhatsAppClient.downloadMedia(mediaUrlResult.url(), accessToken);

            String mimeType = mediaUrlResult.mimeType() != null ? mediaUrlResult.mimeType() : message.mediaMimeType();
            String extension = mimeType != null && mimeType.contains("/") ? mimeType.split("/")[1].split(";")[0] : "bin";
            String fileName = "whatsapp_" + pastaS3 + "_" + System.currentTimeMillis() + "." + extension;

            MultipartFile multipartFile = new ByteArrayMultipartFile(mediaBytes, fileName, mimeType);
            s3Url = s3Service.uploadFile(multipartFile, chatMediaKey(pastaS3, fileName));
        } catch (Exception e) {
            throw new IntegrationException("Falha ao baixar ou enviar mídia recebida via WhatsApp (mediaId=" + message.mediaId() + ")", e);
        }
        handleMessage(optionalProtocolo, participante, message.text(), s3Url, message.externalMessageId());
    }

    private String integrationAccessTokenAtual() {
        return whatsAppIntegrationRepository.findByEmpresaId(TenantContext.get())
                .map(WhatsAppIntegration::getAccessToken)
                .orElseThrow(() -> new ConflictException("Integração WhatsApp não encontrada para esta empresa."));
    }

    private void processarStatusEvento(WhatsAppStatusEvent evento) {
        if (evento.status() == null || evento.externalMessageId() == null) {
            return;
        }
        Optional<Mensagem> mensagemOpt = mensagemRepository.findByExternalMessageId(evento.externalMessageId());
        if (mensagemOpt.isEmpty()) {
            // Comum: status de uma mensagem enviada muito recentemente, cujo save ainda não
            // commitou, ou mensagem enviada antes de existir rastreio de status. Não é erro.
            log.debug("Status recebido para mensagem nao rastreada. externalMessageId={} status={}", evento.externalMessageId(), evento.status());
            return;
        }
        Mensagem mensagem = mensagemOpt.get();
        if (!avancaStatus(mensagem.getStatus(), evento.status())) {
            return; // Meta pode reenviar eventos fora de ordem; nunca regredimos um status já mais avançado
        }
        mensagem.setStatus(evento.status());
        mensagemRepository.save(mensagem);

        if (mensagem.getProtocolo() != null) {
            publicarNoWebSocket("/topic/empresa/" + TenantContext.get() + "/messages/" + mensagem.getProtocolo().getPublicId(), new MensagemResponseDTO(mensagem));
        }
    }

    /** sent -> delivered -> read é a única progressão válida; failed é terminal a partir de qualquer estado. */
    private boolean avancaStatus(MessageStatus atual, MessageStatus novo) {
        if (atual == null) {
            return true;
        }
        if (novo == MessageStatus.FAILED) {
            return true;
        }
        List<MessageStatus> ordem = List.of(MessageStatus.PENDING, MessageStatus.SENT, MessageStatus.DELIVERED, MessageStatus.READ);
        return ordem.indexOf(novo) > ordem.indexOf(atual);
    }

    private Participante resolveParticipante(String from, String profileName) {
        String celular = normalizeCelularFromMeta(from);
        return participanteRepository.findByCelular(celular)
                .orElseGet(() -> criaParticipante(celular, Optional.ofNullable(profileName)));
    }

    /**
     * O "from" do webhook Meta já vem no formato completo (DDI + DDD + 9 dígitos do celular, ex.:
     * "5511987654321") — diferente do "From" da Twilio (sandbox, formato antigo sem o 9º dígito,
     * que exigia reconstrução manual via reverseWhatsAppNumber). Aqui só removemos o DDI.
     * Atenção: participantes cadastrados antes desta migração podem ter celular salvo no formato
     * antigo (sem o 9º dígito) — ver docs/whatsapp/TROUBLESHOOTING.md.
     */
    private String normalizeCelularFromMeta(String from) {
        String digits = from.replaceAll("\\D", "");
        return digits.startsWith("55") ? digits.substring(2) : digits;
    }

    /**
     * Registra o id da mensagem como processado ANTES do processamento pesado (download de
     * mídia, upload S3, persistência) em vez de depois: assim, um reenvio do mesmo webhook pela
     * Meta esbarra na constraint única de whatsapp_webhook_evento.external_message_id em vez de
     * reprocessar tudo em paralelo (duplicando mensagem/participante/oportunidade).
     */
    private boolean tentarRegistrarMensagemComoProcessada(String externalMessageId) {
        if (externalMessageId == null || externalMessageId.isBlank()) {
            return true;
        }
        if (whatsappWebhookEventoRepository.existsByExternalMessageId(externalMessageId)) {
            return false;
        }
        try {
            WhatsappWebhookEvento evento = new WhatsappWebhookEvento();
            evento.setExternalMessageId(externalMessageId);
            evento.setProcessadoEm(LocalDateTime.now());
            whatsappWebhookEventoRepository.save(evento);
            return true;
        } catch (org.springframework.dao.DataIntegrityViolationException e) {
            log.warn("Mensagem {} ja havia sido registrada como processada (corrida concorrente).", externalMessageId);
            return false;
        }
    }

    private void desfazerRegistroDeProcessamento(String externalMessageId) {
        if (externalMessageId == null || externalMessageId.isBlank()) {
            return;
        }
        whatsappWebhookEventoRepository.deleteByExternalMessageId(externalMessageId);
    }

    /** Prefixo "chat/{empresaId}/..." para toda mídia recebida via WhatsApp — mesmo bucket S3 compartilhado, isolado por empresa. */
    private String chatMediaKey(String tipo, String fileName) {
        return "chat/" + TenantContext.get() + "/" + tipo + "/" + fileName;
    }

    private void handleMessage(Optional<Protocolo> optionalProtocolo, Participante participante, String body, String media, String externalMessageId) {
        String empresaId = String.valueOf(TenantContext.get());
        if (optionalProtocolo.isPresent()) {
            Protocolo protocolo = optionalProtocolo.get();
            List<Mensagem> savedMessage = mensagemService.sendMessage(protocolo, participante, body, media, externalMessageId, null);
            savedMessage.forEach(mensagemNew ->
                    publicarNoWebSocket("/topic/empresa/" + empresaId + "/messages/" + protocolo.getPublicId(), new MensagemResponseDTO(mensagemNew)));

            // Notifica o admin responsável pelo protocolo mesmo que ele não esteja com essa
            // conversa aberta (ou nem esteja na tela de Chat) — o frontend mantém uma conexão
            // WebSocket global assinando esse tópico pra tocar o som de notificação em qualquer
            // página. protocolo.getAdmin() vem via JOIN FETCH em findByCelularAndStatus, então é
            // seguro acessar aqui sem risco de LazyInitializationException.
            NovaMensagemNotificacaoDTO notificacao = new NovaMensagemNotificacaoDTO(
                    protocolo.getPublicId(), participante.getPublicId(), participante.getNome(), buildPreview(body, media));
            publicarNoWebSocket("/topic/empresa/" + empresaId + "/notificacoes/" + protocolo.getAdmin().getPublicId(), notificacao);
        } else {
            List<Mensagem> savedMessages = mensagemService.sendMessagePublico(participante, body, media, externalMessageId);
            savedMessages.forEach(mensagemNew ->
                    publicarNoWebSocket("/topic/empresa/" + empresaId + "/messages/public", new MensagemResponseDTO(mensagemNew)));
        }
    }

    private String buildPreview(String body, String media) {
        if (body != null && !body.isBlank()) {
            String trimmed = body.trim();
            return trimmed.length() > 80 ? trimmed.substring(0, 80) + "…" : trimmed;
        }
        return media != null ? "Enviou um anexo" : "Nova mensagem";
    }

    private void publicarNoWebSocket(String destino, Object payload) {
        try {
            messagingTemplate.convertAndSend(destino, payload);
        } catch (Exception e) {
            log.error("Falha ao publicar mensagem já persistida no WebSocket. destino={}", destino, e);
        }
    }

    public Participante criaParticipante(String from, Optional<String> profilename) {
        Participante participante = new Participante();
        participante.setNome(profilename.orElse(from));
        participante.setLogin(null);
        participante.setRg(null);
        participante.setCpf(null);
        participante.setUrlPicture("assets/img/avatar/padrao.jpeg");
        participante.setDataNascimento(null);
        participante.setCelular(from);
        participante.setEndereco(null);
        participante.setNumeroResidencial(null);
        participante.setComplemento(null);
        participante.setBairro(null);
        participante.setUf(null);
        participante.setCidade(null);
        participante.setObservacoes(null);
        participante.setTipoParticipante(TipoParticipante.PARTICIPANTE);
        participanteRepository.save(participante);
        UsuarioContatoDto contatoDto = new UsuarioContatoDto(participante.getPublicId(), participante.getNome(), participante.getUrlPicture());
        messagingTemplate.convertAndSend("/topic/empresa/" + TenantContext.get() + "/usuarios", contatoDto);
        return participante;
    }

    public Oportunidade criaOportunidade(Participante cliente) {
        Oportunidade newOportunidadae = new Oportunidade();
        newOportunidadae.setTitulo(null);
        newOportunidadae.setEtapa(null);
        newOportunidadae.setCliente(cliente);
        newOportunidadae.setValor(java.math.BigDecimal.ZERO);
        newOportunidadae.setData_criacao(LocalDateTime.now());
        newOportunidadae.setOrigem(null);
        newOportunidadae.setInteresse(null);
        newOportunidadae.setUrl_anexo(null);
        newOportunidadae.setIndice(0);
        messagingTemplate.convertAndSend("/topic/empresa/" + TenantContext.get() + "/newoportunidadectt", newOportunidadae);
        return oportunidadeRepository.save(newOportunidadae);
    }
}
