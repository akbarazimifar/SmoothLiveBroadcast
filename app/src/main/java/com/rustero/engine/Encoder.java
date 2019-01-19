package com.rustero.engine;


import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.SurfaceTexture;
import android.graphics.Typeface;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraMetadata;
import android.hardware.camera2.CaptureRequest;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaFormat;
import android.media.MediaRecorder;
import android.opengl.GLES20;
import android.opengl.Matrix;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.util.Log;
import android.view.Surface;
import android.view.SurfaceHolder;

import com.rustero.App;
import com.rustero.Errors;
import com.rustero.core.effects.Effector;
import com.rustero.core.egl.glStill;
import com.rustero.core.effects.glEffect;
import com.rustero.core.egl.glCore;
import com.rustero.core.egl.glScene;
import com.rustero.core.egl.glStage;
import com.rustero.core.egl.glSurface;
import com.rustero.core.egl.glTexture;
import com.rustero.kamera.Kamera;
import com.rustero.kamera.Kamman;
import com.rustero.rtmp.RtmpStateEvents;
import com.rustero.gadgets.Size2D;
import com.rustero.gadgets.AudioPool;
import com.rustero.gadgets.Tools;
import com.rustero.gadgets.FlashLight;
import com.rustero.gadgets.TrackPool;
import com.rustero.rtmp.RtmpJni;

import java.io.File;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.TimeUnit;
import javax.microedition.khronos.opengles.GL10;


@SuppressWarnings("deprecation")





public class Encoder {


	public interface Events {
		void onFault(final String aMessage);
		void onDisconnected();
		void onStatus();
		void onProgress();
	}


	public class Status {
		public boolean online;
		public long duration, traffic;
		public int loopFps, frameFps;
		public int tenSize, tenRate;
		public int queueCount, thisTenco, lastTenco, peakTenco;
	}


	public class Order {
		private static final int ORDER_BEGIN_RECORDING 		= 1;
		private static final int ORDER_CEASE_RECORDING 		= 2;
		private static final int ORDER_FLASH_ON     		= 3;
		private static final int ORDER_FLASH_OFF     		= 4;
		private int code;
	}



    public static boolean CONTROL_BITRATE = true;
    public static boolean USE_CBR = false;

	public static boolean RECORD_AUDIO = true;
	public static final int ACTOR_ID 			= 0;
    //	public static final int ACTOR_ID 			= R.raw.actor5;

	public static final int STATE_IDLE 			= 0;
	public static final int STATE_STARTING	 	= 1;
	public static final int STATE_CONNECTING 	= 2;
	public static final int STATE_CONNECTED     = 3;
    public static final int STATE_WAITING	 	= 4;

	public static String hostUrl ="", hostKey ="";
	private static Encoder self;
	private volatile int mState = STATE_IDLE;
	private Status mStatus = new Status();

    public String lastFault = "";
	private RtmpJni mRtmper;
	private OrderFifo mOrderFifo = new OrderFifo();
	private Size2D mCameraSize = new Size2D();
    private float mMaxiZoom=3.0f, mThisZoom=1.0f, mZoomStep=0.02f;

    private Events mEventer;
	private volatile boolean mAskImalay;
	private Bitmap mImalayBitmap = null;

    private SurfaceHolder mSurfHolder;
	private Size2D mScreenSize = new Size2D();

	private HelperThread mHelperThread;
	private Handler mHelperHandler;

    private Engine mEngine;
    private Enaudio mEnaudio;
    private Entrack mEntrack;
	private MediaCodec mVideoCodec;
	private Kamera mKamera;

    private String mFilmName = "";
    private File mFilmFile;
	private List<String> mAskedEffects;

	private long mKickMils;
	private int mLoopPoll, mFramePoll;
	private int mBitrate = 	1000000;

    private int mWaitCount;
    private boolean mAskedReconnection;




	public static Encoder get() {
		create();
		return self;
	}



	public static void create() {
		if (null != self) return;
		self = new Encoder();
	}



	public static void delete() {
		if (null == self) return;
		self = null;
	}



	private Encoder()	{}



	public String getCastLink() {
		String result = hostUrl;
		if (hostUrl.isEmpty()) return "";
		if (hostKey.isEmpty()) return "";
		if (!result.endsWith("/"))
			result += "/";
		result += hostKey;
		if (result.indexOf("rtmp://") != 0)
			result = "rtmp://" + result;
		return result;
	}


	public String getCastHost() {
		String result = "";
		String host = "";
		String link = getCastLink();
		try {
			URI uri = new URI(link);
			host = uri.getHost();
		} catch (URISyntaxException ex) {}
		if (host == null) return "";

		String[] parts = host.split("\\.");
		if (parts.length < 2) return "";
		result = parts[parts.length-2] + "." + parts[parts.length-1];
		return result;
	}







	public void attachEngine(Events aEncodeEventer) {
		if (mEventer != null) return;
		mEventer = aEncodeEventer;
		createEngine();
		setEffects(App.sMyEffects);
	}


	public void detachEngine() {
		cease();
		mEventer = null;
		deleteEngine();
	}



	public void attachScreen(SurfaceHolder aHolder) {
		mSurfHolder = aHolder;
	}


	public void detachScreen() {
		mSurfHolder = null;
	}



	public void changeScreen(int aWidth, int aHeight) {
		mScreenSize = new Size2D(aWidth, aHeight);
	}




	public void begin() {
		App.log( "begin");
        mAskedReconnection = false;
        doBegin();
	}


	public void cease() {
		if (mState == STATE_IDLE) return;
		App.log("cease");
		mOrderFifo.push(Order.ORDER_CEASE_RECORDING);
		mState = STATE_IDLE;
		if (null != mEventer)
			mEventer.onStatus();
	}


	public void reconnect() {
        mState = STATE_WAITING;
        mWaitCount = 10;
    }




    // * doers


    private void doBegin() {
        if (mState != STATE_IDLE && mState != STATE_WAITING) return;
        mState = STATE_STARTING;
        mEventer.onStatus();
        mOrderFifo.push(Order.ORDER_BEGIN_RECORDING);
    }


	private void createEngine() {
		try {
			lastFault = "";
			mThisZoom = 1.0f;
			mOrderFifo.clear();

			mHelperThread = new HelperThread();
			mHelperThread.start();
			Looper looper = mHelperThread.getLooper();
			mHelperHandler = new Handler(looper);

			mEngine = new Engine();
			mEngine.start();
		} catch (Exception ex) {
			App.log(" *** ex_Enfilm_attachEngine " + ex.getMessage());
		};
	}


	private void deleteEngine() {
		try {
			if (mEngine != null) {
				mEngine.finish();
				mEngine = null;
			}

			if (mHelperHandler != null) {
				mHelperHandler.removeCallbacksAndMessages(null);
				mHelperHandler.getLooper().quit();
				mHelperHandler = null;
			}

		} catch (Exception ex) {
			App.log(" *** ex_Enfilm_attachEngine " + ex.getMessage());
		};
	}



