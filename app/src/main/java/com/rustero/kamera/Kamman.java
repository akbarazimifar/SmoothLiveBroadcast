package com.rustero.kamera;


import android.content.Context;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.media.MediaCodecInfo;
import android.media.MediaCodecList;
import android.os.Handler;
import android.util.Size;

import com.rustero.App;
import com.rustero.gadgets.Size2D;
import com.rustero.gadgets.SizeList;

import java.util.ArrayList;
import java.util.List;


public class Kamman {



	private static Kamman self;
	private static String[] mAskedSizes;
	private CameraManager mManager;
	private List<Kamera> mKameras = new ArrayList<>();
	private String[] mCameraIds;

	private static final String PREF_KEY_NOW_FRONT = "now_front";
	private static final String PREF_KEY_BACK_RESOL = "back_resol";
	private static final String PREF_KEY_FRONT_RESOL = "front_resol";




	public static Kamman get() {
		if (null == self) {
			self = new Kamman();
			self.attach();
		}
		return self;
	}


	public static void create(String[] aSizes) {
		mAskedSizes = aSizes;
		get();
	}


	public static void delete() {
		if (null == self) return;
		self = null;
	}



	private void attach() {
		App.log("Kamman_attach");
		try {
			mManager = (CameraManager) App.self.getSystemService(Context.CAMERA_SERVICE);
			mCameraIds = mManager.getCameraIdList();
			for (String id : mCameraIds) {
				Kamera kamera = new Kamera(id, mManager, mAskedSizes);
				mKameras.add(kamera);
			}

			//check wanted resol
			checkWantedResols(getFrontId(), PREF_KEY_FRONT_RESOL);
			checkWantedResols(getBackId(), PREF_KEY_BACK_RESOL);

		} catch (Exception ex) {
			App.log(" ***_ex: Camman_attach: " + ex.getMessage());
		}
	}



	public int getCameraCount() {
		return mKameras.size();
	}



	public Kamera getKamera(String aId) {
		Kamera result = null;
		for (Kamera kamera : mKameras) {
			if (kamera.id.equals(aId))
				return kamera;
		}
		return result;
	}



	public String getFirstId() {
		return mKameras.get(0).id;
	}



	public String getFrontId() {
		String result = "";
		for (Kamera kamera : mKameras) {
			if (kamera.isFront)
				return kamera.id;
		}
		return result;
	}


	public String getBackId() {
		String result = "";
		for (Kamera kamera : mKameras) {
			if (kamera.isBack)
				return kamera.id;
		}
		return result;
	}


	public boolean isNowFront() {
		boolean result = false;
		if (getCameraCount() > 1) {
			result = App.getPrefBln(PREF_KEY_NOW_FRONT);
		} else {
			if (!getBackId().isEmpty())
				result = false;
			else
				result = true;
		}
		return result;
	}



	public void switchFrontBack() {
		boolean nowFront = App.getPrefBln(PREF_KEY_NOW_FRONT);
		nowFront = !nowFront;
		App.setPrefBln(PREF_KEY_NOW_FRONT, nowFront);
	}


	public SizeList getFrontResoList() {
		return getResoList(getFrontId());
	}


	public SizeList getBackResoList() {
		 return getResoList(getBackId());
	}


	public SizeList getResoList(String aCid) {
		SizeList result = new SizeList();
		if (!aCid.isEmpty()) {
			Kamera kam = Kamman.get().getKamera(aCid);
			if (null != kam)
				result =  kam.getResoList();
		}
		return result;
	}




	public void setWantedFrontReso(String aResoText) {
		App.setPrefStr(PREF_KEY_FRONT_RESOL, aResoText);
	}


	public void setWantedBackReso(String aResoText) {
		App.setPrefStr(PREF_KEY_BACK_RESOL, aResoText);
	}


	public String getWantedFrontReso() {
		String resstr = App.getPrefStr(PREF_KEY_FRONT_RESOL);
		if (resstr.isEmpty())
			resstr = "640x480";
		return resstr;
	}


	public String getWantedBackReso() {
		String resstr = App.getPrefStr(PREF_KEY_BACK_RESOL);
		if (resstr.isEmpty())
			resstr = "640x480";
		return resstr;
	}


