package com.example.project;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;

public class IndexActivity extends AppCompatActivity {

    Button InsertButton;
    Button ExtractButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_index);

        InsertButton = findViewById(R.id.InsertWatermark);
        ExtractButton = findViewById(R.id.ExtractWatermark);

        InsertButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivity(new Intent(IndexActivity.this, InsertActivity.class));
            }
        });

        ExtractButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivity(new Intent(IndexActivity.this, ExtractActivity.class));
            }
        });
    }

}
