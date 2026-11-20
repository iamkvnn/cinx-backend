package com.cinx.learning.media.application;

/** Port: the object store for generated files such as certificates. */
public interface FileStorage {
    /** A short-lived URL the browser can PUT the object to. */
    String generatePresignedUrl(String objectKey, String contentType);

    /** Stores the bytes and returns the public URL of the object. */
    String uploadFile(String objectKey, byte[] content, String contentType);
}
