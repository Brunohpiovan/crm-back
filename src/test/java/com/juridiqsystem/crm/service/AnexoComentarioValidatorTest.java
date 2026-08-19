package com.juridiqsystem.crm.service;

import com.juridiqsystem.crm.model.enums.TipoAnexoComentario;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * O anexo de comentário é conteúdo enviado por usuário e servido de volta por URL. Estes testes
 * fixam a garantia central: o content-type declarado não decide nada sozinho — quem confirma é a
 * assinatura real dos bytes. Sem isso, renomear um HTML para .pdf grava HTML no bucket, e o
 * navegador o executa como página (XSS armazenado).
 */
class AnexoComentarioValidatorTest {

    private static final byte[] PDF = "%PDF-1.7\nconteudo do processo".getBytes(StandardCharsets.US_ASCII);
    private static final byte[] PNG = new byte[] {
            (byte) 0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A, 0, 0, 0, 13, 'I', 'H', 'D', 'R' };
    private static final byte[] DOCX = new byte[] { 'P', 'K', 0x03, 0x04, 0x14, 0, 6, 0, 8, 0, 0, 0, 0, 0, 0, 0 };
    private static final byte[] DOC_OLE2 = new byte[] {
            (byte) 0xD0, (byte) 0xCF, 0x11, (byte) 0xE0, (byte) 0xA1, (byte) 0xB1, 0x1A, (byte) 0xE1, 0, 0, 0, 0, 0, 0, 0, 0 };
    private static final byte[] HTML = "<script>alert(1)</script>".getBytes(StandardCharsets.US_ASCII);

    private AnexoComentarioValidator validator;

    @BeforeEach
    void criarValidator() {
        validator = new AnexoComentarioValidator();
        ReflectionTestUtils.setField(validator, "maxSizeMb", 20);
    }

    @Test
    void validar_pdfLegitimo_aceitaEDevolveOTipo() {
        assertThat(validar("peticao.pdf", "application/pdf", PDF)).isEqualTo(TipoAnexoComentario.PDF);
    }

    @Test
    void validar_docxLegitimo_aceita() {
        assertThat(validar("contrato.docx",
                "application/vnd.openxmlformats-officedocument.wordprocessingml.document", DOCX))
                .isEqualTo(TipoAnexoComentario.DOCX);
    }

    @Test
    void validar_docAntigoOle2_aceita() {
        assertThat(validar("procuracao.doc", "application/msword", DOC_OLE2)).isEqualTo(TipoAnexoComentario.DOC);
    }

    @Test
    void validar_imagemContinuaAceita() {
        assertThat(validar("print.png", "image/png", PNG)).isEqualTo(TipoAnexoComentario.PNG);
    }

    /** image/jpg não é o tipo oficial, mas é o que alguns navegadores mandam. */
    @Test
    void validar_jpegDeclaradoComoImageJpg_aceita() {
        byte[] jpeg = new byte[] { (byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xE0, 0, 16, 'J', 'F', 'I', 'F', 0, 1, 1, 0, 0, 1 };
        assertThat(validar("foto.jpg", "image/jpg", jpeg)).isEqualTo(TipoAnexoComentario.JPEG);
    }

    /** O ataque que motiva a checagem de assinatura. */
    @Test
    void validar_htmlDisfarcadoDePdf_rejeita() {
        assertThatThrownBy(() -> validar("contrato.pdf", "application/pdf", HTML))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("não permitido");
    }

    @Test
    void validar_htmlAssumido_rejeita() {
        assertThatThrownBy(() -> validar("pagina.html", "text/html", HTML))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("não permitido");
    }

    /** SVG é XML com script dentro: servido do bucket, vira XSS armazenado. */
    @Test
    void validar_svg_rejeita() {
        byte[] svg = "<svg xmlns=\"http://www.w3.org/2000/svg\"><script/></svg>".getBytes(StandardCharsets.US_ASCII);
        assertThatThrownBy(() -> validar("logo.svg", "image/svg+xml", svg))
                .isInstanceOf(RuntimeException.class);
    }

    /** ZIP esconde o conteúdo real de qualquer inspeção — fica fora da allowlist. */
    @Test
    void validar_zip_rejeita() {
        assertThatThrownBy(() -> validar("docs.zip", "application/zip", DOCX))
                .isInstanceOf(RuntimeException.class);
    }

    @Test
    void validar_semContentType_rejeita() {
        assertThatThrownBy(() -> validar("arquivo", null, PDF))
                .isInstanceOf(RuntimeException.class);
    }

    @Test
    void validar_acimaDoLimite_rejeitaInformandoOTeto() {
        byte[] grande = new byte[21 * 1024 * 1024];
        System.arraycopy(PDF, 0, grande, 0, PDF.length);

        assertThatThrownBy(() -> validar("gigante.pdf", "application/pdf", grande))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("20 MB");
    }

    /** .txt não tem assinatura; o que dá para exigir é que seja texto, não binário renomeado. */
    @Test
    void validar_txtDeVerdade_aceitaMasBinarioDisfarcadoRejeita() {
        assertThat(validar("anotacoes.txt", "text/plain; charset=UTF-8",
                "audiencia dia 12".getBytes(StandardCharsets.UTF_8)))
                .isEqualTo(TipoAnexoComentario.TXT);

        byte[] binario = new byte[] { 'M', 'Z', 0, 0, 0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11 };
        assertThatThrownBy(() -> validar("virus.txt", "text/plain", binario))
                .isInstanceOf(RuntimeException.class);
    }

    /** RIFF sozinho também é WAV/AVI: sem o marcador WEBP no offset 8, não passa. */
    @Test
    void validar_riffQueNaoEhWebp_rejeita() {
        byte[] wav = new byte[] { 'R', 'I', 'F', 'F', 36, 0, 0, 0, 'W', 'A', 'V', 'E', 'f', 'm', 't', ' ' };
        assertThatThrownBy(() -> validar("audio.webp", "image/webp", wav))
                .isInstanceOf(RuntimeException.class);
    }

    private TipoAnexoComentario validar(String nome, String contentType, byte[] conteudo) {
        return validator.validar(new MockMultipartFile("file", nome, contentType, conteudo));
    }
}
