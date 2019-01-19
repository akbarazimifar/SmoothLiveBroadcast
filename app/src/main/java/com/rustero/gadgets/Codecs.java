package com.rustero.gadgets;


import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaFormat;
import android.view.Surface;

import com.rustero.App;
import com.rustero.core.egl.glCore;
import com.rustero.core.egl.glSurface;

public class Codecs {


	public static boolean isSupported(Size2D aSize) {
		boolean result = true;
		try {
			MediaFormat askedFormat = MediaFormat.createVideoFormat(MediaFormat.MIMETYPE_VIDEO_AVC, aSize.x, aSize.y);
			askedFormat.setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface);
			askedFormat.setInteger(MediaFormat.KEY_BIT_RATE, 2000111);
			askedFormat.setInteger(MediaFormat.KEY_FRAME_RATE, 30);
			askedFormat.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 1);

			MediaCodec videoCodec = MediaCodec.createEncoderByType(MediaFormat.MIMETYPE_VIDEO_AVC);
			videoCodec.configure(askedFormat, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);

			videoCodec.release();
			videoCodec = null;

			result = true;
		} catch (Exception ex) {
			result = false;
			App.log(" ***_ex Codec.isSupported: " + aSize.x + "x" + aSize.y);
		}
		return result;
	}

}
