package com.Hamalog.service.medication;

import com.Hamalog.exception.file.FileSaveFailException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("FileStorageService Tests")
class FileStorageServiceTest {

    @Mock
    private MultipartFile mockFile;

    private FileStorageService fileStorageService;

    @TempDir
    Path tempDir;

    @BeforeEach
    void setUp() {
        fileStorageService = new FileStorageService(tempDir.toString());
    }

    @Test
    @DisplayName("Should throw FileSaveFailException when file is null")
    void save_NullFile_ThrowsException() {
        // when & then
        assertThatThrownBy(() -> fileStorageService.save(null))
                .isInstanceOf(FileSaveFailException.class);
    }

    @Test
    @DisplayName("Should throw FileSaveFailException when file is empty")
    void save_EmptyFile_ThrowsException() {
        // given
        when(mockFile.isEmpty()).thenReturn(true);

        // when & then
        assertThatThrownBy(() -> fileStorageService.save(mockFile))
                .isInstanceOf(FileSaveFailException.class);
    }

    @Test
    @DisplayName("Should throw FileSaveFailException when filename is null")
    void save_NullFilename_ThrowsException() {
        // given
        when(mockFile.isEmpty()).thenReturn(false);
        when(mockFile.getOriginalFilename()).thenReturn(null);

        // when & then
        assertThatThrownBy(() -> fileStorageService.save(mockFile))
                .isInstanceOf(FileSaveFailException.class);
    }

    @Test
    @DisplayName("Should throw FileSaveFailException when filename is empty")
    void save_EmptyFilename_ThrowsException() {
        // given
        when(mockFile.isEmpty()).thenReturn(false);
        when(mockFile.getOriginalFilename()).thenReturn("");

        // when & then
        assertThatThrownBy(() -> fileStorageService.save(mockFile))
                .isInstanceOf(FileSaveFailException.class);
    }

    @Test
    @DisplayName("Should throw FileSaveFailException when file size exceeds limit")
    void save_FileTooLarge_ThrowsException() {
        // given
        long maxFileSize = 5L * 1024 * 1024; // 5MB
        when(mockFile.isEmpty()).thenReturn(false);
        when(mockFile.getOriginalFilename()).thenReturn("test.jpg");
        when(mockFile.getSize()).thenReturn(maxFileSize + 1);

        // when & then
        assertThatThrownBy(() -> fileStorageService.save(mockFile))
                .isInstanceOf(FileSaveFailException.class);
    }

    @Test
    @DisplayName("Should throw FileSaveFailException when MIME type is null")
    void save_NullMimeType_ThrowsException() {
        // given
        when(mockFile.isEmpty()).thenReturn(false);
        when(mockFile.getOriginalFilename()).thenReturn("test.jpg");
        when(mockFile.getSize()).thenReturn(1024L);
        when(mockFile.getContentType()).thenReturn(null);

        // when & then
        assertThatThrownBy(() -> fileStorageService.save(mockFile))
                .isInstanceOf(FileSaveFailException.class);
    }

    @Test
    @DisplayName("Should throw FileSaveFailException when MIME type is not allowed")
    void save_DisallowedMimeType_ThrowsException() {
        // given
        when(mockFile.isEmpty()).thenReturn(false);
        when(mockFile.getOriginalFilename()).thenReturn("test.exe");
        when(mockFile.getSize()).thenReturn(1024L);
        when(mockFile.getContentType()).thenReturn("application/exe");

        // when & then
        assertThatThrownBy(() -> fileStorageService.save(mockFile))
                .isInstanceOf(FileSaveFailException.class);
    }

    @Test
    @DisplayName("Should throw FileSaveFailException when file extension is not allowed")
    void save_DisallowedExtension_ThrowsException() {
        // given
        when(mockFile.isEmpty()).thenReturn(false);
        when(mockFile.getOriginalFilename()).thenReturn("test.exe");
        when(mockFile.getSize()).thenReturn(1024L);
        when(mockFile.getContentType()).thenReturn("image/jpeg");

        // when & then
        assertThatThrownBy(() -> fileStorageService.save(mockFile))
                .isInstanceOf(FileSaveFailException.class);
    }

