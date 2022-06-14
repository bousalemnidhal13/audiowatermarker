package com.example.project;

import static java.nio.ByteOrder.BIG_ENDIAN;
import static java.nio.ByteOrder.LITTLE_ENDIAN;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.AudioAttributes;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;


import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import rm.com.audiowave.AudioWaveView;

public class ExtractActivity extends AppCompatActivity {


    int type [] = {0, 1, 0, 0, 1, 1, 1, 1, 1, 1, 1, 0, 1};
    int numberOfBytes[] = {4, 4, 4, 4, 4, 2, 2, 4, 4, 2, 2, 4, 4};
    int chunkSize, subChunk1Size, sampleRate, byteRate, subChunk2Size=1, bytePerSample;
    short audioFomart, numChannels, blockAlign, bitsPerSample=8;
    String chunkID, format, subChunk1ID, subChunk2ID;


    private static final int[] shiftNum = {1,3,7,15,31,63,127,255};
    public static final String MARKER = "@RM";
    public static final int HEADER_LENGTH = MARKER.length() * 8 + 32 + 32 + 8 + 16 + 8 + 8;
    private int[] dataHeader = new int[HEADER_LENGTH];


    TextView titleTextView;
    TextView durationTextView;

    Button pickFileButton;
    Button playButton;
    Button extractButton;

    ImageView messageImageview;

    SeekBar audioSeekbar;

    AudioWaveView waveView;

    MediaPlayer mediaPlayer;
    String duration;
    ScheduledExecutorService timer;

    Uri audioUri;
    String audioFileAbsolutePath;

