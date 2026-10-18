package com.cinx.user.shared.application;

/** Port: where uploaded files live (avatars, CVs). Keys are stored on profiles; the public address is derived from them. */
public interface ObjectStorage {
    /** The address a file with this key is served from. */
    String publicUrl(String fileKey);

    /** A short-lived address a client can PUT one file of that content type to. */
    String presignedUploadUrl(String fileKey, String contentType);

    /** Removes the file; a failure is logged and ignored, since a leftover file harms no one. */
    void deleteObject(String fileKey);
}
