package com.Hamalog.service.medication;

import com.Hamalog.exception.file.FileSaveFailException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

@Service
public class FileStorageService {

    private final String uploadDir;

    // Spring의 @Value를 통해 경로 주입 가능 (application.properties에서 관리 추천)
    public FileStorageService(@Value("${hamalog.upload.image-dir:C:/Hamalog/medication/images}") String uploadDir) {
        this.uploadDir = uploadDir.endsWith(File.separator) ? uploadDir : uploadDir + File.separator;
    }

    public String save(MultipartFile file) {
        // 파일 null/empty 체크
        if (file == null || file.isEmpty()) {
            throw new FileSaveFailException();
        }

        // 간단한 콘텐츠 타입/크기 검증 (이미지 전용, 최대 5MB)
        String contentType = file.getContentType();
        if (contentType == null || !contentType.toLowerCase().startsWith("image/")) {
            throw new FileSaveFailException();
        }
        long maxSizeBytes = 5L * 1024 * 1024;
        if (file.getSize() > maxSizeBytes) {
            throw new FileSaveFailException();
        }

        // 원본 파일명 추출 및 확장자 추출
        String originalFilename = file.getOriginalFilename();
        String extension = "";

        if (originalFilename != null && originalFilename.lastIndexOf('.') != -1) {
            extension = originalFilename.substring(originalFilename.lastIndexOf('.'));
        }

        // 고유 파일명 생성 (확장자 유지)
        String fileName = UUID.randomUUID() + extension;
        Path savePath = Paths.get(uploadDir);

        // 디렉토리 없으면 생성
        File directory = savePath.toFile();
        if (!directory.exists() && !directory.mkdirs()) {
            throw new FileSaveFailException();
        }

        // 실제 저장
        File dest = new File(directory, fileName);
        try {
            file.transferTo(dest);
        } catch (Exception e) {
            throw new FileSaveFailException();
        }
        return fileName;
    }
}
