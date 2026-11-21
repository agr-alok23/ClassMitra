package com.classmitra.app.models;

/**
 * Model class for Recent PDF Uploads.
 */
public class PdfUpload {
    private final String fileName;
    private final String uploadDate;

    public PdfUpload(String fileName, String uploadDate) {
        this.fileName = fileName;
        this.uploadDate = uploadDate;
    }

    public String getFileName() { return fileName; }
    public String getUploadDate() { return uploadDate; }
}