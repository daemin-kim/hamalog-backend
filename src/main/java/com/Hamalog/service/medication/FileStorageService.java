package com.Hamalog.service.medication;

import com.Hamalog.exception.file.FileSaveFailException;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

@Slf4j
@Service
public class FileStorageService {

    private final String uploadDir;

    private static final Map<String, List<String>> ALLOWED_FILE_SIGNATURES = Map.of(
        "image/jpeg", Arrays.asList("FFD8FF", "FFD8FFE0", "FFD8FFE1", "FFD8FFE2", "FFD8FFE3", "FFD8FFE8"),
        "image/png", List.of("89504E47"),
        "image/gif", Arrays.asList("474946383761", "474946383961"),
        "image/webp", List.of("52494646")
    );

    private static final Set<String> ALLOWED_EXTENSIONS = Set.of(
        ".jpg", ".jpeg", ".png", ".gif", ".webp"
    );

    private static final Set<String> ALLOWED_MIME_TYPES = Set.of(
        "image/jpeg", "image/jpg", "image/png", "image/gif", "image/webp"
    );

    private static final Set<String> MALICIOUS_SIGNATURES = Set.of(
        "4D5A",
        "504B0304",
        "526172211A",
        "377ABCAF271C",
        "D0CF11E0A1B11AE1"
    );

    private static final long MAX_FILE_SIZE = 5L * 1024 * 1024;

    public FileStorageService(@Value("${hamalog.upload.image-dir:/data/hamalog/images}") String uploadDir) {
        this.uploadDir = uploadDir.endsWith(File.separator) ? uploadDir : uploadDir + File.separator;
        log.info("FileStorageService initialized with upload directory: {}", this.uploadDir);
    }

    public String save(MultipartFile file) {
        log.debug("Starting file upload process for file: {}", 
                 file != null ? file.getOriginalFilename() : "null");

        validateBasicFile(file);
        validateFileSize(file);
        String contentType = validateMimeType(file);
        String extension = validateFileExtension(file);
        validateFileSignature(file, contentType);
        scanForMaliciousContent(file);
        return saveSecureFile(file, extension);
    }

    private void validateBasicFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            log.warn("File upload attempt with null or empty file");
            throw new FileSaveFailException();
        }

        String originalFilename = file.getOriginalFilename();
        if (!StringUtils.hasText(originalFilename)) {
            log.warn("File upload attempt with invalid filename");
            throw new FileSaveFailException();
        }
    }

    private void validateFileSize(MultipartFile file) {
        if (file.getSize() > MAX_FILE_SIZE) {
            log.warn("File size {} exceeds maximum allowed size {}", file.getSize(), MAX_FILE_SIZE);
            throw new FileSaveFailException();
        }
    }

    private String validateMimeType(MultipartFile file) {
        String contentType = file.getContentType();
        if (!StringUtils.hasText(contentType)) {
            log.warn("File upload with missing content type");
            throw new FileSaveFailException();
        }

        contentType = contentType.toLowerCase();
        if (!ALLOWED_MIME_TYPES.contains(contentType)) {
            log.warn("File upload with disallowed MIME type: {}", contentType);
            throw new FileSaveFailException();
        }

        return contentType;
    }

    private String validateFileExtension(MultipartFile file) {
        String originalFilename = file.getOriginalFilename();
        if (!StringUtils.hasText(originalFilename)) {
            throw new FileSaveFailException();
        }

        String extension = "";
        int lastDotIndex = originalFilename.lastIndexOf('.');
        if (lastDotIndex != -1 && lastDotIndex < originalFilename.length() - 1) {
            extension = originalFilename.substring(lastDotIndex).toLowerCase();
        }

        if (!StringUtils.hasText(extension) || !ALLOWED_EXTENSIONS.contains(extension)) {
            log.warn("File upload with disallowed extension: {}", extension);
            throw new FileSaveFailException();
        }

        return extension;
    }

    private void validateFileSignature(MultipartFile file, String contentType) {
        try (InputStream inputStream = file.getInputStream()) {
            byte[] header = new byte[12];
            int bytesRead = inputStream.read(header);
            
            if (bytesRead < 4) {
                log.warn("File too small to validate signature");
                throw new FileSaveFailException();
            }

            String fileSignature = bytesToHex(header, Math.min(bytesRead, 8));
            
            List<String> allowedSignatures = ALLOWED_FILE_SIGNATURES.get(contentType);
            if (allowedSignatures != null) {
                boolean signatureValid = allowedSignatures.stream()
                    .anyMatch(signature -> fileSignature.startsWith(signature));
                
                if (!signatureValid) {
                    log.warn("File signature validation failed. Expected signatures for {}: {}, but got: {}", 
                            contentType, allowedSignatures, fileSignature);
                    throw new FileSaveFailException();
                }
            }

        } catch (IOException e) {
            log.error("Error reading file for signature validation", e);
            throw new FileSaveFailException();
        }
    }

    private void scanForMaliciousContent(MultipartFile file) {
        try (InputStream inputStream = file.getInputStream()) {
            byte[] header = new byte[16];
            int bytesRead = inputStream.read(header);
            
            if (bytesRead > 0) {
                String fileSignature = bytesToHex(header, bytesRead);
                
                for (String maliciousSignature : MALICIOUS_SIGNATURES) {
                    if (fileSignature.startsWith(maliciousSignature)) {
                        log.error("Malicious file signature detected: {}", maliciousSignature);
                        throw new FileSaveFailException();
                    }
                }
            }

        } catch (IOException e) {
            log.error("Error scanning file for malicious content", e);
            throw new FileSaveFailException();
        }
    }

    private String saveSecureFile(MultipartFile file, String extension) {
        String fileName = UUID.randomUUID() + extension;
        Path savePath = Paths.get(uploadDir);

        File directory = savePath.toFile();
        if (!directory.exists() && !directory.mkdirs()) {
            log.error("Failed to create upload directory: {}", uploadDir);
            throw new FileSaveFailException();
        }

        File dest = new File(directory, fileName);
        try {
            file.transferTo(dest);
            log.info("File saved successfully: {} (original: {})", fileName, file.getOriginalFilename());
        } catch (Exception e) {
            log.error("Failed to save file: {}", fileName, e);
            throw new FileSaveFailException();
        }

        return fileName;
    }

    private String bytesToHex(byte[] bytes, int length) {
        StringBuilder hexString = new StringBuilder();
        for (int i = 0; i < length; i++) {
            String hex = Integer.toHexString(0xFF & bytes[i]);
            if (hex.length() == 1) {
                hexString.append('0');
            }
            hexString.append(hex);
        }
        return hexString.toString().toUpperCase();
    }

    public FileUploadSecurityInfo getSecurityInfo() {
        return new FileUploadSecurityInfo(
            MAX_FILE_SIZE,
            new HashSet<>(ALLOWED_MIME_TYPES),
            new HashSet<>(ALLOWED_EXTENSIONS),
            uploadDir
        );
    }

    public record FileUploadSecurityInfo(
        long maxFileSize,
        Set<String> allowedMimeTypes,
        Set<String> allowedExtensions,
        String uploadDirectory
    ) {}
}