	public int getWantedFrontIndex() {
		String resstr = App.getPrefStr(PREF_KEY_FRONT_RESOL);
		if (resstr.isEmpty())
			resstr = "640x480";
		SizeList resoList = getFrontResoList();
		int index = resoList.findResoText(resstr);
		return index;
	}


	public int getWantedBackIndex() {
		String resstr = App.getPrefStr(PREF_KEY_BACK_RESOL);
		if (resstr.isEmpty())
			resstr = "640x480";
		SizeList resoList = getBackResoList();
		int index = resoList.findResoText(resstr);
		return index;
	}


	public String getCurrentResoText() {
		String resstr = "640x480";
		String cid = getFirstId();
		if (isNowFront()) {
			cid = getFrontId();
			resstr = getWantedFrontReso();
		} else {
			cid = getBackId();
			resstr = getWantedBackReso();
		}
		return resstr;
	}



	public boolean getCurrentFlash() {
		boolean result = false;
		String cid = getFirstId();
		if (isNowFront()) {
			cid = getFrontId();
		} else {
			cid = getBackId();
		}
		Kamera kamera = getKamera(cid);
		if (null == kamera) return false;
		return kamera.hasFlash;
	}







	public Kamera openCamera(String aId, CameraDevice.StateCallback aCallback, Handler aHandler) {
		Kamera result = Kamman.get().getKamera(aId);
		try {
			mManager.openCamera(aId, aCallback, aHandler);
		} catch (Exception ex) {
			result = null;
			App.log(" ***_ex: Kamman_openCamera: " + ex.getMessage());
		}
		return result;
	}



	private void checkWantedResols(String aId, String aKey) {
		if (aId.isEmpty()) return;
		String resstr = App.getPrefStr(aKey);
		if (!resstr.isEmpty()) return;
		Kamera kamera = getKamera(aId);
		if (kamera == null) return;
		SizeList sizes = kamera.getResoList();
		Size2D size = new Size2D(1280, 720);
		if (sizes.hasSize(size))
			App.setPrefStr(aKey, size.toText());
		else
			App.setPrefStr(aKey, (new Size2D(640, 480)).toText());
	}







	private static MediaCodecInfo getEncoderInfo(String mimeType) {
		int numCodecs = MediaCodecList.getCodecCount();
		for (int i = 0; i < numCodecs; i++) {
			MediaCodecInfo codecInfo = MediaCodecList.getCodecInfoAt(i);
			if (!codecInfo.isEncoder()) {
				continue;
			}
			String[] types = codecInfo.getSupportedTypes();
			for (int j = 0; j < types.length; j++) {
				if (types[j].equalsIgnoreCase(mimeType)) {
					return codecInfo;
				}
			}
		}
		return null;
	}



	public void selfTest() {
//		MediaCodecInfo codecInfo = getEncoderInfo(MediaFormat.MIMETYPE_VIDEO_AVC);
		fpsSizeTest();
		App.log("Camman_selfTest_99");
	}



	private void fpsSizeTest() {
			try {
//				CameraCharacteristics characteristics = mManager.getCameraCharacteristics("0");
//				StreamConfigurationMap config = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);

				int fps = 30;
				Size size = new Size(1920, 1080);
				if (!isSupportedByAVCEncoder(size, fps)) {
						App.log("encoding " + size + "@" + fps + "frameFps"	+ " is not supported");
					}
//					startSlowMotionRecording(/*useMediaRecorder*/true, videoFramerate, captureRate,	fpsRange);
			} catch (Exception ex) {
				App.log(" ***_ex fpsSizeTest: " + ex.getMessage());
			}
		}


//
//	private Range<Integer> getHighestHighSpeedFixedFpsRangeForSize(StreamConfigurationMap config,
//																   Size size) {
//		Range<Integer>[] availableFpsRanges = config.getHighSpeedVideoFpsRangesFor(size);
//		Range<Integer> maxRange = availableFpsRanges[0];
//		boolean foundRange = false;
//		for (Range<Integer> range : availableFpsRanges) {
//			if (range.getLower() == range.getUpper() && range.getLower() >= maxRange.getLower()) {
//				foundRange = true;
//				maxRange = range;
//			}
//		}
//		if (!foundRange) {
//			return null;
//		}
//		return maxRange;
//	}


