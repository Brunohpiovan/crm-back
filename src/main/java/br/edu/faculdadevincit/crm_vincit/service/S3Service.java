package br.edu.faculdadevincit.crm_vincit.service;


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


    public String uploadFile(MultipartFile file, String key) {
        if (file.isEmpty()) {
            throw new RuntimeException("O arquivo está vazio");
        }
        try {
            if (fileExistsInS3(key)) {
                key = generateUniqueKey(key);
            }

            PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                    .bucket(bucketName)
                    .key(key)
                    .contentType(file.getContentType())
                    .build();

            try (InputStream inputStream = file.getInputStream()) {
                PutObjectResponse response = s3Client.putObject(putObjectRequest, RequestBody.fromInputStream(inputStream, file.getSize()));
            }

            return baseUrl + "/" + key;

        } catch (Exception e) {
            throw new RuntimeException("A transferência falhou: " + e.getMessage());
        }
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
            throw new RuntimeException("Falha na deleção do objeto");
        }
    }

    public String saveAudioToS3(MultipartFile audio) {
        String key = "audio/" + audio.getOriginalFilename();
        return uploadFile(audio, key);
    }
}
