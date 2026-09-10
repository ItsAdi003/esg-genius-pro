package dev.esgenius.service;

import dev.esgenius.config.StorageProperties;
import dev.esgenius.exception.BadRequestException;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

/**
 * Stores uploaded files on the local filesystem under a configured upload directory.
 */
@Service
public class LocalFileStorageService {

    private final Path uploadRoot;

    public LocalFileStorageService(StorageProperties storageProperties) throws IOException {
        this.uploadRoot = Paths.get(storageProperties.getUploadDir()).toAbsolutePath().normalize();
        Files.createDirectories(this.uploadRoot);
    }

    public Path store(InputStream inputStream, String storedFilename) throws IOException {
        Path target = resolveStoredPath(storedFilename);
        Files.copy(inputStream, target, StandardCopyOption.REPLACE_EXISTING);
        return target;
    }

    public Path resolveStoredPath(String storedFilename) {
        if (storedFilename == null || storedFilename.isBlank()) {
            throw new BadRequestException("Stored filename must not be blank");
        }
        if (storedFilename.contains("..") || storedFilename.contains("/") || storedFilename.contains("\\")) {
            throw new BadRequestException("Invalid stored filename");
        }

        Path resolved = uploadRoot.resolve(storedFilename).normalize();
        if (!resolved.startsWith(uploadRoot)) {
            throw new BadRequestException("Invalid stored filename path");
        }
        return resolved;
    }

    public void delete(String storedFilename) throws IOException {
        Path path = resolveStoredPath(storedFilename);
        Files.deleteIfExists(path);
    }

    public boolean exists(String storedFilename) {
        return Files.exists(resolveStoredPath(storedFilename));
    }
}
