package com.classmitra.app;

import android.app.Application;
import android.util.Log;

import com.google.firebase.FirebaseApp;

public class ClassMitraApplication extends Application {

    private static final String TAG = "ClassMitraApplication";

    @Override
    public void onCreate() {
        super.onCreate();

        Log.i(TAG, "ClassMitraApplication starting. Initializing Firebase...");
        try {
            FirebaseApp.initializeApp(this);
            Log.i(TAG, "FirebaseApp initialized successfully.");
        } catch (Exception e) {
            Log.e(TAG, "Error initializing FirebaseApp", e);
        }
    }
}