	public String getFilmName() {
		return mFilmName;
	}


	public boolean isIdle() {
		return (mState == STATE_IDLE);
	}


	public boolean isConnecting() {
		return (mState == STATE_CONNECTING);
	}


	public boolean isConnected() {
		return (mState == STATE_CONNECTED);
	}


    public boolean isWaiting() {
        return (mState == STATE_WAITING);
    }


    public int getWaitCount() {
        return mWaitCount;
    }


	public float getZoom() {
		return mThisZoom;
	}


	public void incZoom() {
		mThisZoom += mZoomStep;
		if (mThisZoom > mMaxiZoom)
			mThisZoom = mMaxiZoom;
	}


	public void decZoom() {
		mThisZoom -= mZoomStep;
		if (mThisZoom < 1.0f)
			mThisZoom = 1.0f;
	}



	public boolean hasFlash() {
		if (!App.isPremium()) return false;
		if (null == mEngine) return false;
		return mEngine.mFlashLight.isAvailable();
	}



	public void turnFlash(boolean aOn) {
		if (null == mEngine) return;
//		try {
//			if (aOn)
//				mEngine.mCampars.setFlashMode(Camera.Parameters.FLASH_MODE_TORCH);
//			else
//				mEngine.mCampars.setFlashMode(Camera.Parameters.FLASH_MODE_OFF);
//			mEngine.mCamera.setParameters(mEngine.mCampars);
//			App.log( "setFlashMode: " + aOn);
//		} catch (Exception ex) {
//			App.log( " *** ex_setFlashMode");
//		}
	}



	public void setEffects(List<String> aList) {
		mAskedEffects = aList;
	}



	public Size2D getCameraSize() {
		return mCameraSize;
	}


	public int getState() {
		return mState;
	}


	public Status getStatus() {
		return mStatus;
	}


	public int getQueueCount() {
		if (null == mRtmper) return 0;
		return mRtmper.getQueueCount();
	}


	private void setLastFault(String aError) {
        lastFault = aError;
		cease();
		if (null != mEventer)
			mEventer.onFault(lastFault);
    }


	private boolean isFaulted() {
		return ( (null != lastFault) && (!lastFault.isEmpty()) );
	}


	private int getTopBitrate() {
		int result;
		int aHeight = mCameraSize.y;
		if (aHeight <= 240)
			result = 500111;
		else if (aHeight <= 360)
			result = 750111;
		else if (aHeight <= 480)
			result = 1500111;
		else if (aHeight <= 720)
			result = 3000111;
		else if (aHeight <= 1080)
			result = 5000111;
		else if (aHeight <= 1440)
			result = 8000111;
		else
			result = 14000111;
		return result;
	}


	private int getMinBitrate() {
		int result;
		int aHeight = mCameraSize.y;
		if (aHeight <= 240)
			result = 100111;
		else if (aHeight <= 360)
			result = 150111;
		else if (aHeight <= 480)
			result = 300111;
		else if (aHeight <= 720)
			result = 600111;
		else if (aHeight <= 1080)
			result = 1000111;
		else if (aHeight <= 1440)
			result = 1600111;
		else
			result = 2000111;
		return result;
	}





	private class HelperThread extends HandlerThread {

		public HelperThread() {
			super("Engine.Helper");
		}

		@Override
		public void run() {
			super.run();
		}
	}




	private class Engine extends Thread {
		private volatile boolean mQuit, mDone;
        private volatile boolean mFrameReady = false;

		private glSurface mCameraGurface, mDisplayGurface, mCodecGurface;

        private long mOneTackTick, mFrameNanos, mTenSecTick;
        private glTexture mCameraTexture;
        private SurfaceTexture mCameraSurtex;  // receives the output from the camera preview


		private FlashLight mFlashLight = new FlashLight();
        private MediaFormat mAskedVideoFormat, mCodecVideoFormat;
        private MediaCodec.BufferInfo bufferInfo;

		private Size2D mImalaySpot = new Size2D();
		private Size2D mImalaySize = new Size2D();
		private glScene mImalayScene;
		private glStill mImalayStill;

		private Size2D mStageSize = new Size2D();
        private glScene mCameraScene, mScreenScene, mCodecScene, mStillScene;
        private glStage mStage0, mStage1, mSourceStage, mOutputStage;
		private glStill mActorStill;

		private int mDeviceRotation;
		private CameraDevice mCameraDevice;
		private Surface mCaptureSurface;
		private CameraCaptureSession mCaptureSession;
		private CaptureRequest.Builder mCaptureBuilder;




        public Engine() {
            bufferInfo = new MediaCodec.BufferInfo();
        }



		@Override
		public void run() {
			App.log("Encoder_run_11");
			setName("encoder-gl");
			mDeviceRotation = App.self.getWindowManager().getDefaultDisplay().getRotation();

			attachEgl();
			attachStills();
			attachDisplay();
			attachCamera2();

			doLoop();

			closePreviewSession();
			Effector.get().detach();

			detachCamera2();
			detachDisplay();
			detachStills();
			detachEgl();

			App.log( "Encoder_run_99");
			mDone = true;
		}



		private void doLoop() {
			if (isFaulted()) return;
			try {
				while (!mQuit) {
					Thread.sleep(1);
					mLoopPoll++;

                    checkOrders();
					checkDisplay();
					checkFrame();
                    checkCodec();

                    oneTack();
				}
			} catch (Exception ex) {
				App.log(" ***_ex Engine_doLoop: " + ex.getMessage());
			}
		}



        private void checkOrders() {
            Order order = mOrderFifo.pull();
            if (null == order) return;

            if (order.code == Order.ORDER_BEGIN_RECORDING) {
				openSession();

            } else if (order.code == Order.ORDER_CEASE_RECORDING) {
				closeSession();

            } else if (order.code == Order.ORDER_FLASH_ON) {
                doFlash(true);

            } else if (order.code == Order.ORDER_FLASH_OFF) {
                doFlash(false);
            }
        }


        private void openSession() {
			lastFault = "";
			mStatus = new Status();

			mOneTackTick = Tools.mills();
			mTenSecTick = Tools.mills();
			mBitrate = getTopBitrate();

			attachTracker();
			attachAudioCodec();
			attachVideoCodec();

			if (isFaulted()) {
				cease();
				return;
			}

            mState = STATE_CONNECTING;
            mRtmper = new RtmpJni();
            mRtmper.begin(RTMPER_EVENTER, getCastLink(), mCameraSize.x, mCameraSize.y);
            mEventer.onStatus();
		}


		private void closeSession() {
			App.log("Order.ORDER_CEASE_RECORDING");

			if (null != mRtmper)
				mRtmper.cease();

			detachAudioCodec();
			detachVideoCodec();
			detachTracker();
		}


        private void checkDisplay() {
			if (null == mSurfHolder) {
				if (null != mDisplayGurface)
					detachDisplay();
			} else {
				if (null == mDisplayGurface) {
					attachDisplay();
				}
			}
		}


