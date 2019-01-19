package com.rustero.gadgets;


import android.util.Log;

public class Clerk {

    public long period = 999;
    private long mTack;
    public float rate = 16f;
    private float mBusy = 0;
    private String mTag;
    long mTick;


    public Clerk(String aTag) {
        mTag = aTag;
        mTack = Tools.mills();
    }



    public void start() {
        mTick = Tools.mills();
    }



    public void stop() {
        long span = Tools.mills() - mTick;
        mBusy =  (rate-1)/rate * mBusy;
        mBusy +=  1/rate * span;
        if (Tools.mills() - mTack < 1000) return;
        mTack = Tools.mills();
        Log.d(mTag, "busy: " + Math.round(mBusy));
    }


}
