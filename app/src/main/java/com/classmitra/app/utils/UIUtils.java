package com.classmitra.app.utils;

import android.content.Context;
import androidx.appcompat.app.AlertDialog;
import com.classmitra.app.R;

public class UIUtils {
    public static void showComingSoon(Context context, String featureName) {
        new AlertDialog.Builder(context)
                .setTitle("Coming Soon")
                .setMessage(featureName + " is coming soon! \uD83D\uDE80\n\nMitra is working hard to bring this feature to your personal learning journey.")
                .setPositiveButton("Got it!", null)
                .show();
    }
}