	/**
	 * Check if encoder can support this size and frame rate combination by querying
	 * MediaCodec capability. Check is based on size and frame rate. Ignore the bit rate
	 * as the bit rates targeted in this test are well below the bit rate max value specified
	 * by AVC specification for certain level.
	 */
	public static boolean isSupportedByAVCEncoder(Size aSize, int frameRate) {
		String mimeType = "video/avc";
		MediaCodecInfo codecInfo = getEncoderInfo(mimeType);
		if (codecInfo == null) {
			return false;
		}
		MediaCodecInfo.CodecCapabilities cap = codecInfo.getCapabilitiesForType(mimeType);
		if (cap == null) {
			return false;
		}
		int highestLevel = 0;
		for (MediaCodecInfo.CodecProfileLevel lvl : cap.profileLevels) {
			if (lvl.level > highestLevel) {
				highestLevel = lvl.level;
			}
		}
		// Don't support anything meaningful for level 1 or 2.
		if (highestLevel <= MediaCodecInfo.CodecProfileLevel.AVCLevel2) {
			return false;
		}
		App.log("The highest level supported by encoder is: " + highestLevel);

		// Put bitRate here for future use.
		int maxW, maxH, bitRate;
		// Max encoding speed.
		int maxMacroblocksPerSecond = 0;
		switch(highestLevel) {
			case MediaCodecInfo.CodecProfileLevel.AVCLevel21:
				maxW = 352;
				maxH = 576;
				bitRate = 4000000;
				maxMacroblocksPerSecond = 19800;
				break;
			case MediaCodecInfo.CodecProfileLevel.AVCLevel22:
				maxW = 720;
				maxH = 480;
				bitRate = 4000000;
				maxMacroblocksPerSecond = 20250;
				break;
			case MediaCodecInfo.CodecProfileLevel.AVCLevel3:
				maxW = 720;
				maxH = 480;
				bitRate = 10000000;
				maxMacroblocksPerSecond = 40500;
				break;
			case MediaCodecInfo.CodecProfileLevel.AVCLevel31:
				maxW = 1280;
				maxH = 720;
				bitRate = 14000000;
				maxMacroblocksPerSecond = 108000;
				break;
			case MediaCodecInfo.CodecProfileLevel.AVCLevel32:
				maxW = 1280;
				maxH = 720;
				bitRate = 20000000;
				maxMacroblocksPerSecond = 216000;
				break;
			case MediaCodecInfo.CodecProfileLevel.AVCLevel4:
				maxW = 1920;
				maxH = 1088; // It should be 1088 in terms of AVC capability.
				bitRate = 20000000;
				maxMacroblocksPerSecond = 245760;
				break;
			case MediaCodecInfo.CodecProfileLevel.AVCLevel41:
				maxW = 1920;
				maxH = 1088; // It should be 1088 in terms of AVC capability.
				bitRate = 50000000;
				maxMacroblocksPerSecond = 245760;
				break;
			case MediaCodecInfo.CodecProfileLevel.AVCLevel42:
				maxW = 2048;
				maxH = 1088; // It should be 1088 in terms of AVC capability.
				bitRate = 50000000;
				maxMacroblocksPerSecond = 522240;
				break;
			case MediaCodecInfo.CodecProfileLevel.AVCLevel5:
				maxW = 3672;
				maxH = 1536;
				bitRate = 135000000;
				maxMacroblocksPerSecond = 589824;
				break;
			case MediaCodecInfo.CodecProfileLevel.AVCLevel51:
			default:
				maxW = 4096;
				maxH = 2304;
				bitRate = 240000000;
				maxMacroblocksPerSecond = 983040;
				break;
		}
		// Check size limit.
		if (aSize.getWidth() > maxW || aSize.getHeight() > maxH) {
			App.log("Requested resolution " + aSize.toString() + " exceeds (" + maxW + "," + maxH + ")");
			return false;
		}
		// Check frame rate limit.
		Size sizeInMb = new Size((aSize.getWidth() + 15) / 16, (aSize.getHeight() + 15) / 16);
		int maxFps = maxMacroblocksPerSecond / (sizeInMb.getWidth() * sizeInMb.getHeight());
		if (frameRate > maxFps) {
			App.log("Requested frame rate " + frameRate + " exceeds " + maxFps);
			return false;
		}
		return true;
	}

}






