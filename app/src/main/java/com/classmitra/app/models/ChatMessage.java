package com.classmitra.app.models;

import android.graphics.Bitmap;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * Enhanced Model class for chat messages in Ask Mitra.
 */
public class ChatMessage {
    public static final int TYPE_USER = 0;
    public static final int TYPE_MITRA = 1;

    private final String message;
    private final int type;
    private final long timestamp;
    private Bitmap image;
    private boolean isTyping = false;

    public ChatMessage(String message, int type) {
        this.message = message;
        this.type = type;
        this.timestamp = System.currentTimeMillis();
    }

    public ChatMessage(String message, int type, Bitmap image) {
        this(message, type);
        this.image = image;
    }

    public String getMessage() {
        return message;
    }

    public int getType() {
        return type;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public String getFormattedTime() {
        SimpleDateFormat sdf = new SimpleDateFormat("hh:mm a", Locale.getDefault());
        return sdf.format(new Date(timestamp));
    }

    public Bitmap getImage() {
        return image;
    }

    public void setImage(Bitmap image) {
        this.image = image;
    }

    public boolean isTyping() {
        return isTyping;
    }

    public void setTyping(boolean typing) {
        isTyping = typing;
    }
}