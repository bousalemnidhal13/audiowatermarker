package com.example.project.watermark;

import java.util.HashMap;

import com.example.project.util.Complex;
import com.example.project.util.FFT;
import com.example.project.util.MessageInfo;
import com.example.project.util.WaveInfo;

public class WatermarkingFactory {

    public static final int LOW_BIT_CODING_METHOD = 0;

    // public static final int ECHO_HIDING_METHOD = 1;
    // public static final double ECHO_HIDING_DELAY = 0.4;


    private WaveInfo sourceWave;
    private WaveInfo targetWave;
    private MessageInfo message;
    private HashMap properties;
    private String tempExt = "";
    private static final int[] shiftNum = {1,3,7,15,31,63,127,255};
    public static final String MARKER = "@RM";
    // public static final int ECHO_NUM = 4096;
    public static final int HEADER_LENGTH = WatermarkingFactory.MARKER.length() * 8 + 32 + 32 + 8 + 16 + 8 + 8;


    /** Creates a new instance of Watermarking */
    public WatermarkingFactory(WaveInfo sourceWave, HashMap properties){
        this.sourceWave = sourceWave;
        this.properties = properties;

    }

    public WatermarkingFactory(WaveInfo sourceWave, MessageInfo message, HashMap properties) {
        this.sourceWave = sourceWave;
        this.message = message;
        this.properties = properties;
    }

    public WatermarkingFactory(){

    }





    public WaveInfo getTargetWave() {
        return targetWave;
    }

    public void setTargetWave(WaveInfo targetWave) {
        this.targetWave = targetWave;
    }


    public void setSourceWave(WaveInfo sourceWave) {
        this.sourceWave = sourceWave;
    }

    public void setMessage(MessageInfo message) {
        this.message = message;
    }

    public void setProperties(HashMap properties) {
        this.properties = properties;
    }







    // USED TO APPLY THE WATERMARK EMBEDDING
    public void applyWatermark(){
        int[] newSample = null;
        float[] newFloatSample = null;

        int method = Integer.parseInt((String) this.properties.get("method"));
        if (method == WatermarkingFactory.LOW_BIT_CODING_METHOD){
            newSample = this.EmbbedbyLowBitCoding(this.sourceWave.getAudioSamples(), this.message.getStream());
            this.targetWave = new WaveInfo(newSample, this.sourceWave.getAudioFormat());
        } /* else if (method == WatermarkingFactory.ECHO_HIDING_METHOD){
            newFloatSample = this.EmbeddbyEchoHiding(this.sourceWave.getFloatSamples(),this.message.getStream());
            this.targetWave = new WaveInfo(newFloatSample, this.sourceWave.getAudioFormat());
            this.targetWave = new WaveInfo(this.addHeader4Echo(this.targetWave.getAudioSamples(),this.message.getStream()), this.sourceWave.getAudioFormat()) ;
        } */
        this.targetWave.setRecordedAudio(true);
        this.targetWave.setFileName("Watermarked Audio");

    }






    // USED TO APPLY THE WATERMARK EXTRACTION
    public void ExtractWatermark(){
        byte[] message_stream = null;

        int method = Integer.parseInt((String) this.properties.get("method"));
        if (method == WatermarkingFactory.LOW_BIT_CODING_METHOD){
            message_stream = this.ExtractbyLowBitCoding(this.sourceWave.getAudioSamples());
        }


        /* else if (method == WatermarkingFactory.ECHO_HIDING_METHOD){
            message_stream = this.ExtractbyEchoHiding(this.sourceWave.getFloatSamples());
        } */


        this.message = new MessageInfo(message_stream);
        this.message.setFileEx(this.tempExt);
    }









    // LOW BIT CODING EMBEDDING METHOD
    public int[] EmbbedbyLowBitCoding(int[] audioSamples, byte[] message){
        int LSBUSed = Integer.parseInt((String)this.properties.get("lsb"));
        int shiftNumber = this.shiftNum[LSBUSed-1];
        int[] newSample = new int[audioSamples.length];
        int offset = 0;

        // write marker to audio samples
        for (int i=0;i<WatermarkingFactory.MARKER.length();i++){
            int kar = WatermarkingFactory.MARKER.charAt(i);
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

        //write file message extension
        byte[] extArray = this.message.getFileEx().getBytes();
        for (int i=0;i<extArray.length;i++){
            for (int j=0;j<8;j++){
                int bitExtract = extArray[i] & 1;
                newSample[offset] = audioSamples[offset] & ~1;
                newSample[offset] |= bitExtract;
                extArray[i] >>= 1;
                offset++;
            }
        }

        //write method off = 88
        int method = Integer.parseInt((String) this.properties.get("method"));
        for (int j=0;j<8;j++){
            int bitExtract = method & 1;
            newSample[offset] = audioSamples[offset] & ~1;
            newSample[offset] |= bitExtract;
            method >>= 1;
            offset++;
        }

//write lsb off 96
        int lsb = Integer.parseInt((String) this.properties.get("lsb"));
        for (int j=0;j<16;j++){
            int bitExtract = lsb & 1;
            newSample[offset] = audioSamples[offset] & ~1;
            newSample[offset] |= bitExtract;
            lsb >>= 1;
            offset++;
        }

//write is compress? off 112
        int compress = Integer.parseInt((String) this.properties.get("compress"));
        for (int j=0;j<8;j++){
            int bitExtract = compress & 1;
            newSample[offset] = audioSamples[offset] & ~1;
            newSample[offset] |= bitExtract;
            compress >>= 1;
            offset++;
        }

//write is encrypt? off 120
        int encrypt = Integer.parseInt((String) this.properties.get("encrypt"));
        for (int j=0;j<8;j++){
            int bitExtract = encrypt & 1;
            newSample[offset] = audioSamples[offset] & ~1;
            newSample[offset] |= bitExtract;
            encrypt >>= 1;
            offset++;
        }


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
            }else if(bitRem == lastBit && (offset-(WatermarkingFactory.HEADER_LENGTH-1)) == length2) {
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





    // LOW BIT CODING EXTRACTION METHODE
    public byte[] ExtractbyLowBitCoding(int[] audioSamples){
        int LSBUSed = Integer.parseInt((String)this.properties.get("lsb"));
        int shiftNumber = this.shiftNum[LSBUSed-1];

        int byteExtract = 0;
        int bitDiambil=0;
        this.tempExt = (String) this.properties.get("ext");

        int startIndex = 128;

        int numByte = Integer.parseInt((String) this.properties.get("msgSize"));//lengthMessage * LSBUSed / 8;
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




    // I'LL FIGURE IT OUT (HOPEFULLY
    public MessageInfo getMessage() {
        return message;
    }
}
