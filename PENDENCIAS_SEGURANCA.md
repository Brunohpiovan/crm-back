# Pendências de segurança — pra terminar depois

Contexto: auditoria e hardening de segurança feitos em cima do CRM (frontend `front-crm` +
backend `crm-back`), em 3 rodadas de revisão de código estática. Este arquivo lista só o que
**ainda não foi feito** — o que já foi implementado está descrito no relatório da auditoria
(artifact "Security Hardening Audit — Juridiq CRM").

Tudo aqui foi deixado pendente de propósito: ou porque depende de infraestrutura/decisão que só
vocês têm, ou porque exige rodar/testar a aplicação de verdade (o que não foi possível fazer
durante a auditoria — sem JDK no ambiente).

---

## 0. Verificação obrigatória: IP do cliente não pode ser escolhido pelo cliente (2026-08-17)

`server.forward-headers-strategy` estava em `framework`, o que instala o `ForwardedHeaderFilter`
do Spring — ele roda antes de toda a cadeia do Spring Security e **sobrescreve
`request.getRemoteAddr()` com o `X-Forwarded-For` da requisição, sem validar de quem veio**. Como
`TrustedProxyResolver`/`ClientInfoService` decidem "posso confiar nos headers?" olhando justamente
`getRemoteAddr()`, a proteção anti-spoofing validava o próprio header forjado. Efeito: allowlist de
IP administrativa, rate limit de login/recuperação de senha e log de auditoria eram todos
contornáveis com um header. Trocado para `native` (RemoteIpValve do Tomcat, que só aceita
`X-Forwarded-For` vindo de `server.tomcat.remoteip.internal-proxies`).

- [ ] Com a app rodando, mandar `curl -H "X-Forwarded-For: 1.2.3.4" .../auth/login` (credencial
      inválida) e confirmar no log de acesso/SecurityLogger que o IP registrado **não** é `1.2.3.4`.
- [ ] Confirmar que, em produção, `getClientIp` devolve o IP real do usuário (e não o do proxy da
      plataforma). Se devolver o do proxy, ajustar `server.tomcat.remoteip.internal-proxies` com a
      faixa real — enquanto não ajustar, a allowlist administrativa não casa com ninguém
      (fail-closed: bloqueia o master legítimo em vez de liberar geral).
- [ ] Testar o login de MASTER com IP fora de `ADMIN_IP_ALLOWLIST`: deve devolver 403 com
      "Você não tem acesso a essa função." e **nenhum token** (ver `AuthenticationService`).

## 1. Testar rodando de verdade (prioridade máxima)

Tudo até agora foi revisão de código. Nada foi executado numa aplicação rodando. Antes de
confiar nas mudanças em produção, testar pelo menos:

- [ ] **Login/logout com invalidação de sessão**: logar, copiar o token, fazer logout, confirmar
      que o token antigo passa a ser rejeitado (401) numa chamada autenticada.
- [ ] **Troca de senha revoga sessão**: trocar a própria senha (`PUT /usuario/{id}` ou
      `PUT /master/senha`) e confirmar que o token antigo para de funcionar, e que o token novo
      devolvido na resposta funciona.
- [ ] **Reset de senha por e-mail**: usar o fluxo `/recover-password` → `/reset-password` e
      confirmar que sessões antigas do usuário são revogadas.
- [ ] **Admin resetando senha de outro usuário** (`PUT /usuario/all/{id}`): confirmar que só a
      sessão do usuário-alvo é revogada, não a do admin que fez a alteração.
- [ ] **Deploy**: confirmar que todo usuário com sessão ativa é deslogado na primeira requisição
      após subir essas mudanças (esperado — tokens antigos não têm a claim `sessionVersion`).
- [ ] **Rate limiting**: login (5/min), recover-password, e-mail (20/min), WhatsApp (30/min),
      upload de anexo/áudio (30/min) — bater o limite de propósito e confirmar 429.
- [ ] **Upload rejeitando arquivo inválido**: subir um `.html` renomeado pra `.jpg` como avatar/
      logo/foto de grupo/anexo e confirmar rejeição (não só a extensão errada, mas o conteúdo
      real sendo checado).
- [ ] **ChatGrupo**: confirmar que só ADMIN consegue editar um grupo (`PUT /grupos/{id}`).
- [ ] **Webhooks do Discord**: confirmar que cada um dos 15 eventos chega no canal certo.
      `SUSPICIOUS_REQUEST` dispara ao subir imagem falsificada; `ADMIN_ACTION` dispara ao mudar
      cargo/bloqueado de usuário ou criar/editar empresa; `RESOURCE_ACCESS_DENIED` dispara ao
      tentar acessar/editar cadastro de outro usuário sem ser admin.
- [ ] **Desabilitar o webhook do Discord temporariamente** e confirmar que a API continua
      funcionando normalmente (falha do Discord não pode derrubar nada).
- [ ] **Sanitização do template de e-mail**: colar HTML com `<script>` no editor (ou mandar
      direto pra API) e confirmar que não sobra no banco nem é executado ao reabrir.
- [ ] **`mvn compile` / build completo** — nunca foi rodado nesta auditoria.

---

## 2. Decisões/infra que faltam de vocês