    @Test
    @DisplayName("Should throw FileSaveFailException when filename has no extension")
    void save_NoExtension_ThrowsException() {
        // given
        when(mockFile.isEmpty()).thenReturn(false);
        when(mockFile.getOriginalFilename()).thenReturn("testfile");
        when(mockFile.getSize()).thenReturn(1024L);
        when(mockFile.getContentType()).thenReturn("image/jpeg");

        // when & then
        assertThatThrownBy(() -> fileStorageService.save(mockFile))
                .isInstanceOf(FileSaveFailException.class);
    }

    @Test
    @DisplayName("Should throw FileSaveFailException when file signature is invalid for JPEG")
    void save_InvalidJpegSignature_ThrowsException() throws IOException {
        // given
        byte[] invalidJpegHeader = {(byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00};
        
        when(mockFile.isEmpty()).thenReturn(false);
        when(mockFile.getOriginalFilename()).thenReturn("test.jpg");
        when(mockFile.getSize()).thenReturn(1024L);
        when(mockFile.getContentType()).thenReturn("image/jpeg");
        when(mockFile.getInputStream()).thenReturn(new ByteArrayInputStream(invalidJpegHeader));

        // when & then
        assertThatThrownBy(() -> fileStorageService.save(mockFile))
                .isInstanceOf(FileSaveFailException.class);
    }

    @Test
    @DisplayName("Should throw FileSaveFailException when file signature is invalid for PNG")
    void save_InvalidPngSignature_ThrowsException() throws IOException {
        // given
        byte[] invalidPngHeader = {(byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00};
        
        when(mockFile.isEmpty()).thenReturn(false);
        when(mockFile.getOriginalFilename()).thenReturn("test.png");
        when(mockFile.getSize()).thenReturn(1024L);
        when(mockFile.getContentType()).thenReturn("image/png");
        when(mockFile.getInputStream()).thenReturn(new ByteArrayInputStream(invalidPngHeader));

        // when & then
        assertThatThrownBy(() -> fileStorageService.save(mockFile))
                .isInstanceOf(FileSaveFailException.class);
    }

    @Test
    @DisplayName("Should throw FileSaveFailException when file is too small for signature validation")
    void save_FileTooSmallForSignature_ThrowsException() throws IOException {
        // given
        byte[] tooSmallFile = {(byte) 0xFF, (byte) 0xD8}; // Only 2 bytes
        
        when(mockFile.isEmpty()).thenReturn(false);
        when(mockFile.getOriginalFilename()).thenReturn("test.jpg");
        when(mockFile.getSize()).thenReturn(2L);
        when(mockFile.getContentType()).thenReturn("image/jpeg");
        when(mockFile.getInputStream()).thenReturn(new ByteArrayInputStream(tooSmallFile));

        // when & then
        assertThatThrownBy(() -> fileStorageService.save(mockFile))
                .isInstanceOf(FileSaveFailException.class);
    }

    @Test
    @DisplayName("Should throw FileSaveFailException when malicious EXE signature is detected")
    void save_MaliciousExeSignature_ThrowsException() throws IOException {
        // given
        byte[] maliciousExeHeader = {(byte) 0x4D, (byte) 0x5A, (byte) 0x00, (byte) 0x00}; // MZ header
        
        when(mockFile.isEmpty()).thenReturn(false);
        when(mockFile.getOriginalFilename()).thenReturn("test.jpg");
        when(mockFile.getSize()).thenReturn(1024L);
        when(mockFile.getContentType()).thenReturn("image/jpeg");
        when(mockFile.getInputStream())
                .thenReturn(new ByteArrayInputStream(maliciousExeHeader))
                .thenReturn(new ByteArrayInputStream(maliciousExeHeader));

        // when & then
        assertThatThrownBy(() -> fileStorageService.save(mockFile))
                .isInstanceOf(FileSaveFailException.class);
    }

    @Test
    @DisplayName("Should throw FileSaveFailException when malicious ZIP signature is detected")
    void save_MaliciousZipSignature_ThrowsException() throws IOException {
        // given
        byte[] maliciousZipHeader = {(byte) 0x50, (byte) 0x4B, (byte) 0x03, (byte) 0x04}; // PK header
        
        when(mockFile.isEmpty()).thenReturn(false);
        when(mockFile.getOriginalFilename()).thenReturn("test.jpg");
        when(mockFile.getSize()).thenReturn(1024L);
        when(mockFile.getContentType()).thenReturn("image/jpeg");
        when(mockFile.getInputStream())
                .thenReturn(new ByteArrayInputStream(maliciousZipHeader))
                .thenReturn(new ByteArrayInputStream(maliciousZipHeader));

        // when & then
        assertThatThrownBy(() -> fileStorageService.save(mockFile))
                .isInstanceOf(FileSaveFailException.class);
    }

    @Test
    @DisplayName("Should throw FileSaveFailException when IOException occurs during signature validation")
    void save_IOExceptionDuringSignatureValidation_ThrowsException() throws IOException {
        // given
        when(mockFile.isEmpty()).thenReturn(false);
        when(mockFile.getOriginalFilename()).thenReturn("test.jpg");
        when(mockFile.getSize()).thenReturn(1024L);
        when(mockFile.getContentType()).thenReturn("image/jpeg");
        when(mockFile.getInputStream()).thenThrow(new IOException("Test IO exception"));

        // when & then
        assertThatThrownBy(() -> fileStorageService.save(mockFile))
                .isInstanceOf(FileSaveFailException.class);
    }

    @Test
    @DisplayName("Should throw FileSaveFailException when IOException occurs during malicious scan")
    void save_IOExceptionDuringMaliciousScan_ThrowsException() throws IOException {
        // given
        byte[] validJpegHeader = {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xE0};
        
        when(mockFile.isEmpty()).thenReturn(false);
        when(mockFile.getOriginalFilename()).thenReturn("test.jpg");
        when(mockFile.getSize()).thenReturn(1024L);
        when(mockFile.getContentType()).thenReturn("image/jpeg");
        when(mockFile.getInputStream())
                .thenReturn(new ByteArrayInputStream(validJpegHeader))
                .thenThrow(new IOException("Test IO exception"));

        // when & then
        assertThatThrownBy(() -> fileStorageService.save(mockFile))
                .isInstanceOf(FileSaveFailException.class);
    }

    @Test
    @DisplayName("Should successfully save valid JPEG file")
    void save_ValidJpegFile_Success() throws IOException {
        // given
        byte[] validJpegHeader = {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xE0, 
                                 (byte) 0x00, (byte) 0x10, (byte) 0x4A, (byte) 0x46};
        
        when(mockFile.isEmpty()).thenReturn(false);
        when(mockFile.getOriginalFilename()).thenReturn("test.jpg");
        when(mockFile.getSize()).thenReturn(1024L);
        when(mockFile.getContentType()).thenReturn("image/jpeg");
        when(mockFile.getInputStream())
                .thenReturn(new ByteArrayInputStream(validJpegHeader))
                .thenReturn(new ByteArrayInputStream(validJpegHeader));
        doNothing().when(mockFile).transferTo(any(File.class));

        // when
        String savedFileName = fileStorageService.save(mockFile);

        // then
        assertThat(savedFileName).isNotNull();
        assertThat(savedFileName).endsWith(".jpg");
        assertThat(savedFileName).hasSize(40); // UUID (36) + ".jpg" (4)
        
        verify(mockFile).transferTo(any(File.class));
    }

    @Test
    @DisplayName("Should successfully save valid PNG file")
    void save_ValidPngFile_Success() throws IOException {
        // given
        byte[] validPngHeader = {(byte) 0x89, (byte) 0x50, (byte) 0x4E, (byte) 0x47, 
                                (byte) 0x0D, (byte) 0x0A, (byte) 0x1A, (byte) 0x0A};
        
        when(mockFile.isEmpty()).thenReturn(false);
        when(mockFile.getOriginalFilename()).thenReturn("test.png");
        when(mockFile.getSize()).thenReturn(1024L);
        when(mockFile.getContentType()).thenReturn("image/png");
        when(mockFile.getInputStream())
                .thenReturn(new ByteArrayInputStream(validPngHeader))
                .thenReturn(new ByteArrayInputStream(validPngHeader));
        doNothing().when(mockFile).transferTo(any(File.class));

        // when
        String savedFileName = fileStorageService.save(mockFile);

        // then
        assertThat(savedFileName).isNotNull();
        assertThat(savedFileName).endsWith(".png");
        assertThat(savedFileName).hasSize(40); // UUID (36) + ".png" (4)
        
        verify(mockFile).transferTo(any(File.class));
    }

    @Test
    @DisplayName("Should successfully save valid GIF file")
    void save_ValidGifFile_Success() throws IOException {
        // given
        byte[] validGifHeader = {(byte) 0x47, (byte) 0x49, (byte) 0x46, (byte) 0x38, 
                                (byte) 0x37, (byte) 0x61}; // GIF87a
        
        when(mockFile.isEmpty()).thenReturn(false);
        when(mockFile.getOriginalFilename()).thenReturn("test.gif");
        when(mockFile.getSize()).thenReturn(1024L);
        when(mockFile.getContentType()).thenReturn("image/gif");
        when(mockFile.getInputStream())
                .thenReturn(new ByteArrayInputStream(validGifHeader))
                .thenReturn(new ByteArrayInputStream(validGifHeader));
        doNothing().when(mockFile).transferTo(any(File.class));

        // when
        String savedFileName = fileStorageService.save(mockFile);

        // then
        assertThat(savedFileName).isNotNull();
        assertThat(savedFileName).endsWith(".gif");
        assertThat(savedFileName).hasSize(40); // UUID (36) + ".gif" (4)
        
        verify(mockFile).transferTo(any(File.class));
    }

    @Test
    @DisplayName("Should successfully save valid WebP file")
    void save_ValidWebPFile_Success() throws IOException {
        // given
        byte[] validWebPHeader = {(byte) 0x52, (byte) 0x49, (byte) 0x46, (byte) 0x46, 
                                 (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00};
        
        when(mockFile.isEmpty()).thenReturn(false);
        when(mockFile.getOriginalFilename()).thenReturn("test.webp");
        when(mockFile.getSize()).thenReturn(1024L);
        when(mockFile.getContentType()).thenReturn("image/webp");
        when(mockFile.getInputStream())
                .thenReturn(new ByteArrayInputStream(validWebPHeader))
                .thenReturn(new ByteArrayInputStream(validWebPHeader));
        doNothing().when(mockFile).transferTo(any(File.class));

        // when
        String savedFileName = fileStorageService.save(mockFile);

        // then
        assertThat(savedFileName).isNotNull();
        assertThat(savedFileName).endsWith(".webp");
        assertThat(savedFileName).hasSize(41); // UUID (36) + ".webp" (5)
        
        verify(mockFile).transferTo(any(File.class));
    }

    @Test
    @DisplayName("Should handle case-insensitive MIME types")
    void save_CaseInsensitiveMimeType_Success() throws IOException {
        // given
        byte[] validJpegHeader = {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xE0};
        
        when(mockFile.isEmpty()).thenReturn(false);
        when(mockFile.getOriginalFilename()).thenReturn("test.jpg");
        when(mockFile.getSize()).thenReturn(1024L);
        when(mockFile.getContentType()).thenReturn("IMAGE/JPEG"); // Uppercase
        when(mockFile.getInputStream())
                .thenReturn(new ByteArrayInputStream(validJpegHeader))
                .thenReturn(new ByteArrayInputStream(validJpegHeader));
        doNothing().when(mockFile).transferTo(any(File.class));

        // when
        String savedFileName = fileStorageService.save(mockFile);

        // then
        assertThat(savedFileName).isNotNull();
        assertThat(savedFileName).endsWith(".jpg");
        
        verify(mockFile).transferTo(any(File.class));
    }

    @Test
    @DisplayName("Should handle case-insensitive file extensions")
    void save_CaseInsensitiveExtension_Success() throws IOException {
        // given
        byte[] validJpegHeader = {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xE0};
        
        when(mockFile.isEmpty()).thenReturn(false);
        when(mockFile.getOriginalFilename()).thenReturn("test.JPG"); // Uppercase
        when(mockFile.getSize()).thenReturn(1024L);
        when(mockFile.getContentType()).thenReturn("image/jpeg");
        when(mockFile.getInputStream())
                .thenReturn(new ByteArrayInputStream(validJpegHeader))
                .thenReturn(new ByteArrayInputStream(validJpegHeader));
        doNothing().when(mockFile).transferTo(any(File.class));

        // when
        String savedFileName = fileStorageService.save(mockFile);

        // then
        assertThat(savedFileName).isNotNull();
        assertThat(savedFileName).endsWith(".jpg"); // Should be normalized to lowercase
        
        verify(mockFile).transferTo(any(File.class));
    }

    @Test
    @DisplayName("Should throw FileSaveFailException when file transfer fails")
    void save_FileTransferFails_ThrowsException() throws IOException {
        // given
        byte[] validJpegHeader = {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xE0};
        
        when(mockFile.isEmpty()).thenReturn(false);
        when(mockFile.getOriginalFilename()).thenReturn("test.jpg");
        when(mockFile.getSize()).thenReturn(1024L);
        when(mockFile.getContentType()).thenReturn("image/jpeg");
        when(mockFile.getInputStream())
                .thenReturn(new ByteArrayInputStream(validJpegHeader))
                .thenReturn(new ByteArrayInputStream(validJpegHeader));
        doThrow(new IOException("Transfer failed")).when(mockFile).transferTo(any(File.class));

        // when & then
        assertThatThrownBy(() -> fileStorageService.save(mockFile))
                .isInstanceOf(FileSaveFailException.class);
    }

    @Test
    @DisplayName("Should return correct security info")
    void getSecurityInfo_ReturnsCorrectInfo() {
        // when
        FileStorageService.FileUploadSecurityInfo securityInfo = fileStorageService.getSecurityInfo();

        // then
        assertThat(securityInfo.maxFileSize()).isEqualTo(5L * 1024 * 1024);
        assertThat(securityInfo.allowedMimeTypes()).containsExactlyInAnyOrder(
                "image/jpeg", "image/jpg", "image/png", "image/gif", "image/webp");
        assertThat(securityInfo.allowedExtensions()).containsExactlyInAnyOrder(
                ".jpg", ".jpeg", ".png", ".gif", ".webp");
        assertThat(securityInfo.uploadDirectory()).isEqualTo(tempDir.toString() + "/");
    }

    @Test
    @DisplayName("Should validate file at maximum allowed size")
    void save_FileAtMaxSize_Success() throws IOException {
        // given
        long maxFileSize = 5L * 1024 * 1024; // 5MB
        byte[] validJpegHeader = {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xE0};
        
        when(mockFile.isEmpty()).thenReturn(false);
        when(mockFile.getOriginalFilename()).thenReturn("test.jpg");
        when(mockFile.getSize()).thenReturn(maxFileSize);
        when(mockFile.getContentType()).thenReturn("image/jpeg");
        when(mockFile.getInputStream())
                .thenReturn(new ByteArrayInputStream(validJpegHeader))
                .thenReturn(new ByteArrayInputStream(validJpegHeader));
        doNothing().when(mockFile).transferTo(any(File.class));

        // when
        String savedFileName = fileStorageService.save(mockFile);

        // then
        assertThat(savedFileName).isNotNull();
        assertThat(savedFileName).endsWith(".jpg");
    }
}