package com.vn.backend.services.impl;

import com.vn.backend.configs.PropertiesConfig;
import com.vn.backend.constants.AppConst;
import com.vn.backend.constants.AppConst.MessageConst;
import com.vn.backend.entities.FileStorage;
import com.vn.backend.entities.User;
import com.vn.backend.enums.ProviderStorage;
import com.vn.backend.exceptions.AppException;
import com.vn.backend.repositories.FileStorageRepository;
import com.vn.backend.services.AuthService;
import com.vn.backend.services.FileService;
import com.vn.backend.utils.MessageUtils;
import org.apache.commons.io.FilenameUtils;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.util.UUID;

@Service
public class FileServiceImpl extends BaseService implements FileService {

    private final PropertiesConfig propertiesConfig;
    private final AuthService authService;
    private final FileStorageRepository fileStorageRepository;

    public FileServiceImpl(MessageUtils messageUtils, PropertiesConfig propertiesConfig, AuthService authService, FileStorageRepository fileStorageRepository) {
        super(messageUtils);
        this.propertiesConfig = propertiesConfig;
        this.authService = authService;
        this.fileStorageRepository = fileStorageRepository;
    }


    @Override
    public String handleUploadFiles(MultipartFile uploadedFile) {
        try {
            User user = authService.getCurrentUser();

            if (uploadedFile.isEmpty()) {
                throw new AppException(MessageConst.FILE_IS_EMPTY, messageUtils.getMessage(MessageConst.FILE_IS_EMPTY), HttpStatus.BAD_REQUEST);
            }

            File uploadFolder = this.getDirectoryFolder(uploadedFile);

            String uuid = UUID.randomUUID().toString();
            String extension = FilenameUtils.getExtension(uploadedFile.getOriginalFilename());
            String fileName = uuid + AppConst.DOT_CONSTANT + extension;

            File savedFile = new File(uploadFolder, fileName);
            uploadedFile.transferTo(savedFile);

            FileStorage fileEntity = FileStorage.builder()
                    .originalName(uploadedFile.getOriginalFilename())
                    .fileName(fileName)
                    .contentType(uploadedFile.getContentType())
                    .size(uploadedFile.getSize())
                    .storagePath(uploadFolder.getAbsolutePath())
                    .url(AppConst.ENDPOINT_DOWNLOAD_FILE + fileName)
                    .providerStorage(ProviderStorage.LOCAL)
                    .userId(user.getId())
                    .build();
            fileStorageRepository.save(fileEntity);
            return fileEntity.getUrl();
        } catch (IOException e) {
            log.error("Upload file failed");
            throw new AppException(MessageConst.FILE_TYPE_NOT_ALLOWED, messageUtils.getMessage(MessageConst.FILE_UPLOAD_FAILED), HttpStatus.BAD_REQUEST);
        }
    }

    @Override
    public Resource downloadFile(String fileName) {
        FileStorage fileEntity = fileStorageRepository.findByFileName(fileName)
                .orElseThrow(() -> new AppException(
                        MessageConst.NOT_FOUND,
                        messageUtils.getMessage(MessageConst.NOT_FOUND),
                        HttpStatus.BAD_REQUEST
                ));

        File file = new File(fileEntity.getStoragePath(), fileEntity.getFileName());
        if (!file.exists()) {
            throw new AppException(
                    MessageConst.NOT_FOUND,
                    messageUtils.getMessage(MessageConst.NOT_FOUND),
                    HttpStatus.BAD_REQUEST
            );
        }
        return new FileSystemResource(file);
    }


    /**
     * Checks if the uploaded file is an office file or image.
     *
     * @param file The uploaded file.
     * @return true if the file is an office, false otherwise.
     */
    private boolean isAllowedFile(MultipartFile file) {
        String originalFilename = file.getOriginalFilename();
        if (originalFilename != null) {
            String[] allowedExtensions = {
                    "doc", "docx", "pdf", "zip", "txt", "ppt", "pptx", "xlsx", "xls", // Office
                    "jpg", "jpeg", "png", "gif", "bmp", "webp" // Image
            };
            String fileExtension = originalFilename.substring(originalFilename.lastIndexOf(AppConst.DOT_CONSTANT) + AppConst.DOT_CONSTANT_POSITION);
            for (String extension : allowedExtensions) {
                if (extension.equalsIgnoreCase(fileExtension)) {
                    return true;
                }
            }
        }
        return false;
    }

    private java.io.File getDirectoryFolder(MultipartFile uploadedFile) {
        long fileSizeInMegabytes = uploadedFile.getSize() / AppConst.MEGABYTE;
        if (fileSizeInMegabytes > AppConst.FILE_SIZE) {
            throw new AppException(MessageConst.FILE_TOO_LARGE, messageUtils.getMessage(MessageConst.FILE_TOO_LARGE), HttpStatus.BAD_REQUEST);
        }
        if (!isAllowedFile(uploadedFile)) {
            throw new AppException(MessageConst.FILE_TYPE_NOT_ALLOWED, messageUtils.getMessage(MessageConst.FILE_TYPE_NOT_ALLOWED), HttpStatus.BAD_REQUEST);
        }
        java.io.File myUploadFolder = new java.io.File(propertiesConfig.getOfficePath());
        if (!myUploadFolder.exists()) {
            myUploadFolder.mkdirs();
        }
        return myUploadFolder;
    }

}
