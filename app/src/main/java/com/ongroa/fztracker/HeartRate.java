package com.ongroa.fztracker;

import android.content.Context;
import android.util.Log;

import com.dsi.ant.plugins.antplus.pcc.AntPlusHeartRatePcc;
import com.dsi.ant.plugins.antplus.pcc.defines.DeviceState;
import com.dsi.ant.plugins.antplus.pcc.defines.EventFlag;
import com.dsi.ant.plugins.antplus.pcc.defines.RequestAccessResult;
import com.dsi.ant.plugins.antplus.pccbase.AntPluginPcc;
import com.dsi.ant.plugins.antplus.pccbase.PccReleaseHandle;

public class HeartRate {
    Context applicationContext;

    PccReleaseHandle<AntPlusHeartRatePcc> heartRatePccReleaseHandle = null;
    int[] ids = {661};
    int idx = 0;

    public HeartRate(Context applicationContext) {
        this.applicationContext = applicationContext;
        if (heartRatePccReleaseHandle != null) {
            heartRatePccReleaseHandle.close();
        }
        requestAccess();
    }

    AntPluginPcc.IPluginAccessResultReceiver<AntPlusHeartRatePcc> resultReceiverHeartRate = new AntPluginPcc.IPluginAccessResultReceiver<AntPlusHeartRatePcc>() {

        @Override
        public void onResultReceived(AntPlusHeartRatePcc antPlusHeartRatePcc, RequestAccessResult requestAccessResult, DeviceState deviceState) {
            Log.i("heart", "heart rate onResultReceived " + requestAccessResult);
            Log.i("heart", "deviceState: " + deviceState);

            switch (requestAccessResult) {
                case SUCCESS:
                    Data.heartRatePcc = antPlusHeartRatePcc;
                    subscribeToEventsHeartRate();
                    break;
                default:
                    if (Data.heartRatePcc == null) {
                        requestAccess();
                    }
            }
        }
    };

    private void requestAccess() {
        Log.i("heart", String.format("request access: %d", ids[idx]));
        heartRatePccReleaseHandle = AntPlusHeartRatePcc.requestAccess(applicationContext, ids[idx],
                0, resultReceiverHeartRate, deviceStateChangeReceiverHeartRate);
        idx++;
        if (idx == ids.length) {
            idx = 0;
        }
    }

    AntPluginPcc.IDeviceStateChangeReceiver deviceStateChangeReceiverHeartRate = new AntPluginPcc.IDeviceStateChangeReceiver() {

        @Override
        public void onDeviceStateChange(final DeviceState newDeviceState) {
            Log.i("state heart rate", "newDeviceState: " + newDeviceState);
            if (newDeviceState == DeviceState.DEAD) {
                heartRatePccReleaseHandle.close();
                heartRatePccReleaseHandle = null;
            } else if (newDeviceState == DeviceState.CLOSED) {
                requestAccess();
            }
        }
    };

    void subscribeToEventsHeartRate() {
        Data.heartRatePcc.subscribeHeartRateDataEvent(new AntPlusHeartRatePcc.IHeartRateDataReceiver() {
            @Override
            public void onNewHeartRateData(long estTimestamp, java.util.EnumSet<EventFlag> eventFlags, int computedHeartRate,
                                           long heartBeatCount, java.math.BigDecimal heartBeatEventTime, AntPlusHeartRatePcc.DataState dataState) {
                if (dataState == AntPlusHeartRatePcc.DataState.LIVE_DATA) {
                    Log.i("onNewHeartRateData", "heart rate: " + String.valueOf(computedHeartRate));
                    Data.heartRate = computedHeartRate;
                } else {
                    Log.i("heart", "no heart rate data");
                    Data.heartRate = -1;
                }
            }
        });
    }

}