		private void checkFrame() {
            if (mFrameReady) {
                mFrameReady = false;

				mFrameNanos = System.nanoTime();
                mCameraGurface.makeCurrent();
                mCameraSurtex.updateTexImage();    // latch the next frame from the camera
                mCameraSurtex.getTransformMatrix(mCameraScene.texMatrix);
                resizeStages(mCameraSize);
                drawCamera();

                updateEffects();
                drawFilter();

                drawDisplay();

                if (isConnected()) {
                    mFramePoll++;
                    drawCodec();
                }
            }
		}


		private void checkCodec() {
            if (isConnected()) {
                pullCodec();
            }
        }


		private void oneTack() {
			long mils = Tools.mills();
			if (mils - mOneTackTick < 1000) return;
			mOneTackTick = mils;

            oneTackWaiting();
            oneTackConnected();
            tenTackConnected();
		}


        private void oneTackWaiting() {
            if (!isWaiting()) return;
            mWaitCount--;
            if (0 == mWaitCount) {
                mAskedReconnection = true;
                doBegin();
            } else {
                App.log("oneTackWaiting: " + mWaitCount);
                mEventer.onStatus();
            }
        }


        private void oneTackConnected() {
            if (!isConnected()) return;
            mStatus.loopFps = mLoopPoll;
            mLoopPoll = 0;
            mStatus.frameFps = mFramePoll;
            mFramePoll = 0;

            mStatus.queueCount = getQueueCount();
            if (mStatus.queueCount > mStatus.peakTenco)
                mStatus.peakTenco = mStatus.queueCount;
            mStatus.duration = (Tools.mills() - mKickMils)/1000;
            mEventer.onProgress();
        }


		private void tenTackConnected() {
			long mils = Tools.mills();
			if (mils - mTenSecTick < 10000) return;
			mTenSecTick = mils;

            if (isConnected()) {
                mStatus.tenRate = mStatus.tenSize / 10;
                mStatus.tenSize = 0;

                if (null != mRtmper) {
                    mStatus.lastTenco = mStatus.thisTenco;
                    mStatus.thisTenco = mRtmper.getQueueCount();
                }

                String text = Tools.formatDuration(mStatus.duration);
                text += "  " + Tools.formatTraffic(mStatus.traffic);
                text += "  " + Tools.formatBitrate(mStatus.tenRate);
                text += "  " + mStatus.thisTenco + "-" + mStatus.lastTenco + ">" + mStatus.peakTenco;
                App.log("  TENTack: " + text);

                if (CONTROL_BITRATE) {
                    controlBitrate_1();
                }

                mStatus.peakTenco = 0;
            }
		}



		private void controlBitrate_1() {
			final int BASE_EDGE = 99;
			if (null == mRtmper) return;
			int delta = mStatus.thisTenco - mStatus.lastTenco;
			App.log("dynamicTack: " + mStatus.thisTenco + "-" + mStatus.lastTenco + "=" + delta  + " ^" + mStatus.peakTenco);

			int topBitrate = getTopBitrate();
			int minBitrate = getMinBitrate();
			int newBitrate = mBitrate;

			if (delta > BASE_EDGE*4) {
				newBitrate = minBitrate;
				if (newBitrate < mBitrate) {
					App.log(" * decrease quarter: " + Tools.formatBitrate(newBitrate / 8));
				}

			} else if (delta > BASE_EDGE*2) {
				newBitrate = mBitrate / 2;
				if (newBitrate < minBitrate) newBitrate = minBitrate;
				if (newBitrate < mBitrate) {
					App.log(" * decrease half: " + Tools.formatBitrate(newBitrate / 8));
				}

			} else if (mStatus.thisTenco > BASE_EDGE*2  &&  delta >= 0) {
				newBitrate = mBitrate - mBitrate / 4;
				if (newBitrate < minBitrate) newBitrate = minBitrate;
				if (newBitrate < mBitrate) {
					App.log(" * decrease _4: " + Tools.formatBitrate(newBitrate / 8));
				}

			} else if (mStatus.thisTenco > BASE_EDGE*1  &&  delta >= -(BASE_EDGE/10)) {
				newBitrate = mBitrate - mBitrate / 8;
				if (newBitrate < minBitrate) newBitrate = minBitrate;
				if (newBitrate < mBitrate) {
					App.log(" * decrease _8: " + Tools.formatBitrate(newBitrate / 8));
				}

			} else if (mStatus.thisTenco == 0  &&  mStatus.peakTenco <= 1) {
				newBitrate = mBitrate + mBitrate / 8;
				if (newBitrate > topBitrate) newBitrate = topBitrate;
				if (newBitrate > mBitrate) {
					App.log(" * increase _8: " + Tools.formatBitrate(newBitrate / 8));
				}
			}

			if (newBitrate != mBitrate) {
				mBitrate = newBitrate;
				detachVideoCodec();
				attachVideoCodec();
			}
		}






        private void attachEgl() {
			try {
				glCore.create();

				mCameraGurface = new glSurface(glCore.get(), 128, 72);
				mCameraGurface.makeCurrent();
				GLES20.glBlendFunc(GL10.GL_SRC_ALPHA, GL10.GL_ONE_MINUS_SRC_ALPHA);
				GLES20.glEnable(GL10.GL_BLEND);

				mCameraScene = glScene.create(true);
				mScreenScene = glScene.create();
				mCodecScene = glScene.create();
				mStillScene = glScene.create();

				mCameraTexture = glTexture.create(true);
				mCameraSurtex = new SurfaceTexture(mCameraTexture.id);
				mCameraSurtex.setOnFrameAvailableListener(new SurfaceTextureListener());

			} catch (Exception ex) {
				App.log(" ***_ex attachEgl: " + ex.getMessage());
			}
		}


		private void detachEgl() {
            if (mCameraSurtex != null) {
                mCameraSurtex.release();
                mCameraSurtex = null;
            }

			if (mCameraGurface != null) {
				mCameraGurface.release();
				mCameraGurface = null;
			}

			glScene.delete(mCameraScene);
			glScene.delete(mScreenScene);
			glScene.delete(mCodecScene);
			glScene.delete(mStillScene);

			glStage.delete(mStage0);
			glStage.delete(mStage1);

			glCore.delete();
        }







		private void attachCamera2() {
			String resstr = "640x480";
			String cid = Kamman.get().getFirstId();
			if (Kamman.get().isNowFront()) {
				cid = Kamman.get().getFrontId();
				resstr = Kamman.get().getWantedFrontReso();
			} else {
				cid = Kamman.get().getBackId();
				resstr = Kamman.get().getWantedBackReso();
			}
			mCameraSize = Size2D.parseText(resstr);
			mBitrate = getTopBitrate();

			mKamera = Kamman.get().openCamera(cid, CAMERA_CALLBACK, mHelperHandler);
			App.log("attachCamera2_99");
		}


