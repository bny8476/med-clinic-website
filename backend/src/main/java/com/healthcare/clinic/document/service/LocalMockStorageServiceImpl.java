package com.healthcare.clinic.document.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

@Service
@Slf4j
public class LocalMockStorageServiceImpl implements DocumentStorageService {

    private final Path tempStorageDir;

    public LocalMockStorageServiceImpl() {
        String baseDir = System.getProperty("java.io.tmpdir");
        this.tempStorageDir = Path.of(baseDir, "clinic-temp-uploads");
        try {
            Files.createDirectories(this.tempStorageDir);
            log.info("Initialized local disk temp storage directory at: {}", this.tempStorageDir.toAbsolutePath());
        } catch (IOException e) {
            log.error("Failed to create local temp storage directory", e);
        }
    }

    @Override
    public String uploadFile(MultipartFile file) {
        String safeName = file.getOriginalFilename() != null ? file.getOriginalFilename().replaceAll("[^a-zA-Z0-9._-]", "_") : "file";
        String storageKey = UUID.randomUUID().toString() + "-" + safeName;
        Path targetPath = tempStorageDir.resolve(storageKey);
        try (InputStream inputStream = file.getInputStream()) {
            Files.copy(inputStream, targetPath, StandardCopyOption.REPLACE_EXISTING);
            log.info("Saved upload to disk: {} (size: {} bytes)", targetPath.toAbsolutePath(), file.getSize());
            return storageKey;
        } catch (IOException e) {
            throw new RuntimeException("Failed to save uploaded file to local disk storage", e);
        }
    }

    @Override
    public InputStream downloadFile(String storageKey) {
        Path targetPath = tempStorageDir.resolve(storageKey);
        if (!Files.exists(targetPath)) {
            throw new RuntimeException("File not found in local disk storage: " + storageKey);
        }
        try {
            return Files.newInputStream(targetPath);
        } catch (IOException e) {
            throw new RuntimeException("Failed to open input stream for file: " + storageKey, e);
        }
    }

    @Override
    public String generateDownloadUrl(String storageKey) {
        return "/api/documents/download/" + storageKey;
    }

    @Override
    public void deleteFile(String storageKey) {
        Path targetPath = tempStorageDir.resolve(storageKey);
        try {
            boolean deleted = Files.deleteIfExists(targetPath);
            log.info("Disk delete file key {}: deleted={}", storageKey, deleted);
        } catch (IOException e) {
            log.warn("Failed to delete file from disk storage: {}", storageKey, e);
        }
    }
}
