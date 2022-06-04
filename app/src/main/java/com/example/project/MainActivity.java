package com.example.project;

import static com.example.project.AudioUtils.ReadingAudioFile;
import static com.example.project.AudioUtils.WriteCleanAudioWav;
import static com.example.project.AudioUtils.float32ToInt16;
import static com.example.project.BitmapUtils.bitmapToFile;
import static com.example.project.BitmapUtils.byteToBitmap;
import static com.example.project.WatermarkUtils.applyWatermark;
import static com.example.project.WatermarkUtils.extractWatermark;


import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;


import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.media.AudioAttributes;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import java.util.Arrays;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;


public class MainActivity extends AppCompatActivity {




    // uri for the audio file
    Uri audioUri;

    Bitmap bitmap;

    short[] int16;


    // android views declaration
    ImageView messageImageview;

    TextView titleTextView;
    TextView durationTextView;

    Button pickFileButton;
    Button playButton;
    Button insertButton;
    Button extractButton;
    Button analyzeButton;

    SeekBar audioSeekbar;

    String duration;
    MediaPlayer mediaPlayer;
    ScheduledExecutorService timer;

    // for intent result
    public static final int PICK_AUDIO = 99;
    public static final int PICK_IMAGE = 98;
    public static final int PICK_WATERMARK_AUDIO = 97;


    // convert byte data to number



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // associate java fields with xml views
        messageImageview = findViewById(R.id.messageImageview);

        titleTextView = findViewById(R.id.titleTextView);;
        durationTextView = findViewById(R.id.durationTextView);

        pickFileButton = findViewById(R.id.pickFileButton);
        playButton = findViewById(R.id.playButton);
        insertButton = findViewById(R.id.insertButton);
        extractButton = findViewById(R.id.extractButton);
        analyzeButton = findViewById(R.id.analyzeButton);

        audioSeekbar = findViewById(R.id.audioSeekbar);

        // listener for picking an audio file
        pickFileButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
                intent.addCategory(Intent.CATEGORY_OPENABLE);
                intent.setType("audio/*");
                startActivityForResult(intent, PICK_AUDIO);
            }
        });

        // listener for playing the audio file
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

        // listener for picking an image for the inserting methode
        insertButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent();
                intent.setType("image/*");
                intent.setAction(Intent.ACTION_GET_CONTENT);
                startActivityForResult(Intent.createChooser(intent, "Select Picture"), PICK_IMAGE);
            }
        });


        // listener for the extracting methode
        extractButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
                intent.addCategory(Intent.CATEGORY_OPENABLE);
                intent.setType("audio/*");
                startActivityForResult(intent, PICK_WATERMARK_AUDIO);

            }
        });

        // listener for the analyze methode
        analyzeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                Toast toast = Toast.makeText(getApplicationContext(), "Use analyze watermark methode here", Toast.LENGTH_LONG);
                toast.show();


                // analyzeWatermark(Uri audioUri);  Analyzing methode call...
            }
        });

        // audio player seekbar
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

        // turn off the buttons for the moment
        playButton.setEnabled(false);
        analyzeButton.setEnabled(false);
        // extractButton.setEnabled(false);
        insertButton.setEnabled(false);

    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);


        // get audio uri
        if (requestCode == PICK_AUDIO && resultCode == RESULT_OK) {
            if (data != null) {

                // create the media player from the data in the uri
                audioUri = data.getData();
                createMediaPlayer(audioUri);

            }
        }

        // get watermarked audio uri
        if (requestCode == PICK_WATERMARK_AUDIO && resultCode == RESULT_OK) {
            if (data != null) {

                Uri audioWUri = data.getData();
                String audioFileAbsolutePath = UriUtils.getPathFromUri(this, audioWUri);

                try {
                    int[] audioData = ReadingAudioFile(audioFileAbsolutePath);
                    short[] int16 = float32ToInt16(audioData);
                    byte[] message = extractWatermark(int16);

                    System.out.println(Arrays.toString(message));

                    Bitmap bitmap = BitmapUtils.byteToBitmap(message);
                    messageImageview.setImageBitmap(bitmap);


                } catch (IOException e) {
                    e.printStackTrace();
                }


            }
        }

        // get image uri
        if (requestCode == PICK_IMAGE && resultCode == RESULT_OK){
            if (data != null) {

                // get image data from uri
                Uri imageUri = data.getData();

                // try to get image from uri
                try {

                    // get bitmap(image) from uri
                    bitmap = MediaStore.Images.Media.getBitmap(this.getContentResolver(), imageUri);

                    // convert bitmap to byte[] and put it in byte array (the byte[] here is signed!)
                    ByteArrayOutputStream stream = new ByteArrayOutputStream();
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 70, stream);
                    byte[] message = stream.toByteArray();


                    // TODO : new approach !!!
                    int permissionCheck = ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE);
                    if (permissionCheck == PackageManager.PERMISSION_GRANTED){

                        System.out.println("First condition");

                    } else {
                        requestPermissions(new String[] {Manifest.permission.WRITE_EXTERNAL_STORAGE}, 0);
                        System.out.println("Second condition");
                    }


                    String audioFileAbsolutePath = UriUtils.getPathFromUri(this, audioUri);
                    int[] audioData = ReadingAudioFile(audioFileAbsolutePath);

                    int16 = float32ToInt16(applyWatermark(audioData, message));

                    WriteCleanAudioWav(this, "new_song.wav", int16);
                    System.out.println(Arrays.toString(int16));

                    Toast toast = Toast.makeText(getApplicationContext(), "Watermark inserted and saved", Toast.LENGTH_LONG);
                    toast.show();

                } catch (Exception e) {
                    e.printStackTrace();
                }

            }
        }
    }

    // create media player for the audio selected
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

    // get the name of the audio selected
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


    // release the player when destroying the app
    @Override
    protected void onDestroy() {
        super.onDestroy();
        releaseMediaPlayer();
    }

    // the release player methode
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
        // extractButton.setEnabled(false);
        insertButton.setEnabled(false);

        titleTextView.setText("Title");
        durationTextView.setText("00:00 / 00:00");
        audioSeekbar.setMax(100);
        audioSeekbar.setProgress(0);
    }
}
