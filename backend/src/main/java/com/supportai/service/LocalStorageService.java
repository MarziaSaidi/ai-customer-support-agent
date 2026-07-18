package com.supportai.service;

import com.supportai.config.StorageProperties;
import com.supportai.exception.BadRequestException;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Set;
import java.util.UUID;

@Service
public class LocalStorageService {

    private static final Set<String> ALLOWED_EXTENSIONS = Set.of("pdf", "md", "markdown", "txt", "doc", "docx");

    private final Path storageRoot;

    public LocalStorageService(StorageProperties storageProperties) {
        this.storageRoot = Paths.get(storageProperties.getLocalPath()).toAbsolutePath().normalize();
    }

    public String store(Long companyId, MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BadRequestException("File is required");
        }

        String originalFilename = sanitizeFilename(file.getOriginalFilename());
        String extension = extensionOf(originalFilename);

        if (!ALLOWED_EXTENSIONS.contains(extension)) {
            throw new BadRequestException("Unsupported file type. Allowed: PDF, Markdown, Word, TXT");
        }

        String storedName = UUID.randomUUID() + "-" + originalFilename;
        Path directory = storageRoot.resolve(String.valueOf(companyId));

        try {
            Files.createDirectories(directory);
            Path target = directory.resolve(storedName).normalize();
            if (!target.startsWith(storageRoot)) {
                throw new BadRequestException("Invalid file path");
            }
            try (InputStream inputStream = file.getInputStream()) {
                Files.copy(inputStream, target, StandardCopyOption.REPLACE_EXISTING);
            }
            return Paths.get(String.valueOf(companyId), storedName).toString().replace("\\", "/");
        } catch (IOException ex) {
            throw new BadRequestException("Failed to store file");
        }
    }

    private String sanitizeFilename(String filename) {
        if (filename == null || filename.isBlank()) {
            throw new BadRequestException("Invalid filename");
        }
        return Paths.get(filename).getFileName().toString().replaceAll("[^a-zA-Z0-9._-]", "_");
    }

    private String extensionOf(String filename) {
        int dot = filename.lastIndexOf('.');
        if (dot < 0) {
            throw new BadRequestException("File must have an extension");
        }
        return filename.substring(dot + 1).toLowerCase();
    }

    public Path resolveStoredPath(String relativePath) {
        if (relativePath == null || relativePath.isBlank()) {
            throw new BadRequestException("Invalid file path");
        }
        Path resolved = storageRoot.resolve(relativePath).normalize();
        if (!resolved.startsWith(storageRoot)) {
            throw new BadRequestException("Invalid file path");
        }
        if (!Files.exists(resolved)) {
            throw new BadRequestException("Stored file not found");
        }
        return resolved;
    }
}
