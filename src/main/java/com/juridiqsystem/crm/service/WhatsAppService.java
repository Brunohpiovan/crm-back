package com.juridiqsystem.crm.service;

import com.juridiqsystem.crm.infra.security.TenantContext;
import com.juridiqsystem.crm.model.*;
import com.juridiqsystem.crm.model.dtos.MensagemResponseDTO;
import com.juridiqsystem.crm.model.dtos.NovaMensagemNotificacaoDTO;
import com.juridiqsystem.crm.model.dtos.UsuarioContatoDto;
import com.juridiqsystem.crm.model.enums.StatusProtocolo;
import com.juridiqsystem.crm.model.enums.TipoParticipante;
import com.juridiqsystem.crm.repository.EmpresaTwilioConfigRepository;
import com.juridiqsystem.crm.repository.OportunidadeRepository;
import com.juridiqsystem.crm.repository.ParticipanteRepository;
import com.juridiqsystem.crm.repository.ProtocoloRepository;
import com.juridiqsystem.crm.repository.WhatsappWebhookEventoRepository;
import com.juridiqsystem.crm.service.auth.SlidingWindowRateLimiter;
import com.juridiqsystem.crm.service.exceptions.AccessDeniedException;
import com.juridiqsystem.crm.service.exceptions.ConflictException;
import com.juridiqsystem.crm.service.exceptions.IntegrationException;
import com.juridiqsystem.crm.service.exceptions.ResourceNotFoundException;
import com.juridiqsystem.crm.service.exceptions.TooManyRequestsException;
import com.twilio.exception.ApiConnectionException;
import com.twilio.http.TwilioRestClient;
import com.twilio.rest.api.v2010.account.Message;
import com.twilio.security.RequestValidator;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.math.BigDecimal;
import java.net.URI;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;

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
    private ParticipanteRepository participanteRepository;

    @Autowired
    private S3Service s3Service;

    @Autowired
    private OportunidadeRepository oportunidadeRepository;

    @Autowired
    private WhatsappWebhookEventoRepository whatsappWebhookEventoRepository;

    @Autowired
    private EmpresaTwilioConfigRepository empresaTwilioConfigRepository;

    @Autowired
    private TwilioClientProvider twilioClientProvider;

    @Autowired
    private SlidingWindowRateLimiter rateLimiter;

    // Cada mensagem custa dinheiro (Twilio cobra por envio) e uma conta comprometida poderia
    // usar isso pra enviar spam em massa via WhatsApp — limite por usuário autenticado.
    private static final int MAX_MESSAGES_PER_WINDOW = 30;
    private static final Duration RATE_LIMIT_WINDOW = Duration.ofMinutes(1);

    /**
     * Retry só em falha de conectividade (ApiConnectionException) — não em ApiException, que
     * representa uma resposta de erro válida da Twilio (ex.: número inválido) e não se resolveria
     * tentando de novo.
     */
    @Retryable(retryFor = ApiConnectionException.class, maxAttempts = 3, backoff = @Backoff(delay = 500, multiplier = 2))
    public Message sendWhatsAppMessage(MensagemRequest mensagemRequest) {
        // Chamado tanto por /whatsapp/send (REST, autenticado via SecurityFilter -> SecurityContextHolder
        // populado) quanto por ChatService.sendMessage (STOMP /app/send, onde o Principal fica só na
        // mensagem STOMP em si — StompAuthChannelInterceptor nunca popula o SecurityContextHolder global).
        // Sem esse fallback, todo envio pelo chat interno (não pela tela de disparo avulso) lançava NPE aqui.
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String actor = authentication != null ? authentication.getName() : "empresa:" + TenantContext.get();
        if (!rateLimiter.tryAcquire("whatsapp-send:" + actor, MAX_MESSAGES_PER_WINDOW, RATE_LIMIT_WINDOW)) {
            throw new TooManyRequestsException("Muitas mensagens enviadas em um curto intervalo. Tente novamente em instantes.");
        }

        Long empresaId = TenantContext.get();
        EmpresaTwilioConfig config = empresaTwilioConfigRepository.findByEmpresaId(empresaId)
                .orElseThrow(() -> new ConflictException("Integração com o WhatsApp (Twilio) não configurada para esta empresa."));
        if (!Boolean.TRUE.equals(config.getEnabled())) {
            throw new ConflictException("Integração com o WhatsApp (Twilio) está desativada para esta empresa.");
        }
        TwilioRestClient client = twilioClientProvider.getClient(config);

        String to = mensagemRequest.getTo();
        String media = mensagemRequest.getMedia();

        if (!to.startsWith("+55")) {
            if (to.startsWith("55")) {
                to = "+" + to;
            } else {
                to = "+55" + to;
            }
        }
        if (to.length() > 12 && to.startsWith("+55") && to.charAt(5) == '9') {
            to = to.substring(0, 5) + to.substring(6);
        }

        if (!to.startsWith("whatsapp:")) {
            to = "whatsapp:" + to;
        }
        String messageBody = mensagemRequest.getMessage() != null && !mensagemRequest.getMessage().isEmpty()
                ? mensagemRequest.getMessage()
                : "";

        log.info("Enviando mensagem WhatsApp. empresaId={}", empresaId);
        if (media != null && !media.isEmpty()) {
            return Message.creator(
                            new com.twilio.type.PhoneNumber(to),
                            new com.twilio.type.PhoneNumber("whatsapp:" + config.getWhatsappNumber()),
                            messageBody
                    )
                    .setMediaUrl(Arrays.asList(URI.create(media)))
                    .create(client);
        } else {
            return Message.creator(
                    new com.twilio.type.PhoneNumber(to),
                    new com.twilio.type.PhoneNumber("whatsapp:" + config.getWhatsappNumber()),
                    mensagemRequest.getMessage()
            ).create(client);
        }
    }

    /**
     * Webhook público da Twilio: o tenant não é conhecido de antemão (sem JWT, sem TenantContext
     * populado pelo SecurityFilter). A empresa é resolvida a partir do número de destino ("To") ANTES
     * de qualquer outra coisa — inclusive antes de validar a assinatura, já que o Auth Token usado na
     * validação é o daquela empresa específica. Só depois de validar a assinatura o processamento
     * (idempotência + persistência + mídia + WebSocket) roda dentro de TenantContext.runAs, mesmo
     * padrão já usado por ChatController/ChatInternoController para requisições autenticadas.
     */
    public void receiveRequest(Map<String, String> params, String requestUrl, String twilioSignature) {
        String to = params.get("To");
        String whatsappNumber = normalizeIncomingWhatsappNumber(to);
        EmpresaTwilioConfig config = empresaTwilioConfigRepository.findByWhatsappNumberIgnoringTenant(whatsappNumber)
                .filter(c -> Boolean.TRUE.equals(c.getEnabled()))
                .orElseThrow(() -> {
                    log.warn("Webhook Twilio recebido para numero nao cadastrado/desativado. to={}", to);
                    return new ResourceNotFoundException("Numero de destino nao reconhecido.");
                });

        if (!isValidTwilioRequest(requestUrl, params, twilioSignature, config.getAuthToken())) {
            throw new AccessDeniedException("Assinatura Twilio inválida.");
        }

        TenantContext.runAs(config.getEmpresaId(), () -> {
            processarMensagemRecebida(params, config);
            return null;
        });
    }

    private void processarMensagemRecebida(Map<String, String> params, EmpresaTwilioConfig config) {
        String messageSid = params.get("MessageSid");
        if (!tentarRegistrarMensagemComoProcessada(messageSid)) {
            log.info("Webhook Twilio ignorado: MessageSid {} já foi processado ou está em processamento.", messageSid);
            return;
        }

        try {
            String from = params.get("From");
            String body = params.get("Body");
            String profileName = params.get("ProfileName");
            String mediaUrl = params.get("MediaUrl0");
            String mediaType = params.get("MediaContentType0");
            if (mediaUrl != null && mediaType != null && mediaType.startsWith("image")) {
                receiveImage(config, from, profileName, mediaUrl, mediaType, body);
            } else if (mediaUrl != null && mediaType != null && mediaType.startsWith("audio")) {
                receiveAudio(config, from, profileName, mediaUrl, mediaType, body);
            } else if (mediaType != null && mediaType.startsWith("application/")) {
                if (mediaType.equals("application/pdf") ||
                        mediaType.equals("application/msword") ||
                        mediaType.equals("application/vnd.openxmlformats-officedocument.wordprocessingml.document")) {

                    receiveDocument(config, from, profileName, mediaUrl, mediaType, body);
                }
            } else {
                receiveMessage(from, profileName, body);
            }
        } catch (RuntimeException e) {
            // Sem isso, uma falha real (ex.: S3 fora do ar) deixaria o MessageSid marcado como
            // processado para sempre, e o reenvio automático do webhook pela Twilio nunca mais
            // conseguiria reprocessar essa mensagem.
            desfazerRegistroDeProcessamento(messageSid);
            throw e;
        }
    }

    private String normalizeIncomingWhatsappNumber(String to) {
        if (to == null) {
            return null;
        }
        return to.startsWith("whatsapp:") ? to.substring("whatsapp:".length()) : to;
    }

    /**
     * Registra o MessageSid como processado ANTES do processamento pesado (download de mídia,
     * upload S3, persistência) em vez de depois: assim, um reenvio do mesmo webhook pela Twilio
     * — comum quando o processamento anterior ainda está rodando e estoura o timeout do lado
     * deles — esbarra na constraint única de whatsapp_webhook_evento.message_sid em vez de
     * reprocessar tudo em paralelo (duplicando mensagem/participante/oportunidade).
     * Retorna false se o SID já estava registrado (processado ou em processamento).
     */
    private boolean tentarRegistrarMensagemComoProcessada(String messageSid) {
        if (messageSid == null || messageSid.isBlank()) {
            return true;
        }
        if (whatsappWebhookEventoRepository.existsByMessageSid(messageSid)) {
            return false;
        }
        try {
            WhatsappWebhookEvento evento = new WhatsappWebhookEvento();
            evento.setMessageSid(messageSid);
            evento.setProcessadoEm(LocalDateTime.now());
            whatsappWebhookEventoRepository.save(evento);
            return true;
        } catch (org.springframework.dao.DataIntegrityViolationException e) {
            log.warn("MessageSid {} já havia sido registrado como processado (corrida concorrente).", messageSid);
            return false;
        }
    }

    private void desfazerRegistroDeProcessamento(String messageSid) {
        if (messageSid == null || messageSid.isBlank()) {
            return;
        }
        whatsappWebhookEventoRepository.deleteByMessageSid(messageSid);
    }

    public void receiveAudio(EmpresaTwilioConfig config, String from, String profileName, String mediaUrl, String mediaType, String body) {
        String celular = reverseWhatsAppNumber(from);
        Participante participante = participanteRepository.findByCelular(celular)
                .orElseGet(() -> criaParticipante(celular, Optional.ofNullable(profileName)));
        Optional<Protocolo> optionalProtocolo = protocoloRepository.findByCelularAndStatus(celular, StatusProtocolo.ABERTO);

        String s3Url;
        try {
            byte[] audioBytes = downloadMediaFromTwilio(mediaUrl, config);

            String extension = mediaType.split("/")[1];
            String fileName = "whatsapp_audio_" + System.currentTimeMillis() + "." + extension;

            MultipartFile multipartFile = createMultipartFile(audioBytes, fileName, mediaType);

            s3Url = s3Service.uploadFile(multipartFile, chatMediaKey("audio", fileName));
        } catch (Exception e) {
            throw new IntegrationException("Falha ao baixar ou enviar áudio recebido via WhatsApp (mediaUrl=" + mediaUrl + ")", e);
        }
        handleMessage(optionalProtocolo, participante, body, s3Url);
    }

    public void receiveMessage(String from, String profileName, String body) {
        String celular = reverseWhatsAppNumber(from);
        Participante participante = participanteRepository.findByCelular(celular)
                .orElseGet(() -> criaParticipante(celular, Optional.ofNullable(profileName)));
        Optional<Protocolo> optionalProtocolo = protocoloRepository.findByCelularAndStatus(celular, StatusProtocolo.ABERTO);
        handleMessage(optionalProtocolo, participante, body, null);
    }

    public void receiveImage(EmpresaTwilioConfig config, String from, String profileName, String mediaUrl, String mediaType, String body) {
        String celular = reverseWhatsAppNumber(from);
        Participante participante = participanteRepository.findByCelular(celular)
                .orElseGet(() -> criaParticipante(celular, Optional.ofNullable(profileName)));
        Optional<Protocolo> optionalProtocolo = protocoloRepository.findByCelularAndStatus(celular, StatusProtocolo.ABERTO);
        String s3Url;
        try {
            byte[] imageBytes = downloadMediaFromTwilio(mediaUrl, config);

            String extension = mediaType.split("/")[1];
            String fileName = "whatsapp_" + System.currentTimeMillis() + "." + extension;

            MultipartFile multipartFile = createMultipartFile(imageBytes, fileName, mediaType);
            s3Url = s3Service.uploadFile(multipartFile, chatMediaKey("imagem", fileName));
        } catch (Exception e) {
            throw new IntegrationException("Falha ao baixar ou enviar imagem recebida via WhatsApp (mediaUrl=" + mediaUrl + ")", e);
        }
        handleMessage(optionalProtocolo, participante, body, s3Url);
    }

    /**
     * Prefixo "chat/{empresaId}/..." para toda mídia recebida via WhatsApp — mesmo bucket S3
     * compartilhado, mas cada empresa só enxerga (e só é cobrada por auditoria/lifecycle de) sua
     * própria pasta. empresaId vem de TenantContext.get(), já setado por TenantContext.runAs em
     * receiveRequest.
     */
    private String chatMediaKey(String tipo, String fileName) {
        return "chat/" + TenantContext.get() + "/" + tipo + "/" + fileName;
    }

    private boolean isValidTwilioRequest(String requestUrl, Map<String, String> params, String twilioSignature, String authToken) {
        if (twilioSignature == null || twilioSignature.isBlank()) {
            return false;
        }
        RequestValidator requestValidator = new RequestValidator(authToken);
        return requestValidator.validate(requestUrl, params, twilioSignature);
    }

    private static final int TWILIO_MEDIA_CONNECT_TIMEOUT_MS = 10_000;
    private static final int TWILIO_MEDIA_READ_TIMEOUT_MS = 30_000;

    // O parâmetro MediaUrl0 vem do corpo do próprio webhook (dado "externo", mesmo que a
    // requisição inteira já tenha sido validada pela assinatura Twilio — ver isValidTwilioRequest)
    // — allowlist de host como defesa em profundidade contra SSRF, para nunca deixar o backend
    // fazer uma requisição de saída para um host arbitrário. api.twilio.com é o único host que a
    // Twilio de fato usa para servir mídia de mensagens recebidas.
    private static final String TWILIO_MEDIA_HOST = "api.twilio.com";

    private byte[] downloadMediaFromTwilio(String mediaUrl, EmpresaTwilioConfig config) throws IOException {
        validarHostMidiaTwilio(mediaUrl);

        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(TWILIO_MEDIA_CONNECT_TIMEOUT_MS);
        requestFactory.setReadTimeout(TWILIO_MEDIA_READ_TIMEOUT_MS);
        // Não segue redirecionamentos automaticamente (comportamento padrão do
        // SimpleClientHttpRequestFactory) — um 3xx apontando para fora do host confiável nunca é
        // seguido silenciosamente, mesmo que a resposta inicial tenha vindo de api.twilio.com.
        RestTemplate restTemplate = new RestTemplate(requestFactory);

        HttpHeaders headers = new HttpHeaders();
        headers.setBasicAuth(config.getAccountSid(), config.getAuthToken());
        HttpEntity<String> entity = new HttpEntity<>(headers);

        ResponseEntity<byte[]> response = restTemplate.exchange(mediaUrl, HttpMethod.GET, entity, byte[].class);

        return response.getBody();
    }

    private void validarHostMidiaTwilio(String mediaUrl) {
        URI uri;
        try {
            uri = URI.create(mediaUrl);
        } catch (IllegalArgumentException e) {
            throw new IntegrationException("URL de mídia do WhatsApp inválida.", e);
        }
        String host = uri.getHost();
        if (!"https".equalsIgnoreCase(uri.getScheme()) || host == null
                || !(host.equalsIgnoreCase(TWILIO_MEDIA_HOST) || host.toLowerCase().endsWith("." + TWILIO_MEDIA_HOST))) {
            log.warn("Download de mídia do WhatsApp bloqueado: host fora da allowlist. mediaUrl={}", mediaUrl);
            throw new IntegrationException("Origem de mídia do WhatsApp não permitida.");
        }
    }

    private MultipartFile createMultipartFile(byte[] content, String fileName, String contentType) {
        return new ByteArrayMultipartFile(content, fileName, contentType);
    }


    private void handleMessage(Optional<Protocolo> optionalProtocolo, Participante participante, String body, String media) {
        String empresaId = String.valueOf(TenantContext.get());
        if (optionalProtocolo.isPresent()) {
            Protocolo protocolo = optionalProtocolo.get();
            List<Mensagem> savedMessage = mensagemService.sendMessage(protocolo, participante, body, media);
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
            List<Mensagem> savedMessages = mensagemService.sendMessagePublico(participante, body, media);
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


    public static String reverseWhatsAppNumber(String to) {
        if (to.startsWith("whatsapp:")) {
            to = to.substring("whatsapp:".length());
        }

        if (to.startsWith("+55")) {
            to = to.substring(3);
        }

        if (to.length() > 2) {
            to = to.substring(0, 2) + '9' + to.substring(2);
        }

        return to;
    }


    public void receiveDocument(EmpresaTwilioConfig config, String from, String profileName, String mediaUrl, String mediaType, String body) {
        String celular = reverseWhatsAppNumber(from);
        Participante participante = participanteRepository.findByCelular(celular)
                .orElseGet(() -> criaParticipante(celular, Optional.ofNullable(profileName)));
        Optional<Protocolo> optionalProtocolo = protocoloRepository.findByCelularAndStatus(celular, StatusProtocolo.ABERTO);
        String s3Url;
        try {
            byte[] documentBytes = downloadMediaFromTwilio(mediaUrl, config);
            String extension = mediaType.split("/")[1];
            String fileName = "whatsapp_document_" + System.currentTimeMillis() + "." + extension;
            MultipartFile multipartFile = createMultipartFile(documentBytes, fileName, mediaType);
            s3Url = s3Service.uploadFile(multipartFile, chatMediaKey("documentos", fileName));
        } catch (Exception e) {
            throw new IntegrationException("Falha ao baixar ou enviar documento recebido via WhatsApp (mediaUrl=" + mediaUrl + ")", e);
        }
        handleMessage(optionalProtocolo, participante, body, s3Url);
    }

    public Oportunidade criaOportunidade(Participante cliente) {
        Oportunidade newOportunidadae = new Oportunidade();
        newOportunidadae.setTitulo(null);
        newOportunidadae.setEtapa(null);
        newOportunidadae.setCliente(cliente);
        newOportunidadae.setValor(BigDecimal.ZERO);
        newOportunidadae.setData_criacao(LocalDateTime.now());
        newOportunidadae.setOrigem(null);
        newOportunidadae.setInteresse(null);
        newOportunidadae.setUrl_anexo(null);
        newOportunidadae.setIndice(0);
        messagingTemplate.convertAndSend("/topic/empresa/" + TenantContext.get() + "/newoportunidadectt", newOportunidadae);
        return oportunidadeRepository.save(newOportunidadae);
    }

}
