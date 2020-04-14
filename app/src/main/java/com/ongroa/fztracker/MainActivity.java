package com.ongroa.fztracker;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Looper;
import android.provider.Settings;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.RequiresApi;
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

    int PERMISSION_ID_FILE = 43;
    int PERMISSION_ID_LOCATION = 44;
    long counter;
    ArrayList<TrackPoint> trackPoints;
    FusedLocationProviderClient mFusedLocationClient;
    TextView counterTextView, timeTextView, latTextView, lonTextView;
    Button saveButton;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        counter = 0;
        trackPoints = new ArrayList<>();
        counterTextView = findViewById(R.id.counterTextView);
        timeTextView = findViewById(R.id.timeTextView);
        latTextView = findViewById(R.id.latTextView);
        lonTextView = findViewById(R.id.lonTextView);
        saveButton = findViewById(R.id.saveButton);
        saveButton.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                writeToFile();
                counter = 0;
                trackPoints.clear();
            }
        });
        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        getLastLocation();
    }

    private void writeToFile() {
        if (!trackPoints.isEmpty()) {
            if (checkPermissionsFile()) {
                String data = "<?xml version=\"1.0\" encoding=\"utf-8\" standalone=\"yes\"?>\n" +
                        "<gpx version=\"1.1\" creator=\"FZ Tracker\" xmlns=\"http://www.topografix.com/GPX/1/1\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xsi:schemaLocation=\"http://www.topografix.com/GPX/1/1 http://www.topografix.com/GPX/1/1/gpx.xsd\">\n" +
                        "<trk>\n" +
                        "<name>Morning Ride</name>\n" +
                        "<type>1</type>\n" +
                        "  <trkseg>\n";
                for (TrackPoint trkpt : trackPoints) {
                    data += String.format("    <trkpt lat=\"%.8f\" lon=\"%.8f\">\n", trkpt.getLat(), trkpt.getLon());
                    data += String.format("      <time>%s</time>\n", trkpt.getTime());
                    data += "    </trkpt>";
                }
                data += "  </trkseg>\n" +
                        "</trk>\n" +
                        "</gpx>";
                final String fileName = trackPoints.get(0).getTime() + ".gpx";
                final File path = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
                final File file = new File(path, fileName);
                try {
                    file.createNewFile();
                    FileOutputStream fOut = new FileOutputStream(file);
                    OutputStreamWriter myOutWriter = new OutputStreamWriter(fOut);
                    myOutWriter.append(data);
                    myOutWriter.close();
                    fOut.flush();
                    fOut.close();
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

    private LocationCallback locationCallback = new LocationCallback() {
        @Override
        public void onLocationResult(LocationResult locationResult) {
            counter++;
            Location location = locationResult.getLastLocation();
            final double lan = location.getLatitude();
            final double lon = location.getLongitude();
            counterTextView.setText("Counter: " + counter);
            DateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");
            df.setTimeZone(TimeZone.getTimeZone("UTC"));
            final String nowAsISO = df.format(new Date());
            timeTextView.setText(nowAsISO);
            latTextView.setText("Latitude: " + lan);
            lonTextView.setText("Longitude: " + lon);
            trackPoints.add(new TrackPoint(nowAsISO, lan, lon));
        }
    };

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
        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) || locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_ID_LOCATION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                getLastLocation();
            } else if (requestCode == PERMISSION_ID_FILE) {
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    writeToFile();
                }
            }
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