package com.rustero.core.stickers;




public class StickerObject {
	public String name;
	public int verticalId, horizontalId;

	public StickerObject(String aName, int aHorId, int aVerId) {
		name = aName;
		horizontalId = aHorId;
		verticalId = aVerId;
	}

}
