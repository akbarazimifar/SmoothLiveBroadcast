package com.rustero.rtmp;

public interface RtmpStateEvents {
	void onOpen();
	void onFailed();
	void onClosed();
	void onDisconnected();
}
