package com.cinx.learning.catalog.application;

import java.util.Collection;

/** Port: the object store that holds course media, subtitles and assignment files. */
public interface ObjectStorage {
    /** A short-lived URL the browser can PUT the object to. */
    String generatePresignedUrl(String objectKey, String contentType);

    String readTextObject(String objectKey);

    /** The object's bytes, or empty when no object has that key. */
    java.util.Optional<byte[]> readObject(String objectKey);

    void uploadTextFile(String objectKey, String content, String contentType);

    /** Deletes in batches and returns how many objects the store reported as not deleted; it never throws. */
    int deleteObjects(Collection<String> objectKeys);

    String publicUrl(String objectKey);
}
