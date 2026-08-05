# Auditoria técnica — performance, arquitetura e escalabilidade
**Projeto:** crm-vincit (Spring Boot 3.4.1 / Java 17 / MySQL / Flyway / Railway)
**Data:** 2026-08-05
**Escopo:** 23 controllers, 23 services, 18 repositories, 22 entidades JPA, configuração e integrações externas (Twilio/WhatsApp, S3, SMTP, ffmpeg, WebSocket, scheduler).

> Nota inicial: o projeto já passou por pelo menos uma rodada de otimização anterior (ver migrations `V2026.07.30.*` — índices, conversão de enums `Tag`/`Protocolo`, `@Version` em `Etapa`/`Oportunidade`, `@EntityGraph` em vários repositories). Boa parte do "básico" (fetch LAZY, DTOs na borda HTTP, agregações via JPQL no dashboard) já está correta. Os achados abaixo são o que restou dessa varredura, mais itens de infraestrutura/observabilidade nunca endereçados.

---

## 1. Resumo executivo

- **Nenhum problema de N+1 grosseiro nos fluxos principais** (Oportunidade/Kanban, Dashboard, Protocolo, Etapa) — já resolvidos com `JOIN FETCH`/`@EntityGraph`/batch loading documentado no próprio código.
- **5 problemas críticos** confirmados por leitura direta do código: 2 bugs de persistência de enum (corrupção silenciosa), 1 de exclusão em cascata destrutiva, 1 de autorização ausente em endpoint destrutivo, 2 de concorrência (protocolo duplicado, webhook duplicado) — ver seção 2.
- **Infraestrutura de produção crua**: sem Actuator/Micrometer (zero observabilidade), sem tuning de HikariCP, `open-in-view` no default (`true`), sem timeout em SMTP e no envio Twilio, sem `hibernate.jdbc.batch_size`. Nada disso é "bug" isoladamente, mas juntos deixam a aplicação sem visibilidade e com risco de esgotamento de pool sob carga.
- **Cache**: existe infraestrutura (Caffeine) mas só é usada no Dashboard. Boas oportunidades adicionais são pequenas e de baixo risco (tags, templates, equipes) — não há necessidade de Redis (app roda single-instance no Railway).
- **Autorização granular**: vários endpoints de escrita estruturais (funil, etapa, tag, cadência, template, equipe) exigem só `authenticated()`, permitindo que qualquer VENDEDOR apague estruturas que deveriam ser só de ADMIN.
- Nenhuma alteração de código foi feita ainda — este documento é só o levantamento (etapa "Primeiro" pedida). A implementação será feita por etapas, começando pelos itens críticos, mediante seu aval.

---

## 2. Problemas críticos

| # | Arquivo / Classe / Método | Problema | Impacto | Solução recomendada | Risco da mudança |
|---|---|---|---|---|---|
| C1 | `model/Usuario.java` (campo `cargo`) | `UserRole cargo` sem `@Enumerated(EnumType.STRING)` → persistido como `ORDINAL` contra coluna `BIT(1)` | Reordenar o enum ou adicionar um 3º cargo corrompe silenciosamente o campo `cargo` de todos os usuários existentes, sem erro de compilação nem de runtime imediato | `@Enumerated(EnumType.STRING)` + migration Flyway convertendo `BIT(1)` → `VARCHAR`, mapeando 0→ADMINISTRADOR, 1→VENDEDOR (mesmo padrão já usado nas migrations `V2026.07.30.11`/`.14` para `Tag`/`Protocolo`) | Baixo, migration é reversível e mecânica, mas precisa rodar em produção com cuidado (backup antes) |
| C2 | `model/TemplateEmail.java` (campo `situacao`) | Mesmo bug de C1: `Situacao situacao` sem `@Enumerated`, coluna `BIT(1)` | Mesmo risco de corrupção silenciosa | Mesma solução de C1 | Baixo |
| C3 | `model/Etapa.java` + `EtapaService.delete` / `model/Funil.java` + `FunilService.delete` | `@OneToMany(cascade=ALL, orphanRemoval=true)` sem checagem antes do delete | Excluir uma etapa (ou um funil inteiro) apaga fisicamente todas as oportunidades nela, **inclusive as com `situacao=GANHO`** — contorna o soft-delete (`LIXEIRA`) usado no resto do sistema. Perda de dado de negócio real (vendas fechadas) | Bloquear delete de etapa/funil com oportunidades ativas (ou migrar para "lixeira"/outra etapa antes); revisar se `orphanRemoval` deveria existir | Médio — muda comportamento de um endpoint já em uso; precisa decidir a regra de negócio (bloquear vs. migrar oportunidades) antes de implementar |

