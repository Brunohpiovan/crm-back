# Webhooks da Meta WhatsApp Cloud API

## Verificação (`GET /whatsapp/webhook`)

Chamado **uma vez** pela Meta ao salvar a URL de callback no painel do App (ver `META_SETUP.md`,
item 9-10) — este endpoint é global do App do SaaS, não por empresa (cada empresa conecta sua
própria WABA depois, via `EMBEDDED_SIGNUP.md`). Query params enviados pela Meta:

```
GET /whatsapp/webhook?hub.mode=subscribe&hub.verify_token=SEU_TOKEN&hub.challenge=123456
```

`WhatsAppController.verifyWebhook` → `WhatsAppService.verifyWebhook`: se `hub.mode == "subscribe"`
e `hub.verify_token` bate (comparação em tempo constante) com `META_VERIFY_TOKEN`, responde `200`
com o corpo `hub.challenge` (texto puro). Qualquer outra combinação lança `MetaWebhookException`
(→ `403`).

## Recebimento de eventos (`POST /whatsapp/webhook`)

Sempre `200 OK` rapidamente (o processamento roda de forma síncrona mas leve — ver
`ARCHITECTURE.md` para o racional de manter síncrono neste projeto). Header obrigatório:
`X-Hub-Signature-256: sha256=<hex hmac>`.

### Exemplo — mensagem de texto recebida

```json
{
  "object": "whatsapp_business_account",
  "entry": [{
    "id": "<waba-id>",
    "changes": [{
      "field": "messages",
      "value": {
        "messaging_product": "whatsapp",
        "metadata": { "display_phone_number": "5511999999999", "phone_number_id": "<phone-number-id>" },
        "contacts": [{ "profile": { "name": "Nome do Cliente" }, "wa_id": "5511987654321" }],
        "messages": [{
          "from": "5511987654321",
          "id": "wamid.HBg...",
          "timestamp": "1715600000",
          "type": "text",
          "text": { "body": "Olá, preciso de ajuda" }
        }]
      }
    }]
  }]
}
```

### Exemplo — mídia recebida (imagem)

```json
{
  "messages": [{
    "from": "5511987654321",
    "id": "wamid.HBg...",
    "timestamp": "1715600001",
    "type": "image",
    "image": { "id": "<media-id>", "mime_type": "image/jpeg", "sha256": "...", "caption": "Olha isso" }
  }]
}
```

O campo `image.id` **não** é uma URL — é preciso um segundo request autenticado
(`GET /{media-id}`) para obter uma URL temporária, e um terceiro para baixar o binário
(`MetaWhatsAppClient.fetchMediaUrl` + `downloadMedia`). Tipos suportados hoje: `image`, `audio`,
`document`, `video`.

### Exemplo — evento de status

```json
{
  "statuses": [{
    "id": "wamid.HBg...",
    "status": "delivered",
    "timestamp": "1715600010",
    "recipient_id": "5511987654321"
  }]
}
```

`status` pode ser `sent`, `delivered`, `read` ou `failed` (com `errors[]` adicional em caso de
falha). `MetaWebhookMapper` traduz para o enum interno `MessageStatus`.

## Validação de assinatura

`MetaWebhookSignatureValidator.isValid(rawBody, signatureHeader, appSecret)`:

1. Extrai o hex após `sha256=`.
2. Calcula `HMAC-SHA256(appSecret, rawBody)` sobre o **corpo cru** da requisição (por isso o
   controller recebe `@RequestBody String rawBody`, não um DTO já desserializado — qualquer
   diferença de formatação no JSON quebraria a assinatura).
3. Compara em tempo constante (`MessageDigest.isEqual`), nunca com `String.equals`.
4. `App Secret` nunca é logado, em nenhum ponto do código.

Validado **antes** de qualquer parsing/resolução de tenant — ver `ARCHITECTURE.md` para o porquê
disso ser mais simples/seguro que o modelo antigo da Twilio.

## Resolução de tenant

Depois de validada a assinatura, `MetaWebhookMapper` extrai `phone_number_id` de
`value.metadata.phone_number_id`. `WhatsAppIntegrationRepository.findByPhoneNumberIdIgnoringTenant`
(query nativa, ignora o filtro `@TenantId`) resolve para qual `Empresa` esse número pertence.
Eventos para um `phone_number_id` não cadastrado (ou com integração não `CONNECTED`) são
descartados silenciosamente (log em nível `WARN`), sem erro — a Meta pode reenviar webhooks de
números que já foram desconectados.

## Idempotência

Cada mensagem recebida tem um `id` único (wamid). Antes de processar, o backend tenta inserir esse
id em `whatsapp_webhook_evento.external_message_id` (constraint `UNIQUE`). Se a inserção falhar
(id já existe) ou já existir via `existsByExternalMessageId`, o evento é ignorado — protege contra
reenvios da Meta (comuns quando o processamento anterior demora) sem duplicar
mensagem/participante/oportunidade. Se o processamento falhar **depois** de registrado (ex.: S3
fora do ar), o registro é desfeito para permitir reprocessamento em um reenvio futuro.
