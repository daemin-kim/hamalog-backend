//package com.Hamalog.service;
//
//import com.Hamalog.exception.file.FileSaveFailException;
//import com.Hamalog.service.medication.FileStorageService;
//import org.junit.jupiter.api.*;
//import org.springframework.mock.web.MockMultipartFile;
//
//import java.io.File;
//import java.io.IOException;
//import java.nio.file.Files;
//
//import static org.assertj.core.api.Assertions.*;
//
//class FileStorageServiceTest {
//
//    private static String tempDir;
//    private FileStorageService fileStorageService;
//
//    @BeforeAll
//    static void setupAll() throws IOException {
//        // JUnit이 제공하는 임시 디렉토리 사용(테스트 중 생성/삭제)
//        tempDir = Files.createTempDirectory("hamalog_test_img").toFile().getAbsolutePath() + File.separator;
//    }
//
//    @BeforeEach
//    void setUp() {
//        // 테스트마다 uploadDir만 바꿔서 새로운 FileStorageService 생성
//        fileStorageService = new FileStorageService() {
//            @Override
//            public String save(org.springframework.web.multipart.MultipartFile file) {
//                // uploadDir 필드를 오버라이딩 (이렇게 하면 따로 주입 필요 없음)
//                try {
//                    java.lang.reflect.Field uploadDirField =
//                        FileStorageService.class.getDeclaredField("uploadDir");
//                    uploadDirField.setAccessible(true);
//                    uploadDirField.set(null, tempDir);
//                } catch (Exception e) { throw new RuntimeException(e); }
//                return super.save(file);
//            }
//        };
//    }
//
//    @Test
//    @DisplayName("정상적으로 파일 저장 시 파일명이 반환되고, 실제 파일이 생성된다")
//    void save_success() throws IOException {
//        // given
//        byte[] content = "테스트 데이터".getBytes();
//        MockMultipartFile multipartFile =
//            new MockMultipartFile("file", "testfile.txt", "text/plain", content);
//
//        // when
//        String savedFileName = fileStorageService.save(multipartFile);
//
//        // then
//        assertThat(savedFileName).contains("testfile.txt");
//        File savedFile = new File(tempDir + savedFileName);
//        assertThat(savedFile.exists()).isTrue();
//        assertThat(Files.readAllBytes(savedFile.toPath())).isEqualTo(content);
//
//        // 정리
//        savedFile.delete();
//    }
//
//    @Test
//    @DisplayName("폴더 생성 실패 시 예외 발생")
//    void save_fail_to_create_directory() {
//        // given
//        // 존재할 수 없는 경로를 강제로 넣음(파일로 디렉토리를 만들게 하여 실패 유도)
//        String impossibleDir = tempDir + "file.txt";
//        FileStorageService badService = new FileStorageService() {
//            @Override
//            public String save(org.springframework.web.multipart.MultipartFile file) {
//                try {
//                    java.lang.reflect.Field uploadDirField =
//                        FileStorageService.class.getDeclaredField("uploadDir");
//                    uploadDirField.setAccessible(true);
//                    uploadDirField.set(null, impossibleDir);
//                } catch (Exception e) { throw new RuntimeException(e); }
//                return super.save(file);
//            }
//        };
//
//        MockMultipartFile multipartFile =
//            new MockMultipartFile("file", "test.txt", "text/plain", "abc".getBytes());
//
//        // when & then
//        assertThatThrownBy(() -> badService.save(multipartFile))
//            .isInstanceOf(FileSaveFailException.class);
//    }
//
//    @Test
//    @DisplayName("파일 저장 실패 시 예외 발생")
//    void save_fail_to_save_file() {
//        // given
//        // transferTo에서 예외 발생시키는 Mock 사용
//        MockMultipartFile multipartFile = new MockMultipartFile("file", "file.txt", "text/plain", new byte[0]) {
//            @Override
//            public void transferTo(File file) throws IOException {
//                throw new IOException("강제 실패");
//            }
//        };
//        // when & then
//        assertThatThrownBy(() -> fileStorageService.save(multipartFile))
//            .isInstanceOf(FileSaveFailException.class);
//    }
//}
