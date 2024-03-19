package com.ongroa.fztracker;

import android.content.Context;
import android.util.Log;

import com.dsi.ant.plugins.antplus.pcc.AntPlusBikePowerPcc;
import com.dsi.ant.plugins.antplus.pcc.defines.DeviceState;
import com.dsi.ant.plugins.antplus.pcc.defines.EventFlag;
import com.dsi.ant.plugins.antplus.pcc.defines.RequestAccessResult;
import com.dsi.ant.plugins.antplus.pccbase.AntPluginPcc;
import com.dsi.ant.plugins.antplus.pccbase.PccReleaseHandle;

import java.math.BigDecimal;
import java.util.EnumSet;

public class Power {
    Context applicationContext;

    PccReleaseHandle<AntPlusBikePowerPcc> powerPccReleaseHandle = null;
    int[] ids = {16514, 35816};
    int idx = 0;

    public Power(Context applicationContext) {
        this.applicationContext = applicationContext;
        if (powerPccReleaseHandle != null) {
            powerPccReleaseHandle.close();
        }
        requestAccess();
    }

    private void requestAccess() {
        Log.i("power", String.format("request access: %d", ids[idx]));
        powerPccReleaseHandle = AntPlusBikePowerPcc.requestAccess(applicationContext,
                ids[idx],
                0,
                resultReceiverPower,
                deviceStateChangeReceiverPower);
        idx++;
        if (idx == ids.length) {
            idx = 0;
        }
    }

    AntPluginPcc.IPluginAccessResultReceiver<AntPlusBikePowerPcc> resultReceiverPower = new AntPluginPcc.IPluginAccessResultReceiver<AntPlusBikePowerPcc>() {

        @Override
        public void onResultReceived(AntPlusBikePowerPcc antPlusBikePowerPcc, RequestAccessResult requestAccessResult, DeviceState deviceState) {
            Log.i("power", "power onResultReceived " + requestAccessResult);
            Log.i("power", String.format("deviceState: %s", deviceState));

            switch (requestAccessResult) {
                case SUCCESS:
                    Data.powerPcc = antPlusBikePowerPcc;
                    subscribeToEventsPower();
                    break;
                default:
                    if (Data.powerPcc == null) {
                        requestAccess();
                    }
            }
        }
    };

    AntPluginPcc.IDeviceStateChangeReceiver deviceStateChangeReceiverPower = new AntPluginPcc.IDeviceStateChangeReceiver() {

        @Override
        public void onDeviceStateChange(final DeviceState newDeviceState) {
            if (newDeviceState == DeviceState.DEAD) {
                Log.i("state power", "newDeviceState: " + newDeviceState);
                powerPccReleaseHandle.close();
                powerPccReleaseHandle = null;
            } else if (newDeviceState == DeviceState.CLOSED) {
                requestAccess();
            }
        }
    };

    void subscribeToEventsPower() {
        Data.powerPcc.subscribeCalculatedPowerEvent(new AntPlusBikePowerPcc.ICalculatedPowerReceiver() {

            @Override
            public void onNewCalculatedPower(final long estTimestamp, final EnumSet<EventFlag> eventFlags,
                                             final AntPlusBikePowerPcc.DataSource dataSource,
                                             final BigDecimal calculatedPower) {
                Log.i("onNewCalculatedPower", "calculatedPower: " + String.valueOf(calculatedPower));
                Data.power = calculatedPower.intValue();
                Data.powerPoints.add(new PowerPoint(System.currentTimeMillis(), calculatedPower.intValue()));
            }
        });

        Data.powerPcc.subscribeCalculatedCrankCadenceEvent(new AntPlusBikePowerPcc.ICalculatedCrankCadenceReceiver() {

            @Override
            public void onNewCalculatedCrankCadence(final long estTimestamp, final EnumSet<EventFlag> eventFlags,
                                                    final AntPlusBikePowerPcc.DataSource dataSource,
                                                    final BigDecimal calculatedCrankCadence) {
                Log.i("onNewCalculatedCrankCadence", "calculatedCrankCadence: " + String.valueOf(calculatedCrankCadence));
                Data.cadence = calculatedCrankCadence.intValue();
            }
        });
    }

}
