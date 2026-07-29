package br.edu.faculdadevincit.crm_vincit.service;

import com.amazonaws.services.cloudfront.CloudFrontUrlSigner;
import com.amazonaws.services.cloudfront.util.SignerUtils.Protocol;
import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

import java.io.*;
import java.util.Date;
import java.time.Duration;


@Service
public class CloudFrontService {

    @Value("${cloudfront.domain}")
    private String cloudfrontDomain;

    @Value("${cloudfront.key-id}")
    private String keyPairId;

    @Value("${cloudfront.private-key}")
    private Resource privateKeyResource;

    @Getter
    @Value("${aws.s3.base-url}")
    private String baseUrl;

    public String generateSignedUrl(String s3Url, Duration validDuration) {
        try {
            String objectKey = extractObjectKeyFromS3Url(s3Url);
            File privateKeyFile = privateKeyResource.getFile();

            String domainWithoutProtocol = cloudfrontDomain.replace("https://", "");

            Date expirationDate = new Date(System.currentTimeMillis() + validDuration.toMillis());

            return CloudFrontUrlSigner.getSignedURLWithCannedPolicy(
                    Protocol.https,
                    domainWithoutProtocol,
                    privateKeyFile,
                    objectKey,
                    keyPairId,
                    expirationDate
            );


        } catch (IOException e) {
            throw new RuntimeException("Erro ao acessar arquivo da chave privada: " + e.getMessage(), e);
        } catch (Exception e) {
            throw new RuntimeException("Erro ao gerar URL assinada do CloudFront: " + e.getMessage(), e);
        }
    }

    private String extractObjectKeyFromS3Url(String s3Url) {
        int index = s3Url.indexOf(".amazonaws.com/");
        if (index == -1) {
            throw new IllegalArgumentException("URL S3 inválida: " + s3Url);
        }

        String objectKey = s3Url.substring(index + ".amazonaws.com/".length());

        return objectKey.startsWith("/") ? objectKey.substring(1) : objectKey;
    }


}
