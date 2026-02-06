package com.example.docuscan;

import android.app.AlertDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Environment;
import android.speech.tts.TextToSpeech;
import android.speech.tts.UtteranceProgressListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.RequiresApi;
import androidx.core.app.ActivityCompat;
import androidx.core.content.FileProvider;

import java.io.File;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {

    private EditText Input;
    private Button Speak, Download;
    private TextToSpeech tts;
    private final int PERMISSION_REQUEST_CODE = 100;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Input = findViewById(R.id.Input);
        Speak = findViewById(R.id.Speak);
        Download = findViewById(R.id.Download);
        Button openCapture = findViewById(R.id.openCaptureBtn);

        // Load text from MainActivity2
        String capturedText = getIntent().getStringExtra("captured_text");
        if (capturedText != null) {
            Input.setText(capturedText);
        }

        // Init TextToSpeech
        tts = new TextToSpeech(this, status -> {
            if (status == TextToSpeech.SUCCESS) {
                int result = tts.setLanguage(Locale.US);
                if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                    Toast.makeText(this, "TTS language not supported!", Toast.LENGTH_SHORT).show();
                }
            } else {
                Toast.makeText(this, "TTS initialization failed!", Toast.LENGTH_SHORT).show();
            }
        });

        // Speak Button
        Speak.setOnClickListener(v -> {
            String text = Input.getText().toString();
            if (!text.isEmpty()) {
                tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, null);
            } else {
                Toast.makeText(this, "Please enter or capture text.", Toast.LENGTH_SHORT).show();
            }
        });

        // Download Button
        Download.setOnClickListener(v -> {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                if (checkPermission()) {
                    saveAudioFile();
                } else {
                    requestPermission();
                }
            } else {
                Toast.makeText(this, "TTS file download requires API 21+", Toast.LENGTH_SHORT).show();
            }
        });

        // Open image capture activity
        openCapture.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, MainActivity2.class);
            startActivity(intent);
        });
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    private void saveAudioFile() {
        String text = Input.getText().toString();
        if (text.isEmpty()) {
            Toast.makeText(this, "Please enter text.", Toast.LENGTH_SHORT).show();
            return;
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Rename your file");

        final EditText input = new EditText(this);
        input.setHint("Enter name without extension");
        builder.setView(input);

        builder.setPositiveButton("Save", (dialog, which) -> {
            String customName = input.getText().toString().trim();
            if (customName.isEmpty()) {
                customName = "EchoInk.Audio";
            }

            File downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
            if (!downloadsDir.exists()) {
                downloadsDir.mkdirs();
            }

            int count = 1;
            File[] files = downloadsDir.listFiles();
            if (files != null) {
                for (File f : files) {
                    if (f.getName().startsWith(customName) && f.getName().endsWith(".wav")) {
                        try {
                            String numberPart = f.getName().replace(customName + ".", "").replace(".wav", "");
                            int num = Integer.parseInt(numberPart);
                            if (num >= count) count = num + 1;
                        } catch (NumberFormatException ignored) {}
                    }
                }
            }

            String finalFileName = customName + "." + count + ".wav";
            File file = new File(downloadsDir, finalFileName);

            Bundle params = new Bundle();
            params.putString(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, "ttsDone");

            tts.setOnUtteranceProgressListener(new UtteranceProgressListener() {
                @Override
                public void onStart(String utteranceId) {}

                @Override
                public void onDone(String utteranceId) {
                    runOnUiThread(() -> new AlertDialog.Builder(MainActivity.this)
                            .setTitle("Success")
                            .setMessage("Audio saved as:\n" + finalFileName)
                            .setPositiveButton("Share", (dialog1, which1) -> shareFile(file))
                            .setNegativeButton("Close", null)
                            .show());
                }

                @Override
                public void onError(String utteranceId) {
                    runOnUiThread(() -> Toast.makeText(MainActivity.this, "Failed to save audio.", Toast.LENGTH_SHORT).show());
                }
            });

            tts.synthesizeToFile(text, params, file, "ttsDone");
        });

        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.cancel());
        builder.show();
    }

    private void shareFile(File file) {
        Uri fileUri = FileProvider.getUriForFile(this, getPackageName() + ".provider", file);
        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.setType("audio/wav");
        shareIntent.putExtra(Intent.EXTRA_STREAM, fileUri);
        shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        startActivity(Intent.createChooser(shareIntent, "Share audio via"));
    }

    private boolean checkPermission() {
        return ActivityCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED;
    }

    private void requestPermission() {
        ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, PERMISSION_REQUEST_CODE);
    }

    @Override
    protected void onDestroy() {
        if (tts != null) {
            tts.stop();
            tts.shutdown();
        }
        super.onDestroy();
    }
}
