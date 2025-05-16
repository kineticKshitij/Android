package com.example.myapplication;

public class TaskItem {
    private long id;
    private String description;
    private long timestamp;

    public TaskItem(long id, String description, long timestamp) {
        this.id = id;
        this.description = description;
        this.timestamp = timestamp;
    }

    public long getId() {
        return id;
    }

    public String getDescription() {
        return description;
    }

    public long getTimestamp() {
        return timestamp;
    }
}