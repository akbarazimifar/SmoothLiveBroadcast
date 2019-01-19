package com.rustero.core.stickers;


import com.rustero.R;

import java.util.ArrayList;
import java.util.List;

public class StickerManager {

	public static final String NAME_BEACH = 		"Beach";
	public static final String NAME_BIRTHDAY_1 = 	"Birthday 1";
	public static final String NAME_BIRTHDAY_2 = 	"Birthday 2";
	public static final String NAME_CHRISTMAS_1 = 	"Christmas 1";
	public static final String NAME_CHRISTMAS_2 = 	"Christmas 2";
	public static final String NAME_PARROT = 		"Parrot";
	public static final String NAME_RAIN = 			"Rain";
	public static final String NAME_SNOW = 			"Snow";
	public static final String NAME_SNOWMAN = 		"Snowman";



	private static StickerManager self;

	private List<StickerObject> mList;
	private int mIndex;



	public static StickerManager get() {
		if (null == self)
			self = new StickerManager();
		return self;
	}



	private StickerManager() {
		mList = new ArrayList<>();
//		mList.add(new StickerObject(NAME_BEACH, R.raw.beach_h, R.raw.beach_v));
//		mList.add(new StickerObject(NAME_BIRTHDAY_1, R.raw.birthday_h_1, R.raw.birthday_v_1));
//		mList.add(new StickerObject(NAME_BIRTHDAY_2, R.raw.birthday_h_2, R.raw.birthday_v_2));
//		mList.add(new StickerObject(NAME_CHRISTMAS_1, R.raw.christmas_h_1, R.raw.christmas_v_1));
//		mList.add(new StickerObject(NAME_CHRISTMAS_2, R.raw.christmas_h_2, R.raw.christmas_v_2));
//		mList.add(new StickerObject(NAME_PARROT, R.raw.parrot_h, R.raw.parrot_v));
//		mList.add(new StickerObject(NAME_RAIN, R.raw.rain_h, R.raw.rain_v));
//		mList.add(new StickerObject(NAME_SNOW, R.raw.snow_h, R.raw.snow_v));
//		mList.add(new StickerObject(NAME_SNOWMAN, R.raw.snowman_h, R.raw.snowman_v));
	}



	public List<String> getNames() {
		List<String> result = new ArrayList<>();
		for (StickerObject theme : mList) {
			result.add(theme.name);
		}
		return result;
	}


	public StickerObject getTheme(String aName) {
		if (aName.isEmpty()) return null;
		StickerObject result = null;
		for (StickerObject theme : mList) {
			if (theme.name.equals(aName)) {
				result = theme;
				break;
			}
		}
		return result;
	}


}
