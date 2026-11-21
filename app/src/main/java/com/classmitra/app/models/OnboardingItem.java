package com.classmitra.app.models;

/**
 * Model class for onboarding screen data.
 */
public class OnboardingItem {
    private final String title;
    private final String subtitle;
    private final String description;
    private final int imageRes;

    public OnboardingItem(String title, String subtitle, String description, int imageRes) {
        this.title = title;
        this.subtitle = subtitle;
        this.description = description;
        this.imageRes = imageRes;
    }

    public String getTitle() { return title; }
    public String getSubtitle() { return subtitle; }
    public String getDescription() { return description; }
    public int getImageRes() { return imageRes; }
}