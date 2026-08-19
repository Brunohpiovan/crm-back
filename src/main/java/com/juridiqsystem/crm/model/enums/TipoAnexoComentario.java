package com.juridiqsystem.crm.model.enums;

import java.util.Arrays;
import java.util.Optional;

/**
 * Allowlist dos tipos de anexo aceitos em um comentário de oportunidade. É uma lista fechada de
 * propósito: escritório de advocacia troca petição, procuração e planilha, e nada fora disso
 * precisa entrar no bucket.
 *
 * <p>O que ficou de fora, e por quê:</p>
 * <ul>
 *   <li><b>SVG</b> — é XML com {@code <script>} dentro. Servido do bucket, vira XSS armazenado.</li>
 *   <li><b>HTML/JS</b> — mesma razão, de forma ainda mais direta.</li>
 *   <li><b>ZIP/RAR</b> — container esconde o conteúdo real de qualquer inspeção nossa e é o jeito
 *       mais fácil de transportar executável.</li>
 * </ul>
 *
 * <p>{@code extensao} e {@code contentType} são o que o servidor grava no S3 — nunca o que o
 * cliente mandou. {@code inline} decide se o arquivo pode abrir dentro do navegador (imagem e PDF,
 * que o usuário quer pré-visualizar) ou se desce como download.</p>
 */
public enum TipoAnexoComentario {

    PNG("image/png", ".png", true),
    JPEG("image/jpeg", ".jpg", true),
    GIF("image/gif", ".gif", true),
    WEBP("image/webp", ".webp", true),
    PDF("application/pdf", ".pdf", true),
    DOCX("application/vnd.openxmlformats-officedocument.wordprocessingml.document", ".docx", false),
    DOC("application/msword", ".doc", false),
    XLSX("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", ".xlsx", false),
    XLS("application/vnd.ms-excel", ".xls", false),
    ODT("application/vnd.oasis.opendocument.text", ".odt", false),
    ODS("application/vnd.oasis.opendocument.spreadsheet", ".ods", false),
    TXT("text/plain", ".txt", false);

    private final String contentType;
    private final String extensao;
    private final boolean inline;

    TipoAnexoComentario(String contentType, String extensao, boolean inline) {
        this.contentType = contentType;
        this.extensao = extensao;
        this.inline = inline;
    }

    public String getContentType() {
        return contentType;
    }

    public String getExtensao() {
        return extensao;
    }

    public boolean isInline() {
        return inline;
    }

    /**
     * @param contentTypeDeclarado valor do header do multipart, que vem do cliente e por isso só
     *                             seleciona o candidato — quem confirma é a assinatura do arquivo
     *                             (ver AnexoComentarioValidator).
     */
    public static Optional<TipoAnexoComentario> porContentType(String contentTypeDeclarado) {
        if (contentTypeDeclarado == null) {
            return Optional.empty();
        }
        // "text/plain; charset=UTF-8" -> "text/plain"
        String normalizado = contentTypeDeclarado.split(";")[0].trim().toLowerCase();
        // Alguns navegadores/sistemas mandam image/jpg em vez do oficial image/jpeg.
        if ("image/jpg".equals(normalizado)) {
            return Optional.of(JPEG);
        }
        return Arrays.stream(values())
                .filter(tipo -> tipo.contentType.equals(normalizado))
                .findFirst();
    }

    /** Lista para mensagem de erro e para o atributo accept do input no frontend. */
    public static String extensoesPermitidas() {
        return Arrays.stream(values())
                .map(tipo -> tipo.extensao.substring(1).toUpperCase())
                .distinct()
                .reduce((a, b) -> a + ", " + b)
                .orElse("");
    }
}