		private void detachCamera2() {
			mKamera = null;
			try {
				closePreviewSession();
				if (null != mCameraDevice) {
					mCameraDevice.close();
					mCameraDevice = null;
				}
			} finally {
			}
		}



		private CameraDevice.StateCallback CAMERA_CALLBACK = new CameraDevice.StateCallback() {

			@Override
			public void onOpened(CameraDevice cameraDevice) {
				mCameraDevice = cameraDevice;
				startCaptureSession();
				App.log("onOpened_99");
			}

			@Override
			public void onDisconnected(CameraDevice cameraDevice) {
				cameraDevice.close();
				mCameraDevice = null;
			}

			@Override
			public void onError(CameraDevice cameraDevice, int error) {
				cameraDevice.close();
				mCameraDevice = null;
			}
		};



		// Start the camera preview.
		private void startCaptureSession() {
			if (null == mCameraDevice || mCameraSize.isZero()) return;
			try {
				mCameraSurtex.setDefaultBufferSize(mCameraSize.x, mCameraSize.y);
				mCaptureSurface = new Surface(mCameraSurtex);
				mCameraDevice.createCaptureSession(Arrays.asList(mCaptureSurface), CAPTURE_CALLBACK, null);
			} catch (Exception ex) {
				App.log(" ***_ex startCaptureSession: " + ex.getMessage());
			}
			App.log("startPreviewSession_99");
		}


		CameraCaptureSession.StateCallback CAPTURE_CALLBACK = new CameraCaptureSession.StateCallback() {

			@Override
			public void onConfigured(CameraCaptureSession cameraCaptureSession) {
				mCaptureSession = cameraCaptureSession;
//				Range<Integer> fpsRange = new Range<>(30, 30);
//				Range<Integer> fpsRange = new Range<>(60, 60);
				try {
					mCaptureBuilder = mCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_RECORD);
					mCaptureBuilder.addTarget(mCaptureSurface);
//					mCaptureBuilder.set(CaptureRequest.CONTROL_AE_TARGET_FPS_RANGE, fpsRange);
					mCaptureBuilder.set(CaptureRequest.CONTROL_MODE, CameraMetadata.CONTROL_MODE_AUTO);

					mCaptureSession.setRepeatingRequest(mCaptureBuilder.build(), null, null);
				} catch (Exception ex) {
					App.log(" ***_ex CameraCaptureSession_onConfigured: " + ex.getMessage());
				}
				App.log("onConfigured_99");
			}


			@Override
			public void onConfigureFailed(CameraCaptureSession cameraCaptureSession) {
				App.log(" *** onConfigureFailed");
			}
		};


		private void closePreviewSession() {
			if (mCaptureSession != null) {
				mCaptureSession.close();
				mCaptureSession = null;
			}
		}




		private void attachStills() {
			if (ACTOR_ID > 0) {
				mActorStill = new glStill(App.resbmp(ACTOR_ID));
			}
		}



		private void detachStills() {
			glStill.delete(mActorStill);
		}




		private void attachTracker() {
			if (isFaulted()) return;
			if (App.isFilming()) {
				mFilmName = App.FILM_PREFIX + Tools.getDataTimeStamp2() + ".mp4";
				mFilmFile = new File(App.getOutputFolderPath(), mFilmName);

				mEntrack = new Entrack();
				mEntrack.attach(mFilmFile);
			}
		}


		private void detachTracker() {
			try {
				if (mEntrack != null) {
					mEntrack.detach();
					mEntrack = null;
				}
			} catch (Exception ex) {
				App.log("release-mVideoCodec: " + ex.getMessage());
			}
		}



		private void attachAudioCodec() {
			if (isFaulted()) return;
			if (RECORD_AUDIO) {
				App.log("attachAudioCodec");
				mEnaudio = new Enaudio();
				mEnaudio.mAuTrack = mEntrack;
				mEnaudio.start();
			} else {
				if (null != mEntrack)
					mEntrack.addAudioTrack(null);
			}
		}


		private void detachAudioCodec() {
			if (mEnaudio != null) {
				App.log(" *** detachAudioCodec_11");
				mEnaudio.finish();
				mEnaudio = null;
				App.log(" *** detachAudioCodec_99");
			}
		}




		private void attachVideoCodec() {
			if (isFaulted()) return;
            try {
				//App.log("attachVideoCodec");
                mAskedVideoFormat = MediaFormat.createVideoFormat(MediaFormat.MIMETYPE_VIDEO_AVC, mCameraSize.x, mCameraSize.y);
                mAskedVideoFormat.setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface);
                mAskedVideoFormat.setInteger(MediaFormat.KEY_BIT_RATE, mBitrate);
                mAskedVideoFormat.setInteger(MediaFormat.KEY_FRAME_RATE, 30);
                mAskedVideoFormat.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 1);
                if (USE_CBR) {
                    mAskedVideoFormat.setInteger(MediaFormat.KEY_BITRATE_MODE, MediaCodecInfo.EncoderCapabilities.BITRATE_MODE_CBR);
                }