| C5 | `ProtocoloService.createProtocolo` | Check-then-act sem lock: verifica se já existe protocolo aberto e cria um novo se não houver, sem constraint/lock | Duas requisições concorrentes (webhook + ação manual, ou dois admins) podem criar **dois protocolos abertos** para o mesmo participante, quebrando a invariante da qual `WhatsAppService.receiveMessage` depende (espera resultado único) | Constraint única parcial (`participante_id` onde `status='ABERTO'`) ou lock pessimista na checagem | Médio — precisa garantir que não há dados já inconsistentes em produção antes de aplicar a constraint |
| C6 | `WhatsAppService.receiveRequest` | Idempotência (SID) só é gravada **ao final** do processamento (após download de mídia + upload S3); Twilio reenvia webhook em timeout | Se o processamento demorar mais que o timeout da Twilio, o reenvio automático reprocessa tudo antes de qualquer marcação — duplica mensagem, participante e possivelmente oportunidade | Registrar o `messageSid` (insert único) **antes** de iniciar o processamento pesado; processar só se o insert tiver sucesso | Médio — muda a ordem do fluxo; precisa tratar o caso de falha após o registro (compensação/retry manual) |

---

## 3. Problemas de alta prioridade

| Área | Arquivo / Método | Problema | Impacto | Solução recomendada |
|---|---|---|---|---|
| Autorização | `funil/etapa/tag/cadência/template/equipe` controllers (escrita) | Endpoints estruturais de escrita exigem só `authenticated()`, sem `@PreAuthorize`/ROLE_ADMIN | VENDEDOR comum pode apagar funis/etapas em cascata, mexer em cadências automáticas, equipes e templates | Restringir criação/edição/exclusão a `ROLE_ADMIN`, padrão já usado em `/usuario` |
| Autorização | `DELETE /participante/{id}` | Hard-delete físico sem restrição de papel | Qualquer usuário apaga fisicamente qualquer contato/cliente | Exigir ROLE_ADMIN ou trocar por soft-delete (já existe padrão em `UsuarioService.delete`) |
| Concorrência | `OportunidadeService.reorganizarIndices` | Reindexa **todos** os cards da etapa a cada movimentação; `@Version` de `Oportunidade` amplia superfície de conflito além do necessário, sem retry de `OptimisticLockException` | Drag-and-drop concorrente falha com erro para o usuário mesmo movendo cards diferentes da mesma coluna | Reindexação por posição relativa (índice fracionário/gap) em vez de reescrever todos os irmãos; retry com backoff para conflito otimista |
| Concorrência | `ChatInternoService.sendMessage` | Duas requisições concorrentes sem grupo privado existente podem criar grupos duplicados (mitigado às cegas em `ChatGrupoService.getGrupoByUsuario`, que já escolhe o mais antigo entre duplicatas — evidência de que isso já ocorre) | Grupos de chat privados duplicados entre o mesmo par de usuários | Constraint única na combinação de participantes do grupo privado |
| Concorrência | `model/Protocolo.java` | Sem `@Version` | Transferência/fechamento concorrente do mesmo protocolo por atendentes diferentes: last-write-wins silencioso | Adicionar `@Version` (mesma migration de C5, se fizer sentido combinar) |
| Erro mal tratado | `ProtocoloService.encaminha` | `throw new Error(...)` (não `Exception`) | Não é capturado por handlers HTTP configurados para `Exception`/`RuntimeException` → 500 não tratado ao invés de erro de negócio | Trocar por `ResponseStatusException`/exceção de domínio |
| N+1 | `EquipeRepository`/`EquipeService.findAll` | Sem `@EntityGraph`, `EquipeResponse` acessa `membros` (LAZY) | 1 query extra por equipe listada — mesmo padrão já corrigido em Acesso/Etapa/ChatGrupo/Protocolo, só não replicado aqui | `@EntityGraph(attributePaths={"membros"})` no `findAll()` |
| Paginação | `ParticipanteController/Service.findAll` | `findAll()` sem paginação sobre a maior tabela do sistema (todo contato de WhatsApp) | Degradação de performance/memória conforme a base cresce | Paginar |
| Paginação | `ProtocoloController.getProtocolo` (rota não paginada) | Retorna todo o histórico de protocolos do atendente, apesar de já existir `getProtocolsPaginado` | Payload cresce sem limite com o histórico | Migrar frontend para a rota paginada e depreciar a antiga |
| Paginação | `MensagemController.getMessages` / `MensagemService.getMessagesForProtocol` | Retorna toda a conversa de um protocolo sem limite, apesar de já existir `getMessagesForProtocolLimit` | Atendimentos longos geram payload grande | Idem — consolidar na versão paginada |
| Paginação | `MensagemController`/`MensagemInternaController` (rotas já paginadas) | `limit` sem teto máximo (`@RequestParam(defaultValue="10") int limit`) | Cliente pode pedir `limit=1000000` | Clampar (`Math.min(limit, 100)`), como já feito no Dashboard |
| Índices | `oportunidade.data_criacao`, `protocolo.data_criacao`/`data_encerramento` | Usadas em quase todas as agregações do dashboard, sem índice | Full scan crescente conforme a base cresce | Ver seção 6 (índices recomendados) |
| Infra | HikariCP sem tuning | Só defaults, sem `max-lifetime`/`connection-timeout` alinhados ao MySQL/Railway | Erros esporádicos de "connection is not available" após períodos de baixo tráfego | Configurar pool explicitamente (seção 7) |
| Infra | Sem Actuator/Micrometer | Zero observabilidade de produção | Sem healthcheck real, sem métricas, sem visibilidade de slow query | Adicionar `spring-boot-starter-actuator` |
| Integração | SMTP sem timeout configurado | Default do JavaMail é infinito | Thread da requisição pode travar indefinidamente em falha de rede SMTP | Configurar `connectiontimeout`/`timeout`/`writetimeout` |
| Integração | ffmpeg (`AudioConvertor`) — `process.waitFor()` sem timeout | Processo externo travado prende a thread da requisição | Esgotamento de threads do Tomcat | `waitFor(timeout, TimeUnit)` + `destroyForcibly()` |
| Integração | Retry/backoff ausente em Twilio/SMTP/Geo API | Falha transitória de rede vira erro definitivo pro usuário | `spring-retry` + `@Retryable` nos pontos críticos |

