package com.example.project;

import static java.nio.ByteOrder.BIG_ENDIAN;
import static java.nio.ByteOrder.LITTLE_ENDIAN;

import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.content.PackageManagerCompat;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.AudioAttributes;
import android.media.MediaPlayer;
import android.net.Uri;
import android.nfc.Tag;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Locale;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;


public class MainActivity extends AppCompatActivity {

    // declarations needed for the read and write operations
    int type [] = {0, 1, 0, 0, 1, 1, 1, 1, 1, 1, 1, 0, 1};
    int numberOfBytes[] = {4, 4, 4, 4, 4, 2, 2, 4, 4, 2, 2, 4, 4};
    int chunkSize, subChunk1Size, sampleRate, byteRate, subChunk2Size=1, bytePerSample;
    short audioFomart, numChannels, blockAlign, bitsPerSample=8;
    String chunkID, format, subChunk1ID, subChunk2ID;




    // declarations needed for watermarking methodes
    private static final int[] shiftNum = {1,3,7,15,31,63,127,255};
    public static final String MARKER = "@RM";
    public static final int HEADER_LENGTH = MARKER.length() * 8 + 32 + 32 + 8 + 16 + 8 + 8;
    private int[] dataHeader = new int[HEADER_LENGTH];






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



    // convert byte data to number
    public ByteBuffer ByteArrayToNumber(byte bytes[], int numOfBytes, int type){
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



    // convert data to float
    public float convertToFloat(byte[] array, int type) {
        ByteBuffer buffer = ByteBuffer.wrap(array);
        if (type == 1){
            buffer.order(LITTLE_ENDIAN);
        }
        return (float) buffer.getShort();
    }




    // Methode to read the audio file
    public float[] ReadingAudioFile(String audioFile) throws IOException{
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
                if (i == 0) {chunkID =  new String(byteArray); System.out.println(chunkID);}
                if (i == 1) {chunkSize = byteBuffer.getInt(); System.out.println(chunkSize);}
                if (i == 2) {format =  new String(byteArray); System.out.println(format);}
                if (i == 3) {subChunk1ID = new String(byteArray);System.out.println(subChunk1ID);}
                if (i == 4) {subChunk1Size = byteBuffer.getInt(); System.out.println(subChunk1Size);}
                if (i == 5) {audioFomart = byteBuffer.getShort(); System.out.println(audioFomart);}
                if (i == 6) {numChannels = byteBuffer.getShort();System.out.println(numChannels);}
                if (i == 7) {sampleRate = byteBuffer.getInt();System.out.println(sampleRate);}
                if (i == 8) {byteRate = byteBuffer.getInt();System.out.println(byteRate);}
                if (i == 9) {blockAlign = byteBuffer.getShort();System.out.println(blockAlign);}
                if (i == 10) {bitsPerSample = byteBuffer.getShort();System.out.println(bitsPerSample);}
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
            float value;
            ArrayList<Float> dataVector = new ArrayList<>();
            while (true){
                byte byteArray[] = new byte[bytePerSample];
                int v = fileInputstream.read(byteArray, 0, bytePerSample);
                value = convertToFloat(byteArray,1);
                dataVector.add(value);
                if (v == -1) break;
            }
            float data [] = new float[dataVector.size()];
            for(int i=0;i<dataVector.size();i++){
                data[i] = dataVector.get(i);
            }
            // System.out.println("Total data bytes "+sum);
            return data;
        }
        catch (Exception e){
            System.out.println("Error: "+e);
            float[] f = new float[1];
            return f;
        }
    }




    public byte[] ShortArray2ByteArray(short[] values){
        ByteBuffer buffer = ByteBuffer.allocate(2 * values.length);
        buffer.order(LITTLE_ENDIAN); // data must be in little endian format
        for (short value : values){
            buffer.putInt(value);
        }
        buffer.rewind();
        return buffer.array();
    }





