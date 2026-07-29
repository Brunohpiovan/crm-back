package br.edu.faculdadevincit.crm_vincit.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public class MediaService {

    @Autowired
    private S3Service s3Service;

    final long MAX_FILE_SIZE = 52428800;

    public String uploadAnexo(MultipartFile file) {
        if (file.getSize() > MAX_FILE_SIZE) {
            throw new IllegalArgumentException("O arquivo excede o limite de 50MB.");
        }
        String fileName = file.getOriginalFilename();
        String fileExtension =getFileExtension(fileName);
        String folder = "";
        if (isImage(fileExtension)) {
            folder = "imagem/";
        } else if (isDocument(fileExtension)) {
            folder = "documentos/";
        } else if (isCsv(fileExtension)) {
            folder = "documentos/";
        }
        String key = folder + fileName;
        return s3Service.uploadFile(file, key);
    }

    public String getFileExtension(String fileName) {
        int dotIndex = fileName.lastIndexOf('.');
        return (dotIndex == -1) ? "" : fileName.substring(dotIndex + 1).toLowerCase();
    }

    public boolean isImage(String extension) {
        return extension.equals("jpg") || extension.equals("jpeg") || extension.equals("png") || extension.equals("gif");
    }

    public boolean isDocument(String extension) {
        return extension.equals("pdf") || extension.equals("docx") || extension.equals("doc") || extension.equals("txt");
    }

    public boolean isCsv(String extension) {
        return extension.equals("csv");
    }
}
