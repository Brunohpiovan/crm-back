package com.juridiqsystem.crm.service;

import com.juridiqsystem.crm.model.enums.TipoAnexoComentario;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.HexFormat;

/**
 * Decide se um anexo de comentário pode ser aceito e qual é, de fato, o seu tipo.
 *
 * <p>O ponto central: o {@code Content-Type} do multipart é escolhido por quem envia. Aceitar só
 * ele significaria que renomear {@code payload.html} para {@code contrato.pdf} basta para gravar
 * HTML no bucket — e como os anexos são servidos por URL, isso é XSS armazenado. Por isso o
 * cabeçalho declarado apenas <em>seleciona um candidato</em> na allowlist, e a assinatura real dos
 * primeiros bytes do arquivo é quem confirma.</p>
 *
 * <p>Não é antivírus: um .docx continua sendo um ZIP cujo conteúdo não inspecionamos. O que isto
 * garante é que o arquivo é do tipo que diz ser, que o servidor decide a extensão e o content-type
 * gravados, e que o tamanho tem teto.</p>
 */
@Component
public class AnexoComentarioValidator {

    /** Bytes lidos do início do arquivo para conferir a assinatura. */
    private static final int TAMANHO_CABECALHO = 16;

    private static final byte[] ASSINATURA_PNG = hex("89504E470D0A1A0A");
    private static final byte[] ASSINATURA_JPEG = hex("FFD8FF");
    private static final byte[] ASSINATURA_GIF87 = "GIF87a".getBytes(StandardCharsets.US_ASCII);
    private static final byte[] ASSINATURA_GIF89 = "GIF89a".getBytes(StandardCharsets.US_ASCII);
    private static final byte[] ASSINATURA_RIFF = "RIFF".getBytes(StandardCharsets.US_ASCII);
    private static final byte[] ASSINATURA_WEBP = "WEBP".getBytes(StandardCharsets.US_ASCII);
    private static final byte[] ASSINATURA_PDF = "%PDF-".getBytes(StandardCharsets.US_ASCII);
    /** DOCX, XLSX, ODT e ODS são todos containers ZIP. */
    private static final byte[] ASSINATURA_ZIP = hex("504B0304");
    /** Formato OLE2, usado pelo Office antigo (.doc/.xls). */
    private static final byte[] ASSINATURA_OLE2 = hex("D0CF11E0A1B11AE1");

    @Value("${app.anexo-comentario.max-size-mb:20}")
    private int maxSizeMb;

    /**
     * @return o tipo confirmado, que passa a ser a fonte da extensão e do content-type gravados.
     * @throws RuntimeException com mensagem de negócio (vira 400 no ResourceExceptionHandler).
     */
    public TipoAnexoComentario validar(MultipartFile file) {
        long maxBytes = (long) maxSizeMb * 1024 * 1024;
        if (file.getSize() > maxBytes) {
            throw new RuntimeException("O arquivo excede o limite de " + maxSizeMb + " MB.");
        }

        TipoAnexoComentario tipo = TipoAnexoComentario.porContentType(file.getContentType())
                .orElseThrow(() -> new RuntimeException(
                        "Tipo de arquivo não permitido. Envie um dos formatos: " + TipoAnexoComentario.extensoesPermitidas() + "."));

        if (!assinaturaConfere(tipo, lerCabecalho(file))) {
            // Mensagem propositalmente igual à de tipo não permitido: não vale a pena explicar a
            // quem está testando o upload exatamente qual checagem barrou.
            throw new RuntimeException(
                    "Tipo de arquivo não permitido. Envie um dos formatos: " + TipoAnexoComentario.extensoesPermitidas() + ".");
        }
        return tipo;
    }

    private byte[] lerCabecalho(MultipartFile file) {
        try (InputStream inputStream = file.getInputStream()) {
            return inputStream.readNBytes(TAMANHO_CABECALHO);
        } catch (IOException e) {
            throw new RuntimeException("Não foi possível ler o arquivo enviado.");
        }
    }

    private boolean assinaturaConfere(TipoAnexoComentario tipo, byte[] cabecalho) {
        return switch (tipo) {
            case PNG -> comecaCom(cabecalho, ASSINATURA_PNG);
            case JPEG -> comecaCom(cabecalho, ASSINATURA_JPEG);
            case GIF -> comecaCom(cabecalho, ASSINATURA_GIF87) || comecaCom(cabecalho, ASSINATURA_GIF89);
            // RIFF sozinho também é WAV e AVI; o marcador WEBP no offset 8 é o que distingue.
            case WEBP -> comecaCom(cabecalho, ASSINATURA_RIFF) && contemNoOffset(cabecalho, 8, ASSINATURA_WEBP);
            case PDF -> comecaCom(cabecalho, ASSINATURA_PDF);
            case DOCX, XLSX, ODT, ODS -> comecaCom(cabecalho, ASSINATURA_ZIP);
            case DOC, XLS -> comecaCom(cabecalho, ASSINATURA_OLE2);
            // Texto puro não tem assinatura. O que dá para exigir é que seja realmente texto:
            // byte nulo no início é sinal de binário disfarçado de .txt.
            case TXT -> semBytesNulos(cabecalho);
        };
    }

    private boolean comecaCom(byte[] cabecalho, byte[] assinatura) {
        return contemNoOffset(cabecalho, 0, assinatura);
    }

    private boolean contemNoOffset(byte[] cabecalho, int offset, byte[] assinatura) {
        if (cabecalho.length < offset + assinatura.length) {
            return false;
        }
        for (int i = 0; i < assinatura.length; i++) {
            if (cabecalho[offset + i] != assinatura[i]) {
                return false;
            }
        }
        return true;
    }

    private boolean semBytesNulos(byte[] cabecalho) {
        for (byte b : cabecalho) {
            if (b == 0) {
                return false;
            }
        }
        return true;
    }

    private static byte[] hex(String valor) {
        return HexFormat.of().parseHex(valor);
    }
}
