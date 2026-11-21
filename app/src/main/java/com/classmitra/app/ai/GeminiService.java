package com.classmitra.app.ai;

import android.graphics.Bitmap;
import android.os.Handler;
import android.os.Looper;
import android.util.Base64;
import android.util.Log;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.classmitra.app.BuildConfig;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Service managing ClassMitra AI operations via Cloudflare Worker backend and Groq AI.
 */
public class GeminiService {

    private static final String TAG = "MitraAiService";

    // Public, configurable Worker URL. Groq credentials remain server-side.
    public static String BASE_URL = BuildConfig.WORKER_BASE_URL;

    private final ExecutorService executor;
    private final Handler mainHandler;

    public interface GeminiCallback {
        void onSuccess(String response);
        void onError(Throwable throwable);
    }

    public GeminiService() {
        this.executor = Executors.newSingleThreadExecutor();
        this.mainHandler = new Handler(Looper.getMainLooper());
    }

    public boolean isApiKeyMissing() {
        return false;
    }

    public void generateContentWithImage(String prompt, Bitmap image, GeminiCallback callback) {
        if (callback == null) return;
        if (prompt == null || prompt.trim().isEmpty()) {
            postError(callback, new IllegalArgumentException("Prompt cannot be empty."));
            return;
        }

        fetchFirebaseTokenAndExecute(callback, (idToken) -> {
            try {
                JSONObject jsonPayload = new JSONObject();
                jsonPayload.put("prompt", prompt);

                if (image != null) {
                    ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
                    image.compress(Bitmap.CompressFormat.JPEG, 80, outputStream);
                    String base64Image = Base64.encodeToString(outputStream.toByteArray(), Base64.NO_WRAP);
                    jsonPayload.put("image", "data:image/jpeg;base64," + base64Image);
                }

                postToWorker("/chat", jsonPayload.toString(), idToken, callback);
            } catch (Exception e) {
                postError(callback, e);
            }
        });
    }

    public void generateCustomContent(String prompt, GeminiCallback callback) {
        generateContentWithImage(prompt, null, callback);
    }

    public void getSummary(String text, GeminiCallback callback) {
        if (callback == null) return;
        if (text == null || text.trim().isEmpty()) {
            postError(callback, new IllegalArgumentException("Document text cannot be empty."));
            return;
        }

        fetchFirebaseTokenAndExecute(callback, (idToken) -> {
            try {
                JSONObject jsonPayload = new JSONObject();
                jsonPayload.put("text", text);
                postToWorker("/summary", jsonPayload.toString(), idToken, callback);
            } catch (Exception e) {
                postError(callback, e);
            }
        });
    }

    public void generateQuiz(String text, GeminiCallback callback) {
        if (callback == null) return;
        if (text == null || text.trim().isEmpty()) {
            postError(callback, new IllegalArgumentException("Quiz topic or text cannot be empty."));
            return;
        }

        fetchFirebaseTokenAndExecute(callback, (idToken) -> {
            try {
                JSONObject jsonPayload = new JSONObject();
                jsonPayload.put("text", text);
                postToWorker("/quiz", jsonPayload.toString(), idToken, callback);
            } catch (Exception e) {
                postError(callback, e);
            }
        });
    }

    public void generateFlashcards(String text, GeminiCallback callback) {
        if (callback == null) return;
        if (text == null || text.trim().isEmpty()) {
            postError(callback, new IllegalArgumentException("Flashcard topic or text cannot be empty."));
            return;
        }

        fetchFirebaseTokenAndExecute(callback, (idToken) -> {
            try {
                JSONObject jsonPayload = new JSONObject();
                jsonPayload.put("text", text);
                postToWorker("/flashcards", jsonPayload.toString(), idToken, callback);
            } catch (Exception e) {
                postError(callback, e);
            }
        });
    }

    private interface TokenConsumer {
        void accept(String idToken);
    }

    private void fetchFirebaseTokenAndExecute(GeminiCallback callback, TokenConsumer consumer) {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser != null) {
            currentUser.getIdToken(false)
                    .addOnSuccessListener(getAuthResult -> {
                        String token = getAuthResult.getToken();
                        if (token == null || token.isEmpty()) {
                            postError(callback, new IllegalStateException("Unable to obtain a Firebase ID token."));
                        } else {
                            executor.execute(() -> consumer.accept(token));
                        }
                    })
                    .addOnFailureListener(e -> {
                        Log.w(TAG, "Failed to get Firebase ID Token", e);
                        postError(callback, new IllegalStateException("Unable to obtain a Firebase ID token."));
                    });
        } else {
            postError(callback, new IllegalStateException("Please sign in before using AI features."));
        }
    }

    private void postToWorker(String endpoint, String jsonBody, String idToken, GeminiCallback callback) {
        HttpURLConnection conn = null;
        try {
            String normalizedBaseUrl = BASE_URL == null ? "" : BASE_URL.trim();
            while (normalizedBaseUrl.endsWith("/")) {
                normalizedBaseUrl = normalizedBaseUrl.substring(0, normalizedBaseUrl.length() - 1);
            }
            if (!normalizedBaseUrl.startsWith("https://")) {
                throw new IllegalArgumentException("The ClassMitra Worker URL must use HTTPS.");
            }
            URL url = new URL(normalizedBaseUrl + endpoint);
            conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setConnectTimeout(15000);
            conn.setReadTimeout(30000);
            conn.setRequestProperty("Content-Type", "application/json; charset=UTF-8");

            if (idToken != null && !idToken.isEmpty()) {
                conn.setRequestProperty("Authorization", "Bearer " + idToken);
            }

            conn.setDoOutput(true);
            try (OutputStream os = conn.getOutputStream()) {
                byte[] input = jsonBody.getBytes(StandardCharsets.UTF_8);
                os.write(input, 0, input.length);
            }

            int responseCode = conn.getResponseCode();
            BufferedReader br;
            if (responseCode >= 200 && responseCode < 300) {
                br = new BufferedReader(new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8));
            } else {
                br = new BufferedReader(new InputStreamReader(conn.getErrorStream(), StandardCharsets.UTF_8));
            }

            StringBuilder responseBuilder = new StringBuilder();
            String responseLine;
            while ((responseLine = br.readLine()) != null) {
                responseBuilder.append(responseLine.trim());
            }
            br.close();

            String rawResponse = responseBuilder.toString();
            if (responseCode >= 200 && responseCode < 300) {
                JSONObject jsonResponse = new JSONObject(rawResponse);
                String aiText = jsonResponse.optString("response", "");
                if (!aiText.isEmpty()) {
                    postSuccess(callback, aiText);
                } else {
                    postError(callback, new Exception("Empty AI response received from backend."));
                }
            } else {
                String errorDetail = rawResponse;
                try {
                    JSONObject errJson = new JSONObject(rawResponse);
                    if (errJson.has("error")) {
                        errorDetail = errJson.getString("error");
                    }
                } catch (Exception ignored) {}
                postError(callback, new Exception("Backend Error (HTTP " + responseCode + "): " + errorDetail));
            }

        } catch (Exception e) {
            Log.e(TAG, "Network error contacting backend worker", e);
            postError(callback, new Exception("Unable to connect to Mitra AI backend. Please check your internet connection.", e));
        } finally {
            if (conn != null) {
                conn.disconnect();
            }
        }
    }

    private void postSuccess(GeminiCallback callback, String response) {
        mainHandler.post(() -> callback.onSuccess(response));
    }

    private void postError(GeminiCallback callback, Throwable t) {
        mainHandler.post(() -> callback.onError(t));
    }
}
