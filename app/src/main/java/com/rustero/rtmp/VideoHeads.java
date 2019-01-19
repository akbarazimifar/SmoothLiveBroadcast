
package com.rustero.rtmp;


import java.nio.ByteBuffer;

//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;


public class VideoHeads {

//    private static final Logger logger = LoggerFactory.getLogger("VideoAtoms");
    private static ByteBuffer result = ByteBuffer.allocate(999);




    public static ByteBuffer formatHead(ByteBuffer aSps, ByteBuffer aPps) {
        aSps.getInt();
        aPps.getInt();
        result.clear();
        int spslen = aSps.remaining();
        int ppslen = aPps.remaining();

        int nallen = 5 //header
            + 8 //SPS header
            + spslen //the SPS itself
            + 3 //PPS header
            + ppslen; //the PPS itself

        //header
        result.put((byte) 0x17); //0x10 - key frame; 0x07 - H264_CODEC_ID
        result.put((byte) 0);    //0: AVC sequence header; 1: AVC NALU; 2: AVC end of sequence
        result.put((byte) 0);    //CompositionTime
        result.put((byte) 0);    //CompositionTime
        result.put((byte) 0);    //CompositionTime

        //SPS head
        result.put((byte) 1);       // version

//m        result.put((byte) 0x64);    // AVCProfileIndication   (0x64)
        result.put((byte) aSps.array()[4+1]);    // AVCProfileIndication   (0x64)

//m        result.put((byte) 0);       // profile_compatibility  (0)
        result.put((byte) aSps.array()[4+2]);       // profile_compatibility  (0)

//m        result.put((byte) 0x0d);    // AVCLevelIndication     (0x0d)
        result.put((byte) aSps.array()[4+3]);    // AVCLevelIndication     (0x0d)

        result.put((byte) 0xff);    // 6 bits reserved (111111) + 2 bits nal size length - 1 (11)
        result.put((byte) 0xe1);    // numOfSequenceParameterSets e0+01

        //sps length
        result.put((byte) ((spslen >> 8) & 0xFF));
        result.put((byte) (spslen & 0xFF));
        //copy sps body
        result.put(aSps);

        //PPS head
        result.put((byte) 1); //version
        //pps length
        result.put((byte) ((ppslen >> 8) & 0x000000FF));
        result.put((byte) (ppslen & 0x000000FF));
        //copy pps body
        result.put(aPps);
        result.flip();
        return result;
    }



    public static ByteBuffer sampleHead(ByteBuffer aNalData) {
        aNalData.getInt();  // skip 4 bytes
        int nallen = aNalData.remaining();
        boolean idr = false;
        byte b = (byte) (aNalData.array()[4] & 0x1f);
        //if (b==5 || b==7  || b==6)
        if (b==5 || b==7)
            idr = true;
        else if (b==1)
            idr = false;
        else {
//            logger.info(" ######## unknown NAL: {}", b);
//            logger.info("{}, {}", nallen, Global.HexStr(aNalData));
            ///logger.info(Global.HexDump(aNalData));
            return null;
        }

        result.clear();
        int msgSize = 9 + nallen;
        if (idr)
            result.put((byte) 0x17); //packet tag// key // avc
        else
            result.put((byte) 0x27); //packet tag// key // avc
        result.put((byte) 0x01); //vid tag
        //vid tag presentation off set
        result.put((byte) 0);
        result.put((byte) 0);
        result.put((byte) 0);
        //nal size
        result.putInt(nallen);
        result.flip();
        return result;
    }



    public static ByteBuffer endingHead() {
        result.clear();
        result.put((byte) 0x17); //packet tag// key // avc
        result.put((byte) 0x02); //end of stream
        result.put((byte) 0);
        result.put((byte) 0);
        result.put((byte) 0);
        result.flip();
        return result;
    }



}