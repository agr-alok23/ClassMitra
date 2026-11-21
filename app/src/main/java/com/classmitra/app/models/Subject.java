package com.classmitra.app.models;

/**
 * Model class for academic subjects.
 */
public class Subject {
    private final String name;
    private final int notesCount;
    private final int quizCount;
    private final int color; // Color resource or hex

    public Subject(String name, int notesCount, int quizCount, int color) {
        this.name = name;
        this.notesCount = notesCount;
        this.quizCount = quizCount;
        this.color = color;
    }

    public String getName() { return name; }
    public int getNotesCount() { return notesCount; }
    public int getQuizCount() { return quizCount; }
    public int getColor() { return color; }
}