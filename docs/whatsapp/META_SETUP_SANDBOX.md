# Sandbox — testando a integração localmente

A Meta **não tem** um ambiente de "sandbox" separado como a Twilio tinha (aquele número
`+14155238886` genérico). Em vez disso, todo Meta App com o produto WhatsApp habilitado já vem com
um **número de teste gratuito** e uma **WABA de teste**, prontos para uso assim que você cria o App
— é isso que vamos usar aqui. Nenhum passo deste guia exige Business Verification nem App Review;
tudo funciona com o App em modo **Development**.

Pré-requisito: já ter feito os passos 1-3 do `META_SETUP.md` (criar o App, adicionar o produto
WhatsApp). Não precisa dos demais passos (Tech Provider, App Review, Live) só para testar.

## 1. Pegar o número de teste e o token temporário

- **Onde**: painel do App → **WhatsApp → Introdução/API Setup** (o nome exato da tela muda um
  pouco entre idiomas/versões, procure por "Guia de início rápido" ou "Send messages").
- Você verá:
  - **`Phone number ID`** — um número de teste já provisionado pela Meta (algo como `+1 555 xxx xxxx`).
  - **`WhatsApp Business Account ID`** (o `waba_id` de teste).
  - Um **Access Token temporário** (validade de ~24h) — gerado ali mesmo, sem precisar de OAuth.
- Nessa mesma tela, em **"To"**, cadastre até **5 números de destino de teste** — geralmente o seu
  próprio WhatsApp pessoal. A Meta manda um código por SMS/WhatsApp para confirmar que aquele
  número concorda em receber mensagens de teste.

Anote os três valores (`phone_number_id`, token temporário, seu número de teste cadastrado) — vamos
usar nos próximos passos.

## 2. Enviar uma mensagem de teste via curl (sem precisar do backend rodando)

Confirma que as credenciais básicas funcionam antes de conectar ao CRM:

```bash
curl -X POST "https://graph.facebook.com/v23.0/<PHONE_NUMBER_ID>/messages" \
  -H "Authorization: Bearer <ACCESS_TOKEN_TEMPORARIO>" \
  -H "Content-Type: application/json" \
  -d '{
        "messaging_product": "whatsapp",
        "to": "<SEU_NUMERO_DE_TESTE_COM_DDI>",
        "type": "text",
        "text": { "body": "Teste do sandbox Meta" }
      }'
```

Se a mensagem chegar no seu WhatsApp, as credenciais estão certas.

## 3. Expor o backend local publicamente (necessário para o webhook)

A Meta só entrega webhooks para uma URL **pública HTTPS** — `localhost` não funciona. Use um túnel.
Este projeto já usa VS Code Dev Tunnels em desenvolvimento (ver `front-crm/.env`,
`VITE_API_URL=https://p4gd39l7-8080.brs.devtunnels.ms`) — reaproveite o mesmo mecanismo para o
backend, ou use `ngrok` como alternativa:

```bash
# Opção ngrok (se preferir):
ngrok http 8080
```

Copie a URL pública gerada (ex.: `https://abc123.ngrok-free.app` ou sua URL de devtunnel) — a
URL do webhook será `https://SUA_URL_PUBLICA/whatsapp/webhook`.

## 4. Configurar o webhook de teste no painel

- **Onde**: painel do App → WhatsApp → Configuration → "Webhook".
- **Callback URL**: `https://SUA_URL_PUBLICA/whatsapp/webhook`.
- **Verify Token**: qualquer string que você escolher — precisa ser **idêntica** ao
  `META_VERIFY_TOKEN` configurado no seu `application-dev.properties`/`.env` local (ver
  `meta.whatsapp.verify-token` já com um placeholder de dev nesse arquivo).
- Clique em "Verify and Save". A Meta chama `GET /whatsapp/webhook?hub.mode=subscribe&...` na sua
  URL pública — se o backend estiver rodando e o token bater, ela aceita. Se falhar, confirme:
  1. O backend local está rodando e acessível pela URL pública (teste `curl https://SUA_URL_PUBLICA/actuator/health`).
  2. `META_VERIFY_TOKEN` no backend é **exatamente** igual ao campo preenchido no painel.
