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
import androidx.core.app.NotificationCompat;

import com.dsi.ant.plugins.antplus.pcc.AntPlusBikePowerPcc;
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
            Data.mCounter++;
            Log.i("a", String.format("counter: %d, size: %d, mLastSavedTime: %s", Data.mCounter, Data.mTrackPoints.size(), Data.mLastSavedTime));
            final Location location = locationResult.getLastLocation();
            if (location == null) {
                return;
            }
            if (Data.mPrevTime == 0) {
                Data.mPrevTime = location.getTime();
                return;
            }
            Data.mAccuracy = location.getAccuracy();
            float MIN_ACCURACY_LIMIT = 25.0f;
            if (Data.mAccuracy >= MIN_ACCURACY_LIMIT) {
                Data.bgColor = Color.RED;
            } else if (Data.mState == State.INIT || Data.mState == State.STOPPED) {
                Data.bgColor = Color.BLUE;
            } else {
                if (Data.mSportType == SportType.RIDE) {
                    Data.bgColor = Color.WHITE;
                } else {
                    Data.bgColor = Color.YELLOW;
                }
            }
            Data.mNow = mTimeFormat.format(new Date());
            final long deltaTime = location.getTime() - Data.mPrevTime;
            Data.mPrevTime = location.getTime();
            if (Data.mAccuracy < MIN_ACCURACY_LIMIT) {
                Data.mSpeed = 3.6f * location.getSpeed();
                if (Data.mState == State.STOPPED) {
                    Data.mLatitude = location.getLatitude();
                    Data.mLongitude = location.getLongitude();
                    Data.mBearing = location.getBearing();
                }
            }
            if (Data.mState == State.STARTED && Data.mLastLocation != null && Data.mAccuracy < MIN_ACCURACY_LIMIT) {
                if (Data.mSpeed >= Data.mMinSpeedLimit) {
                    Data.mLatitude = location.getLatitude();
                    Data.mLongitude = location.getLongitude();
                    Data.mBearing = location.getBearing();
                    final float dist = location.distanceTo(Data.mLastLocation);
                    Data.mDistance += dist;
                    Data.mMovingTime += deltaTime;
                    Data.mAltitude = location.getAltitude();
                    final double lastAltitude = Data.mLastLocation.getAltitude();
                    if (Data.mAltitude > lastAltitude) {
                        Data.mTotalAscent += Data.mAltitude - lastAltitude;
                    }
                } else {
                    Data.bgColor = Color.CYAN;
                }
                String mNowAsISO = mDateFormatISO.format(new Date());
                if (Data.mTrackPoints.isEmpty()) {
                    Data.mStartTime = mDateFormat.format(new Date());
                    Data.mLastSavedTime = System.currentTimeMillis();
                }
                if (Data.mLatitude > 0 && Data.mLongitude > 0) {
                    Data.mTrackPoints.add(new TrackPoint(mNowAsISO, Data.mLatitude, Data.mLongitude));
                    if (Data.pwrPcc != null) {
                        Data.mTrackPoints.get(Data.mTrackPoints.size() - 1).setCadenceAndPower(Data.mCadence, Data.mPower);
                    }
                }
                Data.mElapsedTime += deltaTime;
            }
            if (Data.mAccuracy < MIN_ACCURACY_LIMIT) {
                Data.mLastLocation = location;
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
                mResultReceiver,
                mDeviceStateChangeReceiver);
    }

    AntPluginPcc.IPluginAccessResultReceiver<AntPlusBikePowerPcc> mResultReceiver = new AntPluginPcc.IPluginAccessResultReceiver<AntPlusBikePowerPcc>() {

        @Override
        public void onResultReceived(AntPlusBikePowerPcc antPlusBikePowerPcc, RequestAccessResult requestAccessResult, DeviceState deviceState) {
            switch (requestAccessResult) {
                case SUCCESS:
                    Log.i("result", "SUCCESS");
                    Data.pwrPcc = antPlusBikePowerPcc;
                    subscribeToEvents();
                    break;
                case SEARCH_TIMEOUT:
                    Log.i("result", "SEARCH_TIMEOUT");
                    pwrReleaseHandle = AntPlusBikePowerPcc.requestAccess(getApplicationContext(),
                            16514,
                            0,
                            mResultReceiver,
                            mDeviceStateChangeReceiver);
                    break;
            }
        }
    };

    AntPluginPcc.IDeviceStateChangeReceiver mDeviceStateChangeReceiver = new AntPluginPcc.IDeviceStateChangeReceiver() {

        @Override
        public void onDeviceStateChange(final DeviceState newDeviceState) {
            if (newDeviceState == DeviceState.DEAD) {
                Log.i("state", "newDeviceState: " + newDeviceState);
                pwrReleaseHandle = null;
            }
        }
    };

    void subscribeToEvents() {
        Data.pwrPcc.subscribeCalculatedPowerEvent(new AntPlusBikePowerPcc.ICalculatedPowerReceiver() {
            @Override
            public void onNewCalculatedPower(final long estTimestamp, final EnumSet<EventFlag> eventFlags,
                                             final AntPlusBikePowerPcc.DataSource dataSource,
                                             final BigDecimal calculatedPower) {
                Log.i("power", "calculatedPower: " + String.valueOf(calculatedPower));
                Data.mPower = calculatedPower.longValue();
            }
        });

        Data.pwrPcc.subscribeCalculatedCrankCadenceEvent(new AntPlusBikePowerPcc.ICalculatedCrankCadenceReceiver() {

            @Override
            public void onNewCalculatedCrankCadence(final long estTimestamp, final EnumSet<EventFlag> eventFlags,
                                                    final AntPlusBikePowerPcc.DataSource dataSource,
                                                    final BigDecimal calculatedCrankCadence) {
                Log.i("cadence", "calculatedCrankCadence: " + String.valueOf(calculatedCrankCadence));
                Data.mCadence = calculatedCrankCadence.longValue();
            }
        });
    }
}
