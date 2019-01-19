package com.rustero.rtmp;


import android.media.MediaCodec;
import android.media.MediaFormat;
import android.os.SystemClock;

import com.rustero.App;
import com.rustero.gadgets.Tools;

import java.nio.ByteBuffer;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.TimeUnit;


public class RtmpJni {
	static {
		System.loadLibrary("rtmpjni");
	}

	private static final int MAX_BANDWIDTH  = 0;

	// * JNI declarations  <<<
	private	native int jniOpenSession(String aLink, int aWidth, int aHeight);
	private native int jniCloseSession();
	private native int jniIsConnected();
	private native int jniVideoFormat(ByteBuffer aData, int aSize);
	private native int jniVideoSample(ByteBuffer aData, int aSize, long aMils);
	private native int jniAudioFormat(ByteBuffer aData, int aSize);
	private native int jniAudioSample(ByteBuffer aData, int aSize, long aMils);
	private native int jniTest1(ByteBuffer aData, int aSize, long aMils);
	// * JNI declarations  >>>


	private class Chunk {
		boolean audio;
		int size;
		long time;
		ByteBuffer data;
	}


	private static final int MAX_QUEUE 		= 2222;
	private boolean mQuit, mDisconnected;
	private Pump mPump;
	private RtmpStateEvents mRtmpEventer;
	private String mCastLink = "";
	private ArrayBlockingQueue<Chunk> mQueue;
	private ByteBuffer mVideoForsam, mAudioForsam;
	private int mQueueCount, mOneSize;
	private long mOneTackTick;
	private long mZeroVideo, mZeroAudio, mSentTick, mSlowTick;
	private int mVideoWidth, mVideoHeight;





	public void begin(RtmpStateEvents aEventer, String aCastLink, int aWidth, int aHeight) {
		mRtmpEventer = aEventer;
		mCastLink = aCastLink;
		mVideoWidth = aWidth;
        mVideoHeight = aHeight;

		mQueue = new ArrayBlockingQueue<Chunk>(MAX_QUEUE);
		mZeroVideo = -1;
		mZeroAudio = -1;

		try {
			mPump = new Pump();
			mPump.setName("RtmpJni");
			mPump.start();
		} catch (Exception ex) {}

	}



	public void cease() {
		App.log("cease");
		mQuit = true;
	}



	public void setVideoFormat(MediaFormat aFormat) {
		try {
			ByteBuffer bybu = aFormat.getByteBuffer("csd-0");
			if (null == bybu) return;
			bybu.rewind();
			ByteBuffer sps = createBuffer(bybu);

			bybu = aFormat.getByteBuffer("csd-1");
			if (null == bybu) return;
			bybu.rewind();
			ByteBuffer pps = createBuffer(bybu);

			sps.getInt();
			pps.getInt();
			int spslen = sps.remaining();
			int ppslen = pps.remaining();
			bybu = ByteBuffer.allocate(spslen + ppslen + 99);

			//header
			bybu.put((byte) 0x17); //0x10 - key frame; 0x07 - H264_CODEC_ID
			bybu.put((byte) 0);    //0: AVC sequence header; 1: AVC NALU; 2: AVC end of sequence
			bybu.put((byte) 0);    //CompositionTime
			bybu.put((byte) 0);    //CompositionTime
			bybu.put((byte) 0);    //CompositionTime

			//SPS head
			bybu.put((byte) 1);       // version

			bybu.put(sps.array()[4+1]);    // AVCProfileIndication   (0x64)
			bybu.put(sps.array()[4+2]);    // profile_compatibility  (0)
			bybu.put(sps.array()[4+3]);    // AVCLevelIndication     (0x0d)

			bybu.put((byte) 0xff);    // 6 bits reserved (111111) + 2 bits nal size length - 1 (11)
			bybu.put((byte) 0xe1);    // numOfSequenceParameterSets e0+01

			//sps length
			bybu.put((byte) ((spslen >> 8) & 0xFF));
			bybu.put((byte) (spslen & 0xFF));
			//copy sps body
			bybu.put(sps);

			//PPS head
			bybu.put((byte) 1); //version
			//pps length
			bybu.put((byte) ((ppslen >> 8) & 0x000000FF));
			bybu.put((byte) (ppslen & 0x000000FF));
			//copy pps body
			bybu.put(pps);

			bybu.flip();
			mVideoForsam = createDirect(bybu);

			///App.log(String.format("setVideoFormat: %d", bybu.limit()));
		} catch (Exception ex) {
			App.log(" ***_ex RtmpJni_setVideoFormat:" + ex.getMessage());
		}
	}



