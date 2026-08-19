package com.juridiqsystem.crm.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Contact;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import io.swagger.v3.oas.annotations.servers.Server;
import org.springframework.context.annotation.Configuration;

/**
 * Configuração central da documentação OpenAPI/Swagger da API do CRM JuridiqSystem.
 *
 * A UI fica disponível em /swagger-ui.html (redireciona para /swagger-ui/index.html)
 * e o contrato bruto em /v3/api-docs (JSON) e /v3/api-docs.yaml (YAML).
 *
 * Fluxo de autenticação para testar endpoints protegidos pelo Swagger UI:
 * 1. Chamar POST /auth/login com login e senha;
 * 2. Copiar o "token" retornado;
 * 3. Clicar em "Authorize" no topo do Swagger UI e colar apenas o token
 *    (o prefixo "Bearer " é adicionado automaticamente pela UI).
 */
@Configuration
@OpenAPIDefinition(
        info = @Info(
                title = "CRM JuridiqSystem API",
                version = "v1",
                description = """
                        API REST do CRM JuridiqSystem.

                        Documentação destinada aos desenvolvedores do frontend Angular, cobrindo \
                        autenticação, funil de vendas, oportunidades, contatos, chat/mensageria \
                        interna e via WhatsApp, upload de mídia (S3), e-mail e usuários.

                        ## Autenticação
                        A grande maioria dos endpoints exige um token JWT enviado no header:

                        `Authorization: Bearer {token}`

                        O token é obtido em `POST /auth/login` e expira em 12 horas. Endpoints \
                        marcados como públicos na descrição de cada operação não exigem esse header.

                        ## Comunicação em tempo real (WebSocket/STOMP)
                        Além dos endpoints REST documentados aqui, o sistema expõe um canal \
                        WebSocket (STOMP sobre SockJS) em `/ws` para chat interno, chat de grupo e \
                        notificações de protocolo/atendimento em tempo real. Esse canal é descrito \
                        separadamente em um documento AsyncAPI (`asyncapi.yaml`, disponível na raiz \
                        de recursos estáticos do backend), já que o padrão OpenAPI não cobre \
                        comunicação assíncrona baseada em tópicos.

                        ## Formato padrão de erros
                        Erros de negócio/infra seguem o formato `StandardError` (timestamp, status, \
                        error, message, path). Erros de validação de campos (`@Valid` em DTOs de \
                        request) retornam `ValidationError`, que estende `StandardError` com uma \
                        lista `errors` de `{field, message}` por campo inválido. Alguns fluxos mais \
                        antigos (ver descrição de cada operação) retornam os erros de validação como \
                        um mapa simples `{"campo": "mensagem"}` em vez de `ValidationError`, por \
                        compatibilidade com o frontend existente.
                        """,
                contact = @Contact(
                        name = "Equipe de Desenvolvimento - CRM JuridiqSystem",
                        email = "contato@juridiqsystem.com.br"
                )
        ),
        servers = {
                @Server(url = "/", description = "Servidor atual")
        }
)
@SecurityScheme(
        name = "bearerAuth",
        type = SecuritySchemeType.HTTP,
        scheme = "bearer",
        bearerFormat = "JWT",
        description = "Token JWT obtido em POST /auth/login. Enviar como 'Authorization: Bearer {token}'."
)
public class OpenApiConfig {
}
