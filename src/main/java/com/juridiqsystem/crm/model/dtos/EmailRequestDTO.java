package com.juridiqsystem.crm.model.dtos;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.JoinColumn;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Schema(description = "Dados para envio de um e-mail avulso. Montado internamente a partir dos campos multipart/form-data recebidos pelo controller (não é vinculado diretamente como corpo de requisição JSON).")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class EmailRequestDTO {
    @Schema(description = "Endereço de e-mail do destinatário.")
    private String destinatario;
    private String assunto;
    @Schema(description = "Corpo do e-mail (HTML).")
    private String corpo;
    @JoinColumn(name = "remetente_id")
    @Schema(description = "Id do usuário remetente, usado apenas para registro no histórico de envio (não altera o endereço 'De:' do e-mail).")
    private String id_remetente;
    @Schema(description = "Arquivos a serem anexados ao e-mail.")
    private List<MultipartFile> anexos;
}
