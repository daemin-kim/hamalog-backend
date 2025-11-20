package com.Hamalog.service.medication;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

@Slf4j
@Service
public class SecureFileStorageService {

    @Value("${file.upload.directory:/uploads/medication-images}")
    private String uploadDirectory;

    /**
     * 안전한 파일 저장
     */
    public String saveFile(MultipartFile file, Long medicationScheduleId) {
        if (file == null || file.isEmpty()) {
            return null;
        }

        try {
            // ✅ 원본 파일명 제거, 무작위 이름 생성
            String filename = generateSecureFilename(file);

            // ✅ 디렉토리 경로 조작 방지
            Path uploadPath = Paths.get(uploadDirectory)
                .resolve(medicationScheduleId.toString())
                .normalize();

            if (!uploadPath.startsWith(Paths.get(uploadDirectory).normalize())) {
                throw new SecurityException("Invalid upload path");
            }

            Files.createDirectories(uploadPath);

            Path filePath = uploadPath.resolve(filename);
            file.transferTo(filePath.toFile());

            // ✅ 파일 권한 설정 (소유자만 읽기/쓰기)
            filePath.toFile().setReadable(true, true);
            filePath.toFile().setWritable(true, true);

            log.info("[FILE] Image saved securely - schedule: {}, filename: {}",
                medicationScheduleId, filename);

            return filePath.toString();

        } catch (Exception e) {
            log.error("[FILE] Failed to save file - schedule: {}", medicationScheduleId, e);
            throw new RuntimeException("Failed to save file", e);
        }
    }

    /**
     * 안전한 파일명 생성
     */
    private String generateSecureFilename(MultipartFile file) {
        String originalExtension = getFileExtension(file.getOriginalFilename());
        return UUID.randomUUID().toString() + "." + originalExtension;
    }

    /**
     * 안전한 파일 확장자 추출
     */
    private String getFileExtension(String filename) {
        if (filename == null || !filename.contains(".")) {
            return "jpg";
        }

        String extension = filename.substring(filename.lastIndexOf(".") + 1)
            .toLowerCase();

        // ✅ 허용된 확장자만
        if (!extension.matches("^(jpg|jpeg|png|gif|webp)$")) {
            return "jpg";
        }

        return extension;
    }

    /**
     * 파일 삭제
     */
    public void deleteFile(String filePath) {
        try {
            File file = new File(filePath);
            if (file.exists() && !file.delete()) {
                log.warn("[FILE] Failed to delete file: {}", filePath);
            }
        } catch (Exception e) {
            log.error("[FILE] Error deleting file: {}", filePath, e);
        }
    }
}

