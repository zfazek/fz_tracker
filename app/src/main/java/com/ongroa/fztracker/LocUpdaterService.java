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

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

public class LocUpdaterService extends Service {
    FusedLocationProviderClient mFusedLocationClient;

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
                Log.i("location", String.format("counter: %d, size: %d, mLastSavedTime: %s", Data.counter, Data.trackPoints.size(), Data.lastSavedTime));
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
                        if (Data.powerPcc != null) {
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
                Data.lastLocation = location;
            }
        }
    };

}