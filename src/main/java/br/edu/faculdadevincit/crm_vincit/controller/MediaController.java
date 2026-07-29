package br.edu.faculdadevincit.crm_vincit.controller;

import br.edu.faculdadevincit.crm_vincit.service.AudioConvertor;
import br.edu.faculdadevincit.crm_vincit.service.MediaService;
import br.edu.faculdadevincit.crm_vincit.service.S3Service;
import io.jsonwebtoken.io.IOException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.nio.file.Files;
import java.util.Collections;

import org.springframework.web.multipart.MultipartFile;
import org.springframework.mock.web.MockMultipartFile;

@RestController
@RequestMapping("/api")
public class MediaController {

    @Autowired
    private S3Service s3Service;

    @Autowired
    private MediaService mediaService;

    @Autowired
    private AudioConvertor audioConvertor;

    @PostMapping("/audio/upload")
    public ResponseEntity<?> uploadAudio(@RequestParam("file") MultipartFile file) throws Exception {
        String fileUrl = audioConvertor.convertAndUpload(file);
        return ResponseEntity.ok(Collections.singletonMap("url", fileUrl));
    }

    @PostMapping("/anexo/upload")
    public ResponseEntity<?> uploadAnexo(@RequestParam("file") MultipartFile file) {
        String fileUrl = mediaService.uploadAnexo(file);
        return ResponseEntity.ok(Collections.singletonMap("url", fileUrl));
    }

}