- [ ] **CSP da SPA do frontend** — falta saber onde o `front-crm` é hospedado (Vercel/Netlify/
      outro) pra configurar o header lá. O backend já tem a CSP dele; a SPA ainda não.
- [ ] **`DISCORD_WEBHOOK_SUSPICIOUS_REQUEST` / `ADMIN_ACTION` / `RESOURCE_ACCESS_DENIED`** — já
      têm gatilho no código (rodada atual), mas vale confirmar que os canais certos foram
      escolhidos pro volume esperado de cada um (`ADMIN_ACTION` deve ser bem mais raro que os
      outros, por exemplo).

---

## 3. Manutenção (dependências desatualizadas)

Nenhuma foi atualizada nesta auditoria — todas exigem teste de regressão antes de subir, e não
foi possível rodar a aplicação para validar.

- [ ] **AWS SDK** (`software.amazon.awssdk`) — fixado em `2.20.0` no `pom.xml`, versão antiga da
      série 2.x.
- [x] **Twilio SDK** (`com.twilio.sdk:twilio`) — resolvido: dependência removida por completo na
      migração para a Meta WhatsApp Cloud API (2026-08-13), ver `docs/whatsapp/ARCHITECTURE.md`.
- [ ] **`react-quill-new` / `quill`** (frontend) — `npm audit` encontrou XSS conhecido
      (GHSA-v3m3-f69x-jf25). Mitigado por sanitização (DOMPurify no frontend + jsoup no backend,
      as duas já implementadas), mas o pacote em si continua vulnerável. Avaliar downgrade
      sugerido pelo `npm audit` (`react-quill-new@3.7.0`) ou alternativa, com teste manual do
      editor de template de e-mail depois.

---

## 4. Menor prioridade / avaliar depois

- [ ] **Sessão WebSocket ignora revogação de token** — `StompAuthChannelInterceptor.authenticate`
      valida assinatura e expiração do JWT, mas não checa a claim `sessionVersion` nem
      `usuario.bloqueado`, que o `SecurityFilter` checa nas rotas HTTP. Consequência: depois de um
      logout, troca de senha ou bloqueio do usuário, uma conexão STOMP aberta continua recebendo
      mensagens até o token expirar (12h). Vale replicar as duas checagens no CONNECT.
- [ ] **`ADMIN_ROUTE_SECRET` não pode viver no bundle do frontend** — o header
      `X-Admin-Route-Secret` só é exigido em `/master/**` (não no login, de propósito). Se algum dia
      o frontend público passar a enviá-lo, o segredo estará legível no JS de qualquer visitante e
      deixa de valer como camada. Se for usado, injetar por um proxy/gateway na frente da API.
- [ ] **`JWT_SECRET` sem validação de força** — `TokenService` usa `Algorithm.HMAC256(secret)` com o
      que vier da env var, sem exigir tamanho mínimo. Um segredo curto é quebrável offline a partir
      de qualquer token capturado, e quem quebrar forja um JWT `ROLE_MASTER` válido. Garantir ≥ 32
      bytes aleatórios e, idealmente, falhar no startup se for menor.

- [ ] **Rate limiting em busca/listagem** — decisão consciente de não aplicar agora: um CRM usado
      o dia todo faz paginação/busca o tempo inteiro, e limitar isso sem poder testar throughput
      real arriscava quebrar uso normal. Se decidirem que vale a pena, usar o mesmo
      `SlidingWindowRateLimiter` já existente (`service/auth/SlidingWindowRateLimiter.java`).
- [ ] **`OportunidadeService`** valida imagem de anexo por `Content-Type` declarado pelo cliente,
      não por assinatura de bytes como o resto do sistema (`ImageContentValidator`) — não foi
      trocado porque o detector nativo do Java não reconhece WEBP de forma confiável, e essa rota
      aceita WEBP explicitamente. Se quiserem migrar pra checagem de bytes, vale trocar o
      detector nativo por uma lib melhor (ex.: Apache Tika) que reconheça WEBP.
- [ ] **Varredura de código morto** foi feita por referência cruzada automatizada em
      `model`/`service`/`repository` (achou e removeu `CustomMultipartFile` e `UploadResponse`),
      mas não cobre métodos não usados dentro de classes que continuam ativas — exigiria uma
      ferramenta de análise estática dedicada (ex.: um linter Java com detecção de dead code).

---

## Onde estão as coisas (referência rápida)

- Rate limiters: `service/auth/LoginRateLimiter.java`, `PasswordRecoveryRateLimiter.java`,
  `SlidingWindowRateLimiter.java` (genérico, reaproveitado em e-mail/WhatsApp/upload).
- Logger de segurança: `infra/security/logging/` (`SecurityLogger`, `SecurityEventType`,
  `DiscordWebhookService`, `DiscordWebhookProperties`).
- Invalidação de sessão: coluna `sessao_versao` em `Usuario`, claim `sessionVersion` no JWT
  (`TokenService`), checagem em `SecurityFilter`.
- Sanitização de HTML: `sanitize.ts` (frontend, DOMPurify) e `service/HtmlSanitizer.java`
  (backend, jsoup) — mesma allowlist nos dois lados.
- Validação de conteúdo de imagem: `service/ImageContentValidator.java`.
- Segredos do Discord: `.env` na raiz do `crm-back` (gitignored — nunca commitar).
