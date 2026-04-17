package com.vn.backend;

import com.vn.backend.configs.PropertiesConfig;
import com.vn.backend.constants.AppConst;
import com.vn.backend.entities.FileStorage;
import com.vn.backend.entities.User;
import com.vn.backend.enums.Role;
import com.vn.backend.exceptions.AppException;
import com.vn.backend.repositories.FileStorageRepository;
import com.vn.backend.services.AuthService;
import com.vn.backend.services.impl.FileServiceImpl;
import com.vn.backend.utils.MessageUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("FileServiceImpl Unit Tests")
class FileServiceImplTest {

    @Mock
    private PropertiesConfig propertiesConfig;

    @Mock
    private AuthService authService;

    @Mock
    private FileStorageRepository fileStorageRepository;

    @Mock
    private MessageUtils messageUtils;

    @InjectMocks
    private FileServiceImpl fileService;

    @TempDir
    Path tempDir;

    private User currentUser;

    @BeforeEach
    void setUp() {
        currentUser = User.builder()
                .id(1L)
                .username("testuser")
                .role(Role.STUDENT)
                .build();
    }

    // ===================== handleUploadFiles =====================

    @Test
    @DisplayName("handleUploadFiles - ném exception khi file trống")
    void handleUploadFiles_ThrowsException_WhenFileIsEmpty() {
        when(authService.getCurrentUser()).thenReturn(currentUser);
        MultipartFile emptyFile = new MockMultipartFile("file", "test.txt", "text/plain", new byte[0]);
        when(messageUtils.getMessage(AppConst.MessageConst.FILE_IS_EMPTY)).thenReturn("File is empty");

        assertThatThrownBy(() -> fileService.handleUploadFiles(emptyFile))
                .isInstanceOf(AppException.class)
                .satisfies(ex -> {
                    AppException appEx = (AppException) ex;
                    assertThat(appEx.getCode()).isEqualTo(AppConst.MessageConst.FILE_IS_EMPTY);
                    assertThat(appEx.getHttpStatus()).isEqualTo(HttpStatus.BAD_REQUEST);
                });
    }

    @Test
    @DisplayName("handleUploadFiles - ném exception khi đuôi file không hợp lệ")
    void handleUploadFiles_ThrowsException_WhenInvalidExtension() {
        when(authService.getCurrentUser()).thenReturn(currentUser);
        MultipartFile invalidFile = new MockMultipartFile("file", "test.exe", "application/x-msdownload", "print('hello')".getBytes());
        when(messageUtils.getMessage(AppConst.MessageConst.FILE_TYPE_NOT_ALLOWED)).thenReturn("File type not allowed");

        assertThatThrownBy(() -> fileService.handleUploadFiles(invalidFile))
                .isInstanceOf(AppException.class)
                .satisfies(ex -> {
                    AppException appEx = (AppException) ex;
                    assertThat(appEx.getCode()).isEqualTo(AppConst.MessageConst.FILE_TYPE_NOT_ALLOWED);
                });
    }

    @Test
    @DisplayName("handleUploadFiles - thành công với thẻ ảnh")
    void handleUploadFiles_Success_WithImageFile() throws IOException {
        when(authService.getCurrentUser()).thenReturn(currentUser);
        MockMultipartFile validFile = new MockMultipartFile("file", "test.png", "image/png", "dummy content".getBytes());
        when(propertiesConfig.getOfficePath()).thenReturn(tempDir.toString());

        when(fileStorageRepository.save(any(FileStorage.class))).thenAnswer(inv -> inv.getArgument(0));

        String url = fileService.handleUploadFiles(validFile);

        assertThat(url).contains(AppConst.ENDPOINT_DOWNLOAD_FILE);
        assertThat(url).endsWith(".png");
        verify(fileStorageRepository).save(any(FileStorage.class));
    }

    // ===================== downloadFile =====================

    @Test
    @DisplayName("downloadFile - ném exception khi tên file không tồn tại trong DB")
    void downloadFile_ThrowsException_WhenFileNameNotFoundInDB() {
        when(fileStorageRepository.findByFileName("nonexistent.png")).thenReturn(Optional.empty());
        when(messageUtils.getMessage(AppConst.MessageConst.NOT_FOUND)).thenReturn("Not found");

        assertThatThrownBy(() -> fileService.downloadFile("nonexistent.png"))
                .isInstanceOf(AppException.class)
                .satisfies(ex -> {
                    AppException appEx = (AppException) ex;
                    assertThat(appEx.getCode()).isEqualTo(AppConst.MessageConst.NOT_FOUND);
                });
    }

    @Test
    @DisplayName("downloadFile - ném exception khi file vật lý không tồn tại")
    void downloadFile_ThrowsException_WhenPhysicalFileNotFound() {
        FileStorage storage = FileStorage.builder()
                .fileName("test.png")
                .storagePath(tempDir.toString()) // File vật lý chưa được tạo
                .build();
        when(fileStorageRepository.findByFileName("test.png")).thenReturn(Optional.of(storage));
        when(messageUtils.getMessage(AppConst.MessageConst.NOT_FOUND)).thenReturn("Not found");

        assertThatThrownBy(() -> fileService.downloadFile("test.png"))
                .isInstanceOf(AppException.class)
                .satisfies(ex -> {
                    AppException appEx = (AppException) ex;
                    assertThat(appEx.getCode()).isEqualTo(AppConst.MessageConst.NOT_FOUND);
                });
    }

    @Test
    @DisplayName("downloadFile - thành công khi file vật lý tồn tại")
    void downloadFile_Success() throws IOException {
        File physicalFile = tempDir.resolve("actual_file.png").toFile();
        Files.write(physicalFile.toPath(), "dummy content".getBytes());

        FileStorage storage = FileStorage.builder()
                .fileName("actual_file.png")
                .storagePath(tempDir.toString())
                .build();
        when(fileStorageRepository.findByFileName("actual_file.png")).thenReturn(Optional.of(storage));

        Resource resource = fileService.downloadFile("actual_file.png");

        assertThat(resource).isNotNull();
        assertThat(resource.exists()).isTrue();
        assertThat(resource.getFilename()).isEqualTo("actual_file.png");
    }
}
