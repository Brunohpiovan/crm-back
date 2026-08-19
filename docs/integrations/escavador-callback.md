# Callbacks da Escavador (monitoramento de processos)

Como o juriq-crm recebe, autentica e processa os avisos que a Escavador dispara quando há
novidade em um processo monitorado. Cobre só a parte de **automação** (Prompt 2): assinatura de
monitoramento, webhook, cota e notificação em tempo real.

## Modelo de conta

Uma única conta/token da Escavador, do juriq-crm, compartilhada por todas as empresas-clientes —
não é a Empresa que traz o próprio token. A unidade vendida ao cliente é **quantidade de
processos monitorados simultaneamente** (`empresa.processos_monitorados_limite`, `NULL` =
ilimitado). O ledger `escavador_credito_lancamento` é mecanismo **interno** de auditoria contra
gasto descontrolado, nunca exibido ao usuário final como unidade de consumo.

## Endpoints usados (API v2)

Fonte: <https://api.escavador.com/v2/docs/monitoramento-de-processos>

| Método | Path                                        | Uso                                      |
| ------ | ------------------------------------------- | ---------------------------------------- |
| POST   | `/api/v2/monitoramentos/processos`          | Cria a assinatura de um processo por CNJ |
| GET    | `/api/v2/monitoramentos/processos/{id}`     | Consulta o status da assinatura          |
| DELETE | `/api/v2/monitoramentos/processos/{id}`     | Remove a assinatura                      |

Corpo do POST: `numero` (CNJ, obrigatório), `tribunal` (sigla, opcional), `frequencia`,
`documentos_publicos`.

**`frequencia` só aceita `DIARIA` ou `SEMANAL`.** Não existe frequência mensal na API — por isso
`FrequenciaMonitoramento` tem exatamente esses dois valores. Oferecer "mensal" na UI significaria
enviar um valor que o provedor rejeita (ou trocar silenciosamente por `DIARIA`, gerando cobrança
acima do esperado).

Status da assinatura: `PENDENTE` (recém-criada) → `ENCONTRADO` (robô localizou o processo e está
monitorando) ou `NAO_ENCONTRADO` (sem cobrança).

O monitoramento de **novos** processos (`/api/v2/monitoramentos/novos-processos`) não é exposto:
o produto monitora processos já vinculados a uma oportunidade, e método sem caso de uso é porta
aberta para gasto não previsto na conta compartilhada.

## Autenticação do webhook

Fonte: <https://api.escavador.com/v2/docs/callbacks>