	public void setAudioFormat(MediaFormat aFormat) {
		try {
			ByteBuffer csd = aFormat.getByteBuffer("csd-0");
			csd.rewind();
			mAudioForsam = ByteBuffer.allocateDirect(2 + csd.remaining());
			mAudioForsam.put((byte) 0xaf);
			mAudioForsam.put((byte) 0);
			mAudioForsam.put(csd);
			mAudioForsam.flip();
			App.log(String.format("setAudioFormat: %d", mAudioForsam.limit()));
		} catch (Exception ex) {
			App.log(" ***_ex RtmpJni_setAudioFormat: " + ex.getMessage());
		}
	}




	public void postSample(ByteBuffer aBufData, MediaCodec.BufferInfo aBufInfo, boolean aAudio)
	{
//		if (!USE_VIDEO) return;
//		if (null == mVideoForsam) return;
		if (aBufData.limit() < 1) return;

		if (aAudio  &&  mZeroVideo < 0) {
			///App.log("RtmpJni_postSample: wait for first video");
			return;
		}

		mQueueCount = mQueue.size();
		if (mQueueCount > MAX_QUEUE-9) return;

		try {
			Chunk chunk = new Chunk();
			chunk.audio = aAudio;
			chunk.time = aBufInfo.presentationTimeUs / 1000;
			chunk.size = aBufData.limit();
			chunk.data = createDirect(aBufData);
			boolean okay = mQueue.offer(chunk);
			if (!okay) {App.log("RtmpJni postSample offer error: " + mQueueCount);}

			if (aAudio) {
				App.log(" RtmpJni_postAudio: " + chunk.time + ", " + chunk.size);
			} else {
				App.log("RtmpJni_postVideo: " + chunk.time + ", " + chunk.size);
			}
		} catch (Exception ex) {
			App.log(String.format(" ***_ex RtmpJni_postSample: %s", ex.getMessage() ));
		}
	}


	public int getQueueCount() {
		return mQueueCount;
	}



	private class Pump extends Thread {
		@Override
		public void run() {
			doPump();
		}
	}




	private void doPump() {
		App.log("RtmpJni_pump_00");
		try {
			doConnect();
			Thread.sleep(999);
			mSlowTick = Tools.mills();
			mSentTick = Tools.mills();

			if (jniIsConnected() > 0) {
				App.log("RtmpJni connect success");
				mRtmpEventer.onOpen();
			}
			else {
				App.log("RtmpJni connect FAILED");
				mRtmpEventer.onFailed();
				return;
			}

			while (!mQuit) {
				Thread.sleep(1);
				doSend();
				oneTack();
			}

			doClose();
			if (mDisconnected) {
				App.log("RtmpJni disconnected");
				mRtmpEventer.onDisconnected();
			}
			else {
				App.log("RtmpJni closed");
				mRtmpEventer.onClosed();
			}

		} catch (Exception ex) {
			App.log("  ***_ex RtmpJni_pump: " + ex.getMessage());
		}
		App.log("RtmpJni_pump_99");
	}



	private void oneTack() {
		long mils = Tools.mills();
		if (mils - mOneTackTick < 1000) return;
		mOneTackTick = mils;
		//App.log("RtmpJni_oneTack");
		mOneSize = 0;
	}