---

## 4. Problemas de média prioridade

- **Transações incompletas em escritas relacionadas** (sem `@Transactional` amarrando as duas pontas): `UsuarioService.save/update/updateAll` (Usuario + Participante espelhado), `FunilService.adicionarFuncionarioFunil` (Funil + Usuario). Falha parcial deixa dado órfão/dessincronizado.
- **S3/e-mail sem atomicidade com o `save()` final**: `ChatGrupoService.create/update`, `TemplateEmailService.create/update` fazem upload/delete no S3 antes ou depois do save do banco sem compensação — se o save falhar, ficam artefatos órfãos ou referências quebradas (já documentado via comentário/log no próprio código, não corrigido).
- **`EtapaService.updateAddValor/updateSubValor`**: `@Version` existe em `Etapa` mas nenhum chamador trata `OptimisticLockException` — pode gerar erro 500 ao usuário em movimentação concorrente.
- **`WhatsAppService.receiveAudio/Image/Document`**: download de mídia + upload S3 síncronos dentro do handler do webhook — aumenta latência de resposta ao Twilio, retroalimentando o risco C6.
- **`WhatsAppService.criaOportunidade`**: publica no WebSocket a entidade crua **antes** do `save()` (sem `id` ainda).
- **`spring.jpa.open-in-view`** não desabilitado (default `true`).
- **Circuit breaker ausente** nas integrações externas (Twilio/S3/SMTP).
- **`hibernate.jdbc.batch_size`/`order_inserts`/`order_updates`** não configurados.
- **`server.shutdown=graceful`** ausente — deploy pode interromper job do scheduler ou entrega WebSocket em andamento.
- **Índices médios**: `log_movimentacao_cadencia(etapa_destino_id, executado_em)`, `funil_funcionarios(usuario_id)`, `chat_grupo_usuario(usuario_id)` — ver seção 6.
- **Uploads sem validação de tipo**: `MediaController.uploadAnexo` (sem allowlist de extensão), `TemplateEmailController` (anexos sem validação de tamanho/tipo) — diferente do padrão já usado em `OportunidadeService.validarArquivo`.
- **`S3Service.uploadFile`** descarta a causa original da exceção (`new RuntimeException(msg)` sem `e` como causa).
- **Cache ausente** em tags, templates, equipes (candidatos naturais, dados que mudam pouco) — ver seção 5.
- **`server.compression.enabled`** não configurado.

