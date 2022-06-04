package com.example.project;

public class WatermarkUtils {

    // declarations needed for watermarking methodes
    private static final int[] shiftNum = {1,3,7,15,31,63,127,255};
    public static final String MARKER = "@RM";
    public static final int HEADER_LENGTH = MARKER.length() * 8 + 32 + 32 + 8 + 16 + 8 + 8;
    private int[] dataHeader = new int[HEADER_LENGTH];

    // the methode for inserting the watermark
    public static int[] applyWatermark(int[] audioSamples, byte[] message) {

        int LSBUSed = 1; // Integer.parseInt((String)this.properties.get("lsb"));
        int shiftNumber = shiftNum[LSBUSed-1];
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
                newSample[offset] = audioSamples[offset] & ~ shiftNum[lastBit];
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
    public static byte[] extractWatermark(short[] audioSamples) {

        int LSBUSed = 1; // Integer.parseInt((String)this.properties.get("lsb"));
        int shiftNumber = shiftNum[LSBUSed-1];

        int byteExtract = 0;
        int bitDiambil=0;
        // this.tempExt = (String) this.properties.get("ext");

        int startIndex = 128;

        int numByte = 10000;//lengthMessage * LSBUSed / 8;
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

}
