package com.virtualvet.util;


import org.springframework.web.multipart.MultipartFile;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Base64;

public class FileUtils {

    private static final String[] ALLOWED_EXTENSIONS = {"jpg", "jpeg", "png", "gif", "bmp"};
    private static final long MAX_FILE_SIZE = 5 * 1024 * 1024; // 5MB

    public static boolean isValidImageFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            return false;
        }

        String filename = file.getOriginalFilename();
        if (filename == null) {
            return false;
        }

        String extension = getFileExtension(filename).toLowerCase();
        for (String allowed : ALLOWED_EXTENSIONS) {
            if (allowed.equals(extension)) {
                return file.getSize() <= MAX_FILE_SIZE;
            }
        }

        return false;
    }

    public static String getFileExtension(String filename) {
        if (filename == null || !filename.contains(".")) {
            return "";
        }
        return filename.substring(filename.lastIndexOf(".") + 1);
    }

    public static byte[] convertBase64ToBytes(String base64String) {
        try {
            // Remove data URL prefix if present
            if (base64String.contains(",")) {
                base64String = base64String.substring(base64String.indexOf(",") + 1);
            }
            return Base64.getDecoder().decode(base64String);
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid base64 string");
        }
    }

    public static String saveFile(byte[] fileBytes, String directory, String filename) throws IOException {
        Path dirPath = Paths.get(directory);
        Files.createDirectories(dirPath);
        
        Path filePath = dirPath.resolve(filename);
        Files.write(filePath, fileBytes);
        
        return filePath.toString();
    }

    public static void deleteFile(String filePath) {
        try {
            Files.deleteIfExists(Paths.get(filePath));
        } catch (IOException e) {
            // Log error but don't throw
            System.err.println("Failed to delete file: " + filePath);
        }
    }

    public static long getDirectorySize(String directoryPath) {
        try {
            return Files.walk(Paths.get(directoryPath))
                .filter(Files::isRegularFile)
                .mapToLong(path -> {
                    try {
                        return Files.size(path);
                    } catch (IOException e) {
                        return 0;
                    }
                })
                .sum();
        } catch (IOException e) {
            return 0;
        }
    }
}
