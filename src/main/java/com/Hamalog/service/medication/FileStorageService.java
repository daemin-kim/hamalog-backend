package com.Hamalog.service.medication;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.util.UUID;

@Service
public class FileStorageService {

    private static final String uploadDir = "C:/Hamalog/medication/images";

    public String save(MultipartFile file) {
        File uploadPath = new File(uploadDir);
        if (!uploadPath.exists()) {
            boolean made = uploadPath.mkdirs(); // 디렉토리 생성 시도
            if (!made) {
                throw new RuntimeException("파일 저장 경로 생성에 실패했습니다: " + uploadDir);
            }
        }

        String fileName = UUID.randomUUID() + "_" + file.getOriginalFilename();
        File dest = new File(uploadPath, fileName);

        try {
            file.transferTo(dest);
        } catch (Exception e) {
            throw new RuntimeException("파일 저장에 실패했습니다.", e);
        }
        return fileName;
    }

}
