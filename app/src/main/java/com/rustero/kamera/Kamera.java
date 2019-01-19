package com.rustero.kamera;


import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.media.MediaCodec;
import android.view.SurfaceHolder;

import com.rustero.App;
import com.rustero.gadgets.Codecs;
import com.rustero.gadgets.Size2D;
import com.rustero.gadgets.SizeList;


public class Kamera {

	public String id;
	public boolean isBack, isFront;
	public boolean hasFlash;
	public int sensorOrientation;

	private CameraCharacteristics mCharacteristics;
	private SizeList mSurfaceSizes, mSurtexSizes, mCodecSizes, mFrameSizes;



	public Kamera(String aId, CameraManager aManager, String[] aSizes) {
		id = aId;
		try {
			mCharacteristics = aManager.getCameraCharacteristics(aId);
			if (mCharacteristics.get(CameraCharacteristics.LENS_FACING) == CameraCharacteristics.LENS_FACING_BACK)
				isBack = true;
			else if (mCharacteristics.get(CameraCharacteristics.LENS_FACING) == CameraCharacteristics.LENS_FACING_FRONT)
				isFront = true;
			sensorOrientation = mCharacteristics.get(CameraCharacteristics.SENSOR_ORIENTATION);
			hasFlash = mCharacteristics.get(CameraCharacteristics.FLASH_INFO_AVAILABLE);

//			if (isBack)
//				App.log("  back kamera: " + sensorOrientation);
//			else
//				App.log("  front kamera: " + sensorOrientation);

			StreamConfigurationMap configMap = mCharacteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
			mSurfaceSizes = new SizeList(configMap.getOutputSizes(SurfaceHolder.class));
			mSurtexSizes = new SizeList(configMap.getOutputSizes(SurfaceTexture.class));
			mCodecSizes = new SizeList(configMap.getOutputSizes(MediaCodec.class));

			mFrameSizes = new SizeList();
			for (int i=0; i<aSizes.length; i++) {
				String resstr = aSizes[i];
				Size2D size2 = Size2D.parseText(resstr);
				checkSize(size2);
			}
		} catch (Exception ex) {
			App.log(" ***_ex: Kamera(): " + ex.getMessage());
		}
	}



	public SizeList getResoList() {
		return mFrameSizes;
	}



	private void checkSize(Size2D aSize) {
		if (!mSurfaceSizes.hasSize(aSize)) return;
		if (!mSurtexSizes.hasSize(aSize)) return;
		if (!mCodecSizes.hasSize(aSize)) return;

		if (aSize.y >= 1080) {
			if (!Codecs.isSupported(aSize)) {
				return;
			}
		}

		mFrameSizes.addSize(aSize);
	}

}