A Escavador **não assina o corpo do callback por HMAC**. O que ela oferece é um token gerado por
nós no [painel da API](https://api.escavador.com/callbacks), enviado no header `Authorization` de
todo callback. É isso que `EscavadorCallbackTokenValidator` valida, em tempo constante
(`MessageDigest.isEqual`), antes de qualquer resolução de tenant — mesmo princípio de
`MetaWebhookSignatureValidator`.

O token também é aceito em `?token=`, para o caso de o painel só permitir embuti-lo na própria
URL. **Prefira o header:** query strings aparecem em log de acesso, proxy e `Referer`.

Se a Escavador passar a oferecer assinatura HMAC do corpo, ela deve virar a validação primária e
o token continua como camada extra.

`ESCAVADOR_CALLBACK_TOKEN` vazio deixa o endpoint **fail-closed**: nenhum callback é aceito. É
deliberado — um endpoint público capaz de inserir movimentações em processos de qualquer empresa
não pode ficar aberto por esquecimento de configuração.

## Configuração

1. Gere o token em <https://api.escavador.com/callbacks>.
2. Cadastre a URL de callback: `https://<host-da-api>/webhooks/escavador/callback`
   (ou `...?token=<token>`, se preferir o token na URL).
3. Defina `ESCAVADOR_CALLBACK_TOKEN` no ambiente da aplicação.

## Eventos

Sete eventos, todos com a mesma casca (`event`, `monitoramento`, `uuid`):

| `event`                          | Efeito no CRM                                    |
| -------------------------------- | ------------------------------------------------ |
| `nova_movimentacao`              | Registra a movimentação e notifica em tempo real |
| `processo_nao_encontrado`        | Desliga o monitoramento e libera a vaga da cota  |
| `novo_processo`                  | Só auditoria                                     |
| `atualizacao_processo_concluida` | Só auditoria                                     |
| `novo_documento`                 | Só auditoria                                     |
| `processo_verificado`            | Só auditoria                                     |
| `processo_encontrado`            | Só auditoria                                     |

`processo_nao_encontrado` é terminal: a Escavador foi ao tribunal, não localizou o processo, não
vai monitorá-lo e não cobra por ele. Manter a linha ativa mostraria "monitorando" para algo que
ninguém acompanha e ainda ocuparia uma vaga do plano — por isso o monitoramento é desligado
localmente, sem chamar a API (a assinatura já não existe do lado de lá).

Exemplo de `nova_movimentacao` (payload real da documentação, também usado em
`EscavadorCallbackMapperTest`):

```json
{
  "event": "nova_movimentacao",
  "monitoramento": {
    "id": 1567024,
    "numero": "1002089-72.2023.8.26.0260",
    "frequencia": "DIARIA",
    "status": "ENCONTRADO"
  },
  "movimentacao": {
    "id": 23895909833,
    "data": "2024-10-01",
    "tipo": "ANDAMENTO",
    "conteudo": "Pedido de Liminar/Antecipação de Tutela"
  },
  "uuid": "65b45990e91de83f8f40483102ce97ca"
}
```

`movimentacao.tipo` distingue a origem: `ANDAMENTO` (sistema do tribunal) ou `PUBLICACAO`
(diário oficial). `movimentacao.data` vem sem hora (`yyyy-MM-dd`).

## Fluxo de recebimento

1. `EscavadorWebhookController` recebe o corpo cru (endpoint `permitAll`).
2. Token inválido → 403, **sem gravar nada** (endpoint público que persiste tudo que chega é
   vetor de enchimento de banco).
3. Token válido → grava `escavador_callback_evento` com o payload cru, sempre.
4. Resolve os `Processo` por `findAllByNumeroCnjIgnoringTenant` (o webhook roda sem sessão, então
   o tenant vem de cada processo encontrado) e processa dentro de `TenantContext.runAs`.

   O callback identifica o processo só pelo número CNJ, que é único **por empresa** e não
   globalmente: duas empresas clientes podem, de forma legítima, acompanhar o mesmo processo
   público. A ação roda para **cada** empresa com monitoramento ativo daquele CNJ — atender só a
   primeira faria as demais pagarem a cota e nunca receberem nada. Empresas que apenas consultaram
   o processo, sem monitoramento ativo, ficam de fora: não estão pagando por acompanhamento.
5. Delega a `ProcessoMovimentacaoService.registrarMovimentacoes`, que deduplica e publica
   `NovasMovimentacoesDetectadasEvent`.
6. `ProcessoNotificacaoService` publica, após o commit, em
   `/topic/empresa/{empresaId}/processo/{processoPublicId}/movimentacao`.

**Sempre responde 200 quando o token é válido.** A Escavador reenvia até 11 vezes em caso de
erro, com intervalo de 2^n minutos — o que não resolve nada quando a causa é permanente (processo
inexistente nesta instalação). O erro fica em `escavador_callback_evento.erro` para diagnóstico, e
o reenvio manual continua disponível pelo painel (`POST /api/v2/callbacks/{id}/reenviar`).

## Reconciliação

Ativar o monitoramento grava a intenção (`ativo = true`) e só então cria a assinatura. Se a
criação falhar por erro transitório, a linha fica com `escavador_monitoramento_id` nulo e o
`EscavadorMonitoramentoScheduler` retenta de hora em hora (teto de 50 por execução, para que uma
indisponibilidade prolongada não vire uma rajada de chamadas — e de cobranças — quando a API
voltar). O frontend distingue os dois estados por `confirmadoNaEscavador`.
