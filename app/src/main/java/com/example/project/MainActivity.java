package com.example.project;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.database.Cursor;
import android.media.AudioAttributes;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.View;
import android.widget.Button;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;


public class MainActivity extends AppCompatActivity {

    TextView titleTextView;
    TextView titleTextView2;
    TextView durationTextView;
    TextView durationTextView2;

    Button pickFileButton;
    Button playButton;
    Button playButton2;
    Button insertButton;
    Button extractButton;
    Button analyzeButton;
    Button saveButton;

    SeekBar audioSeekbar;
    SeekBar audioSeekbar2;

    String duration;
    MediaPlayer mediaPlayer;
    ScheduledExecutorService timer;

    public static final int PICK_AUDIO = 99;
    public static final int PICK_IMAGE = 98;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        titleTextView = findViewById(R.id.titleTextView);
        titleTextView2 = findViewById(R.id.titleTextView2);
        durationTextView = findViewById(R.id.durationTextView);
        durationTextView2 = findViewById(R.id.durationTextView2);

        pickFileButton = findViewById(R.id.pickFileButton);
        playButton = findViewById(R.id.playButton);
        playButton2 = findViewById(R.id.playButton2);
        insertButton = findViewById(R.id.insertButton);
        extractButton = findViewById(R.id.extractButton);
        analyzeButton = findViewById(R.id.analyzeButton);
        saveButton = findViewById(R.id.saveButton);

        audioSeekbar = findViewById(R.id.audioSeekbar);
        audioSeekbar2 = findViewById(R.id.audioSeekbar2);


        pickFileButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
                intent.addCategory(Intent.CATEGORY_OPENABLE);
                intent.setType("audio/*");
                startActivityForResult(intent, PICK_AUDIO);
            }
        });

        playButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (mediaPlayer != null) {
                    if (mediaPlayer.isPlaying()) {
                        mediaPlayer.pause();
                        playButton.setText("PLAY");
                        timer.shutdown();
                    } else {
                        mediaPlayer.start();
                        playButton.setText("PAUSE");
                        timer = Executors.newScheduledThreadPool(1);
                        timer.scheduleAtFixedRate(new Runnable() {
                            @Override
                            public void run() {
                                if (mediaPlayer != null) {
                                    if (!audioSeekbar.isPressed()) {
                                        audioSeekbar.setProgress(mediaPlayer.getCurrentPosition());
                                    }
                                }
                            }
                        }, 10, 10, TimeUnit.MILLISECONDS);
                    }
                }
            }
        });

        insertButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent();
                intent.setType("image/*");
                intent.setAction(Intent.ACTION_GET_CONTENT);
                startActivityForResult(Intent.createChooser(intent, "Select Picture"), PICK_IMAGE);
            }
        });

        extractButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                Toast toast = Toast.makeText(getApplicationContext(), "Use extract watermark methode here", Toast.LENGTH_LONG);
                toast.show();


                // extractWatermark(audioUri); Extracting methode call...
            }
        });

        analyzeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                Toast toast = Toast.makeText(getApplicationContext(), "Use analyze watermark methode here", Toast.LENGTH_LONG);
                toast.show();


                // analyzeWatermark(Uri audioUri);  Analyzing methode call...
            }
        });

        audioSeekbar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
                if (mediaPlayer != null) {
                    int millis = mediaPlayer.getCurrentPosition();
                    long total_secs = TimeUnit.SECONDS.convert(millis, TimeUnit.MILLISECONDS);
                    long mins = TimeUnit.MINUTES.convert(total_secs, TimeUnit.SECONDS);
                    long secs = total_secs - (mins * 60);
                    durationTextView.setText(mins + ":" + secs + " / " + duration);
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                if (mediaPlayer != null) {
                    mediaPlayer.seekTo(audioSeekbar.getProgress());
                }
            }
        });

        playButton.setEnabled(false);
        playButton2.setEnabled(false);
        analyzeButton.setEnabled(false);
        extractButton.setEnabled(false);
        insertButton.setEnabled(false);
        saveButton.setEnabled(false);

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == PICK_AUDIO && resultCode == RESULT_OK) {
            if (data != null) {
                Uri audioUri = data.getData();
                createMediaPlayer(audioUri);
            }
        }

        if (requestCode == PICK_IMAGE && resultCode == RESULT_OK){
            if (data != null) {
                Toast toast = Toast.makeText(getApplicationContext(), "Use Insert watermark methode here", Toast.LENGTH_LONG);
                toast.show();

                Uri imageUri = data.getData();
                // applyWatermark(imageUri, audioUri);  Watermarking methode call...
            }
        }
    }

    public void createMediaPlayer(Uri uri) {
        mediaPlayer = new MediaPlayer();
        mediaPlayer.setAudioAttributes(
                new AudioAttributes.Builder()
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .build()
        );

        try {
            mediaPlayer.setDataSource(getApplicationContext(), uri);
            mediaPlayer.prepare();

            titleTextView.setText(getNameFromUri(uri));
            playButton.setEnabled(true);
            insertButton.setEnabled(true);
            analyzeButton.setEnabled(true);

            int millis = mediaPlayer.getDuration();
            long total_secs = TimeUnit.SECONDS.convert(millis, TimeUnit.MILLISECONDS);
            long mins = TimeUnit.MINUTES.convert(total_secs, TimeUnit.SECONDS);
            long secs = total_secs - (mins * 60);

            duration = mins + ":" + secs;

            durationTextView.setText(("00:00 / " + duration));

            audioSeekbar.setMax(millis);
            audioSeekbar.setProgress(0);

            mediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                @Override
                public void onCompletion(MediaPlayer mediaPlayer) {
                    releaseMediaPlayer();
                }
            });

        } catch (IOException e) {
            titleTextView.setText(e.toString());
        }
    }

    @SuppressLint("Range")
    public String getNameFromUri(Uri uri) {

        String fileName = "";
        Cursor cursor = null;

        cursor = getContentResolver().query(uri, new String[]{
                MediaStore.Images.ImageColumns.DISPLAY_NAME
        }, null, null, null);

        if (cursor != null && cursor.moveToFirst()) {
            fileName = cursor.getString(cursor.getColumnIndex(MediaStore.Images.ImageColumns.DISPLAY_NAME));
        }

        if (cursor != null) {
            cursor.close();
        }

        return fileName;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        releaseMediaPlayer();
    }

    public void releaseMediaPlayer() {
        if (timer != null) {
            timer.shutdown();
        }
        if (mediaPlayer != null) {
            mediaPlayer.release();
            mediaPlayer = null;
        }
        playButton.setEnabled(false);
        analyzeButton.setEnabled(false);
        extractButton.setEnabled(false);
        insertButton.setEnabled(false);

        titleTextView.setText("Title");
        durationTextView.setText("00:00 / 00:00");
        audioSeekbar.setMax(100);
        audioSeekbar.setProgress(0);
    }



    // TODO: Implements the watermarking methods here:

    public void applyWatermark(Uri imageUri, Uri audioUri) {

    }

    public void extractWatermark(Uri audioUri) {

    }

    public boolean analyzeWatermark(Uri audioUri) {
        return false;
    }

}