package com.example.currencyguard.model;

/**
 * Educational model for currency security features displayed in the Learn section.
 */
public class LearnTopic {
    private final String id;
    private final String title;
    private final String category;
    private final String description;
    private final String howToCheck;

    public LearnTopic(String id, String title, String category, String description, String howToCheck) {
        this.id = id;
        this.title = title;
        this.category = category;
        this.description = description;
        this.howToCheck = howToCheck;
    }

    public String getId() { return id; }
    public String getTitle() { return title; }
    public String getCategory() { return category; }
    public String getDescription() { return description; }
    public String getHowToCheck() { return howToCheck; }
}
