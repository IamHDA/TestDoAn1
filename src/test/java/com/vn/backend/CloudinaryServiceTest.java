package com.vn.backend;

import com.cloudinary.Cloudinary;
import com.cloudinary.Uploader;
import com.vn.backend.services.CloudinaryService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("CloudinaryService Unit Tests")
class CloudinaryServiceTest {

    @Mock
    private Cloudinary cloudinary;

    @Mock
    private Uploader uploader;

    @Mock
    private MultipartFile multipartFile;

    @InjectMocks
    private CloudinaryService cloudinaryService;

    @BeforeEach
    void setUp() {
        // Since cloudinary.uploader() is a method call, we need to stub its behavior
        lenient().when(cloudinary.uploader()).thenReturn(uploader);
    }

    @Test
    @DisplayName("uploadImage - tải ảnh lên thành công và trả về URL")
    void uploadImage_Success() throws IOException {
        byte[] fileBytes = "test image content".getBytes();
        Map<String, Object> uploadResult = new HashMap<>();
        uploadResult.put("secure_url", "https://res.cloudinary.com/demo/image/upload/sample.jpg");

        when(multipartFile.getBytes()).thenReturn(fileBytes);
        when(uploader.upload(eq(fileBytes), anyMap())).thenReturn(uploadResult);

        String result = cloudinaryService.uploadImage(multipartFile);

        assertThat(result).isEqualTo("https://res.cloudinary.com/demo/image/upload/sample.jpg");
        verify(uploader).upload(eq(fileBytes), anyMap());
    }

    @Test
    @DisplayName("uploadImage - ném ngoại lệ khi getBytes lỗi")
    void uploadImage_GetBytesError_ThrowsIOException() throws IOException {
        when(multipartFile.getBytes()).thenThrow(new IOException("Read error"));

        assertThatThrownBy(() -> cloudinaryService.uploadImage(multipartFile))
                .isInstanceOf(IOException.class)
                .hasMessage("Read error");
    }

    @Test
    @DisplayName("uploadImage - xử lý khi secure_url bị null hoặc lỗi kết quả trả về")
    void uploadImage_NullSecureUrl_ThrowsException() throws IOException {
        byte[] fileBytes = "test".getBytes();
        Map<String, Object> uploadResult = new HashMap<>(); // Empty map, no secure_url

        when(multipartFile.getBytes()).thenReturn(fileBytes);
        when(uploader.upload(eq(fileBytes), anyMap())).thenReturn(uploadResult);

        assertThatThrownBy(() -> cloudinaryService.uploadImage(multipartFile))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    @DisplayName("deleteImage - xóa ảnh thành công")
    void deleteImage_Success() throws IOException {
        String publicId = "sample_id";
        Map<String, Object> deleteResult = new HashMap<>();
        deleteResult.put("result", "ok");

        when(uploader.destroy(eq(publicId), anyMap())).thenReturn(deleteResult);

        Map result = cloudinaryService.deleteImage(publicId);

        assertThat(result).isNotNull();
        assertThat(result.get("result")).isEqualTo("ok");
        verify(uploader).destroy(eq(publicId), anyMap());
    }

    @Test
    @DisplayName("deleteImage - ném ngoại lệ khi Cloudinary gặp sự cố")
    void deleteImage_Error() throws IOException {
        String publicId = "error_id";
        when(uploader.destroy(eq(publicId), anyMap())).thenThrow(new IOException("API Error"));

        assertThatThrownBy(() -> cloudinaryService.deleteImage(publicId))
                .isInstanceOf(IOException.class)
                .hasMessage("API Error");
    }
}
