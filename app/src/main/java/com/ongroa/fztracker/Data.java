package com.ongroa.fztracker;

import android.location.Location;
import android.util.Log;

import com.dsi.ant.plugins.antplus.pcc.AntPlusBikePowerPcc;
import com.dsi.ant.plugins.antplus.pcc.AntPlusHeartRatePcc;

import java.util.ArrayList;

public class Data {
    static AntPlusBikePowerPcc pwrPcc;
    static AntPlusHeartRatePcc heartRatePcc;

    static State state;
    static SportType sportType;
    static float minSpeedLimit;
    static float speed;
    static float accuracy;
    static float bearing;
    static long counter;
    static float distance;
    static long elapsedTime;
    static long prevTime;
    static long movingTime;
    static long lastSavedTime;

    static double latitude;
    static double longitude;
    static double totalAscent;
    static double altitude;
    static String now;
    static String startTime;
    static ArrayList<TrackPoint> trackPoints;
    static ArrayList<PowerPoint> powerPoints;
    static Location lastLocation;
    static int power;
    static int cadence;
    static int heartRate;

    static int bgColor;

    static public void init() {
        pwrPcc = null;
        heartRatePcc = null;
        state = State.INIT;
        sportType = SportType.RIDE;
        cadence = 0;
        power = 0;
        heartRate = 0;
        prevTime = 0;
        counter = 0;
        trackPoints.clear();
        powerPoints.clear();
        distance = 0;
        movingTime = 0;
        elapsedTime = 0;
        totalAscent = 0;
        lastSavedTime = 0;
        latitude = 0;
        longitude = 0;
    }

    static public int getAveragePower(long millis) {
        long now = System.currentTimeMillis();
        int n = 0;
        double power = 0;
        while (true) {
            int idx = powerPoints.size() - n - 1;
            if (idx < 0 || idx >= powerPoints.size()) {
                break;
            }
            if (powerPoints.get(idx).millis < now - millis) {
                break;
            }
            n++;
            power += powerPoints.get(powerPoints.size() - n).power;
        }
        Log.i("getAveragePower", String.format("n: %d, avg: %.2f", n, power / n));
        if (n > 0) {
            return (int)(power / n);
        } else {
            return 0;
        }
    }

}
