package com.classmitra.app.ai;

import android.content.Context;
import android.net.Uri;
import android.util.Log;

import com.tom_roush.pdfbox.android.PDFBoxResourceLoader;
import com.tom_roush.pdfbox.pdmodel.PDDocument;
import com.tom_roush.pdfbox.text.PDFTextStripper;

import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * PdfTextExtractor handles text extraction from PDF files using PDFBox-Android.
 */
public class PdfTextExtractor {
    private static final String TAG = "PdfTextExtractor";
    private static final ExecutorService executor = Executors.newSingleThreadExecutor();

    public interface Callback {
        void onTextExtracted(String text);
        void onError(Throwable t);
    }

    public static void extractText(Context context, Uri uri, Callback callback) {
        PDFBoxResourceLoader.init(context.getApplicationContext());

        executor.execute(() -> {
            try (InputStream inputStream = context.getContentResolver().openInputStream(uri)) {
                if (inputStream == null) {
                    callback.onError(new Exception("Unable to open file stream"));
                    return;
                }

                PDDocument document = PDDocument.load(inputStream);
                PDFTextStripper stripper = new PDFTextStripper();
                String text = stripper.getText(document);
                document.close();

                if (text == null || text.trim().isEmpty()) {
                    callback.onError(new Exception("PDF is empty or contains no readable text"));
                } else {
                    callback.onTextExtracted(text.trim());
                }
            } catch (IOException e) {
                Log.e(TAG, "Error extracting text", e);
                callback.onError(new Exception("Unable to read PDF."));
            } catch (Exception e) {
                Log.e(TAG, "Unexpected error", e);
                callback.onError(new Exception("Unable to read PDF."));
            }
        });
    }
}
