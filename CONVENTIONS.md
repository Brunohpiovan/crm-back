# Convenções de arquitetura — crm-juridiqsystem

Este documento registra as convenções adotadas na etapa de padronização da fundação
arquitetural do backend. Ele é a base para as próximas etapas da refatoração gradual
e não implica, por si só, alteração de módulos já existentes.

## 1. Pacotes

A estrutura de pacotes existente foi mantida (não houve reorganização geral, conforme
regra de não reorganizar tudo de uma vez):

- `model` — entidades JPA.
- `model.dtos` — DTOs e records de request/response. Continua sendo o pacote padrão
  para DTOs de todos os módulos (não introduzimos sub-pacotes por módulo nesta etapa).
- `service` — regras de negócio e coordenação de casos de uso.
- `service.exceptions` — exceções de negócio/infra e o tratamento global de erros
  (`ResourceExceptionHandler`, `StandardError`, `ValidationError`, `FieldMessage`).
- `repository` — acesso a dados, filtros, projeções e agregações via Spring Data JPA.
- `controller` — camada REST.

Mappers, quando adotados em uma etapa futura para um módulo específico, devem viver
junto ao service daquele módulo (ex.: `service.mapper` ou um método estático na
própria DTO, como já é feito hoje) — não criar um pacote `mapper` genérico e vazio
antecipadamente.

## 2. Convenção de DTOs (para DTOs novos, a partir de agora)

Não renomeamos DTOs existentes (alto risco de quebra de contrato sem ganho funcional).
Para DTOs **novos**, adotar sufixo por papel:

- `...CreateRequest` — payload de criação.
- `...UpdateRequest` — payload de atualização.
- `...ListResponse` / `...SummaryResponse` — item de listagem (dados resumidos).
- `...DetailResponse` — detalhe completo de um recurso.

Usar **records** para respostas imutáveis (sem necessidade de setters); usar classes
com `@Getter/@Setter` (Lombok) apenas quando o binding do Spring (formulário
multipart, por exemplo) exigir mutabilidade.

Não criar um DTO genérico universal reaproveitado entre módulos distintos — cada
caso de uso tem seu próprio DTO, ainda que pareça repetitivo.

Nunca expor senha, hash, token ou dado pessoal desnecessário em nenhum desses DTOs.

## 3. Mapeamento (entidade ↔ DTO)

Estratégia confirmada e mantida: **mapeamento explícito**, hoje feito majoritariamente
por construtores na própria DTO (ex.: `new OportunidadeDTO(oportunidade)`). Não
introduzir MapStruct, ModelMapper ou qualquer biblioteca de mapeamento por reflection.
Para casos futuros com muitos campos, preferir um método estático `from(Entity)` na
DTO ou um pequeno mapper dedicado ao módulo, sempre com atribuições explícitas.

## 4. Validação

- Bean Validation (`jakarta.validation`) para formato e presença de campos
  (`@NotNull`, `@NotBlank`, `@Size`, etc.) nos DTOs de request.
- Regras de negócio complexas (que dependem de estado do banco, de outras entidades
  ou de lógica condicional) permanecem no service — nunca só em annotation.
- `@Valid` já está em uso em `TagController`, `ContatoController`,
  `AuthenticationController`, `CadenciaFunilController` e `PasswordResetController`.
  O formato de resposta desses fluxos (`Map<String,String>` via
  `MethodArgumentNotValidException`) foi mantido nesta etapa para não quebrar o
  frontend; a unificação desse formato com `ValidationError` fica para uma etapa que
  também ajuste o frontend correspondente.

## 5. Tratamento de erros (`ResourceExceptionHandler`)

Handler global único, agora com `@RestControllerAdvice`, cobrindo as categorias:

| Categoria             | Exceção                                              | Status |
|-----------------------|-------------------------------------------------------|--------|
| Validação              | `ConstraintViolationException`, `MethodArgumentNotValidException` | 400 |
| Recurso não encontrado | `ResourceNotFoundException` (nova)                    | 404 |
| Conflito               | `ConflictException` (nova), `DataIntegrityViolationException` (própria e a real do Spring/JPA) | 409 / 400* |
| Proibido               | `AccessDeniedException`, `UsuarioBloqueadoException`  | 403 |
| Erro de integração     | `IntegrationException` (nova)                         | 502 |
| Erro inesperado        | `Exception` (fallback)                                | 500 |

`ResourceNotFoundException`, `ConflictException` e `IntegrationException` são novas
e ainda **não são usadas** por nenhum service — foram criadas apenas para que os
próximos prompts, ao refatorarem cada módulo, possam substituir os atuais
`throw new RuntimeException("... não encontrada")` por essas exceções específicas,
sem precisar tocar novamente no handler global.

Nenhuma resposta expõe stack trace, SQL ou mensagem de driver. O handler genérico de
`Exception` e o novo handler de `org.springframework.dao.DataIntegrityViolationException`
(a exceção real do Spring/JPA, que antes não tinha handler dedicado e vazava mensagem
de banco através do handler genérico de `RuntimeException`) agora registram o erro
via log e retornam apenas uma mensagem segura ao cliente.

\* Mantido como 400 (e não 409) para não alterar o contrato de status já observado
pelo frontend nos fluxos existentes. Migrar para 409 é uma decisão a tomar quando o
módulo correspondente for refatorado (com ajuste do frontend na mesma etapa).

### Pendência conhecida (fora do escopo desta etapa)

Não há `AuthenticationEntryPoint`/`AccessDeniedHandler` customizado em
`SecurityConfiguration`. Erros de autenticação ausente/inválida são tratados pelo
filtro de segurança do Spring, **antes** de chegar ao `@RestControllerAdvice`, e por
isso não seguem o padrão de erro definido aqui. Ajustar isso afeta toda rota
autenticada do sistema e deve ser tratado em uma etapa dedicada à camada de
segurança, não nesta etapa de fundação de DTOs/erros.

## 6. Paginação

Nenhum endpoint expõe hoje um `Page` do Spring Data diretamente ao frontend — os
usos atuais de `Page`/`Pageable` (ex.: `MensagemService`) já convertem para `List`
via `.getContent()` antes de retornar. Convenção para quando paginação for exposta
em um endpoint futuro: um record dedicado (`PageResponse<T>` com `content`,
`page`, `size`, `totalElements`, `totalPages`), nunca serializar `PageImpl`
diretamente (formato instável entre versões do Spring Data).

## 7. Repositories e projeções

Convenção para próximas etapas (não aplicada retroativamente nesta etapa, pois
implica alterar services que hoje fazem `findAll()` e filtram em memória — regra de
negócio, fora do escopo aqui):

- Usar `@Query` com JPQL explícita e parâmetros nomeados (`@Param`) para filtros,
  joins e agregações.
- Projeção direta para DTO/record via `SELECT new pacote.MeuDto(...)` quando o
  endpoint não precisa da entidade inteira — nunca `Object[]`.
- Evitar `findAll()` seguido de filtro/ordenação/agregação em memória para grandes
  coleções; mover o filtro para a query.
- Evitar SQL nativo, salvo necessidade comprovada e documentada no próprio
  repository.
