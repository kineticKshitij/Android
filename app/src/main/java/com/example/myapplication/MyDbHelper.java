package com.example.myapplication;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import androidx.annotation.NonNull;

import java.util.ArrayList;
import java.util.List;

/**
 * Database helper class for the Todo List application.
 * Manages database creation, version management, and CRUD operations.
 */
public class MyDbHelper extends SQLiteOpenHelper {

    private static final String DATABASE_NAME = "todo_list.db";
    private static final int DATABASE_VERSION = 1;

    // Table and column names
    private static final String TABLE_TASKS = "tasks";
    private static final String COLUMN_ID = "_id";
    private static final String COLUMN_DESCRIPTION = "description";
    private static final String COLUMN_TIMESTAMP = "timestamp";

    // Create table SQL statement
    private static final String SQL_CREATE_TABLE =
            "CREATE TABLE " + TABLE_TASKS + " (" +
                    COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    COLUMN_DESCRIPTION + " TEXT NOT NULL, " +
                    COLUMN_TIMESTAMP + " INTEGER NOT NULL)";

    public MyDbHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(@NonNull SQLiteDatabase db) {
        db.execSQL(SQL_CREATE_TABLE);
    }

    @Override
    public void onUpgrade(@NonNull SQLiteDatabase db, int oldVersion, int newVersion) {
        // For simplicity, drop and recreate the table
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_TASKS);
        onCreate(db);
    }

    /**
     * Add a new task to the database
     * @param description The task description
     * @param timestamp The timestamp when the task was created
     * @return The row ID of the newly inserted task, or -1 if an error occurred
     */
    public long addTask(String description, long timestamp) {
        SQLiteDatabase db = this.getWritableDatabase();

        ContentValues values = new ContentValues();
        values.put(COLUMN_DESCRIPTION, description);
        values.put(COLUMN_TIMESTAMP, timestamp);

        return db.insert(TABLE_TASKS, null, values);
    }

    /**
     * Get all tasks from the database
     * @return A list of TaskItem objects
     */
    public List<MainActivity.TaskItem> getAllTasks() {
        List<MainActivity.TaskItem> taskList = new ArrayList<>();

        SQLiteDatabase db = this.getReadableDatabase();
        String[] projection = {
                COLUMN_ID,
                COLUMN_DESCRIPTION,
                COLUMN_TIMESTAMP
        };

        try (Cursor cursor = db.query(
                TABLE_TASKS,
                projection,
                null,
                null,
                null,
                null,
                COLUMN_TIMESTAMP + " DESC")) {

            while (cursor.moveToNext()) {
                long id = cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_ID));
                String description = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_DESCRIPTION));
                long timestamp = cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_TIMESTAMP));

                taskList.add(new MainActivity.TaskItem(id, description, timestamp));
            }
        }

        return taskList;
    }

    /**
     * Delete a task from the database
     * @param id The ID of the task to delete
     * @return The number of rows affected (should be 1 if successful)
     */
    public int deleteTask(long id) {
        SQLiteDatabase db = this.getWritableDatabase();

        String selection = COLUMN_ID + " = ?";
        String[] selectionArgs = { String.valueOf(id) };

        return db.delete(TABLE_TASKS, selection, selectionArgs);
    }

    /**
     * Update an existing task
     * @param id The ID of the task to update
     * @param description The new description
     * @return The number of rows affected (should be 1 if successful)
     */
    public int updateTask(long id, String description) {
        SQLiteDatabase db = this.getWritableDatabase();

        ContentValues values = new ContentValues();
        values.put(COLUMN_DESCRIPTION, description);

        String selection = COLUMN_ID + " = ?";
        String[] selectionArgs = { String.valueOf(id) };

        return db.update(TABLE_TASKS, values, selection, selectionArgs);
    }
}