- Em "Webhook fields", assine **`messages`**.

## 5. Testar o recebimento de mensagem de verdade

Com o webhook configurado, mande uma mensagem do **seu WhatsApp pessoal** (o número que você
cadastrou como destino de teste no passo 1) para o **número de teste** da Meta. Você deve ver:

1. Log no backend: `WhatsAppService` processando o evento.
2. Se já existir uma `WhatsAppIntegration` local apontando para esse `phone_number_id` (ver passo
   6 abaixo), a mensagem aparece no chat do CRM em tempo real.

## 6. Atalho: testar o backend sem passar pelo Embedded Signup

O fluxo completo de Embedded Signup (tela Configurações → Conectar WhatsApp) exige que seu
usuário FB tenha um papel no App (Admin/Developer/Tester) enquanto o App estiver em modo
Development — ver seção 8. Para testar **só o backend** (envio/recebimento/webhook) mais rápido,
você pode inserir a integração manualmente no banco local, usando os dados do passo 1:

```sql
INSERT INTO whatsapp_integration
  (empresa_id, waba_id, phone_number_id, access_token_encrypted, status, connected_at, criado_em, atualizado_em)
VALUES
  (1, '<WABA_ID_DE_TESTE>', '<PHONE_NUMBER_ID_DE_TESTE>', NULL, 'CONNECTED', NOW(), NOW(), NOW());
```

`access_token_encrypted` precisa estar **cifrado** pelo mesmo `EncryptedStringConverter` (não dá
para colar o token em texto puro direto no banco). Duas formas de resolver:

- **Mais simples**: rode a aplicação, chame manualmente o service em um teste/`CommandLineRunner`
  temporário que grave `whatsAppIntegrationRepository.save(...)` com `setAccessToken(token)` — o
  converter cifra automaticamente ao persistir via JPA.
- **Alternativa**: implemente o fluxo de Embedded Signup de ponta a ponta (passo 8) — mais
  trabalhoso na primeira vez, mas é o caminho real de produção.

Lembre-se de trocar o access_token temporário (expira em ~24h) por um novo sempre que for testar
de novo, se optar pelo atalho manual.

## 7. Testar o envio pelo CRM

Com a integração conectada (via atalho do passo 6 ou via Embedded Signup completo), abra o chat do
CRM, envie uma mensagem para o contato correspondente ao seu número de teste, e confirme que ela
chega no seu WhatsApp pessoal.

## 8. Testar o Embedded Signup em modo Development

Para testar o fluxo real de conexão (tela Configurações → "Conectar WhatsApp") sem sair do modo
Development:

- **Onde**: painel do App → "Funções do App" (App Roles) → adicione o(s) usuário(s) Facebook que
  vão testar como **Administrador**, **Desenvolvedor** ou **Testador**.
- Só contas com um desses papéis conseguem completar o `FB.login()`/Embedded Signup enquanto o App
  não está em modo Live.
- Você também precisa ter criado uma **Configuration** de Embedded Signup (`META_SETUP.md`, item
  7) e preenchido `META_CONFIG_ID` no backend — sem isso, `FB.login({config_id: ...})` falha.
- Nesse modo, o Embedded Signup ainda cria uma WABA de verdade (não é "fake") — mas associada à
  sua conta de teste.

## Resumo do que preencher no `application-dev.properties` local

```properties
meta.whatsapp.app-id=<APP_ID do seu Meta App de teste>
meta.whatsapp.app-secret=<APP_SECRET do seu Meta App de teste>
meta.whatsapp.config-id=<CONFIG_ID do Embedded Signup, se for testar o passo 8>
meta.whatsapp.verify-token=<qualquer string, igual ao cadastrado no painel>
```

## Limitações do modo de teste

- Só os até 5 números cadastrados como destino de teste recebem mensagens do número de teste.
- O access token temporário do passo 1 expira em ~24h — gere um novo quando precisar.
- O número de teste não pode ser usado como número real de uma empresa em produção — cada empresa
  real conecta o **próprio** número via Embedded Signup completo (`EMBEDDED_SIGNUP.md`), depois do
  App estar em modo Live (`META_SETUP.md`, item 15).
