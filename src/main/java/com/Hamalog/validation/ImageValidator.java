package com.Hamalog.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

@Slf4j
public class ImageValidator implements ConstraintValidator<ValidImage, MultipartFile> {

    private long maxSize;
    private List<String> allowedContentTypes;

    @Override
    public void initialize(ValidImage annotation) {
        this.maxSize = annotation.maxSize();
        this.allowedContentTypes = Arrays.asList(annotation.allowedContentTypes());
    }

    @Override
    public boolean isValid(MultipartFile file, ConstraintValidatorContext context) {
        // null 또는 빈 파일은 유효함 (선택사항)
        if (file == null || file.isEmpty()) {
            return true;
        }

        // ✅ 파일 크기 검증
        if (file.getSize() > maxSize) {
            context.disableDefaultConstraintViolation();
            context.buildConstraintViolationWithTemplate(
                String.format("File size exceeds maximum allowed size of %d MB", maxSize / (1024 * 1024))
            ).addConstraintViolation();

            log.warn("[UPLOAD] File size exceeds limit - size: {} bytes, max: {} bytes",
                file.getSize(), maxSize);
            return false;
        }

        // ✅ 파일 타입 검증
        String contentType = file.getContentType();
        if (contentType == null || !allowedContentTypes.contains(contentType)) {
            context.disableDefaultConstraintViolation();
            context.buildConstraintViolationWithTemplate(
                String.format("Invalid file type. Allowed types: %s", String.join(", ", allowedContentTypes))
            ).addConstraintViolation();

            log.warn("[UPLOAD] Invalid content type - type: {}", contentType);
            return false;
        }

        // ✅ 파일 헤더 검증 (Magic Number)
        try {
            if (!isValidImageHeader(file)) {
                context.disableDefaultConstraintViolation();
                context.buildConstraintViolationWithTemplate(
                    "File header does not match its content type"
                ).addConstraintViolation();

                log.warn("[UPLOAD] Invalid file header - possible malicious file");
                return false;
            }
        } catch (IOException e) {
            log.error("[UPLOAD] Error reading file header", e);
            return false;
        }

        return true;
    }

    /**
     * 파일 헤더(Magic Number)로 실제 파일 타입 검증
     */
    private boolean isValidImageHeader(MultipartFile file) throws IOException {
        byte[] header = new byte[8];
        if (file.getInputStream().read(header) != 8) {
            return false;
        }

        // JPEG: FF D8 FF
        if (header[0] == (byte) 0xFF && header[1] == (byte) 0xD8 && header[2] == (byte) 0xFF) {
            return "image/jpeg".equals(file.getContentType());
        }

        // PNG: 89 50 4E 47
        if (header[0] == (byte) 0x89 && header[1] == 0x50 &&
            header[2] == 0x4E && header[3] == 0x47) {
            return "image/png".equals(file.getContentType());
        }

        // GIF: 47 49 46
        if (header[0] == 0x47 && header[1] == 0x49 && header[2] == 0x46) {
            return "image/gif".equals(file.getContentType());
        }

        // WebP: 52 49 46 46 ... 57 45 42 50
        if (header[0] == 0x52 && header[1] == 0x49 &&
            header[2] == 0x46 && header[3] == 0x46) {
            return "image/webp".equals(file.getContentType());
        }

        return false;
    }
}