				mVideoCodec = MediaCodec.createEncoderByType(MediaFormat.MIMETYPE_VIDEO_AVC);
				mVideoCodec.configure(mAskedVideoFormat, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);
				Surface surface = mVideoCodec.createInputSurface();
				mCodecGurface = new glSurface(glCore.get(), surface);
				mVideoCodec.start();
            } catch (Exception ex) {
				setLastFault("Error starting encoder.");
                App.log(" ***_ex attachVideoCodec: " + ex.getMessage());
            }
        }


        private void detachVideoCodec() {
            try {
                if (mVideoCodec != null) {
					//App.log("detachVideoCodec");
					mVideoCodec.stop();
					mVideoCodec.release();
					mVideoCodec = null;
                }

				if (mCodecGurface != null) {
					mCodecGurface.release();
					mCodecGurface = null;
				}

            } catch (Exception ex) {
                App.log("***_ex detachVideoCodec: " + ex.getMessage());
            }
        }




		private void attachDisplay() {
			if (null != mDisplayGurface) return;
			if (null == mSurfHolder) return;
			if (null == mSurfHolder.getSurfaceFrame()) return;
			if (mScreenSize.isZero()) return;
			try {
				Surface surface = mSurfHolder.getSurface();
				if (null == surface) return;

				mDisplayGurface = new glSurface(glCore.get(), surface);
				mDisplayGurface.makeCurrent();
				GLES20.glBlendFunc(GL10.GL_SRC_ALPHA, GL10.GL_ONE_MINUS_SRC_ALPHA);
				GLES20.glEnable(GL10.GL_BLEND);
			} catch (Exception ex) {
				App.log(" ***_ex attachCodec: " + ex.getMessage());
			}
		}


		private void detachDisplay() {
			if (mDisplayGurface != null) {
				mDisplayGurface.release();
				mDisplayGurface = null;
			}
		}





		private void drawCamera() {
			try {
				glCore.setViewport(mCameraSize);
				mCameraGurface.makeCurrent();

				mCameraScene.sourceTexid = mCameraTexture.id;
				mCameraScene.outputStage = mStage0;

				mCameraScene.resetMatrix();
				mCameraScene.scaleXY(mThisZoom);
				if (Surface.ROTATION_90 == mDeviceRotation || Surface.ROTATION_270 == mDeviceRotation) {
					int angle = 90 * mDeviceRotation;
					Matrix.rotateM(mCameraScene.mvpMatrix, 0, 1f*angle, 0f, 0f, 1f);
				}
				mCameraScene.draw();

				mOutputStage = mStage0;
				mSourceStage = mStage1;

				if (mActorStill != null) {
					mStillScene.sourceTexid = mActorStill.getTextureId();
					mStillScene.outputStage = mStage0;
					mStillScene.resetMatrix();
					mStillScene.draw();
				}

				mCameraGurface.swapBuffers();
			} catch (Exception ex) {
				App.log( " ***_ex drawCamera: " + ex.getMessage());
			}
		}



		private void drawFilter() {
			Effector.get().goFirst();
			while (true) {
				glEffect effect = Effector.get().getCurrent();
				if (null == effect) break;
				try {
					Matrix.setIdentityM(effect.mvpMatrix, 0);
					glCore.setViewport(mCameraSize);

					effect.sourceStage = mOutputStage;
					effect.outputStage = mSourceStage;
					effect.draw();
					mOutputStage = effect.outputStage;
					mSourceStage = effect.sourceStage;

				} catch (Exception ex) {
					App.log(" ***_ex drawDisplay: " + ex.getMessage());
				}
				Effector.get().goNext();
			}
		}



		private void drawImalay() {
			if (null == mImalayStill) return;
			try {
				Matrix.setIdentityM(mStillScene.mvpMatrix, 0);
				glCore.setViewport(mImalaySpot, mImalaySize);
				mStillScene.sourceTexid = mImalayStill.getTextureId();
				mStillScene.outputStage = mOutputStage;
				mStillScene.draw();
			} catch (Exception ex) {
				App.log( " ***_ex drawTheme: " + ex.getMessage());
			}
		}

		private void drawCodec() {
			try {
				mCodecGurface.makeCurrent();
				mCodecScene.sourceTexid = mOutputStage.getTextureId();
				glCore.setViewport(mCameraSize);

				mCodecScene.resetMatrix();
				mCodecScene.draw();

				long stamp = mFrameNanos;
				mCodecGurface.setPresentationTime(stamp);
				mCodecGurface.swapBuffers();
			} catch (Exception ex) {
				App.log( " ***_ex drawCodec: " + ex.getMessage());
			}
		}



		private void drawDisplay() {
			if (null == mDisplayGurface) return;
			try {
				mDisplayGurface.makeCurrent();
				glCore.setViewport(mScreenSize);
				glCore.clearScreen(0,0,0);

				mScreenScene.sourceTexid = mOutputStage.getTextureId();
				mScreenScene.resetMatrix();
				mScreenScene.cropAspect(mCameraSize, mScreenSize);
				mScreenScene.draw();

				mDisplayGurface.swapBuffers();
			} catch (Exception ex) {
				App.log( " ***_ex drawDisplay: " + ex.getMessage());
			}
		}



		private void updateEffects() {
			if (mAskedEffects == null) return;

			Effector.get().updateEffects(mAskedEffects, mCameraSize);
			mAskedEffects = null;
//			int degrees = 0;
//			if (Surface.ROTATION_90 == mDeviceRotation || Surface.ROTATION_270 == mDeviceRotation)
//				degrees = 90;
//			Effector.get().updateRotation(degrees);
		}



		private void resizeStages(Size2D aSize) {
			if (mStageSize.equals(aSize)) return;
			if (aSize.isZero()) return;
			mStageSize = new Size2D(aSize);

			glStage.delete(mStage0);
			mStage0 = new glStage(mStageSize);
			mStage0.tag = "0";

			glStage.delete(mStage1);
			mStage1 = new glStage(mStageSize);
			mStage1.tag = "1";
		}




		private void resizeImalay() {
			if (null == mImalayStill) return;
			int width = mImalayStill.size.x;
			int height = mImalayStill.size.y;
			mImalaySize = new Size2D(width, height);
			mImalaySpot = new Size2D((mCameraSize.x-width)/2, 0);
		}



		private void doFlash(boolean aOn) {
			if (null == mCaptureSession) return;
			try {
				if (aOn)
					mCaptureBuilder.set(CaptureRequest.FLASH_MODE, CameraMetadata.FLASH_MODE_TORCH);
				else
					mCaptureBuilder.set(CaptureRequest.FLASH_MODE, CameraMetadata.FLASH_MODE_OFF);
				mCaptureSession.setRepeatingRequest(mCaptureBuilder.build(), null, null);
			} catch (Exception ex) {
				App.log(" ***_ex CameraCaptureSession_onConfigured: " + ex.getMessage());
			}
		}



		private void checkImalay() {
			if (mAskImalay) {
				if (null == mImalayStill  &&  null != mImalayBitmap) {
					mImalayStill = new glStill(mImalayBitmap);
					resizeImalay();
				}
			} else {
				if (null != mImalayStill) {
					mImalayStill.release();
					mImalayStill = null;
				}
			}
		}




		private void finish() {
			App.log( "Engine finish 11");
			mEngine.mQuit = true;
			for (int i = 0; i < 3000; i++) {
				Tools.delay(1);
				if (mEngine.mDone) {
					App.log( "Engine finish: DONE");
					break;
				}
			}
			Tools.delay(9);
			App.log( "Engine finish 99");
		}



		// Drains all pending output from the encoder
        private void pullCodec() {
            try {
                int bufIndex = mVideoCodec.dequeueOutputBuffer(bufferInfo, 0);
                if (bufIndex >= 0) {
					pullVideoSample(bufIndex);
                } else if (bufIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
					pullVideoFormat();
                }
            } catch (Exception ex) {
                App.log( " ***_ex Encoder_pullCodec: " + ex.getMessage());
            }
        }



		private void pullVideoSample(int bufIndex) {
			ByteBuffer bufferData = mVideoCodec.getOutputBuffer(bufIndex);
			if (bufferData != null) {
				bufferData.position(bufferInfo.offset);
				bufferData.limit(bufferInfo.offset + bufferInfo.size);
				boolean intra =  ((bufferInfo.flags & MediaCodec.BUFFER_FLAG_KEY_FRAME) != 0);

				if ((bufferInfo.flags & MediaCodec.BUFFER_FLAG_CODEC_CONFIG) != 0) {
					//App.log("VideoCodec.BUFFER_FLAG_CODEC_CONFIG");
				} else {
					if (null != mEntrack) {
						mEntrack.writeVideoSample(bufferData, bufferInfo);
						bufferData.rewind();
					}
					bufferData.position(0);

					mRtmper.postSample(bufferData, bufferInfo, false);
					mStatus.traffic += bufferInfo.size;
					mStatus.tenSize += bufferInfo.size;
				}
				mVideoCodec.releaseOutputBuffer(bufIndex, false);
			}
		}



		private void pullVideoFormat() {
			mCodecVideoFormat = mVideoCodec.getOutputFormat();
			if (null != mEntrack) {
				mEntrack.addVideoTrack(mCodecVideoFormat);
			}
			mRtmper.setVideoFormat(mCodecVideoFormat);
			//App.log("pullVideoFormat: " + mCodecVideoFormat);
		}



        private class SurfaceTextureListener implements SurfaceTexture.OnFrameAvailableListener {
            @Override   // SurfaceTexture.OnFrameAvailableListener; runs on arbitrary thread
            public void onFrameAvailable(SurfaceTexture surfaceTexture) {
                mFrameReady = true;
            }
        }


    }








    private class Enaudio extends Thread {

        private volatile boolean mQuit = false;
        private volatile boolean mDone = false;
		private static final String LOG_TAG = "AudioRecorder";


        // audio format settings
		private static final String MIME_TYPE_AUDIO = "audio/mp4a-latm";
		private static final int SAMPLE_RATE = 44100;
		private static final int CHANNEL_CONFIG = AudioFormat.CHANNEL_IN_MONO;
		private static final int BYTES_PER_SLICE = 1024; // AAC
		private static final int AUDIO_FORMAT = AudioFormat.ENCODING_PCM_16BIT;
		private static final int AUDIO_SOURCE = MediaRecorder.AudioSource.DEFAULT;

		private static final int SLICE_BYTES = BYTES_PER_SLICE * 1;
		private static final long MICS_PER_FRAME = 1000000 * BYTES_PER_SLICE / (SAMPLE_RATE*2);

		private ByteBuffer mBuffer = ByteBuffer.allocateDirect(SLICE_BYTES);
        private MediaCodec mCodec;
        private MediaFormat mAudioFormat, mCodecAudioFormat;
        private AudioRecord mAudioRecorder;
        private MediaCodec.BufferInfo mBufInfo;
        private ByteBuffer bufData;
        private Entrack mAuTrack;
		private long mMikeEpoch, mMikeCount;
		private AudioPool mMikePool = new AudioPool();





        Enaudio() {
            mBufInfo = new MediaCodec.BufferInfo();
        }



        private void attach2() {
            // prepare encoder
            try {
                mAudioFormat = new MediaFormat();
                mAudioFormat.setString(MediaFormat.KEY_MIME, MIME_TYPE_AUDIO);
                mAudioFormat.setInteger(MediaFormat.KEY_AAC_PROFILE, MediaCodecInfo.CodecProfileLevel.AACObjectLC);
                mAudioFormat.setInteger(MediaFormat.KEY_SAMPLE_RATE, 44100);
                mAudioFormat.setInteger(MediaFormat.KEY_CHANNEL_COUNT, 1);
                mAudioFormat.setInteger(MediaFormat.KEY_BIT_RATE, 128 * 1024);
                mAudioFormat.setInteger(MediaFormat.KEY_MAX_INPUT_SIZE, 16384);

                mCodec = MediaCodec.createEncoderByType(MIME_TYPE_AUDIO);
                mCodec.configure(mAudioFormat, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);
                mCodec.start();
            } catch (Exception ex) {
                Log.e(LOG_TAG, "ex: " + ex.getMessage());
            }

            // prepare recorder
            try {
                int iMinBufferSize = AudioRecord.getMinBufferSize(SAMPLE_RATE, CHANNEL_CONFIG, AUDIO_FORMAT);
                int bufferSize = iMinBufferSize * 16;

                // Ensure buffer is adequately sized for the AudioRecord object to initialize
                if (bufferSize < iMinBufferSize)
                    bufferSize = ((iMinBufferSize / BYTES_PER_SLICE) + 1) * BYTES_PER_SLICE * 2;

                mAudioRecorder = new AudioRecord(
                    AUDIO_SOURCE,   // source
                    SAMPLE_RATE,    // sample bitrate, hz
                    CHANNEL_CONFIG, // channels
                    AUDIO_FORMAT,   // audio format
                    bufferSize);   // buffer size (bytes)

                mAudioRecorder.startRecording();

				mMikeEpoch = 0;
				mMikeCount = 0;
            } catch (Exception ex) {
                Log.e(LOG_TAG, "ex: " + ex.getMessage());
            }
        }



        private void detach() {
            if (mCodec != null) {
                mCodec.stop();
                mCodec.release();
                mCodec = null;
            }

            if (mAudioRecorder != null) {
                mAudioRecorder.stop();
                mAudioRecorder.release();
                mAudioRecorder = null;
            }

			if (null != mMikePool)
				mMikePool.printStats();
        }



        @Override
        public void run() {
            App.log( "Enaudio_run_11");
			try {
				attach2();
				while (true) {
                    if (mQuit) {
                        break;
                    }
                    Thread.sleep(1);
					takeSlice();
					pushFrame();
					pullChunk();
				}

				detach();
            } catch (Exception ex) {
                App.log( " *** Enaudio_run ex: " + ex.getMessage());
            }

            mDone = true;
            App.log( "Enaudio_run_99");
        }



        private void finish() {
            App.log( "autor_finish_11");
            mQuit = true;
            while (true) {
                if (mDone) {
                    App.log( "autor_quit_done");
                    break;
                }
                try {
                    Thread.sleep(22);
                } catch (InterruptedException ie) {
                }
            }
            App.log( "autor_finish_99");
        }




		public void takeSlice() {
			try {
				int sliceTotal = mAudioRecorder.read(mBuffer, BYTES_PER_SLICE);
				if (sliceTotal != BYTES_PER_SLICE) {
					App.log(String.format(Locale.getDefault(), "weird_audio_frame: %d", sliceTotal));
				}

				int sliceCount = 0;
				while (sliceCount < sliceTotal) {
					AudioPool.AudioPail pail = mMikePool.takeDepot(BYTES_PER_SLICE);
					if (null != pail) {
						mBuffer.position(sliceCount);
						mBuffer.limit(sliceCount + BYTES_PER_SLICE);
						deepCopy(mBuffer, pail.data);
						pail.data.limit(BYTES_PER_SLICE);
					}
					sliceCount += BYTES_PER_SLICE;

					if (0 == mMikeCount) {
						mMikeEpoch = System.nanoTime() / 1000;
						App.log(" * mMikeEpoch: " + mMikeEpoch / 1000000 + "  mics:" + mMikeEpoch);
					}
					mMikeCount++;

					long mics = mMikeCount * 1000000 * BYTES_PER_SLICE / (SAMPLE_RATE * 2);
					pail.stamp = mMikeEpoch + mics;
					mMikePool.pushQueue(pail);
					//App.log(String.format(Locale.getDefault(), "takeSlice_frame: %d, %d", BYTES_PER_SLICE, pail.stamp / 1000));
				}
			} catch (Exception ex) {
				Log.e(LOG_TAG, " ***_ex takeSlice: " + ex.getMessage());
			}
		}




        public void pushFrame() {
            try {
				while (mMikePool.getCount() > 0) {
					ByteBuffer[] buffers = mCodec.getInputBuffers();
					int inputBufferIndex = mCodec.dequeueInputBuffer(1000);
					if (inputBufferIndex >= 0) {
						ByteBuffer inputBuffer = buffers[inputBufferIndex];
						inputBuffer.clear();

						AudioPool.AudioPail pail = mMikePool.pullQueue();
						if (null != pail) {
							deepCopy(pail.data, inputBuffer);
							mCodec.queueInputBuffer(inputBufferIndex, 0, pail.data.limit(), pail.stamp, 0);
							mMikePool.feedDepot(pail);
							//App.log(String.format(Locale.getDefault(), "pushFrame_frame: %d, %d", pail.bufData.limit(), pail.stamp / 1000));
						}
					}
				}
            } catch (Exception ex) {
                Log.e(LOG_TAG, " ***_ex pushFrame: " + ex.getMessage());
            }
        }



        private void pullChunk() {
            if (null==mCodec) return;
			if (null==mRtmper) return;

            try {
                int bufIndex = mCodec.dequeueOutputBuffer(mBufInfo, 0);
                if (bufIndex >= 0) {
                    bufData = mCodec.getOutputBuffer(bufIndex);
                    if (bufData != null) {
                        bufData.position(mBufInfo.offset);
                        bufData.limit(mBufInfo.offset + mBufInfo.size);
                        if ((mBufInfo.flags & MediaCodec.BUFFER_FLAG_CODEC_CONFIG) != 0) {
                            App.log( "AudioCodec.BUFFER_FLAG_CODEC_CONFIG");
						} else {
							//audio chunk
							if (null != mAuTrack) {
								mAuTrack.writeAudioSample(bufData, mBufInfo);
								bufData.rewind();
							}
							bufData.position(0);
							mRtmper.postSample(bufData, mBufInfo, true);
                        }
                        mCodec.releaseOutputBuffer(bufIndex, false);
                    }

                } else if (bufIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
					mCodecAudioFormat = mCodec.getOutputFormat();
					if (null != mAuTrack) {
						mAuTrack.addAudioTrack(mCodecAudioFormat);
					}
					mRtmper.setAudioFormat(mCodecAudioFormat);
					App.log( "mCodecAudioFormat: " + mCodecAudioFormat);
                }

            } catch (Exception ex) {
                Log.e(LOG_TAG, " ***_ex pullChunk: " + ex.getMessage());
            }
        }


    }







    public class Entrack extends Thread {

		private Object mWriteLock = new Object();
		private Enmuxer mEnmuxer;

        private volatile boolean mQuit = false;
        private volatile boolean mDone = false;
		private volatile boolean mVideoReady, mAudioReady;
        private File mMp4File, mTmpFile;

		private int mVideoCount;
        private long mVideoBytes, mAudioBytes;
		private long mTrackKick, mTrackMils;

        private int mAudioTrack=-1, mVideoTrack=-1;
        private long mLastPull;
		private TrackPool mTrackPool = new TrackPool();



        public Entrack() {  }



        @Override
        public void run() {
            App.log( " * Tracker_run_11");


            try {
                while (true) {
                    Thread.sleep(1);
                    if (mQuit) {
                        App.log( "Tracker_need_quit");
                        break;
                    }

                    long span = Tools.mills() - mLastPull;
                    if (span > 5) {
                        mLastPull = Tools.mills();
                        pullQueue();
                    }
                }

            } catch (Exception ex) {
                App.log( " *** ex_Entrack_run: " + ex.getMessage());
            }
            mDone = true;
            App.log( "Tracker_run_99");
        }



        private void pullQueue() {
			while (mTrackPool.getCount() > 0) {
                long took = Tools.mills();
				TrackPool.TrackPail pail = mTrackPool.pullQueue();
				if (null != pail) {
					boolean result = false;

					synchronized (mWriteLock) {
						if (pail.trackIndex == mVideoTrack) {
							if (pail.bufInfo.flags > 0)
								App.log("pullQueue: sync frame - " + pail.bufInfo.presentationTimeUs/1000);
							result = mEnmuxer.writeVideoSample(pail.bufData, pail.bufInfo);

						} else if (pail.trackIndex == mAudioTrack) {
							result = mEnmuxer.writeAudioSample(pail.bufData, pail.bufInfo);

						}

						if (!result) {
							setLastFault(Errors.getText(Errors.WRITE_FILE));
						}
					}
				}
				mTrackPool.feedDepot(pail);
                took = Tools.mills() - took;
                if (took > 55) App.log( "### long writeSampleData " + took);
            }
            //App.log( "pullQueue " + "  audioCount:" + audioCount + "  videoCount: " + videoCount);
            //App.log( "  posi0: "+mPool.get(0).size() + "  posi1: "+mPool.get(1).size() + "  posi2: "+mPool.get(2).size() + "  posi3: " + mPool.get(3).size());
        }



		private void makeTempFile() {
			String path = mMp4File.getPath();
			int p = path.lastIndexOf('.');
			if (p < 1)
			    p = path.length();
			path = path.substring(0, p) + ".temp";
			mTmpFile = new File(path);
		}



        public void attach(File aFile) {
			mTrackPool = new TrackPool();
			mMp4File = aFile;
			makeTempFile();
            try {
				mEnmuxer = new Enmuxer();
				mEnmuxer.attach(mTmpFile.getPath());
            }
            catch (Exception ex) {
				App.log(" *** ex_Entrack_attach " + ex.getMessage());
            }
        }



        public void detach() {
			App.log( "  Tracker_detach_11");
			mQuit = true;
            try {
				sleep(99);
				pullQueue();

				if (mEnmuxer != null) {
					mEnmuxer.detach();

					if (isFaulted() || mVideoCount < 22) {
						App.log( "  # Tracker_detach_delete");
						mTmpFile.delete();
					}
					else {
						App.log( "  Tracker_detach_rename");
						mTmpFile.renameTo(mMp4File);
					}

					if (null != mTrackPool)
						mTrackPool.printStats();
				}

            } catch (Exception ex) {
                App.log(" *** ex_Entrack_detach " + ex.getMessage());
            }
            App.log( "  Tracker_detach_99");
        }



		private void tryToStart() {
			if (!mEnmuxer.isStarted()) return;
			start();  // thread
			App.log("tryToStart");
		}



        public void addVideoTrack(MediaFormat aFormat) {
			if (mVideoReady) return;
			if (mEnmuxer == null) return;
			try {
				App.log("  addVideoTrack_11");
				if (!isFaulted()) {
					synchronized (mWriteLock) {
						mVideoTrack = mEnmuxer.addVideoTrack(aFormat);
						if (mVideoTrack < 0) {
							setLastFault(Errors.getText(mVideoTrack));
						}
					}
				}

				if (!isFaulted()) {
					App.log("mVideoReady = true");
					mVideoReady = true;
					tryToStart();
				}

				//App.log( "addVideoTrack_99 " + aFormat);
			} catch (Exception ex) {
				App.log("addVideoTrack " + ex.getMessage());
			}
        }



		public void addAudioTrack(MediaFormat aFormat) {
			if (mEnmuxer == null) return;
			if (aFormat == null) return;
			try {
				App.log( " * addAudioTrack_11");

				if (!isFaulted()) {
					synchronized (mWriteLock) {
						mAudioTrack = mEnmuxer.addAudioTrack(aFormat);
						if (mAudioTrack < 0) {
							setLastFault(Errors.getText(mAudioTrack));
						}
					}
				}

				if (!isFaulted()) {
					App.log("mAudioReady = true");
					mAudioReady = true;
					tryToStart();
				}

				//App.log( "addAudioTrack_99 " + mAudioTrack);
			} catch (Exception ex) {
				App.log(" ***_ex Tracker addAudioTrack " + ex.getMessage());
			}
		}



        public void writeVideoSample(ByteBuffer aBufData, MediaCodec.BufferInfo aBufInfo) {
			if (!mVideoReady || !mAudioReady) return;

			if (0 == mTrackKick) {
				if ( (aBufInfo.flags & MediaCodec.BUFFER_FLAG_KEY_FRAME) == 0) {
					return;  // wait for a sync frame
				}
			}

			queueForTrack(mVideoTrack, aBufData, aBufInfo);
			mVideoCount++;
			mVideoBytes += aBufInfo.size;

			mTrackMils = aBufInfo.presentationTimeUs / 1000;
			if (0 == mTrackKick) {
				mTrackKick = mTrackMils;
				App.log("  * Entrack_writeVideoSample: mTarkReady = true");
			}
        }



        public void writeAudioSample(ByteBuffer aBufData, MediaCodec.BufferInfo aBufInfo) {
			if (0 == mTrackKick) return;
			queueForTrack(mAudioTrack, aBufData, aBufInfo);
            mAudioBytes += aBufInfo.size;
        }



        private void queueForTrack(int aTrackIndex, ByteBuffer aBufData, MediaCodec.BufferInfo aBufInfo) {
            long took = Tools.mills();
			try {
				TrackPool.TrackPail pail = mTrackPool.takeDepot(aBufInfo.size);
				pail.trackIndex = aTrackIndex;
				pail.bufInfo.set(0, aBufInfo.size, aBufInfo.presentationTimeUs, aBufInfo.flags);
				aBufData.rewind();
				pail.bufData.clear();
				pail.bufData.put(aBufData);
				pail.bufData.flip();
				mTrackPool.pushQueue(pail);
            }
            catch (Exception ex) {
				App.log(" *** ex_queueForTrack " + ex.getMessage());
            }
            took = Tools.mills() - took;
            if (took > 11) App.log( "### long queueForTrack " + took);
        }

    }








	public Bitmap createTextBitmap(String aText, int aSize, int aColor, int aBackgr) {
		Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
		paint.setTextSize(aSize);
		paint.setColor(aColor);
		paint.setTypeface(Typeface.create("Arial", Typeface.BOLD_ITALIC));

		Rect rect = new Rect();
		paint.getTextBounds(aText, 0, aText.length(), rect);
		int width = rect.width() + aSize;
		int height = rect.height() + aSize;
		int x = rect.left + aSize/2;
		int y = rect.height() + aSize/2;
		//App.log("  * " + String.format("x: %d,  y: %d,  x: %d, y: %d, left: %d, top: %d, right: %d, bottom: %d",
		//		x, y, x, y, rect.left, rect.top, rect.right, rect.bottom));

		width = Tools.nextPowerOf2(width);
		height = Tools.nextPowerOf2(height);
		final Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
		bitmap.setDensity(Bitmap.DENSITY_NONE);
		bitmap.eraseColor(aBackgr);
		Canvas canvas = new Canvas(bitmap);
		canvas.setDensity(Bitmap.DENSITY_NONE);
		canvas.drawText(aText, x, y, paint);

		return bitmap;
	}



	public Bitmap createGridBitmap(Size2D aSize) {
		Paint paint = new Paint();
		paint.setStyle(Paint.Style.STROKE);
		paint.setColor(Color.WHITE);
		paint.setStrokeWidth(1);
		paint.setAntiAlias(true);

		int width = Tools.nextPowerOf2(aSize.x);
		int height = Tools.nextPowerOf2(aSize.y);

		final Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
		bitmap.setDensity(Bitmap.DENSITY_NONE);
		bitmap.eraseColor(Color.TRANSPARENT);
		Canvas canvas = new Canvas(bitmap);
		canvas.setDensity(Bitmap.DENSITY_NONE);

		float y0 = height/3;
		canvas.drawLine(0, y0, width, y0, paint);
		canvas.drawLine(0, 2*height/3, width, 2*height/3, paint);
		canvas.drawLine(width/3, 0, width/3, height, paint);
		canvas.drawLine(2*width/3, 0, 2*width/3, height, paint);

		return bitmap;
	}




	public class OrderFifo {

		ArrayBlockingQueue<Order> mQueue;

		public OrderFifo() {
			mQueue = new ArrayBlockingQueue<Order>(9);
		}



		public boolean push(int aCode) {
			Order order = new Order();
			order.code = aCode;
			return mQueue.offer(order);
		}


		public Order pull() {
			Order result = null;
			try {
				result = mQueue.poll(1, TimeUnit.MICROSECONDS);
			} catch (Exception ex) {};
			return result;
		}


		public void clear() {
			mQueue.clear();
		}

	}



	private void deepCopy(ByteBuffer aSource, ByteBuffer aTarget) {
		aTarget.clear();
		aTarget.put(aSource);
		aTarget.flip();
		aSource.flip();
	}







	private RtmpStateEvents RTMPER_EVENTER = new RtmpStateEvents() {


		public void onOpen() {
			App.log("RtmpStateEvents_onOpen");
			if (!isIdle()) {
				mState = STATE_CONNECTED;
				mKickMils = Tools.mills();
			}
			if (null != mEventer)
				mEventer.onStatus();
		}


		public void onFailed() {
			App.log("RtmpStateEvents_onFailed");
			cease();
			if (!isIdle())
				mState = STATE_IDLE;
            if (mAskedReconnection) {
                reconnect();
            } else {
                mEventer.onFault("Connection failed!\n\nPlease check the broadcast parameters.");
            }
		}


		public void onClosed() {
			App.log("RtmpStateEvents_onClosed");
			cease();
			mEventer.onStatus();
		}


		public void onDisconnected() {
			App.log("RtmpStateEvents_onDisconnected");
			cease();
            reconnect();
//			if (null != mEventer)
//				mEventer.onDisconnected();
		}

	};


}

