package com.vn.backend.services;

import org.springframework.core.io.Resource;
import org.springframework.web.multipart.MultipartFile;

public interface FileService {

    String handleUploadFiles(MultipartFile uploadedFile);

    Resource downloadFile(String fileName);
}
