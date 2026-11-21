package com.classmitra.app.ai;

/**
 * Singleton to store extracted PDF text in memory to reuse across activities.
 */
public class DocumentDataManager {
    private static DocumentDataManager instance;
    private String extractedText;

    private DocumentDataManager() {}

    public static synchronized DocumentDataManager getInstance() {
        if (instance == null) {
            instance = new DocumentDataManager();
        }
        return instance;
    }

    public void setExtractedText(String text) {
        this.extractedText = text;
    }

    public String getExtractedText() {
        return extractedText != null ? extractedText : "";
    }
}
