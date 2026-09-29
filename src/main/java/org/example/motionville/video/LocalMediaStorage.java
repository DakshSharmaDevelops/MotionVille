package org.example.motionville.video;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

@Component
public class LocalMediaStorage {
    private static final Map<String, String> EXTENSIONS = Map.of(
            "video/mp4", ".mp4",
            "video/webm", ".webm",
            "video/quicktime", ".mov");

    private final Path root;

    public LocalMediaStorage(@Value("${motionville.storage.directory:./data/media}") String directory) {
        this.root = Path.of(directory).toAbsolutePath().normalize();
    }

    public String store(MultipartFile file, String contentType) {
        String extension = EXTENSIONS.get(contentType);
        if (extension == null) {
            throw new MediaStorageException("Only MP4, WebM, and MOV videos are supported.");
        }
        String storageKey = UUID.randomUUID() + extension;
        Path destination = resolve(storageKey);
        try {
            Files.createDirectories(root);
            file.transferTo(destination);
            return storageKey;
        } catch (IOException exception) {
            try {
                Files.deleteIfExists(destination);
            } catch (IOException cleanupException) {
                exception.addSuppressed(cleanupException);
            }
            throw new MediaStorageException("Unable to save the uploaded video.", exception);
        }
    }

    public Resource load(String storageKey) {
        Path path = resolve(storageKey);
        if (!Files.isRegularFile(path)) {
            throw new MediaStorageException("The video file is unavailable.");
        }
        return new FileSystemResource(path);
    }

    public void delete(String storageKey) {
        try {
            Files.deleteIfExists(resolve(storageKey));
        } catch (IOException exception) {
            throw new MediaStorageException("Unable to remove the video file.", exception);
        }
    }

    private Path resolve(String storageKey) {
        if (!storageKey.matches("[a-f0-9-]+\\.(mp4|webm|mov)")) {
            throw new MediaStorageException("Invalid media key.");
        }
        Path path = root.resolve(storageKey).normalize();
        if (!path.startsWith(root)) {
            throw new MediaStorageException("Invalid media path.");
        }
        return path;
    }
}
