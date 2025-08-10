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

    public FileStorageService(@Value("${hamalog.upload.image-dir:C:/Hamalog/medication/images}") String uploadDir) {
        this.uploadDir = uploadDir.endsWith(File.separator) ? uploadDir : uploadDir + File.separator;
    }

    public String save(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new FileSaveFailException();
        }

        String contentType = file.getContentType();
        if (contentType == null || !contentType.toLowerCase().startsWith("image/")) {
            throw new FileSaveFailException();
        }
        long maxSizeBytes = 5L * 1024 * 1024;
        if (file.getSize() > maxSizeBytes) {
            throw new FileSaveFailException();
        }

        String originalFilename = file.getOriginalFilename();
        String extension = "";

        if (originalFilename != null && originalFilename.lastIndexOf('.') != -1) {
            extension = originalFilename.substring(originalFilename.lastIndexOf('.'));
        }

        String fileName = UUID.randomUUID() + extension;
        Path savePath = Paths.get(uploadDir);

        File directory = savePath.toFile();
        if (!directory.exists() && !directory.mkdirs()) {
            throw new FileSaveFailException();
        }

        File dest = new File(directory, fileName);
        try {
            file.transferTo(dest);
        } catch (Exception e) {
            throw new FileSaveFailException();
        }
        return fileName;
    }
}