---

## 5. Problemas de baixa prioridade

- `PUT /etapa/{id}` sem `@Valid` (inconsistente com `create`).
- `UsuarioController` não usa `@Valid` em DTOs recebidos via `@RequestPart` (reconhecido no próprio Javadoc do controller) — anotações Bean Validation nunca disparam.
- `PUT /funil/{id}` usa `FunilAllDTO` (DTO de resposta) como corpo de request — risco de mass assignment futuro (hoje só `nome` é usado).
- `PUT /participante/{id}` e `GET /participante/celular/{celular}` retornam a entidade `Participante` diretamente em vez de DTO (baixo risco: entidade sem relacionamentos).
- `AcessoService.preencheLog`: bug — `acesso.setIdiomaNavegador(acesso.getIdiomaNavegador())` (self-reference), o campo nunca é populado a partir do request.
- `AcessoRepository.findAll`/`UsuarioRepository.findAllCriadores`/`CadenciaFunilRepository.findAllWithDetails` sem paginação (baixo volume hoje).
- `MensagemService.sendMessage/sendMessagePublico`: até 2 `save()` individuais em vez de `saveAll`.
- `OportunidadeService.delete` não remove o anexo do S3 — objetos órfãos no bucket.
- `Tag.nome` é `unique` mas não `nullable=false` — MySQL permite múltiplas linhas `NULL` sob índice único.
- `log_movimentacao_cadencia.etapa_origem_id/etapa_destino_id` sem FK (inconsistente com o resto do schema, mas pode ser intencional — não documentado).
- Uso disseminado de `RuntimeException`/`Exception` genérica em vez de exceções de domínio (parcialmente já corrigido em `UsuarioService`/`WhatsAppService`).
- Broadcasts WebSocket amplos e não direcionados: `/topic/usuarios`, `/topic/messages/public`, `/topic/newoportunidadectt` (sem impacto real no volume atual, CRM interno).
- `RestTemplate` instanciado a cada chamada (`downloadMediaFromTwilio`, `GeoLocationService`, `ClientInfoService`) em vez de bean singleton.
- `S3Service.uploadFile` faz `HeadObject` + `PutObject` sempre (2 chamadas de rede por upload).
- Credenciais reais (AWS, Gmail app password, Twilio) em `application-dev.properties` em texto puro — **arquivo já está no `.gitignore` e nunca foi commitado** (confirmado via `git log --all`), então não há vazamento em histórico; ainda assim, recomenda-se mover para `.env` local (já suportado via `spring.config.import`) e rotacionar por precaução, já que são credenciais reais de produção circulando em texto puro na máquina de desenvolvimento.

---

## 6. Índices recomendados (Flyway)

Já existem da auditoria anterior: `idx_protocolo_status`, `idx_mensagem_protocolo_id_id`, `idx_mensagem_data_envio`, `idx_participante_celular`, `idx_cadencia_funil_situacao`, `idx_oportunidade_situacao`, `idx_oportunidade_card_data_entrada`, `idx_mensagem_interna_chat_grupo_id_id`, `idx_log_movimentacao_cadencia_executado_em`.

Gaps confirmados cruzando queries reais com o schema:

