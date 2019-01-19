
package com.rustero.rtmp;


//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;


public class AudioHeads {

//    private static final Logger logger = LoggerFactory.getLogger("AudioHeads");
    private static ByteBuffer result = ByteBuffer.allocate(999);


    public static ByteBuffer formatHead(ByteBuffer aCsd) {
        result.clear();
        result.put((byte) 0xaf);
        result.put((byte) 0);
        result.put(aCsd);
        result.flip();
        return result;
    }



    public static ByteBuffer sampleHead() {
        result.clear();
        result.put((byte) 0xaf);
        result.put((byte) 1);
        result.flip();
        return result;
    }



    public static ByteBuffer endingHead() {
        result.clear();
        result.put((byte) 0xaf);
        result.put((byte) 0x02); //end of stream
        result.flip();
        return result;
    }



}