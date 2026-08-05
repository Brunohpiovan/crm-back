package br.edu.faculdadevincit.crm_vincit.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

@Service
public class AudioConvertor {

    @Autowired
    private S3Service s3Service;

    @Value("${ffmpeg.path}")
    private String ffmpegPath;

    public String convertAndUpload(MultipartFile file) throws Exception {
        if (file.getContentType() == null || !file.getContentType().startsWith("audio/")) {
            throw new IllegalArgumentException("O arquivo enviado não é um áudio.");
        }

        File inputFile = File.createTempFile("input", ".tmp");
        file.transferTo(inputFile);

        File outputFile = new File(inputFile.getAbsolutePath().replace(".tmp", ".ogg"));

        try {
            runFfmpeg(inputFile, outputFile);

            MultipartFile oggMultipart = convertFileToMultipartFile(outputFile);
            return s3Service.saveAudioToS3(oggMultipart);
        } finally {
            if (inputFile.exists()) inputFile.delete();
            if (outputFile.exists()) outputFile.delete();
        }
    }

    private void runFfmpeg(File inputFile, File outputFile) throws IOException, InterruptedException {
        ProcessBuilder processBuilder = new ProcessBuilder(
                ffmpegPath, "-y",
                "-i", inputFile.getAbsolutePath(),
                "-vn",
                "-c:a", "libopus",
                "-b:a", "96k",
                outputFile.getAbsolutePath()
        );
        processBuilder.redirectErrorStream(true);

        Process process = processBuilder.start();
        String output = new String(process.getInputStream().readAllBytes());
        int exitCode = process.waitFor();

        if (exitCode != 0) {
            throw new IllegalStateException("Falha ao converter áudio com FFmpeg (exit=" + exitCode + "): " + output);
        }
    }

    private MultipartFile convertFileToMultipartFile(File file) throws Exception {
        String fileName = file.getName();
        byte[] content = Files.readAllBytes(file.toPath());
        return new MockMultipartFile(fileName, fileName, "audio/ogg", content);
    }

}
