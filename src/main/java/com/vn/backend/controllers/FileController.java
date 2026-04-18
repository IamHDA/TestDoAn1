package com.vn.backend.controllers;

import com.vn.backend.constants.AppConst;
import com.vn.backend.dto.response.common.AppResponse;
import com.vn.backend.services.FileService;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;

@RestController
@RequestMapping(AppConst.API + "/files")
public class FileController extends BaseController {

    private final FileService fileService;

    public FileController(FileService fileService) {
        this.fileService = fileService;
    }

    @PostMapping("/upload")
    public AppResponse<String> uploadFile(@RequestParam("file") MultipartFile file) {
        log.info("Received request to upload file");
        String url = fileService.handleUploadFiles(file);
        log.info("Successfully upload file");
        return success(url);
    }

    /**
     * Download a file by name
     */
    @GetMapping("/download/{fileName}")
    public ResponseEntity<Resource> downloadFile(@PathVariable String fileName) {
        log.info("Received request to download file");
        Resource resource = fileService.downloadFile(fileName);
        String contentType = null;
        try {
            contentType = Files.probeContentType(resource.getFile().toPath());
        } catch (IOException e) {
            // Ignore, default to octet-stream
        }
        if (contentType == null) {
            // fallback cho các loại không detect được
            if (fileName.endsWith(".docx")) {
                contentType = "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
            } else if (fileName.endsWith(".doc")) {
                contentType = "application/msword";
            } else {
                contentType = "application/octet-stream";
            }
        }
        log.info("Successfully download file");
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(contentType))
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + resource.getFilename() + "\"")
                .body(resource);
    }
}
