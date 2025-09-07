package com.virtualvet.util;


import org.springframework.web.multipart.MultipartFile;
import java.io.*;

public class Base64MultipartFile implements MultipartFile {
    
    private final byte[] content;
    private final String name;
    private final String originalFilename;
    private final String contentType;

    public Base64MultipartFile(byte[] content, String name, String originalFilename, String contentType) {
        this.content = content;
        this.name = name;
        this.originalFilename = originalFilename;
        this.contentType = contentType;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public String getOriginalFilename() {
        return originalFilename;
    }

    @Override
    public String getContentType() {
        return contentType;
    }

    @Override
    public boolean isEmpty() {
        return content.length == 0;
    }

    @Override
    public long getSize() {
        return content.length;
    }

    @Override
    public byte[] getBytes() {
        return content;
    }

    @Override
    public InputStream getInputStream() {
        return new ByteArrayInputStream(content);
    }

    @Override
    public void transferTo(File dest) throws IOException {
        try (FileOutputStream fos = new FileOutputStream(dest)) {
            fos.write(content);
        }
    }

    public static Base64MultipartFile fromBase64(String base64String, String filename) {
        byte[] content = FileUtils.convertBase64ToBytes(base64String);
        String contentType = determineContentType(filename);
        return new Base64MultipartFile(content, "image", filename, contentType);
    }

    private static String determineContentType(String filename) {
        if (filename == null) {
            return "application/octet-stream";
        }
        
        String extension = FileUtils.getFileExtension(filename).toLowerCase();
        switch (extension) {
            case "jpg":
            case "jpeg":
                return "image/jpeg";
            case "png":
                return "image/png";
            case "gif":
                return "image/gif";
            case "bmp":
                return "image/bmp";
            default:
                return "application/octet-stream";
        }
    }
}