```sql
-- usado em ~5 agregações do dashboard (sumValorPorSituacao, countPorSituacao, funilPorEtapa, leadsPorOrigem, ranking)
CREATE INDEX idx_oportunidade_situacao_data_criacao ON oportunidade(situacao, data_criacao);

-- usado em countAbertos, countEmRisco, avgTempoAtendimentoMinutos, rankingProtocolosPorUsuario, countAbertosPorDia
CREATE INDEX idx_protocolo_data_criacao ON protocolo(data_criacao);

-- usado em countFechadosPorDia (status='FECHADO' + data_encerramento)
CREATE INDEX idx_protocolo_status_data_encerramento ON protocolo(status, data_encerramento);

-- log de auditoria do scheduler (roda a cada minuto), filtro por etapa_destino_id + executado_em
CREATE INDEX idx_log_movimentacao_etapa_destino_executado ON log_movimentacao_cadencia(etapa_destino_id, executado_em);

-- tabelas de junção: PK composta (parent_id, usuario_id) não cobre busca só por usuario_id (2ª coluna)
CREATE INDEX idx_funil_funcionarios_usuario ON funil_funcionarios(usuario_id);
CREATE INDEX idx_chat_grupo_usuario_usuario ON chat_grupo_usuario(usuario_id);
```

Todos de baixo risco de escrita (tabelas com volume de INSERT moderado, não são hot-path de altíssima frequência de update).

---

## 7. Cache — plano por caso

| Dado | Cache | Chave | TTL | Invalidação | Observação |
|---|---|---|---|---|---|
| Dashboard (já implementado) | Caffeine, cache `dashboard` | usuário + filtros | 30s | Expira por tempo (sem evict manual) | Mantido como está — bem dimensionado |
| Tags ativas (`GET /tag/ativas`) | Caffeine, novo cache `tags` | fixo (lista única) | 5 min | `@CacheEvict` em create/update/delete de Tag | Volume pequeno (~dezenas), leitura popula seletores em várias telas |
| Templates de e-mail (`GET /template`) | Caffeine, novo cache `templates` | fixo | 5 min | `@CacheEvict` em create/update/delete | Idem |
| Equipes (`GET /equipe`) | Caffeine, novo cache `equipes` | fixo | 5 min | `@CacheEvict` em create/rename/addMembro/removeMembro/delete | Idem |

Todos com Caffeine local (app roda single-instance no Railway — Redis não se justifica hoje; se escalar horizontalmente, reavaliar). `CacheConfig` precisa registrar os novos nomes de cache explicitamente (hoje só registra `"dashboard"`).

**Não cachear**: qualquer coisa por participante/protocolo/usuário autenticado sem chave que isole por usuário (risco de vazar dado entre usuários), dados do WhatsApp/mensagens (sempre precisam estar atualizados), oportunidades/funil (mutação frequente e visão em tempo real via WebSocket já cobre isso).

---

## 8. Plano de implementação por etapas

**Etapa 1 — Correções críticas** (seção 2 completa): enums sem `@Enumerated`, cascade delete destrutivo, autorização ausente em `/messages/public`, race condition de protocolo duplicado, idempotência do webhook Twilio.

**Etapa 2 — Banco de dados**: índices da seção 6 (migration Flyway), paginação nos endpoints listados na seção 3 (Participante, Protocolo legado, Mensagem legado), `@EntityGraph` em `EquipeRepository`, `@Version` em `Protocolo`.

**Etapa 3 — Cache**: registrar caches de tags/templates/equipes conforme seção 7.

**Etapa 4 — Integrações e assincronismo**: timeouts SMTP/ffmpeg/Twilio, retry (`spring-retry`), `@Async` em envio de e-mail e upload/conversão de áudio, `spring.jpa.open-in-view=false`, `hibernate.jdbc.batch_size`.

**Etapa 5 — Observabilidade**: `spring-boot-starter-actuator`, tuning HikariCP, `server.shutdown=graceful`, `server.compression.enabled`.

**Etapa 6 — Autorização e limpeza**: `@PreAuthorize` de ADMIN nos endpoints estruturais (funil/etapa/tag/cadência/template/equipe/participante delete), clamp de `limit`, validação de upload (allowlist), correções de baixa prioridade da seção 5.

Cada etapa será implementada, testada (compilação + testes existentes) e reportada individualmente antes de avançar para a próxima, dado que várias mudanças (C3, C5, autorização) alteram comportamento observável e merecem sua confirmação explícita antes de irem para produção.