	private void doSend() {
		mQueueCount = mQueue.size();
		if (mQueueCount == 0) return;

		if (App.isDevel() && MAX_BANDWIDTH > 0) {
			if (mOneSize > MAX_BANDWIDTH/8) {
				//App.log("Throttling...");
				return;
			}
		}

		Chunk chunk = null;
		try {
			chunk = mQueue.poll(1, TimeUnit.MICROSECONDS);
		} catch (Exception ex) {
			App.log("  ***_ex RtmpJni_doSend");
		}
		if (null == chunk) return;

		mOneSize += chunk.size;

		int result = 0;
		long took = SystemClock.uptimeMillis();

		try {
			///App.log("about to send: " + chunk.time + "  " + chunk.size);
			if (chunk.audio) {
				result = sendAudio(chunk);
			} else {
				result = sendVideo(chunk);
			}

			took = Tools.mills() - took;
			if (took > 99) {
				//App.log("  jniSend took: " + took + "  chunk: " + chunk.time + "  " + chunk.size + "  " + chunk.audio);
			}

			if (result > 0) {
				mSentTick = Tools.mills();
				//App.log("sent ok: " + mSentTick);
			} else {
				mDisconnected = true;
				mQuit = true;
				App.log("  jniSend disconnected: " + result);
				App.log("chunk: " + chunk.time + "  " + chunk.size);
			}
		} catch (Exception ex) {
			App.log("  ***_ex RtmpJni_doSend: " + ex.getMessage());
		}
	}



	private int sendVideo(Chunk aChunk) {
		int result = aChunk.size;
		if (null == mVideoForsam) return result;  // video format is still not available
		try {
			if (mZeroVideo < 0) {
				mZeroVideo = aChunk.time;
				jniVideoFormat(mVideoForsam, mVideoForsam.limit());
				App.log(String.format(" * zero video starting: %d", aChunk.time));
			}
			long stamp = aChunk.time - mZeroVideo;

			result = jniVideoSample(aChunk.data, aChunk.size, stamp);
			///App.log("sendVideo: " + stamp + "  " + aChunk.size);
		} catch (Exception ex) {
			App.log("  ***_ex RtmpJni_sendVideo: " + ex.getMessage());
		}
		return result;
	}



	private int sendAudio(Chunk aChunk) {
		int result = aChunk.size;
		if (null == mAudioForsam) return result;  // audio format is still not available
		try {
			if (mZeroAudio < 0) {
				mZeroAudio = aChunk.time;
				jniAudioFormat(mAudioForsam, mAudioForsam.limit());
				App.log(String.format(" * zero audio starting: %d", aChunk.time));
			}
			long stamp = aChunk.time - mZeroVideo;
			result = jniAudioSample(aChunk.data, aChunk.size, stamp);
			///App.log("sendAUDIO: " + stamp + "  " + aChunk.size);
		} catch (Exception ex) {
			App.log("  ***_ex RtmpJni_sendAudio: " + ex.getMessage());
		}
		return result;
	}








	private void doConnect() {
		int result = 0;
		App.log("RtmpJni doConnect: " + mCastLink);
		try {
			String link = mCastLink;
			result = jniOpenSession(link, mVideoWidth, mVideoHeight);
			if (result < 0) {
				App.log("RtmpJni doConnect FAILED.");
			}
		} catch (Exception ex) {
			App.log("  ***_ex RtmpJni_doConnect: " + ex.getMessage());
		}
	}



	private void doClose() {
		//App.log("RtmpJni_doClose");
		int result = 0;
		try {
			result = jniCloseSession();
		} catch (Exception ex) {
			App.log("  ***_ex RtmpJni_doConnect: " + ex.getMessage());
		}
	}



	private ByteBuffer createDirect(ByteBuffer aSource) {
		ByteBuffer result = ByteBuffer.allocateDirect(aSource.remaining());
		aSource.rewind();
		result.put(aSource);
		result.flip();
		aSource.rewind();
		return result;
	}



	private ByteBuffer createBuffer(ByteBuffer aSource) {
		ByteBuffer result = ByteBuffer.allocate(aSource.remaining());
		aSource.rewind();
		result.put(aSource);
		result.flip();
		aSource.rewind();
		return result;
	}


}



