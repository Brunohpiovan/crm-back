package com.juridiqsystem.crm.service;

import com.juridiqsystem.crm.infra.security.logging.SecurityEventType;
import com.juridiqsystem.crm.infra.security.logging.SecurityLogger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URLConnection;

/**
 * Confere a assinatura real dos bytes de um arquivo antes de aceitá-lo como imagem — a extensão
 * do nome ou o Content-Type declarado pelo cliente não bastam pra impedir upload de HTML/SVG
 * malicioso disfarçado de .jpg/.png/.gif. Usado em todo ponto que aceita imagem: avatar de
 * usuário, logo de empresa, foto de grupo de chat e anexo genérico (MediaService).
 */
@Component
public class ImageContentValidator {

    @Autowired
    private SecurityLogger securityLogger;

    public void validar(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            return;
        }
        try (InputStream inputStream = new BufferedInputStream(file.getInputStream())) {
            String detectedType = URLConnection.guessContentTypeFromStream(inputStream);
            if (detectedType == null || !detectedType.startsWith("image/")) {
                // Cliente mandou um arquivo alegando ser imagem (extensão .jpg/.png/.gif) cujo
                // conteúdo real não é — sinal concreto de tentativa de disfarçar o arquivo, não
                // só um erro de negócio comum.
                securityLogger.log(SecurityEventType.SUSPICIOUS_REQUEST,
                        "Upload rejeitado: arquivo declarado como imagem, mas conteúdo real é "
                                + (detectedType != null ? detectedType : "não identificado"),
                        currentActorLogin(), null, null);
                throw new IllegalArgumentException("O conteúdo do arquivo não corresponde a uma imagem válida.");
            }
        } catch (IOException e) {
            throw new IllegalArgumentException("Não foi possível ler o arquivo enviado.");
        }
    }

    private String currentActorLogin() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication != null ? authentication.getName() : null;
    }
}