    public static final int PICK_AUDIO = 99;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_extract);

        pickFileButton = findViewById(R.id.pickFileButtonE);
        playButton = findViewById(R.id.playButtonE);
        extractButton = findViewById(R.id.extractButtonE);

        titleTextView = findViewById(R.id.titleTextViewE);
        durationTextView = findViewById(R.id.durationTextViewE);

        audioSeekbar = findViewById(R.id.audioSeekbarE);

        waveView = findViewById(R.id.waveE);

        messageImageview = findViewById(R.id.messageImageviewE);


        pickFileButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
                intent.addCategory(Intent.CATEGORY_OPENABLE);
                intent.setType("audio/*");
                startActivityForResult(intent, PICK_AUDIO);
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


        extractButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                Toast tst = Toast.makeText(getApplicationContext(), "EXTRACTING...", Toast.LENGTH_LONG);
                tst.show();

                    new Thread(new Runnable() {
                        @Override
                        public void run() {
                            try {
                                int[] audioData = ReadingAudioFile(audioFileAbsolutePath);

                                if (isContainedWatermark(audioData)){
                                    short[] int16 = float32ToInt16(audioData);
                                    byte[] message = extractWatermark(int16);

                                    System.out.println(Arrays.toString(message));
                                    System.out.println(message.length);

                                    byte[] trueMessage = addElement(message, (byte) -119);

                                    System.out.println(Arrays.toString(trueMessage));
                                    System.out.println(message.length);

                                    writeToFile(trueMessage);

                                    File sd = Environment.getExternalStorageDirectory();
                                    File image = new File(sd + "/watermarked", "watermark.png");

                                    BitmapFactory.Options bmOptions = new BitmapFactory.Options();
                                    Bitmap bmp = BitmapFactory.decodeFile(image.getAbsolutePath(), bmOptions);
                                    bmp = Bitmap.createScaledBitmap(bmp,messageImageview.getWidth(),messageImageview.getHeight(),true);

                                    Bitmap finalBmp = bmp;
                                    messageImageview.post(new Runnable() {
                                        @Override
                                        public void run() {
                                            messageImageview.setImageBitmap(finalBmp);
                                        }
                                    });

                                    runOnUiThread(new Runnable() {
                                        @Override
                                        public void run() {
                                            Toast toast = Toast.makeText(getApplicationContext(), "WATERMARK FOUND AND SAVED", Toast.LENGTH_LONG);
                                            toast.show();
                                        }
                                    });
                                } else {
                                    runOnUiThread(new Runnable() {
                                        @Override
                                        public void run() {
                                            Toast toast = Toast.makeText(getApplicationContext(), "WATERMARK NOT FOUND", Toast.LENGTH_LONG);
                                            toast.show();
                                        }
                                    });
                                }
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        }
                    }).start();

            }
        });

        playButton.setEnabled(false);
        extractButton.setEnabled(false);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        // get audio uri
        if (requestCode == PICK_AUDIO && resultCode == RESULT_OK) {
            if (data != null) {

                playButton.setEnabled(false);
                extractButton.setEnabled(false);

                // create the media player from the data in the uri
                audioUri = data.getData();
                createMediaPlayer(audioUri);

                audioFileAbsolutePath = UriUtils.getPathFromUri(this, audioUri);

                Toast tst = Toast.makeText(getApplicationContext(), "SAMPLING...", Toast.LENGTH_LONG);
                tst.show();

                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            int[] audioData = new int[0];
                            audioData = ReadingAudioFile(audioFileAbsolutePath);
                            short[] int16 = float32ToInt16(audioData);
                            waveView.setScaledData(ShortArray2ByteArray(int16));

                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    Toast toast = Toast.makeText(getApplicationContext(), "DONE", Toast.LENGTH_LONG);
                                    toast.show();
                                    playButton.setEnabled(true);
                                    extractButton.setEnabled(true);
                                }
                            });
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }

                }).start();

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

        extractButton.setEnabled(false);

        titleTextView.setText("Title");
        durationTextView.setText("00:00 / 00:00");
        audioSeekbar.setMax(100);
        audioSeekbar.setProgress(0);
    }


    // the methode for extracting the watermark
    public byte[] extractWatermark(short[] audioSamples) {

        int LSBUSed = 1; // Integer.parseInt((String)this.properties.get("lsb"));
        int shiftNumber = this.shiftNum[LSBUSed-1];

        int byteExtract = 0;
        int bitDiambil=0;

        int startIndex = 128;

        int numByte = audioSamples.length * LSBUSed / 8; // Integer.parseInt((String) this.properties.get("msgSize"));// lengthMessage * LSBUSed / 8;
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
    public boolean isContainedWatermark(int[] audioData) {

        for (int i=0;i<this.dataHeader.length;i++){
            this.dataHeader[i] = audioData[i];
        }

        byte[] dataHead = new byte[MARKER.length()];

        int startIndex = 0;
        for (int i=0;i<dataHead.length;i++){
            int charEx = 0;
            for (int j=0;j<8;j++){
                int bitExtract = dataHeader[startIndex] & 1;
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


    public static ByteBuffer ByteArrayToNumber(byte bytes[], int numOfBytes, int type){
        ByteBuffer buffer = ByteBuffer.allocate(numOfBytes);
        if (type == 0){
            buffer.order(BIG_ENDIAN); // Check the illustration. If it says little endian, use LITTLE_ENDIAN
        }
        else{
            buffer.order(LITTLE_ENDIAN);
        }
        buffer.put(bytes);
        buffer.rewind();
        return buffer;
    }


    public static float convertToFloat(byte[] array, int type) {
        ByteBuffer buffer = ByteBuffer.wrap(array);
        if (type == 1){
            buffer.order(LITTLE_ENDIAN);
        }
        return (float) buffer.getShort();
    }


    public int[] ReadingAudioFile(String audioFile) throws IOException {

        try {
            File file = new File(audioFile);
            int length = (int) file.length();
            //System.out.println(length);
            InputStream fileInputstream = new FileInputStream(file);
            ByteBuffer byteBuffer;
            for(int i=0; i<numberOfBytes.length; i++){
                byte byteArray[] = new byte[numberOfBytes[i]];
                int r = fileInputstream.read(byteArray, 0, numberOfBytes[i]);
                byteBuffer = ByteArrayToNumber(byteArray, numberOfBytes[i], type[i]);
                if (i == 0) {chunkID =  new String(byteArray);/* System.out.println(chunkID); */}
                if (i == 1) {chunkSize = byteBuffer.getInt();/* System.out.println(chunkSize); */}
                if (i == 2) {format =  new String(byteArray);/* System.out.println(format); */}
                if (i == 3) {subChunk1ID = new String(byteArray);/* System.out.println(subChunk1ID); */}
                if (i == 4) {subChunk1Size = byteBuffer.getInt();/* System.out.println(subChunk1Size); */}
                if (i == 5) {audioFomart = byteBuffer.getShort();/* System.out.println(audioFomart); */}
                if (i == 6) {numChannels = byteBuffer.getShort();/* System.out.println(numChannels); */}
                if (i == 7) {sampleRate = byteBuffer.getInt();/* System.out.println(sampleRate); */}
                if (i == 8) {byteRate = byteBuffer.getInt();/* System.out.println(byteRate); */}
                if (i == 9) {blockAlign = byteBuffer.getShort();/* System.out.println(blockAlign); */}
                if (i == 10) {bitsPerSample = byteBuffer.getShort();/* System.out.println(bitsPerSample); */}
                if (i == 11) {
                    subChunk2ID = new String(byteArray) ;
                    if(subChunk2ID.compareTo("data") == 0) {
                        continue;
                    }
                    else if( subChunk2ID.compareTo("LIST") == 0) {
                        byte byteArray2[] = new byte[4];
                        r = fileInputstream.read(byteArray2, 0, 4);
                        byteBuffer = ByteArrayToNumber(byteArray2, 4, 1);
                        int temp = byteBuffer.getInt();
                        //redundant data reading
                        byte byteArray3[] = new byte[temp];
                        r = fileInputstream.read(byteArray3, 0, temp);
                        r = fileInputstream.read(byteArray2, 0, 4);
                        subChunk2ID = new String(byteArray2) ;
                    }
                }
                if (i == 12) {subChunk2Size = byteBuffer.getInt();System.out.println(subChunk2Size);}
            }
            bytePerSample = bitsPerSample/8;
            int value;
            ArrayList<Integer> dataVector = new ArrayList<>();
            while (true){
                byte byteArray[] = new byte[bytePerSample];
                int v = fileInputstream.read(byteArray, 0, bytePerSample);
                value = (int) convertToFloat(byteArray,1);
                dataVector.add(value);
                if (v == -1) break;
            }
            int data [] = new int[dataVector.size()];
            for(int i=0;i<dataVector.size();i++){
                data[i] = dataVector.get(i);
            }
            // System.out.println("Total data bytes "+sum);
            return data;
        }
        catch (Exception e){
            System.out.println("Error: "+e);
            int[] f = new int[1];
            return f;
        }
    }


    public static void checkExternalMedia(){
        boolean mExternalStorageAvailable = false;
        boolean mExternalStorageWriteable = false;
        String state = Environment.getExternalStorageState();

        if (Environment.MEDIA_MOUNTED.equals(state)) {
            // Can read and write the media
            mExternalStorageAvailable = mExternalStorageWriteable = true;
        } else if (Environment.MEDIA_MOUNTED_READ_ONLY.equals(state)) {
            // Can only read the media
            mExternalStorageAvailable = true;
            mExternalStorageWriteable = false;
        } else {
            // Can't read or write
            mExternalStorageAvailable = mExternalStorageWriteable = false;
        }
        System.out.println("\n\nExternal Media: readable="
                +mExternalStorageAvailable+" writable="+mExternalStorageWriteable);
    }


    public static short[] float32ToInt16(int arr[]){

        short int16Arr[] = new short [arr.length];
        for(int i=0; i<arr.length; i++){
            if(arr[i]<0) {
                if (arr[i]>-1) {
                    int16Arr[i] = 0;
                }
                else{
                    int16Arr[i] = (short) Math.ceil((double)arr[i]);
                }
            }
            else if (arr[i]>0){
                int16Arr[i] = (short) Math.floor((double)arr[i]);
            }
            else{
                int16Arr[i] = 0;
            }
        }
        return int16Arr;
    }


    public void writeToFile(byte[] array) {
        try {
            checkExternalMedia();
            File root = android.os.Environment.getExternalStorageDirectory();
            String path = root.getAbsolutePath();
            File file = new File(path + "/watermarked/watermark.png");

            FileOutputStream stream = new FileOutputStream(file);

            stream.write(array);

        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    public static byte[] addElement(byte[] arr, byte element) {
        byte newArr[] = new byte[arr.length + 1];
        newArr[0] = element;
        for (int i = 0; i < arr.length; i++) {
            newArr[i + 1] = arr[i];
        }
        return newArr;
    }

    public static byte[] ShortArray2ByteArray(short[] values){
        ByteBuffer buffer = ByteBuffer.allocate(2 * values.length);
        buffer.order(LITTLE_ENDIAN); // data must be in little endian format
        for (short value : values){
            buffer.putShort(value);
        }
        buffer.rewind();
        return buffer.array();
    }

}
