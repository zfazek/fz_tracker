package com.ongroa.fztracker;

import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.graphics.Color;
import android.location.Location;
import android.os.IBinder;
import android.util.Log;

import androidx.annotation.Nullable;

import com.dsi.ant.plugins.antplus.pcc.AntPlusBikePowerPcc;
import com.dsi.ant.plugins.antplus.pcc.AntPlusHeartRatePcc;
import com.dsi.ant.plugins.antplus.pcc.defines.DeviceState;
import com.dsi.ant.plugins.antplus.pcc.defines.EventFlag;
import com.dsi.ant.plugins.antplus.pcc.defines.RequestAccessResult;
import com.dsi.ant.plugins.antplus.pccbase.AntPluginPcc;
import com.dsi.ant.plugins.antplus.pccbase.PccReleaseHandle;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;

import java.math.BigDecimal;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.EnumSet;
import java.util.TimeZone;

public class LocUpdaterService extends Service {
    FusedLocationProviderClient mFusedLocationClient;
    PccReleaseHandle<AntPlusBikePowerPcc> pwrReleaseHandle = null;
    PccReleaseHandle<AntPlusHeartRatePcc> heartRatePccPccReleaseHandle = null;

    DateFormat mDateFormat;
    DateFormat mTimeFormat;
    DateFormat mDateFormatISO;

    @Override
    public void onCreate() {
        mTimeFormat = new SimpleDateFormat("HH:mm:ss");
        mDateFormat = new SimpleDateFormat("yyyy-MM-dd' 'HH:mm:ss");
        mDateFormatISO = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");
        mDateFormatISO.setTimeZone(TimeZone.getTimeZone("UTC"));
        Log.i("onCreate", "onCreate");
        Intent notificationIntent = new Intent(this, MainActivity.class);

        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0,
                notificationIntent, 0);
        NotificationManager notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        CharSequence name = getString(R.string.app_name);
        NotificationChannel mChannel = new NotificationChannel("CHANNEL_ID", name, NotificationManager.IMPORTANCE_LOW);
        if (notificationManager != null) {
            notificationManager.createNotificationChannel(mChannel);
        }
        Notification notification = new Notification.Builder(this, "CHANNEL_ID")
                .setContentTitle("FZ Tracker")
                .setContentText("location")
                .setContentIntent(pendingIntent).build();
        startForeground(9876123, notification);

