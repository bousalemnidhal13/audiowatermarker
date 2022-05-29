package com.example.project;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;


public class MainActivity extends AppCompatActivity {




    // declarations needed for watermarking methodes
    private static final int[] shiftNum = {1,3,7,15,31,63,127,255};
    public static final String MARKER = "@RM";
    public static final int HEADER_LENGTH = MARKER.length() * 8 + 32 + 32 + 8 + 16 + 8 + 8;
    private int[] dataHeader = new int[HEADER_LENGTH];









    // android views declaration
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

    // for intent result
    public static final int PICK_AUDIO = 99;
    public static final int PICK_IMAGE = 98;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // associate java fields with xml views
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

                Toast toast = Toast.makeText(getApplicationContext(), "Use extract watermark methode here", Toast.LENGTH_LONG);
                toast.show();


                // extractWatermark(audioUri); Extracting methode call...
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
        playButton2.setEnabled(false);
        analyzeButton.setEnabled(false);
        extractButton.setEnabled(false);
        insertButton.setEnabled(false);
        saveButton.setEnabled(false);

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        // get audio uri
        if (requestCode == PICK_AUDIO && resultCode == RESULT_OK) {
            if (data != null) {
                Uri audioUri = data.getData();
                createMediaPlayer(audioUri);
            }
        }

        // get image uri
        if (requestCode == PICK_IMAGE && resultCode == RESULT_OK){
            if (data != null) {

                // get image data from uri
                Uri imageUri = data.getData();

                // try to get image from uri
                try {
                    Bitmap bitmap = null;

                    // get bitmap(image) from uri
                    bitmap = MediaStore.Images.Media.getBitmap(this.getContentResolver(), imageUri);

                    // convert bitmap to byte[] and put it in byte array
                    ByteArrayOutputStream stream = new ByteArrayOutputStream();
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 70, stream);
                    byte[] message = new byte[0];
                    message = stream.toByteArray();

                    Toast toast = Toast.makeText(getApplicationContext(), message.toString(), Toast.LENGTH_LONG);
                    toast.show();

                } catch (IOException e) {
                    e.printStackTrace();
                }

                // applyWatermark(imageUri, audioUri);  Watermarking methode call...
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
        extractButton.setEnabled(false);
        insertButton.setEnabled(false);

        titleTextView.setText("Title");
        durationTextView.setText("00:00 / 00:00");
        audioSeekbar.setMax(100);
        audioSeekbar.setProgress(0);
    }



    // TODO: Implements the watermarking methods here:


    // the methode for inserting the watermark
    public int[] applyWatermark(int[] audioSamples, byte[] message) {

        int LSBUSed = 1; // Integer.parseInt((String)this.properties.get("lsb"));
        int shiftNumber = this.shiftNum[LSBUSed-1];
        int[] newSample = new int[audioSamples.length];
        int offset = 0;

        // write marker to audio samples
        for (int i=0;i<MARKER.length();i++){
            int kar = MARKER.charAt(i);
            for (int j=0;j<8;j++){
                int bitExtract = kar & 1;
                newSample[offset] = audioSamples[offset] & ~1;
                newSample[offset] |= bitExtract;
                kar >>= 1;
                offset++;
            }
        }
        // end write marker --- next offset = 24




        // write length message to samples
        int lengthMessage = (int) Math.ceil((double)message.length * 8 /(double) LSBUSed);
        int length2 = lengthMessage;
        for (int i=0;i<32;i++){
            int bitExtract = lengthMessage & 1;
            newSample[offset] = audioSamples[offset] & ~1;
            newSample[offset] |= bitExtract;
            lengthMessage >>= 1;
            offset++;   
        }
        // end write length message --- next offset = 56
        //System.out.println("next:" + offset);





        // write message
        int realLength = 0;
        int byteExtract = 0;
        int bitRem=0;
        int bitDiambil = 0;
        int i = 0;
        int lastBit = (message.length * 8) % LSBUSed;

        while (i<message.length){
            int bitExtract = message[i] & 1;
            message[i] >>= 1;
            bitDiambil++;

            byteExtract |= bitExtract << bitRem;

            bitRem++;

            if (bitDiambil >= 8){
                i++;
                bitDiambil = 0;
                realLength++;
            }

            if (bitRem >=LSBUSed){
                newSample[offset] = audioSamples[offset] & ~shiftNumber;
                newSample[offset] |= byteExtract;
                offset++;
                byteExtract = 0;
                bitRem = 0;
            }else if(bitRem == lastBit && (offset-(HEADER_LENGTH-1)) == length2) {
                newSample[offset] = audioSamples[offset] & ~ this.shiftNum[lastBit];
                newSample[offset] |= byteExtract;
                offset++;
                byteExtract = 0;
                bitRem = 0;
                i++;
                bitDiambil=0;
            }

            if (offset >=audioSamples.length){
                break;
            }
        }

        for (int k=offset;k<audioSamples.length;k++){
            newSample[k] = audioSamples[k];
        }

        offset = 24;
        //System.out.println("len:" + realLength);
        int realLen = (int) Math.ceil((double)realLength * 8 /(double) LSBUSed);
        for (int j=0;j<32;j++){
            int bitExtract = realLen & 1;
            newSample[offset] = newSample[offset] & ~1;
            newSample[offset] |= bitExtract;
            realLen >>= 1;
            offset++;
        }

        return newSample;

    }

    // the methode for extracting the watermark
    public byte[] extractWatermark(int[] audioSamples) {

        int LSBUSed = 1; // Integer.parseInt((String)this.properties.get("lsb"));

        int byteExtract = 0;
        int bitDiambil=0;


        int startIndex = 128;


        // Testing with 10000
        int numByte = 10000; // Integer.parseInt((String) this.properties.get("msgSize"));//lengthMessage * LSBUSed / 8;
        byte[] message = new byte[numByte];

        int i = 0;
        int bitRem = 0;

        while(i<numByte){
            int bitExtract = audioSamples[startIndex] & 1;
            audioSamples[startIndex] >>=1;
            bitDiambil++;
            byteExtract |= bitExtract << bitRem;
            bitRem++;

            if (bitDiambil >=LSBUSed){
                startIndex++;
                bitDiambil = 0;
            }
            if (bitRem >=8){
                message[i] = (byte) byteExtract;
                i++;
                bitRem = 0;
                byteExtract = 0;
            }

            if (startIndex >= audioSamples.length)
                break;

        }
        return message;

    }

    // the methode for analyzing the watermark
    public boolean isContainedWatermark() {
        byte[] dataHead = new byte[MARKER.length()];

        int startIndex = 0;
        for (int i=0;i<dataHead.length;i++){
            int charEx = 0;
            for (int j=0;j<8;j++){
                int bitExtract = this.dataHeader[startIndex] & 1;
                charEx |= bitExtract << j;
                startIndex++;
            }
            dataHead[i] = (byte) charEx;
        }
        String head = new String(dataHead);
        System.out.println(head);
        if (head.equals(MARKER))
            return true;
        else
            return false;
    }

}