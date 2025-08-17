package com.Hamalog.service.medication;

import com.Hamalog.exception.file.FileSaveFailException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

/**
 * 파일 저장 서비스
 * 
 * 보안 강화된 파일 업로드 서비스로 파일 시그니처 검증, MIME 타입 검증,
 * 확장자 검증, 악성 파일 스캐닝 등을 제공합니다.
 */
@Slf4j
@Service
public class FileStorageService {

    private final String uploadDir;

    // 허용된 이미지 파일 시그니처 (Magic Numbers)
    private static final Map<String, List<String>> ALLOWED_FILE_SIGNATURES = Map.of(
        "image/jpeg", Arrays.asList("FFD8FF", "FFD8FFE0", "FFD8FFE1", "FFD8FFE2", "FFD8FFE3", "FFD8FFE8"),
        "image/png", List.of("89504E47"),
        "image/gif", Arrays.asList("474946383761", "474946383961"),
        "image/webp", List.of("52494646")
    );

    // 허용된 파일 확장자
    private static final Set<String> ALLOWED_EXTENSIONS = Set.of(
        ".jpg", ".jpeg", ".png", ".gif", ".webp"
    );

    // 허용된 MIME 타입
    private static final Set<String> ALLOWED_MIME_TYPES = Set.of(
        "image/jpeg", "image/jpg", "image/png", "image/gif", "image/webp"
    );

    // 악성 파일 시그니처 (차단할 파일 유형)
    private static final Set<String> MALICIOUS_SIGNATURES = Set.of(
        "4D5A", // PE/EXE files
        "504B0304", // ZIP files
        "526172211A", // RAR files
        "377ABCAF271C", // 7Z files
        "D0CF11E0A1B11AE1" // MS Office files
    );

    // 최대 파일 크기 (5MB)
    private static final long MAX_FILE_SIZE = 5L * 1024 * 1024;

    public FileStorageService(@Value("${hamalog.upload.image-dir:/data/hamalog/images}") String uploadDir) {
        this.uploadDir = uploadDir.endsWith(File.separator) ? uploadDir : uploadDir + File.separator;
        log.info("FileStorageService initialized with upload directory: {}", this.uploadDir);
    }

    /**
     * 보안 검증된 파일 저장
     * 
     * @param file 업로드할 파일
     * @return 저장된 파일명
     * @throws FileSaveFailException 파일 저장 실패 또는 보안 검증 실패 시
     */
    public String save(MultipartFile file) {
        log.debug("Starting file upload process for file: {}", 
                 file != null ? file.getOriginalFilename() : "null");

        // 1. 기본 유효성 검사
        validateBasicFile(file);

        // 2. 파일 크기 검사
        validateFileSize(file);

        // 3. MIME 타입 검사
        String contentType = validateMimeType(file);

        // 4. 파일 확장자 검사
        String extension = validateFileExtension(file);

        // 5. 파일 시그니처 (Magic Number) 검사
        validateFileSignature(file, contentType);

        // 6. 악성 파일 스캔
        scanForMaliciousContent(file);

        // 7. 파일명 생성 및 저장
        return saveSecureFile(file, extension);
    }

    /**
     * 기본 파일 유효성 검사
     */
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

    /**
     * 파일 크기 검증
     */
    private void validateFileSize(MultipartFile file) {
        if (file.getSize() > MAX_FILE_SIZE) {
            log.warn("File size {} exceeds maximum allowed size {}", file.getSize(), MAX_FILE_SIZE);
            throw new FileSaveFailException();
        }
    }

    /**
     * MIME 타입 검증
     */
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

    /**
     * 파일 확장자 검증
     */
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

    /**
     * 파일 시그니처 (Magic Number) 검증
     */
    private void validateFileSignature(MultipartFile file, String contentType) {
        try (InputStream inputStream = file.getInputStream()) {
            byte[] header = new byte[12]; // 충분한 크기로 헤더 읽기
            int bytesRead = inputStream.read(header);
            
            if (bytesRead < 4) {
                log.warn("File too small to validate signature");
                throw new FileSaveFailException();
            }

            String fileSignature = bytesToHex(header, Math.min(bytesRead, 8));
            
            // 허용된 시그니처 확인
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

    /**
     * 악성 파일 콘텐츠 스캔
     */
    private void scanForMaliciousContent(MultipartFile file) {
        try (InputStream inputStream = file.getInputStream()) {
            byte[] header = new byte[16];
            int bytesRead = inputStream.read(header);
            
            if (bytesRead > 0) {
                String fileSignature = bytesToHex(header, bytesRead);
                
                // 악성 시그니처 확인
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

    /**
     * 보안 검증된 파일 저장
     */
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

    /**
     * 바이트 배열을 16진수 문자열로 변환
     */
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

    /**
     * 현재 파일 업로드 보안 설정 조회 (모니터링 목적)
     */
    public FileUploadSecurityInfo getSecurityInfo() {
        return new FileUploadSecurityInfo(
            MAX_FILE_SIZE,
            new HashSet<>(ALLOWED_MIME_TYPES),
            new HashSet<>(ALLOWED_EXTENSIONS),
            uploadDir
        );
    }

    /**
     * 파일 업로드 보안 정보를 담는 레코드
     */
    public record FileUploadSecurityInfo(
        long maxFileSize,
        Set<String> allowedMimeTypes,
        Set<String> allowedExtensions,
        String uploadDirectory
    ) {}
}
