# Setup manual no painel da Meta

Este roteiro cobre exatamente o que precisa ser feito **manualmente** no
[Meta for Developers](https://developers.facebook.com/) e no
[Business Manager](https://business.facebook.com/) para que a integração implementada neste
projeto funcione de ponta a ponta. Sempre confirme os passos contra a documentação oficial atual
antes de reproduzir — telas e nomes de campos da Meta mudam com frequência.

Cada linha abaixo segue o formato: **onde configurar → o que colocar → de onde obter → onde usar no código**.

> **Só quer testar localmente?** Não é preciso passar por todos os passos abaixo (Tech Provider,
> Business Verification, App Review, modo Live) para testar envio/recebimento/webhook — veja
> `docs/whatsapp/META_SETUP_SANDBOX.md` para o caminho rápido usando o número de teste gratuito que
> toda WhatsApp Business App já vem com.

## 1. Meta Developer App

- **Onde**: [developers.facebook.com](https://developers.facebook.com/) → "My Apps" → "Create App" → tipo "Business".
- **O que**: crie um único App para o SaaS (não um App por empresa-cliente).
- **De onde obter**: o próprio processo de criação gera o **App ID** e o **App Secret** (em
  Configurações → Básico).
- **Onde usar**: `META_APP_ID` e `META_APP_SECRET` (`application.properties` /
  `meta.whatsapp.app-id` / `meta.whatsapp.app-secret`). O App Secret nunca vai para o frontend.

## 2. Business Portfolio (Gerenciador de Negócios)

- **Onde**: [business.facebook.com](https://business.facebook.com/) → crie ou use um Business
  Portfolio do seu SaaS.
- **O que**: este é o portfólio que atua como **Tech Provider** — ele é quem gerencia, em nome dos
  clientes, as WABAs criadas via Embedded Signup.
- **Onde usar**: não entra diretamente em nenhuma variável de ambiente; é o contexto onde o App e o
  fluxo de Embedded Signup são configurados.

## 3. Produto "WhatsApp" no App

- **Onde**: no painel do App → "Add Product" → **WhatsApp**.
- **O que**: isso habilita as APIs de WhatsApp Business (Cloud API) para o App.

## 4. Tech Provider / Solution Partner

- **Onde**: Business Manager → configurações do Portfolio → "Tech Provider" (ou "Solution
  Partner", dependendo da nomenclatura vigente na documentação da Meta no momento do setup).
- **O que**: é essa qualificação que permite ao App do SaaS gerenciar WABAs de **outras** empresas
  (Embedded Signup em nome de terceiros), em vez de só a própria conta.
- **Confirme na documentação oficial atual** os requisitos vigentes (pode envolver aceite de termos
  específicos e/ou pré-requisitos de Business Verification).

## 5. Business Verification

- **Onde**: Business Manager → "Segurança do Negócio" → "Verificação do Negócio".
- **O que**: verificação formal do negócio dono do App — normalmente exigida antes de ir para
  produção com Advanced Access.

## 6. App Review / Advanced Access

- **Onde**: painel do App → "App Review" → "Permissions and Features".
- **O que**: solicite (e passe por review) as permissões necessárias para operar como Tech
  Provider gerenciando WABAs de terceiros — tipicamente `whatsapp_business_management` e
  `whatsapp_business_messaging`, em nível **Advanced Access** (Standard Access só funciona com
  usuários de teste, não com clientes reais). Confirme a lista exata e o processo de review na
  documentação oficial atual — critérios de aprovação mudam com frequência.

## 7. Embedded Signup — Configuration

- **Onde**: painel do App → WhatsApp → "Embedded Signup" (ou "Configuration") → criar uma nova
  configuração.
- **O que**: defina o fluxo de onboarding (quais campos/telas aparecem para o cliente durante o
  Embedded Signup).
- **De onde obter**: a criação gera um **Configuration ID**.
- **Onde usar**: `META_CONFIG_ID` (`meta.whatsapp.config-id`) — é o `config_id` passado para
  `FB.login()` no frontend (`src/lib/metaEmbeddedSignup.ts`).

## 8. Domains (App Domains / Valid OAuth Redirect URIs)

- **Onde**: painel do App → Configurações → Básico → "App Domains"; e em Facebook Login →
  Configurações → "Valid OAuth Redirect URIs" (se o fluxo usado exigir redirect — o Embedded
  Signup padrão via `FB.login()` roda em popup/iframe e normalmente não precisa de redirect URI
  próprio, mas confirme a variante atual da documentação).
- **O que**: o domínio onde o `front-crm` roda em produção (ex.: `app.seudominio.com`).

## 9. Webhook callback URL

- **Onde**: painel do App → WhatsApp → Configuration → "Webhook".
- **O que**: URL pública do backend: `https://SEU_BACKEND/whatsapp/webhook`.
- **Onde usar**: não é uma variável de ambiente — é o endpoint já implementado em
  `WhatsAppController` (`GET` para verificação, `POST` para eventos).

## 10. Verify Token

- **Onde**: mesmo formulário do passo 9 — campo "Verify Token".
- **O que**: uma string arbitrária que **você escolhe** (não vem da Meta).
- **De onde obter**: gere você mesmo (ex.: `openssl rand -hex 32`).
- **Onde usar**: `META_VERIFY_TOKEN` (`meta.whatsapp.verify-token`) — precisa ser **exatamente** o
  mesmo valor cadastrado aqui e na variável de ambiente do backend. `WhatsAppService.verifyWebhook`
  compara os dois.

## 11. Subscriptions (Webhook Fields)

- **Onde**: mesmo formulário do passo 9, seção "Webhook fields".
- **O que**: assine pelo menos `messages` (mensagens recebidas + status de entrega). Sem isso,
  nenhum evento chega no `/whatsapp/webhook`.

## 12. Subscribed Apps por WABA

- **Onde**: automático, feito pelo próprio backend.
- **O que**: além de assinar os campos no App, cada **WABA individual** de cada empresa-cliente
  precisa "inscrever" o App do SaaS para receber webhooks daquela WABA especificamente
  (`POST /{waba-id}/subscribed_apps`). Isso é feito automaticamente pelo backend ao final do
  Embedded Signup — ver `WhatsAppIntegrationService.conectar` → `MetaWhatsAppClient.subscribeAppToWaba`.
  Não é necessário nenhum passo manual no painel para isso.

## 13. Graph API Version

- **Onde**: não é uma tela específica — é a versão usada em cada chamada de URL
  (`/vXX.X/...`).
- **O que**: confirme na documentação oficial qual é a versão estável atual recomendada.
- **Onde usar**: `META_GRAPH_API_VERSION` (`meta.whatsapp.graph-api-version`, default `v23.0` neste
  projeto) — centralizado em `MetaWhatsAppProperties`, nunca hardcoded em URLs espalhadas pelo
  código (`MetaWhatsAppClient`).

## 14. Token de produção (System User Token)

- **Onde**: Business Manager → "Usuários do Sistema" (System Users).
- **O que**: para operação de produção estável, o token de acesso de negócio trocado durante o
  Embedded Signup (`MetaWhatsAppClient.exchangeCodeForToken`) deve ser de longa duração,
  idealmente vinculado a um System User do seu Business Portfolio, não um token temporário gerado
  manualmente no painel de testes.
- **Nunca** use, em produção, um token copiado manualmente da tela de testes do Graph API Explorer
  como solução permanente — ele expira e não segue o fluxo de Embedded Signup por empresa.

## 15. Colocar o App em modo Live

- **Onde**: painel do App → topo da página, toggle "Development" / "Live".
- **O que**: enquanto o App estiver em "Development", só usuários com papel no App (admins,
  desenvolvedores, testers) conseguem completar o Embedded Signup. Para clientes reais, o App
  precisa estar **Live** e ter passado por App Review (passo 6).

## Resumo das variáveis de ambiente

| Variável | Onde é usada | Segredo? |
|---|---|---|
| `META_APP_ID` | Frontend (via `/empresa/whatsapp-integration/meta-config`) + backend (troca de código) | Não |
| `META_APP_SECRET` | Backend apenas (troca de código, validação de assinatura) | **Sim** |
| `META_CONFIG_ID` | Frontend (via `/empresa/whatsapp-integration/meta-config`) | Não |
| `META_VERIFY_TOKEN` | Backend (verificação `GET /whatsapp/webhook`) | Sim (baixo risco) |
| `META_GRAPH_API_VERSION` | Backend (`MetaWhatsAppClient`) | Não |
| `APP_ENCRYPTION_KEY` | Backend (cifra `WhatsAppIntegration.accessToken`) | **Sim** |

Nunca versione valores reais dessas variáveis — cadastre-as diretamente no ambiente de execução
(Railway, `.env` local gitignorado, etc.).
