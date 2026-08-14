package com.juridiqsystem.crm.infra.security;


import com.juridiqsystem.crm.infra.security.logging.SecurityEventType;
import com.juridiqsystem.crm.infra.security.logging.SecurityLogger;
import com.juridiqsystem.crm.service.auth.ClientInfoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.DefaultSecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.header.writers.StaticHeadersWriter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Configuration
@EnableWebSecurity
public class SecurityConfiguration {

    @Autowired
    SecurityFilter securityFilter;

    @Autowired
    private AdminRouteGuardFilter adminRouteGuardFilter;

    @Autowired
    private SecurityLogger securityLogger;

    @Autowired
    private ClientInfoService clientInfoService;

    @Value("${app.cors.allowed-origins}")
    private String allowedOrigins;

    @Bean
    public DefaultSecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        return http
                .csrf(csrf -> csrf.disable())
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                // Headers de resposta desta API (inclui a página pública do Swagger UI). CSP
                // restrita ao que a própria API/Swagger usa — a SPA do frontend é servida por
                // outra origem/hospedagem e precisa da sua própria CSP configurada lá. HSTS não
                // é tocado aqui: o writer padrão do Spring Security já só o adiciona em requests
                // HTTPS, e "preload" fica pra quando o domínio estiver 100% pronto (fora de escopo
                // agora).
                .headers(headers -> headers
                        .frameOptions(frame -> frame.deny())
                        .contentSecurityPolicy(csp -> csp.policyDirectives(
                                "default-src 'self'; " +
                                "script-src 'self'; " +
                                "style-src 'self' 'unsafe-inline'; " +
                                "img-src 'self' data: https:; " +
                                "font-src 'self' https:; " +
                                "connect-src 'self'; " +
                                "object-src 'none'; " +
                                "base-uri 'self'; " +
                                "form-action 'self'; " +
                                "frame-ancestors 'none';"
                        ))
                        .addHeaderWriter(new StaticHeadersWriter("Referrer-Policy", "strict-origin-when-cross-origin"))
                        .addHeaderWriter(new StaticHeadersWriter("Permissions-Policy", "camera=(), microphone=(), geolocation=()"))
                )
                .authorizeHttpRequests(authorize -> authorize
                        .requestMatchers(
                                "/swagger-ui.html",
                                "/swagger-ui/**",
                                "/v3/api-docs",
                                "/v3/api-docs/**",
                                "/asyncapi.yaml"
                        ).permitAll()
                        // Healthcheck da plataforma (Railway) não envia JWT. show-details=never
                        // (application.properties) garante que só o status UP/DOWN fica visível
                        // aqui, sem detalhe de infraestrutura (ex.: datasource) exposto sem auth.
                        .requestMatchers(HttpMethod.GET, "/actuator/health", "/actuator/health/**").permitAll()
                        .requestMatchers(HttpMethod.POST,"/auth/login").permitAll()
                        // /auth/register: sem controller mapeado hoje (ver RegisterDTO). Regra
                        // removida até o endpoint existir de fato — não deixar permitAll "morto"
                        // esquecido aqui, que viraria self-registration público sem revisão se
                        // alguém implementar o endpoint sem lembrar de checar esta linha.
                        .requestMatchers(HttpMethod.POST, "/recover-password").permitAll()
                        .requestMatchers(HttpMethod.POST, "/reset-password").permitAll()
                        .requestMatchers(HttpMethod.POST, "/whatsapp/webhook").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/contato").permitAll()
                        .requestMatchers(HttpMethod.POST, "/usuario").hasAuthority("ROLE_ADMIN")
                        .requestMatchers(HttpMethod.PUT, "/usuario/all/**").hasAuthority("ROLE_ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/usuario/**").hasAuthority("ROLE_ADMIN")
                        // Listagem paginada (tela "Usuários", gestão de toda a equipe) é só pra
                        // ADMIN; os demais GETs de /usuario/** (perfil próprio, contatos, listas
                        // de apoio a outras telas como "criador"/"admin") continuam liberados a
                        // qualquer autenticado — regra específica precisa vir antes da genérica
                        // abaixo, senão nunca seria alcançada.
                        .requestMatchers(HttpMethod.GET, "/usuario").hasAuthority("ROLE_ADMIN")
                        .requestMatchers(HttpMethod.GET, "/usuario/**").hasAuthority("ROLE_VENDEDOR")
                        // Autoatendimento da empresa (aba Configurações): qualquer usuário logado
                        // pode ver, só ROLE_ADMIN pode editar (afeta toda a empresa, não só o
                        // próprio cadastro).
                        .requestMatchers(HttpMethod.GET, "/empresa").hasAuthority("ROLE_VENDEDOR")
                        .requestMatchers(HttpMethod.PUT, "/empresa").hasAuthority("ROLE_ADMIN")
                        // Só ADMINISTRADOR cria/atualiza/exclui funil e etapa (afeta o pipeline
                        // de vendas de todos os vendedores). Match exato em "/funil" (POST) não
                        // pega POST /funil/filtro (consulta) nem POST /funil/add-funcionario/**.
                        .requestMatchers(HttpMethod.POST, "/funil").hasAuthority("ROLE_ADMIN")
                        .requestMatchers(HttpMethod.PUT, "/funil/**").hasAuthority("ROLE_ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/funil/**").hasAuthority("ROLE_ADMIN")
                        // Adicionar vendedor a um funil é gestão de equipe, não uso básico do
                        // funil — só ADMIN. Não bate no matcher exato "/funil" (POST) acima.
                        .requestMatchers(HttpMethod.POST, "/funil/add-funcionario/**").hasAuthority("ROLE_ADMIN")
                        .requestMatchers(HttpMethod.POST, "/etapa").hasAuthority("ROLE_ADMIN")
                        .requestMatchers(HttpMethod.PUT, "/etapa/**").hasAuthority("ROLE_ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/etapa/**").hasAuthority("ROLE_ADMIN")
                        // Oportunidade continua liberada de propósito: é o card individual de
                        // trabalho do vendedor (criar, editar, mover entre etapas, excluir) — só
                        // a estrutura do funil/etapa em si (acima) é restrita a ADMIN.
                        // Exclusão física (hard delete) de participante/cliente: sem uso hoje no
                        // frontend (não há botão de excluir participante na UI), então restringir
                        // não quebra nenhum fluxo existente — só fecha a porta pra chamada direta
                        // à API por um usuário comum apagar o cadastro de qualquer cliente.
                        .requestMatchers(HttpMethod.DELETE, "/participante/**").hasAuthority("ROLE_ADMIN")
                        // Cadência, template de e-mail e tag são configuração compartilhada da
                        // empresa inteira (diferente de Oportunidade, que é liberada de propósito
                        // — ver comentário acima). Mesmo padrão já usado em Funil/Etapa: só ADMIN
                        // cria/edita/exclui.
                        .requestMatchers(HttpMethod.POST, "/cadencia").hasAuthority("ROLE_ADMIN")
                        .requestMatchers(HttpMethod.PUT, "/cadencia/**").hasAuthority("ROLE_ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/cadencia/**").hasAuthority("ROLE_ADMIN")
                        .requestMatchers(HttpMethod.POST, "/template/create").hasAuthority("ROLE_ADMIN")
                        .requestMatchers(HttpMethod.PUT, "/template/**").hasAuthority("ROLE_ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/template/**").hasAuthority("ROLE_ADMIN")
                        .requestMatchers(HttpMethod.POST, "/tag").hasAuthority("ROLE_ADMIN")
                        .requestMatchers(HttpMethod.PUT, "/tag/**").hasAuthority("ROLE_ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/tag/**").hasAuthority("ROLE_ADMIN")
                        // Editar grupo de chat (renomear, trocar foto, adicionar/remover membros)
                        // não tinha nenhuma checagem — qualquer autenticado podia mexer em
                        // qualquer grupo, mesmo sem ser membro. ChatGrupo não tem campo de
                        // "criador" (só a lista de membros), então restringir a ADMIN é o corte
                        // mais simples sem exigir migration. Criar grupo continua liberado.
                        .requestMatchers(HttpMethod.PUT, "/grupos/**").hasAuthority("ROLE_ADMIN")
                        .requestMatchers("/ws/**").permitAll()
                        .requestMatchers(HttpMethod.PUT, "/log/finalizar/**").permitAll()
                        // Histórico de acesso (IP/geolocalização/dispositivo) de usuários da
                        // empresa: dado sensível, restrito a administradores — antes qualquer
                        // autenticado (inclusive ROLE_VENDEDOR) podia listar o log de todo mundo.
                        .requestMatchers(HttpMethod.GET, "/log/**").hasAuthority("ROLE_ADMIN")
                        // Área do usuário master (super-admin multi-empresa): CRUD de empresas e
                        // de usuários de qualquer empresa. Cargo MASTER só recebe a authority
                        // ROLE_MASTER (ver Usuario.getAuthorities()), então ele já é rejeitado
                        // por todas as regras ROLE_ADMIN/ROLE_VENDEDOR acima automaticamente.
                        .requestMatchers("/master/**").hasAuthority("ROLE_MASTER")
                        .anyRequest().authenticated()
                )
                // Comportamento padrão do Spring Security aqui (sem formLogin/httpBasic
                // configurado) devolveria corpo vazio nesses dois casos — e potencialmente 403
                // em vez de 401 pra requisição não autenticada, já que não há um
                // AuthenticationEntryPoint explícito. Definimos os dois aqui pra garantir o
                // status correto e logar o evento de segurança.
                .exceptionHandling(exceptions -> exceptions
                        .authenticationEntryPoint((httpRequest, httpResponse, authException) -> {
                            securityLogger.log(SecurityEventType.UNAUTHORIZED_ACCESS, authException.getMessage(), null,
                                    clientInfoService.getClientIp(httpRequest), httpRequest.getRequestURI());
                            httpResponse.setStatus(jakarta.servlet.http.HttpServletResponse.SC_UNAUTHORIZED);
                            httpResponse.setContentType("application/json");
                            httpResponse.getWriter().write("{\"message\":\"Autenticação necessária.\"}");
                        })
                        .accessDeniedHandler((httpRequest, httpResponse, accessDeniedException) -> {
                            var authentication = SecurityContextHolder.getContext().getAuthentication();
                            String actor = authentication != null ? authentication.getName() : null;
                            securityLogger.log(SecurityEventType.ACCESS_DENIED, accessDeniedException.getMessage(), actor,
                                    clientInfoService.getClientIp(httpRequest), httpRequest.getRequestURI());
                            httpResponse.setStatus(jakarta.servlet.http.HttpServletResponse.SC_FORBIDDEN);
                            httpResponse.setContentType("application/json");
                            httpResponse.getWriter().write("{\"message\":\"Você não tem permissão para acessar este recurso.\"}");
                        })
                )
                .addFilterBefore(securityFilter, UsernamePasswordAuthenticationFilter.class)
                // Antes até do SecurityFilter: /master/** nem chega a ter o JWT decodificado se
                // o segredo/allowlist de IP (quando configurados) barrar aqui — ver
                // AdminRouteGuardFilter. Camada adicional, não substitui a checagem de
                // ROLE_MASTER feita mais abaixo em authorizeHttpRequests.
                .addFilterBefore(adminRouteGuardFilter, SecurityFilter.class)
                .build();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration authenticationConfiguration) throws Exception {
        return authenticationConfiguration.getAuthenticationManager();
    }

    @Bean
    public PasswordEncoder passwordEncoder(){
        return new BCryptPasswordEncoder();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        List<String> origins = Arrays.stream(allowedOrigins.split(","))
                .map(String::trim)
                .filter(origin -> !origin.isEmpty())
                .collect(Collectors.toList());

        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOriginPatterns(origins);
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(Arrays.asList("*"));
        configuration.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}