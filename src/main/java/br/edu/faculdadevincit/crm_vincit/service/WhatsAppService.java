package br.edu.faculdadevincit.crm_vincit.service;

import br.edu.faculdadevincit.crm_vincit.model.*;
import br.edu.faculdadevincit.crm_vincit.model.dtos.MensagemResponseDTO;
import br.edu.faculdadevincit.crm_vincit.model.dtos.UsuarioContatoDto;
import br.edu.faculdadevincit.crm_vincit.model.enums.StatusProtocolo;
import br.edu.faculdadevincit.crm_vincit.model.enums.TipoParticipante;
import br.edu.faculdadevincit.crm_vincit.repository.OportunidadeRepository;
import br.edu.faculdadevincit.crm_vincit.repository.ParticipanteRepository;
import br.edu.faculdadevincit.crm_vincit.repository.ProtocoloRepository;
import br.edu.faculdadevincit.crm_vincit.repository.WhatsappWebhookEventoRepository;
import br.edu.faculdadevincit.crm_vincit.service.exceptions.AccessDeniedException;
import br.edu.faculdadevincit.crm_vincit.service.exceptions.IntegrationException;
import com.twilio.Twilio;
import com.twilio.rest.api.v2010.account.Message;
import com.twilio.security.RequestValidator;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.messaging.simp.SimpMessagingTemplate;
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

    @Value("${twilio.account.sid}")
    private String accountSid;

    @Value("${twilio.auth.token}")
    private String authToken;

    @Value("${twilio.whatsapp.number}")
    private String twilioNumber;


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
    private CloudFrontService cloudFrontService;

    @Autowired
    private WhatsappWebhookEventoRepository whatsappWebhookEventoRepository;

    String media;

    public Message sendWhatsAppMessage(MensagemRequest mensagemRequest) {
        Twilio.init(accountSid, authToken);

        String to = mensagemRequest.getTo();
        media = mensagemRequest.getMedia();

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
        if (media != null && !media.isEmpty()) {
            Message message = Message.creator(
                            new com.twilio.type.PhoneNumber(to),
                            new com.twilio.type.PhoneNumber("whatsapp:" + twilioNumber),
                            messageBody
                    )
                    .setMediaUrl(Arrays.asList(URI.create(cloudFrontService.generateSignedUrl(media, Duration.ofMinutes(60)))))
                    .create();
            return message;
        } else {
            return Message.creator(
                    new com.twilio.type.PhoneNumber(to),
                    new com.twilio.type.PhoneNumber("whatsapp:" + twilioNumber),
                    mensagemRequest.getMessage()
            ).create();
        }
    }

    public void receiveRequest(Map<String, String> params, String requestUrl, String twilioSignature){
        if (!isValidTwilioRequest(requestUrl, params, twilioSignature)) {
            throw new AccessDeniedException("Assinatura Twilio inválida.");
        }

        String messageSid = params.get("MessageSid");
        if (isMensagemJaProcessada(messageSid)) {
            log.info("Webhook Twilio ignorado: MessageSid {} já foi processado anteriormente.", messageSid);
            return;
        }

        String from = params.get("From");
        String body = params.get("Body");
        String profileName = params.get("ProfileName");
        String mediaUrl = params.get("MediaUrl0");
        String mediaType = params.get("MediaContentType0");
        if (mediaUrl != null && mediaType != null && mediaType.startsWith("image")) {
            receiveImage(from,profileName, mediaUrl, mediaType,body);
        }
        else if (mediaUrl != null && mediaType != null && mediaType.startsWith("audio")) {
            receiveAudio(from,profileName, mediaUrl, mediaType,body);
        }else if (mediaType!= null && mediaType.startsWith("application/")) {
            if (mediaType.equals("application/pdf") ||
                    mediaType.equals("application/msword") ||
                    mediaType.equals("application/vnd.openxmlformats-officedocument.wordprocessingml.document")) {

                receiveDocument(from,profileName, mediaUrl, mediaType,body);
            }
        } else {
            receiveMessage(from,profileName, body);
        }

        registrarMensagemProcessada(messageSid);
    }

    private boolean isMensagemJaProcessada(String messageSid) {
        if (messageSid == null || messageSid.isBlank()) {
            return false;
        }
        return whatsappWebhookEventoRepository.existsByMessageSid(messageSid);
    }

    private void registrarMensagemProcessada(String messageSid) {
        if (messageSid == null || messageSid.isBlank()) {
            return;
        }
        try {
            WhatsappWebhookEvento evento = new WhatsappWebhookEvento();
            evento.setMessageSid(messageSid);
            evento.setProcessadoEm(LocalDateTime.now());
            whatsappWebhookEventoRepository.save(evento);
        } catch (org.springframework.dao.DataIntegrityViolationException e) {
            log.warn("MessageSid {} já havia sido registrado como processado (corrida concorrente).", messageSid);
        }
    }
    public void receiveAudio(String from,String profileName, String mediaUrl, String mediaType ,String body)  {
        String celular = reverseWhatsAppNumber(from);
        Participante participante = participanteRepository.findByCelular(celular)
                .orElseGet(() -> criaParticipante(celular,Optional.ofNullable(profileName)));
        Optional<Protocolo> optionalProtocolo = protocoloRepository.findByCelularAndStatus(celular, StatusProtocolo.ABERTO);

        String s3Url;
        try {
            byte[] audioBytes = downloadMediaFromTwilio(mediaUrl);

            String extension = mediaType.split("/")[1];
            String fileName = "whatsapp_audio_" + System.currentTimeMillis() + "." + extension;

            MultipartFile multipartFile = createMultipartFile(audioBytes, fileName, mediaType);

            s3Url = s3Service.uploadFile(multipartFile, "audio/" + fileName);
        } catch (Exception e) {
            throw new IntegrationException("Falha ao baixar ou enviar áudio recebido via WhatsApp (mediaUrl=" + mediaUrl + ")", e);
        }
        handleMessage(optionalProtocolo, participante,body, s3Url);
    }



    public void receiveMessage(String from,String profileName, String body) {
        String celular = reverseWhatsAppNumber(from);
        Participante participante = participanteRepository.findByCelular(celular)
                .orElseGet(() -> criaParticipante(celular, Optional.ofNullable(profileName)));
        Optional<Protocolo> optionalProtocolo = protocoloRepository.findByCelularAndStatus(celular, StatusProtocolo.ABERTO);
        handleMessage(optionalProtocolo, participante, body,null);
    }
    public void receiveImage(String from,String profileName, String mediaUrl,String mediaType,String body) {
        String celular = reverseWhatsAppNumber(from);
        Participante participante = participanteRepository.findByCelular(celular)
                .orElseGet(() -> criaParticipante(celular,Optional.ofNullable(profileName)));
        Optional<Protocolo> optionalProtocolo = protocoloRepository.findByCelularAndStatus(celular, StatusProtocolo.ABERTO);
        String s3Url;
        try {
            byte[] imageBytes = downloadMediaFromTwilio(mediaUrl);

            String extension = mediaType.split("/")[1];
            String fileName = "whatsapp_" + System.currentTimeMillis() + "." + extension;

            MultipartFile multipartFile = createMultipartFile(imageBytes, fileName, mediaType);
            s3Url = s3Service.uploadFile(multipartFile, "imagem/" + fileName);
        } catch (Exception e) {
            throw new IntegrationException("Falha ao baixar ou enviar imagem recebida via WhatsApp (mediaUrl=" + mediaUrl + ")", e);
        }
        handleMessage(optionalProtocolo, participante,body, s3Url);
    }
    private boolean isValidTwilioRequest(String requestUrl, Map<String, String> params, String twilioSignature) {
        if (twilioSignature == null || twilioSignature.isBlank()) {
            return false;
        }
        RequestValidator requestValidator = new RequestValidator(authToken);
        return requestValidator.validate(requestUrl, params, twilioSignature);
    }

    private static final int TWILIO_MEDIA_CONNECT_TIMEOUT_MS = 10_000;
    private static final int TWILIO_MEDIA_READ_TIMEOUT_MS = 30_000;

    private byte[] downloadMediaFromTwilio(String mediaUrl) throws IOException {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(TWILIO_MEDIA_CONNECT_TIMEOUT_MS);
        requestFactory.setReadTimeout(TWILIO_MEDIA_READ_TIMEOUT_MS);
        RestTemplate restTemplate = new RestTemplate(requestFactory);

        HttpHeaders headers = new HttpHeaders();
        headers.setBasicAuth(accountSid, authToken);
        HttpEntity<String> entity = new HttpEntity<>(headers);

        ResponseEntity<byte[]> response = restTemplate.exchange(mediaUrl, HttpMethod.GET, entity, byte[].class);

        return response.getBody();
    }
    private MultipartFile createMultipartFile(byte[] content, String fileName, String contentType) {
        return new ByteArrayMultipartFile(content, fileName, contentType);
    }


    private void handleMessage(Optional<Protocolo> optionalProtocolo, Participante participante, String body,String media) {
        if (optionalProtocolo.isPresent()) {
            Protocolo protocolo = optionalProtocolo.get();
            List<Mensagem> savedMessage = mensagemService.sendMessage(protocolo, participante, body,media);
            savedMessage.forEach(mensagemNew ->
                    publicarNoWebSocket("/topic/messages/" + protocolo.getId(), new MensagemResponseDTO(mensagemNew)));
        } else {
            Mensagem commun = mensagemService.sendMessagePublico(participante, body);
            publicarNoWebSocket("/topic/messages/public", new MensagemResponseDTO(commun));
        }
    }

    private void publicarNoWebSocket(String destino, Object payload) {
        try {
            messagingTemplate.convertAndSend(destino, payload);
        } catch (Exception e) {
            log.error("Falha ao publicar mensagem já persistida no WebSocket. destino={}", destino, e);
        }
    }


    public Participante criaParticipante(String from,Optional<String> profilename) {
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
        UsuarioContatoDto contatoDto = new UsuarioContatoDto(participante.getId(), participante.getNome(), participante.getUrlPicture());
        messagingTemplate.convertAndSend("/topic/usuarios", contatoDto);
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


    public void receiveDocument(String from,String profileName, String mediaUrl, String mediaType,String body)  {
        String celular = reverseWhatsAppNumber(from);
        Participante participante = participanteRepository.findByCelular(celular)
                .orElseGet(() -> criaParticipante(celular,Optional.ofNullable(profileName)));
        Optional<Protocolo> optionalProtocolo = protocoloRepository.findByCelularAndStatus(celular, StatusProtocolo.ABERTO);
        String s3Url;
        try {
            byte[] documentBytes = downloadMediaFromTwilio(mediaUrl);
            String extension = mediaType.split("/")[1];
            String fileName = "whatsapp_document_" + System.currentTimeMillis() + "." + extension;
            MultipartFile multipartFile = createMultipartFile(documentBytes, fileName, mediaType);
            s3Url = s3Service.uploadFile(multipartFile, "documentos/" + fileName);
        } catch (Exception e) {
            throw new IntegrationException("Falha ao baixar ou enviar documento recebido via WhatsApp (mediaUrl=" + mediaUrl + ")", e);
        }
        handleMessage(optionalProtocolo, participante,body, s3Url);
    }

    public Oportunidade criaOportunidade(Participante cliente){
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
        messagingTemplate.convertAndSend("/topic/newoportunidadectt", newOportunidadae);
        return oportunidadeRepository.save(newOportunidadae);
    }

}
