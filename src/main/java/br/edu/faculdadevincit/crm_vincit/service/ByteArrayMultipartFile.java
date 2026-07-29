package br.edu.faculdadevincit.crm_vincit.service;

import org.springframework.web.multipart.MultipartFile;

import java.io.*;

public class ByteArrayMultipartFile implements MultipartFile {
    private final byte[] content;
    private final String fileName;
    private final String contentType;

    public ByteArrayMultipartFile(byte[] content, String fileName, String contentType) {
        this.content = content;
        this.fileName = fileName;
        this.contentType = contentType;
    }

    @Override
    public String getName() { return fileName; }

    @Override
    public String getOriginalFilename() { return fileName; }

    @Override
    public String getContentType() { return contentType; }

    @Override
    public boolean isEmpty() { return content.length == 0; }

    @Override
    public long getSize() { return content.length; }

    @Override
    public byte[] getBytes() throws IOException { return content; }

    @Override
    public InputStream getInputStream() throws IOException { return new ByteArrayInputStream(content); }

    @Override
    public void transferTo(File file) throws IOException { new FileOutputStream(file).write(content); }
}