        requestNewLocationData();
        resetPcc();
    }

    @Override
    public void onDestroy() {
        mFusedLocationClient.removeLocationUpdates(locationCallback);
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @SuppressLint("MissingPermission")
    void requestNewLocationData() {
        LocationRequest locationRequest = new LocationRequest();
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        locationRequest.setInterval(1000);
        locationRequest.setFastestInterval(1000);

        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        mFusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, null);
    }

    public LocationCallback locationCallback = new LocationCallback() {
        @Override
        public void onLocationResult(LocationResult locationResult) {
            Data.counter++;
            if (Data.trackPoints != null) {
                Log.i("a", String.format("counter: %d, size: %d, mLastSavedTime: %s", Data.counter, Data.trackPoints.size(), Data.lastSavedTime));
            }
            final Location location = locationResult.getLastLocation();
            if (location == null) {
                return;
            }
            if (Data.prevTime == 0) {
                Data.prevTime = location.getTime();
                return;
            }
            Data.accuracy = location.getAccuracy();
            float MIN_ACCURACY_LIMIT = 25.0f;
            if (Data.accuracy >= MIN_ACCURACY_LIMIT) {
                Data.bgColor = Color.RED;
            } else if (Data.state == State.INIT || Data.state == State.STOPPED) {
                Data.bgColor = Color.BLUE;
            } else {
                if (Data.sportType == SportType.RIDE) {
                    Data.bgColor = Color.WHITE;
                } else {
                    Data.bgColor = Color.YELLOW;
                }
            }
            Data.now = mTimeFormat.format(new Date());
            final long deltaTime = location.getTime() - Data.prevTime;
            Data.prevTime = location.getTime();
            if (Data.accuracy < MIN_ACCURACY_LIMIT) {
                Data.speed = 3.6f * location.getSpeed();
                Data.latitude = location.getLatitude();
                Data.longitude = location.getLongitude();
                Data.bearing = location.getBearing();
                Data.lastLocation = location;
                if (Data.state == State.STARTED && Data.lastLocation != null) {
                    if (Data.speed >= Data.minSpeedLimit) {
                        final float dist = location.distanceTo(Data.lastLocation);
                        Data.distance += dist;
                        Data.movingTime += deltaTime;
                        Data.altitude = location.getAltitude();
                        final double lastAltitude = Data.lastLocation.getAltitude();
                        if (Data.altitude > lastAltitude) {
                            Data.totalAscent += Data.altitude - lastAltitude;
                        }
                    } else {
                        Data.bgColor = Color.CYAN;
                    }
                    String mNowAsISO = mDateFormatISO.format(new Date());
                    if (Data.trackPoints.isEmpty()) {
                        Data.startTime = mDateFormat.format(new Date());
                        Data.lastSavedTime = System.currentTimeMillis();
                    }
                    if (Data.latitude > 0 && Data.longitude > 0) {
                        Data.trackPoints.add(new TrackPoint(System.currentTimeMillis(), mNowAsISO, Data.latitude, Data.longitude));
                        if (Data.pwrPcc != null) {
                            Data.trackPoints.get(Data.trackPoints.size() - 1).setCadenceAndPower(Data.cadence, Data.power);
                        } else {
                            Data.trackPoints.get(Data.trackPoints.size() - 1).setCadenceAndPower(-1, -1);
                        }
                        if (Data.heartRatePcc != null) {
                            Data.trackPoints.get(Data.trackPoints.size() - 1).setHeartRate(Data.heartRate);
                        } else {
                            Data.trackPoints.get(Data.trackPoints.size() - 1).setHeartRate(-1);
                        }
                    }
                    Data.elapsedTime += deltaTime;
                }
            }
        }
    };

    void resetPcc() {
        if (pwrReleaseHandle != null) {
            pwrReleaseHandle.close();
        }
        pwrReleaseHandle = AntPlusBikePowerPcc.requestAccess(getApplicationContext(),
                16514,
                0,
                mResultReceiverPower,
                mDeviceStateChangeReceiverPower);

        if (heartRatePccPccReleaseHandle != null) {
            heartRatePccPccReleaseHandle.close();
        }
        heartRatePccPccReleaseHandle = AntPlusHeartRatePcc.requestAccess(getApplicationContext(), 661,
                0, mResultReceiverHeartRate, mDeviceStateChangeReceiverHeartRate);
    }

    AntPluginPcc.IPluginAccessResultReceiver<AntPlusBikePowerPcc> mResultReceiverPower = new AntPluginPcc.IPluginAccessResultReceiver<AntPlusBikePowerPcc>() {

        @Override
        public void onResultReceived(AntPlusBikePowerPcc antPlusBikePowerPcc, RequestAccessResult requestAccessResult, DeviceState deviceState) {
            Log.i("power", "power onResultReceived " + requestAccessResult);

            switch (requestAccessResult) {
                case SUCCESS:
                    Log.i("result power", "SUCCESS");
                    Data.pwrPcc = antPlusBikePowerPcc;
                    subscribeToEventsPower();
                    break;
                case SEARCH_TIMEOUT:
                    Log.i("result power", "SEARCH_TIMEOUT");
                    pwrReleaseHandle = AntPlusBikePowerPcc.requestAccess(getApplicationContext(),
                            16514,
                            0,
                            mResultReceiverPower,
                            mDeviceStateChangeReceiverPower);
                    break;
            }
        }
    };

    AntPluginPcc.IPluginAccessResultReceiver<AntPlusHeartRatePcc> mResultReceiverHeartRate = new AntPluginPcc.IPluginAccessResultReceiver<AntPlusHeartRatePcc>() {

        @Override
        public void onResultReceived(AntPlusHeartRatePcc antPlusHeartRatePcc, RequestAccessResult requestAccessResult, DeviceState deviceState) {
            Log.i("heart", "heart rate onResultReceived " + requestAccessResult);

            switch (requestAccessResult) {
                case SUCCESS:
                    Log.i("result heart rate", "SUCCESS");
                    Data.heartRatePcc = antPlusHeartRatePcc;
                    subscribeToEventsHeartRate();
                    break;
                case SEARCH_TIMEOUT:
                    Log.i("result heart rate", "SEARCH_TIMEOUT");
                    heartRatePccPccReleaseHandle = AntPlusHeartRatePcc.requestAccess(getApplicationContext(),
                            661,
                            0,
                            mResultReceiverHeartRate,
                            mDeviceStateChangeReceiverHeartRate);
                    break;
            }
        }
    };

    AntPluginPcc.IDeviceStateChangeReceiver mDeviceStateChangeReceiverPower = new AntPluginPcc.IDeviceStateChangeReceiver() {

        @Override
        public void onDeviceStateChange(final DeviceState newDeviceState) {
            if (newDeviceState == DeviceState.DEAD) {
                Log.i("state power", "newDeviceState: " + newDeviceState);
                pwrReleaseHandle = null;
            }
        }
    };

    AntPluginPcc.IDeviceStateChangeReceiver mDeviceStateChangeReceiverHeartRate = new AntPluginPcc.IDeviceStateChangeReceiver() {

        @Override
        public void onDeviceStateChange(final DeviceState newDeviceState) {
            if (newDeviceState == DeviceState.DEAD) {
                Log.i("state heart rate", "newDeviceState: " + newDeviceState);
                heartRatePccPccReleaseHandle = null;
            }
        }
    };

    void subscribeToEventsPower() {
        Data.pwrPcc.subscribeCalculatedPowerEvent(new AntPlusBikePowerPcc.ICalculatedPowerReceiver() {

            @Override
            public void onNewCalculatedPower(final long estTimestamp, final EnumSet<EventFlag> eventFlags,
                                             final AntPlusBikePowerPcc.DataSource dataSource,
                                             final BigDecimal calculatedPower) {
                Log.i("onNewCalculatedPower", "calculatedPower: " + String.valueOf(calculatedPower));
                Data.power = calculatedPower.intValue();
                Data.powerPoints.add(new PowerPoint(System.currentTimeMillis(), calculatedPower.intValue()));
            }
        });

        Data.pwrPcc.subscribeCalculatedCrankCadenceEvent(new AntPlusBikePowerPcc.ICalculatedCrankCadenceReceiver() {

            @Override
            public void onNewCalculatedCrankCadence(final long estTimestamp, final EnumSet<EventFlag> eventFlags,
                                                    final AntPlusBikePowerPcc.DataSource dataSource,
                                                    final BigDecimal calculatedCrankCadence) {
                Log.i("onNewCalculatedCrankCadence", "calculatedCrankCadence: " + String.valueOf(calculatedCrankCadence));
                Data.cadence = calculatedCrankCadence.intValue();
            }
        });
    }

    void subscribeToEventsHeartRate() {
        Data.heartRatePcc.subscribeHeartRateDataEvent(new AntPlusHeartRatePcc.IHeartRateDataReceiver() {
            @Override
            public void onNewHeartRateData(long estTimestamp, java.util.EnumSet<EventFlag> eventFlags, int computedHeartRate, long heartBeatCount, java.math.BigDecimal heartBeatEventTime, AntPlusHeartRatePcc.DataState dataState) {
                if (dataState == AntPlusHeartRatePcc.DataState.LIVE_DATA) {
                    Log.i("onNewHeartRateData", "heart rate: " + String.valueOf(computedHeartRate));
                    Data.heartRate = computedHeartRate;
                }
            }
        });
    }
}