package com.Hamalog.service.medication;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.util.UUID;

@Service
public class FileStorageService {

    private static final String uploadDir = "C:/Hamalog/medication/images";

    public String save(MultipartFile file) {
        String fileName = UUID.randomUUID() + "_" + file.getOriginalFilename();
        File dest = new File(uploadDir + fileName);
        try {
            file.transferTo(dest);
        } catch (Exception e) {
            throw new RuntimeException("파일 저장에 실패했습니다.", e);
        }
        return fileName;
    }
}
