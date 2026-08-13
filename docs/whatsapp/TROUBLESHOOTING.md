# Troubleshooting e limitações conhecidas

## Números de celular com formato herdado do fluxo Twilio

O fluxo Twilio (sandbox) normalizava números removendo o 9º dígito do celular brasileiro antes de
enviar, e o reconstruía manualmente ao receber (`reverseWhatsAppNumber`). A Meta Cloud API entrega
o número completo (com o 9º dígito) tanto no envio quanto no recebimento — o código atual
(`WhatsAppService.normalizeToE164Digits` / `normalizeCelularFromMeta`) **não** remove/reinsere mais
esse dígito.

**Impacto real**: `Participante.celular` cadastrados **antes** desta migração podem estar salvos no
formato antigo (sem o 9º dígito). Para esses contatos:
- Envio (`sendWhatsAppMessage`) vai gerar um número sem o 9, que a Meta pode rejeitar ou entregar
  incorretamente.
- Um novo evento recebido do mesmo cliente vai gerar um `Participante` **duplicado** (celular com
  o 9, não encontra o registro antigo por `findByCelular`).

**Não existe uma migração de dados automática para isso neste PR** — normalizar o histórico de
`participante.celular` exige decidir regras de negócio (nem todo número de 8 dígitos é "celular
sem o 9"; alguns são fixos) e acesso aos dados reais de produção, que não estavam disponíveis nesta
migração. Antes de ir para produção, recomenda-se:
1. Auditar quantos `participante.celular` têm 10 dígitos (DDD + 8) vs. 11 dígitos (DDD + 9).
2. Decidir se vale a pena um script de backfill (ex.: inserir "9" após o DDD nos que têm 10
   dígitos) ou se é aceitável que contatos antigos gerem um novo cadastro na primeira mensagem
   pós-migração.

## Templates de mensagem — só backend, sem UI própria

`WhatsAppService.sendTemplateMessage` / `POST /whatsapp/send-template` já enviam um template
aprovado via Graph API. **Não foi construída uma tela de gestão/composição de templates** no
React: o produto nunca teve esse conceito antes (a única tela "Templates" existente é de
**e-mail**, sem relação), e criar um construtor de templates WhatsApp do zero — com preview,
variáveis, aprovação — é uma feature de produto nova, não uma tradução direta de algo que existia
na Twilio (que também nunca teve templates implementados neste projeto). Fica registrado como
gap conhecido para uma decisão de produto: os templates em si são criados/aprovados no Business
Manager da Meta; a tela do CRM só precisaria listar os nomes já aprovados e permitir escolher
qual disparar.

## Mídia enviada por nós ainda depende de URL pública

O envio de mídia (`MetaWhatsAppClient.sendMedia`) usa o parâmetro `link` da Graph API (URL pública
já hospedada no S3), no mesmo padrão do fluxo Twilio anterior — não foi implementado upload direto
de binário (`multipart/form-data` para `/{phone-number-id}/media` seguido de envio por `media_id`).
Funciona porque o CRM já hospeda toda mídia do chat em S3 antes de enviar, mas significa que a URL
precisa estar publicamente acessível no momento em que a Meta busca o conteúdo.

## Sem circuit breaker

Assim como no fluxo Twilio anterior, não há circuit breaker nas chamadas à Graph API — só retry
(`@Retryable`, só em falhas de rede/timeout) e timeout configurável. Uma indisponibilidade
prolongada da Meta degrada, mas não derruba, o restante do sistema (erros viram `502` via
`MetaIntegrationException`).

## App em modo Development bloqueia clientes reais

Se o Embedded Signup falhar silenciosamente ou nunca disparar o evento `FINISH`, confirme
primeiro se o Meta App está em modo **Live** (não `Development`) e se o usuário logado no fluxo
tem um papel válido no App caso ainda esteja em desenvolvimento — ver `META_SETUP.md`, item 15.

## Erros comuns da Graph API mapeados

| Situação | Exceção interna | HTTP retornado |
|---|---|---|
| Token inválido/expirado | `MetaAuthenticationException` | 401 |
| Rate limit da Meta | `MetaRateLimitException` | 429 |
| Falha ao enviar (número inválido, etc.) | `MetaMessageException` | 502 |
| Template inexistente/parâmetros incompatíveis | `MetaTemplateException` | 400 |
| Assinatura/verify token inválido no webhook | `MetaWebhookException` | 403 |
| Timeout/5xx da Graph API | `MetaIntegrationException` | 502 |

Nenhum desses handlers expõe o payload cru retornado pela Meta ao frontend — só uma mensagem de
negócio padronizada (ver `ResourceExceptionHandler`).
