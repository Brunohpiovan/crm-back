package br.edu.faculdadevincit.crm_vincit.service;

import org.bytedeco.ffmpeg.global.avcodec;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.bytedeco.javacv.*;

import java.io.File;
import java.nio.file.Files;

@Service
public class AudioConvertor {

    @Autowired
    private S3Service s3Service;

    @Autowired
    private CloudFrontService cloudFrontService;


    public String convertAndUpload(MultipartFile file) throws Exception {
        if (!file.getContentType().startsWith("audio/")) {
            throw new IllegalArgumentException("O arquivo enviado não é um áudio.");
        }

        File inputFile = File.createTempFile("input", ".tmp");
        file.transferTo(inputFile);

        File outputFile = new File(inputFile.getAbsolutePath().replace(".tmp", ".ogg"));

        FFmpegFrameGrabber grabber = null;
        FFmpegFrameRecorder recorder = null;

        try {
            grabber = new FFmpegFrameGrabber(inputFile);
            grabber.start();

            recorder = new FFmpegFrameRecorder(outputFile, grabber.getAudioChannels());
            recorder.setAudioCodec(avcodec.AV_CODEC_ID_OPUS);
            recorder.setFormat("ogg");
            recorder.setAudioBitrate(96000);
            recorder.setSampleRate(grabber.getSampleRate());
            recorder.setAudioChannels(grabber.getAudioChannels());

            recorder.start();

            Frame frame;
            while ((frame = grabber.grabFrame()) != null) {
                recorder.record(frame);
            }

            recorder.stop();
            grabber.stop();

            MultipartFile oggMultipart = convertFileToMultipartFile(outputFile);
            return s3Service.saveAudioToS3(oggMultipart);

        } finally {
            if (grabber != null) grabber.release();
            if (recorder != null) recorder.release();
            if (inputFile.exists()) inputFile.delete();
            if (outputFile.exists()) outputFile.delete();
        }
    }

    private MultipartFile convertFileToMultipartFile(File file) throws Exception {
        String fileName = file.getName();
        byte[] content = Files.readAllBytes(file.toPath());
        return new MockMultipartFile(fileName, fileName, "audio/ogg", content);
    }

}
