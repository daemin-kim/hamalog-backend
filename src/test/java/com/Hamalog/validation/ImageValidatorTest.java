package com.Hamalog.validation;

import jakarta.validation.ConstraintValidatorContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@DisplayName("Image Validator Tests")
class ImageValidatorTest {

    private ImageValidator validator;

    @Mock
    private ConstraintValidatorContext context;

    @Mock
    private ConstraintValidatorContext.ConstraintViolationBuilder builder;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        validator = new ImageValidator();

        ValidImage annotation = mock(ValidImage.class);
        when(annotation.maxSize()).thenReturn(5L * 1024 * 1024);
        when(annotation.allowedContentTypes()).thenReturn(
            new String[]{"image/jpeg", "image/png", "image/gif", "image/webp"}
        );

        // Mock 컨텍스트 빌더 체인 설정
        when(context.buildConstraintViolationWithTemplate(anyString())).thenReturn(builder);
        when(builder.addConstraintViolation()).thenReturn(context);

        validator.initialize(annotation);
    }

    @Test
    @DisplayName("빈 파일은 유효함 (선택사항)")
    void testNullFileIsValid() {
        assertTrue(validator.isValid(null, context));
    }

    @Test
    @DisplayName("빈 파일은 유효함 (선택사항)")
    void testEmptyFileIsValid() {
        MultipartFile emptyFile = new MockMultipartFile("file", new byte[0]);
        assertTrue(validator.isValid(emptyFile, context));
    }

    @Test
    @DisplayName("크기 초과 파일 거부")
    void testFileSizeExceedsLimit() {
        byte[] content = new byte[6 * 1024 * 1024];  // 6MB
        MultipartFile largeFile = new MockMultipartFile("file", "test.png", "image/png", content);

        assertFalse(validator.isValid(largeFile, context));
        verify(context).buildConstraintViolationWithTemplate(contains("exceeds maximum"));
    }

    @Test
    @DisplayName("잘못된 파일 타입 거부")
    void testInvalidContentType() {
        MultipartFile invalidFile = new MockMultipartFile("file", "test.txt", "text/plain", "content".getBytes());

        assertFalse(validator.isValid(invalidFile, context));
        verify(context).buildConstraintViolationWithTemplate(contains("Invalid file type"));
    }

    @Test
    @DisplayName("null 파일 타입 거부")
    void testNullContentType() {
        MultipartFile fileWithoutType = new MockMultipartFile("file", "test", null, "content".getBytes());

        assertFalse(validator.isValid(fileWithoutType, context));
    }

    @Test
    @DisplayName("유효한 PNG 파일 승인")
    void testValidPngFile() {
        // PNG 매직 넘버: 89 50 4E 47
        byte[] pngHeader = new byte[]{(byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A};
        byte[] content = new byte[1024];
        System.arraycopy(pngHeader, 0, content, 0, pngHeader.length);

        MultipartFile validFile = new MockMultipartFile("file", "test.png", "image/png", content);

        assertTrue(validator.isValid(validFile, context));
    }

    @Test
    @DisplayName("유효한 JPEG 파일 승인")
    void testValidJpegFile() {
        // JPEG 매직 넘버: FF D8 FF
        byte[] jpegHeader = new byte[]{(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, 0x00, 0x00, 0x00, 0x00, 0x00};
        byte[] content = new byte[1024];
        System.arraycopy(jpegHeader, 0, content, 0, jpegHeader.length);

        MultipartFile validFile = new MockMultipartFile("file", "test.jpg", "image/jpeg", content);

        assertTrue(validator.isValid(validFile, context));
    }

    @Test
    @DisplayName("조작된 파일 헤더 거부")
    void testTamperedFileHeader() {
        // PNG로 표시되었지만 JPEG 헤더
        byte[] tamperedHeader = new byte[]{(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, 0x00, 0x00, 0x00, 0x00, 0x00};
        byte[] content = new byte[1024];
        System.arraycopy(tamperedHeader, 0, content, 0, tamperedHeader.length);

        MultipartFile tamperedFile = new MockMultipartFile("file", "test.png", "image/png", content);

        assertFalse(validator.isValid(tamperedFile, context));
        verify(context).buildConstraintViolationWithTemplate(contains("File header does not match"));
    }
}

