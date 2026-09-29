package org.example.motionville.video;

public class MediaStorageException extends RuntimeException {
    public MediaStorageException(String message, Throwable cause) {
        super(message, cause);
    }

    public MediaStorageException(String message) {
        super(message);
    }
}
