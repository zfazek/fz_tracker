package com.ongroa.fztracker;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Environment;
import android.os.Looper;
import android.provider.Settings;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.TimeZone;

public class MainActivity extends AppCompatActivity {

    private final int PERMISSION_ID_FILE = 43;
    private final int PERMISSION_ID_LOCATION = 44;
    private final float MIN_SPEED_LIMIT = 2.0f;
    private final float MIN_ACCURACY_LIMIT = 20.0f;
    private State mState;
    private DateFormat mTimeFormat;
    private DateFormat mDateFormat;
    private DateFormat mDateFormatISO;
    private float mSpeed;
    private float mAccuracy;
    private float mBearing;
    private long mCounter;
    private float mDistance;
    private long mElapsedTime;
    private long mLastTime;
    private long mMovingTime;
    private double mLatitude;
    private double mLongitude;
    private double mTotalAscent;
    private double mAltitude;
    private String mNow;
    private String mDateNow;
    private String mNowAsISO;
    private ArrayList<TrackPoint> mTrackPoints;
    private FusedLocationProviderClient mFusedLocationClient;
    private Location mLastLocation;
    private TextView speedTextView;
    private TextView averageSpeedTextView;
    private TextView distanceTextView;
    private TextView elapsedTimeTextView;
    private TextView movingTimeTextView;
    private TextView accuracyTextView;
    private TextView altitudeTextView;
    private TextView totalAscentTextView;
    private TextView bearingTextView;
    private TextView counterTextView;
    private TextView timeTextView;
    private TextView latTextView;
    private TextView lonTextView;
    private Button mButton1;
    private Button mButton2;

    private final LocationCallback locationCallback = new LocationCallback() {
        @Override
        public void onLocationResult(LocationResult locationResult) {
            final Location location = locationResult.getLastLocation();
            if (mLastTime == 0) {
                mLastTime = location.getTime();
            }
            mSpeed = 3.6f * location.getSpeed();
            mAccuracy = location.getAccuracy();
            mBearing = location.getBearing();
            mNow = mTimeFormat.format(new Date());
            final long deltaTime = location.getTime() - mLastTime;
            mLastTime = location.getTime();
            if (mState == State.STARTED && mLastLocation != null && mAccuracy < MIN_ACCURACY_LIMIT) {
                if (mSpeed >= MIN_SPEED_LIMIT) {
                    mLatitude = location.getLatitude();
                    mLongitude = location.getLongitude();
                    final float dist = location.distanceTo(mLastLocation);
                    mDistance += dist;
                    mMovingTime += deltaTime;
                    mAltitude = location.getAltitude();
                    final double lastAltitude = mLastLocation.getAltitude();
                    if (mAltitude > lastAltitude) {
                        mTotalAscent += mAltitude - lastAltitude;
                    }
                }
                mCounter++;
                mNowAsISO = mDateFormatISO.format(new Date());
                mTrackPoints.add(new TrackPoint(mNowAsISO, mLatitude, mLongitude));
                mElapsedTime += deltaTime;
                mLastLocation = location;
            }
            if (mState == State.STOPPED) {
                mLastLocation = null;
            }
            if (mLastLocation == null && mAccuracy < MIN_ACCURACY_LIMIT) {
                mLastLocation = location;
            }
            draw();
        }
    };

    private void init() {
        mState = State.STOPPED;
        mButton1.setText("START");
        mButton2.setEnabled(false);
        mButton2.setVisibility(View.INVISIBLE);
        mCounter = 0;
        mTrackPoints.clear();
        mDistance = 0;
        mMovingTime = 0;
        mElapsedTime = 0;
        mTotalAscent = 0;
    }

