package com.example.docuscan;

import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.provider.MediaStore;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.text.TextRecognition;
import com.google.mlkit.vision.text.TextRecognizer;
import com.google.mlkit.vision.text.latin.TextRecognizerOptions;

public class MainActivity2 extends AppCompatActivity {

    private static final int REQUEST_IMAGE_CAPTURE = 1;

    ImageView imageView;
    Button captureBtn, extractBtn;
    Bitmap imageBitmap;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main2);

        imageView = findViewById(R.id.capturedImageView);
        captureBtn = findViewById(R.id.captureBtn);
        extractBtn = findViewById(R.id.extractBtn);

        // Open camera
        captureBtn.setOnClickListener(v -> {
            Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
            if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
                startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE);
            }
        });

        // Extract text using ML Kit
        extractBtn.setOnClickListener(v -> {
            if (imageBitmap != null) {
                recognizeText(imageBitmap);
            } else {
                Toast.makeText(this, "Please capture an image first.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == REQUEST_IMAGE_CAPTURE && resultCode == RESULT_OK && data != null) {
            Bundle extras = data.getExtras();
            imageBitmap = (Bitmap) extras.get("data");
            imageView.setImageBitmap(imageBitmap);
        }
    }

    private void recognizeText(Bitmap bitmap) {
        InputImage image = InputImage.fromBitmap(bitmap, 0);
        TextRecognizer recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS);

        recognizer.process(image)
                .addOnSuccessListener(result -> {
                    String extractedText = result.getText();
                    Intent intent = new Intent(MainActivity2.this, MainActivity.class);
                    intent.putExtra("captured_text", extractedText);
                    startActivity(intent);
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Text recognition failed.", Toast.LENGTH_SHORT).show();
                    e.printStackTrace();
                });
    }
}
