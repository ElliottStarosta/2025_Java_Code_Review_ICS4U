package com.virtualvet.util;


import org.springframework.web.multipart.MultipartFile;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Base64;

/**
 * Utility class for file operations and image handling in the Virtual Vet application.
 * 
 * This class provides comprehensive file management capabilities including image validation,
 * file upload processing, Base64 encoding/decoding, file storage, and directory management.
 * It includes security features such as file type validation, size limits, and safe file
 * storage operations to prevent security vulnerabilities.
 * 
 * The class supports common image formats and provides utilities for managing uploaded
 * pet images, converting between different file formats, and maintaining organized
 * file storage structures.
 * 
 * @author Elliott Starosta
 * @version 1.0
 * @since 2025
 */
public class FileUtils {

    /** Array of allowed image file extensions for upload validation */
    private static final String[] ALLOWED_EXTENSIONS = {"jpg", "jpeg", "png", "gif", "bmp"};
    
    /** Maximum allowed file size for uploads (5MB in bytes) */
    private static final long MAX_FILE_SIZE = 5 * 1024 * 1024; // 5MB

    /**
     * Validates whether an uploaded file is a valid image file.
     * 
     * This method checks if the file is not null, not empty, has a valid filename,
     * has an allowed extension, and does not exceed the maximum file size limit.
     * 
     * @param file the multipart file to validate
     * @return true if the file is a valid image file, false otherwise
     */
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

    /**
     * Extracts the file extension from a filename.
     * 
     * @param filename the filename to extract the extension from
     * @return the file extension (without the dot), or empty string if no extension found
     */
    public static String getFileExtension(String filename) {
        if (filename == null || !filename.contains(".")) {
            return "";
        }
        return filename.substring(filename.lastIndexOf(".") + 1);
    }

    /**
     * Converts a Base64 encoded string to byte array.
     * 
     * This method handles Base64 strings that may include data URL prefixes (e.g., "data:image/jpeg;base64,")
     * by removing the prefix before decoding. It's commonly used for processing image data
     * received from web clients.
     * 
     * @param base64String the Base64 encoded string to convert
     * @return the decoded byte array
     * @throws IllegalArgumentException if the Base64 string is invalid
     */
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

    /**
     * Saves file bytes to the specified directory with the given filename.
     * 
     * This method creates the directory structure if it doesn't exist and saves the
     * file bytes to the specified location. It's commonly used for storing uploaded
     * images and other files in organized directory structures.
     * 
     * @param fileBytes the byte array containing the file data
     * @param directory the target directory path where the file should be saved
     * @param filename the name of the file to save
     * @return the full path to the saved file
     * @throws IOException if there's an error creating directories or writing the file
     */
    public static String saveFile(byte[] fileBytes, String directory, String filename) throws IOException {
        Path dirPath = Paths.get(directory);
        Files.createDirectories(dirPath);
        
        Path filePath = dirPath.resolve(filename);
        Files.write(filePath, fileBytes);
        
        return filePath.toString();
    }

    /**
     * Safely deletes a file from the filesystem.
     * 
     * This method attempts to delete the file at the specified path. If the file
     * doesn't exist or there's an error during deletion, it logs the error but
     * doesn't throw an exception to prevent application crashes.
     * 
     * @param filePath the path to the file to delete
     */
    public static void deleteFile(String filePath) {
        try {
            Files.deleteIfExists(Paths.get(filePath));
        } catch (IOException e) {
            // Log error but don't throw
            System.err.println("Failed to delete file: " + filePath);
        }
    }

    /**
     * Calculates the total size of all files in a directory and its subdirectories.
     * 
     * This method recursively walks through the directory tree and sums up the sizes
     * of all regular files found. It's useful for monitoring disk usage and storage
     * management. If there's an error accessing the directory, it returns 0.
     * 
     * @param directoryPath the path to the directory to analyze
     * @return the total size in bytes of all files in the directory tree, or 0 if there's an error
     */
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