    private void draw() {
        altitudeTextView.setText(String.format("Alt.: %.0f m", mAltitude));
        totalAscentTextView.setText(String.format("Tot. Ascent: %.0f m", mTotalAscent));
        distanceTextView.setText(String.format("Distance: %.2f km", mDistance / 1000.0));
        elapsedTimeTextView.setText(String.format("Elapsed Time: %.0f s", mElapsedTime / 1000.0));
        movingTimeTextView.setText(String.format("Moving Time: %.0f s", mMovingTime / 1000.0));
        timeTextView.setText(mNow);
        averageSpeedTextView.setText(String.format("Average Speed: %.1f km/h", 3.6f * mDistance / (mMovingTime + 1) * 1000.0f));
        accuracyTextView.setText(String.format("%.0f m", mAccuracy));
        speedTextView.setText(String.format("Speed: %.1f km/h", mSpeed));
        bearingTextView.setText(String.format("%.0f deg", mBearing));
        latTextView.setText("" + mLatitude);
        lonTextView.setText("" + mLongitude);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mTimeFormat = new SimpleDateFormat("HH:mm:ss");
        mDateFormat = new SimpleDateFormat("yyyy-MM-dd' 'HH:mm:ss");
        mDateFormatISO = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");
        mDateFormatISO.setTimeZone(TimeZone.getTimeZone("UTC"));
        mTrackPoints = new ArrayList<>();
        accuracyTextView = findViewById(R.id.accuracyTextView);
        bearingTextView = findViewById(R.id.bearingTextView);
        altitudeTextView = findViewById(R.id.altitudeTextView);
        totalAscentTextView = findViewById(R.id.totalAscentTextView);
        speedTextView = findViewById(R.id.speedTextView);
        averageSpeedTextView = findViewById(R.id.averageSpeedTextView);
        distanceTextView = findViewById(R.id.distanceTextView);
        elapsedTimeTextView = findViewById(R.id.elapsedTimeTextView);
        movingTimeTextView = findViewById(R.id.movingTimeTextView);
        timeTextView = findViewById(R.id.timeTextView);
        latTextView = findViewById(R.id.latTextView);
        lonTextView = findViewById(R.id.lonTextView);
        mButton1 = findViewById(R.id.button1);
        mButton2 = findViewById(R.id.button2);
        init();
        mButton1.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                switch (mState) {
                    case STARTED:
                        mState = State.STOPPED;
                        mButton1.setText("RESUME");
                        mButton2.setEnabled(true);
                        mButton2.setVisibility(View.VISIBLE);
                        mButton2.setText("SAVE");
                        break;
                    case STOPPED:
                        mState = State.STARTED;
                        mButton1.setText("STOP");
                        mButton2.setEnabled(false);
                        mButton2.setVisibility(View.INVISIBLE);
                        break;
                }
            }
        });
        mButton2.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                switch (mState) {
                    case STOPPED:
                        writeToFile();
                        init();
                        break;
                }
            }
        });
        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        getLastLocation();
    }

    private void writeToFile() {
        if (!mTrackPoints.isEmpty()) {
            if (checkPermissionsFile()) {
                StringBuilder data = new StringBuilder("<?xml version=\"1.0\" encoding=\"utf-8\" standalone=\"yes\"?>\n" +
                        "<gpx version=\"1.1\" creator=\"FZ Tracker\" xmlns=\"http://www.topografix.com/GPX/1/1\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xsi:schemaLocation=\"http://www.topografix.com/GPX/1/1 http://www.topografix.com/GPX/1/1/gpx.xsd\">\n" +
                        "<trk>\n" +
                        "<name>Ride</name>\n" +
                        "<type>1</type>\n" +
                        "  <trkseg>\n");
                for (TrackPoint trkpt : mTrackPoints) {
                    data.append(String.format("    <trkpt lat=\"%.8f\" lon=\"%.8f\">\n", trkpt.getLat(), trkpt.getLon()));
                    data.append(String.format("      <time>%s</time>\n", trkpt.getTime()));
                    data.append("    </trkpt>");
                }
                data.append("  </trkseg>\n" + "</trk>\n" + "</gpx>");
                mDateNow = mDateFormat.format(new Date());
                final String fileName = mDateNow + ".gpx";
                final File path = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
                final File file = new File(path, fileName);
                try {
                    if (file.createNewFile()) {
                        FileOutputStream fOut = new FileOutputStream(file);
                        OutputStreamWriter myOutWriter = new OutputStreamWriter(fOut);
                        myOutWriter.append(data.toString());
                        myOutWriter.close();
                        fOut.flush();
                        fOut.close();
                    }
                } catch (Exception e) {
                    Log.e("Exception", "File write failed: " + e.toString());
                }
            } else {
                requestPermissionsFile();
            }
        }
    }

    @SuppressLint("MissingPermission")
    private void getLastLocation() {
        if (checkPermissionsLocation()) {
            if (isLocationEnabled()) {
                requestNewLocationData();
            } else {
                Toast.makeText(this, "Turn on location", Toast.LENGTH_LONG).show();
                Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                startActivity(intent);
            }
        } else {
            requestPermissionsLocation();
        }
    }

    @SuppressLint("MissingPermission")
    private void requestNewLocationData() {
        LocationRequest locationRequest = new LocationRequest();
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        locationRequest.setInterval(1000);
        locationRequest.setFastestInterval(1000);

        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        mFusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, Looper.myLooper());
    }

    private boolean checkPermissionsFile() {
        return ActivityCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED;
    }

    private boolean checkPermissionsLocation() {
        return ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED;
    }

    private void requestPermissionsFile() {
        ActivityCompat.requestPermissions(
                this,
                new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                PERMISSION_ID_FILE
        );
    }

    private void requestPermissionsLocation() {
        ActivityCompat.requestPermissions(
                this,
                new String[]{Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION},
                PERMISSION_ID_LOCATION
        );
    }

    private boolean isLocationEnabled() {
        LocationManager locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        return locationManager != null && locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) || locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_ID_LOCATION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                getLastLocation();
            }
        } else if (requestCode == PERMISSION_ID_FILE) {
            writeToFile();
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        getLastLocation();
    }

    @Override
    public void onBackPressed() {
        moveTaskToBack(true);
    }
}