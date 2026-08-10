package com.juridiqsystem.crm.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public class MediaService {

    @Autowired
    private S3Service s3Service;

    @Autowired
    private ImageContentValidator imageContentValidator;

    final long MAX_FILE_SIZE = 52428800;

    public String uploadAnexo(MultipartFile file) {
        if (file.getSize() > MAX_FILE_SIZE) {
            throw new IllegalArgumentException("O arquivo excede o limite de 50MB.");
        }
        String fileName = file.getOriginalFilename();
        String fileExtension = getFileExtension(fileName);

        String folder;
        if (isImage(fileExtension)) {
            imageContentValidator.validar(file);
            folder = "imagem/";
        } else if (isDocument(fileExtension) || isCsv(fileExtension)) {
            folder = "documentos/";
        } else {
            // Antes, qualquer extensão fora da whitelist ainda subia pra raiz do bucket sem
            // rejeição nenhuma — permitindo upload de HTML/SVG/executável disfarçado de anexo.
            // Só aceitamos os tipos que o próprio endpoint documenta suportar.
            throw new IllegalArgumentException(
                    "Tipo de arquivo não permitido. Extensões aceitas: imagem (jpg, jpeg, png, gif), "
                            + "documento (pdf, doc, docx, odt, xls, xlsx, ppt, pptx, txt) ou csv.");
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
        // Mantido em sincronia com ALLOWED_ATTACHMENT_TYPES em front-crm/src/lib/mediaUpload.ts —
        // o frontend já anuncia esses formatos como aceitos, então o backend precisa aceitá-los.
        return extension.equals("pdf") || extension.equals("docx") || extension.equals("doc")
                || extension.equals("txt") || extension.equals("odt")
                || extension.equals("xls") || extension.equals("xlsx")
                || extension.equals("ppt") || extension.equals("pptx");
    }

    public boolean isCsv(String extension) {
        return extension.equals("csv");
    }
}
