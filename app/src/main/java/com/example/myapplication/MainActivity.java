package com.example.myapplication;

import android.Manifest;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.ContentValues;
import android.content.Context;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.os.Build;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.ContextCompat;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {

    private EditText todoInput;
    private Button addButton;
    private ListView todoListView;
    private ArrayList<TaskItem> taskList;
    private TaskAdapter adapter;
    private MyDbHelper dbHelper;

    private static final String CHANNEL_ID = "task_notification_channel";
    private int notificationIdCounter = 0;

    // ActivityResultLauncher for requesting notification permission (Android 13+)
    private final ActivityResultLauncher<String> requestPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
                if (isGranted) {
                    // Permission granted, can now show notifications
                    Toast.makeText(this, R.string.permission_granted, Toast.LENGTH_SHORT).show();
                } else {
                    // Permission denied, inform the user
                    Toast.makeText(this, R.string.permission_denied, Toast.LENGTH_LONG).show();
                }
            });

    // Static class to hold task data with an ID for database operations
    public static class TaskItem {
        long id;
        String description;
        long timestamp;

        TaskItem(long id, String description, long timestamp) {
            this.id = id;
            this.description = description;
            this.timestamp = timestamp;
        }

        TaskItem(String description, long timestamp) {
            this.id = -1;
            this.description = description;
            this.timestamp = timestamp;
        }

        @NonNull
        @Override
        public String toString() {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
            String formattedTime = sdf.format(new Date(timestamp));
            return description + " (" + formattedTime + ")";
        }
    }

    // Custom ArrayAdapter to handle list item layout and delete button
    private class TaskAdapter extends ArrayAdapter<TaskItem> {
        private final Context context;

        public TaskAdapter(Context context, List<TaskItem> taskList) {
            super(context, 0, taskList);
            this.context = context;
        }

        @NonNull
        @Override
        public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
            View listItemView = convertView;
            if (listItemView == null) {
                listItemView = LayoutInflater.from(context).inflate(
                        R.layout.list_item_task, parent, false);
            }

            TaskItem currentTask = getItem(position);

            if (currentTask != null) {
                TextView taskTextView = listItemView.findViewById(R.id.taskTextView);
                TextView taskDescriptionView = listItemView.findViewById(R.id.taskDescription);
                Button deleteButton = listItemView.findViewById(R.id.deleteButton);

                // Format timestamp
                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
                String formattedTime = sdf.format(new Date(currentTask.timestamp));

                // Set title and description block
                taskTextView.setText(currentTask.description);

                // Use string resource with formatting
                String createdOnText = getString(R.string.created_on_format, formattedTime);
                taskDescriptionView.setText(createdOnText);

                // Delete button listener
                final TaskItem taskToDelete = currentTask;
                deleteButton.setOnClickListener(v -> {
                    dbHelper.deleteTask(taskToDelete.id);
                    remove(taskToDelete);
                    notifyDataSetChanged();
                    showNotification(
                            getString(R.string.task_deleted_title),
                            getString(R.string.task_deleted_message, taskToDelete.description)
                    );
                });
            }

            return listItemView;
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        todoInput = findViewById(R.id.todoInput);
        addButton = findViewById(R.id.addButton);
        todoListView = findViewById(R.id.todoListView);

        dbHelper = new MyDbHelper(this);
        taskList = new ArrayList<>();
        adapter = new TaskAdapter(this, taskList);
        todoListView.setAdapter(adapter);

        createNotificationChannel();

        // Load existing tasks from the database
        loadTasksFromDatabase();

        // Request notification permission if necessary (Android 13+)
        askNotificationPermission();

        addButton.setOnClickListener(v -> {
            String taskDescription = todoInput.getText().toString().trim();
            if (!taskDescription.isEmpty()) {
                long currentTime = System.currentTimeMillis();
                TaskItem newTask = new TaskItem(taskDescription, currentTime);

                // Add to database and get the generated ID
                long newRowId = dbHelper.addTask(newTask.description, newTask.timestamp);
                if (newRowId != -1) {
                    newTask.id = newRowId;
                    taskList.add(newTask);
                    adapter.notifyDataSetChanged();
                    todoInput.setText("");
                    showNotification(
                            getString(R.string.task_added_title),
                            getString(R.string.task_added_message, newTask.description)
                    );
                } else {
                    Toast.makeText(this, R.string.db_error, Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    // Method to load tasks from the database into the list and update the adapter
    private void loadTasksFromDatabase() {
        taskList.clear();
        List<TaskItem> loadedTasks = dbHelper.getAllTasks();
        taskList.addAll(loadedTasks);
        adapter.notifyDataSetChanged();
    }

    // Method to create the notification channel (required for Android 8+)
    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = getString(R.string.channel_name);
            String description = getString(R.string.channel_description);
            int importance = NotificationManager.IMPORTANCE_DEFAULT;
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, name, importance);
            channel.setDescription(description);

            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            if (notificationManager != null) {
                notificationManager.createNotificationChannel(channel);
            }
        }
    }

    // Method to show a simple notification
    private void showNotification(String title, String message) {
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_notification)
                .setContentTitle(title)
                .setContentText(message)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT);

        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(this);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                    == PackageManager.PERMISSION_GRANTED) {
                notificationManager.notify(notificationIdCounter++, builder.build());
            } else {
                Toast.makeText(this, R.string.notification_permission_missing, Toast.LENGTH_SHORT).show();
            }
        } else {
            // Below Android 13
            notificationManager.notify(notificationIdCounter++, builder.build());
        }
    }

    // Method to request notification permission on Android 13+
    private void askNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                    != PackageManager.PERMISSION_GRANTED) {
                requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS);
            }
        }
    }
}