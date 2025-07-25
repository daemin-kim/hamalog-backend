package com.Hamalog.service.medication;

import com.Hamalog.exception.file.FileSaveFailException;
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
                throw new FileSaveFailException();
            }
        }

        String fileName = UUID.randomUUID() + "_" + file.getOriginalFilename();
        File dest = new File(uploadPath, fileName);

        try {
            file.transferTo(dest);
        } catch (Exception e) {
            throw new FileSaveFailException();
        }
        return fileName;
    }

}