    public void checkExternalMedia(){
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






    public void WriteCleanAudioWav(Context context, String newFileName, short[] wavData) throws Exception{
        File dir = null;
        try{
            checkExternalMedia();
            File root = android.os.Environment.getExternalStorageDirectory();
            System.out.println("\nExternal file system root: "+root);
            dir = new File (root.getAbsolutePath() + "/watermarked");
            if (dir.exists() == false){
                dir.mkdirs();
                System.out.println("Directory created");
                System.out.println(dir);
            }
            else{
                System.out.println("Directory exists");
                System.out.println(dir);
            }
        }
        catch (Exception e){
            System.out.println("Error "+e);
        }
        try {
            File file = new File(dir, "new_song.wav");
            if (file.exists()) {
                System.out.println("YES file exists");
            }
            //System.out.println(file);
            OutputStream os;
            //System.out.println(newFileName);
            os = new FileOutputStream(file);
            BufferedOutputStream bos = new BufferedOutputStream(os);
            DataOutputStream outFile = new DataOutputStream(bos);

            try {
                long mySubChunk1Size = subChunk1Size;
                int myBitsPerSample= bitsPerSample;
                int myFormat = audioFomart;
                long myChannels = numChannels;
                long mySampleRate = sampleRate;
                long myByteRate = mySampleRate * myChannels * myBitsPerSample/8;
                int myBlockAlign = (int) (myChannels * myBitsPerSample/8);
                // System.out.println("Ei porjonto completed");
                byte clipData[] = ShortArray2ByteArray(wavData);
                // System.out.println("Ei porjonto completed 1");
                long myDataSize = clipData.length;
                long myChunk2Size =  myDataSize * myChannels * myBitsPerSample/8;
                long myChunkSize = 36 + myChunk2Size;

                outFile.writeBytes("RIFF");  // 00 - RIFF
                outFile.writeInt(Integer.reverseBytes((int)myChunkSize)); // 04 - how big is the rest of this file?
                outFile.writeBytes("WAVE");                                 // 08 - WAVE
                outFile.writeBytes("fmt ");                                 // 12 - fmt
                outFile.writeInt(Integer.reverseBytes((int)mySubChunk1Size));  // 16 - size of this chunk
                outFile.writeShort(Short.reverseBytes((short)myFormat));     // 20 - what is the audio format? 1 for PCM = Pulse Code Modulation
                outFile.writeShort(Short.reverseBytes((short)myChannels));   // 22 - mono or stereo? 1 or 2?  (or 5 or ???)
                outFile.writeInt(Integer.reverseBytes((int)mySampleRate));     // 24 - samples per second (numbers per second)
                outFile.writeInt(Integer.reverseBytes((int)myByteRate));       // 28 - bytes per second
                outFile.writeShort(Short.reverseBytes((short)myBlockAlign)); // 32 - # of bytes in one sample, for all channels
                outFile.writeShort(Short.reverseBytes((short)myBitsPerSample));  // 34 - how many bits in a sample(number)?  usually 16 or 24
                outFile.writeBytes("data");                                 // 36 - data
                outFile.writeInt(Integer.reverseBytes((int)myDataSize));       // 40 - how big is this data chunk
                outFile.write(clipData);
                System.out.println("File creation complete");
            }
            catch (Exception e){
                System.out.println("Error "+e);

            }
            outFile.flush();
            outFile.close();
        }
        catch (Exception e){
            System.out.println("Error: "+e);
        }
    }















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
        analyzeButton.setEnabled(false);
        extractButton.setEnabled(false);
        insertButton.setEnabled(false);

    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);


        // get audio uri
        if (requestCode == PICK_AUDIO && resultCode == RESULT_OK) {
            if (data != null) {

                // create the mediaplayer from the data in the uri
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

                    // convert bitmap to byte[] and put it in byte array (the byte[] here is signed!)
                    ByteArrayOutputStream stream = new ByteArrayOutputStream();
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 70, stream);
                    byte[] message;
                    message = stream.toByteArray();

                    // convert byte array to image an display it in the activity
                    Bitmap bmp = BitmapFactory.decodeByteArray(message, 0, message.length);
                    messageImageview.setImageBitmap(bmp);


                    // TODO : new approach !!!
                    int permissionCheck = ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE);
                    if (permissionCheck == PackageManager.PERMISSION_GRANTED){

                        Toast toast = Toast.makeText(getApplicationContext(), "WATERMARK ADDED AND FILE SAVED!", Toast.LENGTH_LONG);
                        toast.show();

                        System.out.println("First condition");

                    } else {
                        requestPermissions(new String[] {Manifest.permission.WRITE_EXTERNAL_STORAGE}, 0);
                        System.out.println("Second condition");
                    }

                    // ISSUE: the path is always null for some reason
                    String audioFileAbsolutePath = null;
                    audioFileAbsolutePath = assetFilePath(this, "song.wav");
                    System.out.println("Path is : " + audioFileAbsolutePath);

                    MainActivity mainActivity = new MainActivity();
                    float[] audioData = mainActivity.ReadingAudioFile(audioFileAbsolutePath);


                    float[] manipulatedAudioData = new float[audioData.length];


                    // TODO: do the changes in the audio data here !




                    short int16[] = float32ToInt16(manipulatedAudioData);

                    mainActivity.WriteCleanAudioWav(this, "new_song.wav", int16);


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
        extractButton.setEnabled(false);
        insertButton.setEnabled(false);

        titleTextView.setText("Title");
        durationTextView.setText("00:00 / 00:00");
        audioSeekbar.setMax(100);
        audioSeekbar.setProgress(0);
    }




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

        for (int k=offset; k<audioSamples.length; k++){
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










    public static short[] float32ToInt16(float arr[]){  //int[]

        // logic is built to support the numpy.int16 output
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





    // Copies specified asset to the file in /files app directory and returns this file absolute path.
    public static String assetFilePath(Context context, String assetName) throws IOException {
        String path = null;
        File file = new File(context.getFilesDir(), assetName);
        if (file.exists() && file.length() > 0) {
            path = file.getAbsolutePath();
        }
        return path;
    }


}
