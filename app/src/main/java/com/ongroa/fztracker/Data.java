package com.ongroa.fztracker;

import android.location.Location;
import android.util.Log;

import com.dsi.ant.plugins.antplus.pcc.AntPlusBikePowerPcc;

import java.util.ArrayList;

public class Data {
    static AntPlusBikePowerPcc pwrPcc;

    static State mState;
    static SportType mSportType;
    static float mMinSpeedLimit;
    static float mSpeed;
    static float mAccuracy;
    static float mBearing;
    static long mCounter;
    static float mDistance;
    static long mElapsedTime;
    static long mPrevTime;
    static long mMovingTime;
    static long mLastSavedTime;

    static double mLatitude;
    static double mLongitude;
    static double mTotalAscent;
    static double mAltitude;
    static String mNow;
    static String mStartTime;
    static ArrayList<TrackPoint> mTrackPoints;
    static ArrayList<PowerPoint> mPowerPoints;
    static Location mLastLocation;
    static int mPower;
    static int mCadence;

    static int bgColor;

    static public void init() {
        pwrPcc = null;
        mState = State.INIT;
        mSportType = SportType.RIDE;
        mCadence = 0;
        mPower = 0;
        mPrevTime = 0;
        mCounter = 0;
        mTrackPoints.clear();
        mPowerPoints.clear();
        mDistance = 0;
        mMovingTime = 0;
        mElapsedTime = 0;
        mTotalAscent = 0;
        mLastSavedTime = 0;
        mLatitude = 0;
        mLongitude = 0;
    }

    static public int getAveragePower(long millis) {
        long now = System.currentTimeMillis();
        int n = 0;
        double power = 0;
        while (true) {
            int idx = mPowerPoints.size() - n - 1;
            if (idx < 0 || idx >= mPowerPoints.size()) {
                break;
            }
            if (mPowerPoints.get(idx).millis < now - millis) {
                break;
            }
            n++;
            power += mPowerPoints.get(mPowerPoints.size() - n).power;
        }
        Log.i("getAveragePower", String.format("n: %d, avg: %.2f", n, power / n));
        if (n > 0) {
            return (int)(power / n);
        } else {
            return 0;
        }
    }

}
