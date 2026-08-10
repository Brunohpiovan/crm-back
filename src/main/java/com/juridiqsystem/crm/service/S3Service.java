package com.juridiqsystem.crm.service;


import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;

import java.io.InputStream;

@Service
public class S3Service {

    @Autowired
    private S3Client s3Client;

    @Value("${aws.s3.bucket-name}")
    private String bucketName;

    @Value("${aws.s3.base-url}")
    private String baseUrl;

    public String getBaseUrl() {
        return baseUrl;
    }

    public String uploadFile(MultipartFile file, String key) {
        if (file.isEmpty()) {
            throw new RuntimeException("O arquivo está vazio");
        }
        String safeKey = sanitizeKey(key);
        try {
            if (fileExistsInS3(safeKey)) {
                safeKey = generateUniqueKey(safeKey);
            }

            PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                    .bucket(bucketName)
                    .key(safeKey)
                    .contentType(file.getContentType())
                    .build();

            try (InputStream inputStream = file.getInputStream()) {
                PutObjectResponse response = s3Client.putObject(putObjectRequest, RequestBody.fromInputStream(inputStream, file.getSize()));
            }

            return baseUrl + "/" + safeKey;

        } catch (Exception e) {
            throw new RuntimeException("A transferência falhou: " + e.getMessage());
        }
    }

    /**
     * O prefixo da key (ex.: "logo/", "anexos/", "imagem/") é sempre escolhido pelo código, nunca
     * pelo cliente — só o segmento final costuma vir de entrada do usuário (nome original do
     * arquivo, nome da empresa/usuário). Isolar e limpar só esse último segmento evita que um
     * nome de arquivo malicioso (".." repetido, separadores de caminho, caracteres de controle)
     * produza uma key inesperada, sem quebrar a estrutura de pastas que o próprio código define.
     */
    private String sanitizeKey(String key) {
        if (key == null || key.isBlank()) {
            return "arquivo";
        }
        int lastSlash = key.lastIndexOf('/');
        String prefix = lastSlash >= 0 ? key.substring(0, lastSlash + 1) : "";
        String fileName = lastSlash >= 0 ? key.substring(lastSlash + 1) : key;

        String sanitizedFileName = fileName
                .replaceAll("[^a-zA-Z0-9._-]", "_")
                .replaceAll("\\.{2,}", "_");
        if (sanitizedFileName.isBlank()) {
            sanitizedFileName = "arquivo";
        }

        return prefix + sanitizedFileName;
    }
    private boolean fileExistsInS3(String key) {
        try {
            HeadObjectRequest headObjectRequest = HeadObjectRequest.builder()
                    .bucket(bucketName)
                    .key(key)
                    .build();

            s3Client.headObject(headObjectRequest);
            return true;
        } catch (NoSuchKeyException e) {
            return false;
        }
    }
    private String generateUniqueKey(String key) {
        int dotIndex = key.lastIndexOf(".");
        if (dotIndex != -1) {
            String nameWithoutExtension = key.substring(0, dotIndex);
            String extension = key.substring(dotIndex);

            return nameWithoutExtension + "_" + System.currentTimeMillis() + extension;
        } else {
            return key + "_" + System.currentTimeMillis();
        }
    }

    public void deleteFile(String key) {
        try {
            DeleteObjectRequest deleteObjectRequest = DeleteObjectRequest.builder()
                    .bucket(bucketName)
                    .key(key)
                    .build();
            s3Client.deleteObject(deleteObjectRequest);
        } catch (Exception e) {
            throw new RuntimeException("Falha na deleção do objeto: " + e.getMessage(), e);
        }
    }

    public String saveAudioToS3(MultipartFile audio) {
        String key = "audio/" + audio.getOriginalFilename();
        return uploadFile(audio, key);
    }
}
