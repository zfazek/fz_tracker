package com.ongroa.fztracker;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.provider.Settings;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.Timer;
import java.util.TimerTask;

public class MainActivity extends AppCompatActivity {

    Timer timer;
    TimerTask timerTask;

    final int PERMISSION_ID_FILE = 43;
    final int PERMISSION_ID_LOCATION = 44;

    final float MIN_SPEED_LIMIT_RIDE = 0.0f;
    final float MIN_SPEED_LIMIT_RUN = 0.0f;

    TextView speedTextView;
    TextView averageSpeedTextView;
    TextView distanceTextView;
    TextView elapsedTimeTextView;
    TextView movingTimeTextView;
    TextView accuracyTextView;
    TextView altitudeTextView;
    TextView totalAscentTextView;
    TextView bearingTextView;
    TextView counterTextView;
    TextView timeTextView;
    TextView latTextView;
    TextView lonTextView;
    TextView heartRateTextView;
    TextView power3sTextView;
    TextView power30sTextView;
    TextView cadenceTextView;
    TextView debugTextView;
    Button mButton1;
    Button mButton2;
    LinearLayout bgElement;
    Handler mGuiHandler = new Handler();

    @SuppressLint("SourceLockedOrientationActivity")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR_PORTRAIT);
        setContentView(R.layout.activity_main);
        Data.trackPoints = new ArrayList<>();
        Data.powerPoints = new ArrayList<>();
        accuracyTextView = findViewById(R.id.accuracyTextView);
        bearingTextView = findViewById(R.id.bearingTextView);
//        altitudeTextView = findViewById(R.id.altitudeTextView);
//        totalAscentTextView = findViewById(R.id.totalAscentTextView);
        speedTextView = findViewById(R.id.speedTextView);
        averageSpeedTextView = findViewById(R.id.averageSpeedTextView);
        distanceTextView = findViewById(R.id.distanceTextView);
        elapsedTimeTextView = findViewById(R.id.elapsedTimeTextView);
        movingTimeTextView = findViewById(R.id.movingTimeTextView);
        timeTextView = findViewById(R.id.timeTextView);
        latTextView = findViewById(R.id.latTextView);
        lonTextView = findViewById(R.id.lonTextView);
        heartRateTextView = findViewById(R.id.powerTextView);
        power3sTextView = findViewById(R.id.power3sTextView);
        power30sTextView = findViewById(R.id.power30sTextView);
        cadenceTextView = findViewById(R.id.cadenceTextView);
//        debugTextView = findViewById(R.id.debugTextView);
        mButton1 = findViewById(R.id.button1);
        mButton2 = findViewById(R.id.button2);
        bgElement = (LinearLayout) findViewById(R.id.container);
        timer = new Timer();
        timerTask = new TimerTask() {

            @Override
            public void run() {
                mGuiHandler.post(new Runnable() {

                    @Override
                    public void run() {
                        draw();
                        long DURATION_BETWEEN_FILE_WRITES = 60000;
                        if (System.currentTimeMillis() - Data.lastSavedTime > DURATION_BETWEEN_FILE_WRITES && Data.startTime != null) {
                            writeToFile();
                        }
                    }
                });
            }
        };
        timer.schedule(timerTask, 1000, 1000);
        init();
        startLocationClient();
        mButton1.setOnClickListener(new View.OnClickListener() {

            @SuppressLint("SetTextI18n")
            @Override
            public void onClick(View v) {
                switch (Data.state) {
                    case INIT:
                        Data.state = State.STARTED;
                        Data.sportType = SportType.RIDE;
                        Data.minSpeedLimit = MIN_SPEED_LIMIT_RIDE;
                        mButton1.setText("STOP");
                        mButton2.setEnabled(false);
                        mButton2.setVisibility(View.INVISIBLE);
                        break;
                    case STARTED:
                        Data.state = State.STOPPED;
                        mButton1.setText("RESUME");
                        mButton2.setEnabled(true);
                        mButton2.setVisibility(View.VISIBLE);
                        mButton2.setText("SAVE");
                        break;
                    case STOPPED:
                        Data.state = State.STARTED;
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
                switch (Data.state) {
                    case INIT:
                        Data.state = State.STARTED;
                        Data.sportType = SportType.RUN;
                        Data.minSpeedLimit = MIN_SPEED_LIMIT_RUN;
                        mButton1.setText("STOP");
                        mButton2.setEnabled(false);
                        mButton2.setVisibility(View.INVISIBLE);
                        break;
                    case STOPPED:
                        writeToFile();
                        init();
                        break;
                }
            }
        });
    }

    void init() {
        mButton1.setText("START RIDE");
        mButton2.setText("START RUN");
        mButton1.setEnabled(true);
        mButton1.setVisibility(View.VISIBLE);
        mButton2.setEnabled(false);
        mButton2.setVisibility(View.INVISIBLE);
        Data.init();
    }

    @SuppressLint("MissingPermission")
    void startLocationClient() {
        if (checkPermissionsLocation()) {
            if (isLocationEnabled()) {
                Log.i("startLocationClient", "isLocationEnabled = true");
                Intent intent = new Intent(this, LocUpdaterService.class);
                startService(intent);
            } else {
                Toast.makeText(this, "Turn on location", Toast.LENGTH_LONG).show();
                Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                startActivity(intent);
            }
        } else {
            requestPermissionsLocation();
        }
    }

    boolean checkPermissionsFile() {
        return ActivityCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED;
    }

    boolean checkPermissionsLocation() {
        return ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED;
    }

    void requestPermissionsFile() {
        ActivityCompat.requestPermissions(
                this,
                new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                PERMISSION_ID_FILE
        );
    }

    void requestPermissionsLocation() {
        ActivityCompat.requestPermissions(
                this,
                new String[]{Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION},
                PERMISSION_ID_LOCATION
        );
    }

    boolean isLocationEnabled() {
        LocationManager locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        return locationManager != null && locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) || locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_ID_LOCATION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startLocationClient();
            }
        } else if (requestCode == PERMISSION_ID_FILE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                writeToFile();
            }
        }
    }

    @Override
    public void onResume() {
        super.onResume();
    }

    @Override
    public void onBackPressed() {
        // TODO Auto-generated method stub

        new AlertDialog.Builder(MainActivity.this)
                .setTitle("Exit?")
                .setMessage("Do you really want to exit?")
                .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        ActivityCompat.finishAffinity(MainActivity.this);
                        finish();
                    }
                })
                .setNegativeButton(android.R.string.no, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                })
                .setIcon(android.R.drawable.ic_dialog_alert)
                .show();

    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Intent intent = new Intent(this, LocUpdaterService.class);
        stopService(intent);
    }

    void draw() {
        bgElement.setBackgroundColor(Data.bgColor);
//        altitudeTextView.setText(String.format("%.0f m", Data.mAltitude));
//        totalAscentTextView.setText(String.format("%.0f m", Data.mTotalAscent));
        distanceTextView.setText(String.format("%.2f km", Data.distance / 1000.0));
        elapsedTimeTextView.setText(String.format("%s", Util.millisecondsToHuman(Data.elapsedTime)));
        movingTimeTextView.setText(String.format("%s", Util.millisecondsToHuman(Data.movingTime)));
        timeTextView.setText(Data.now);
        averageSpeedTextView.setText(String.format("%.2f", 3.6f * Data.distance / (Data.movingTime + 1) * 1000.0f));
        accuracyTextView.setText(String.format("%.0f m", Data.accuracy));
        speedTextView.setText(String.format("%.1f", Data.speed));
        bearingTextView.setText(String.format("%.0f deg", Data.bearing));
        latTextView.setText("" + Data.latitude);
        lonTextView.setText("" + Data.longitude);
        if (Data.pwrPcc == null) {
            power3sTextView.setText("---");
            power30sTextView.setText("---");
            cadenceTextView.setText("---");
        } else {
            power3sTextView.setText("" + Data.getAveragePower(3900));
            power30sTextView.setText("" + Data.getAveragePower(30900));
            cadenceTextView.setText("" + Data.cadence);
        }
        if (Data.heartRatePcc == null) {
            heartRateTextView.setText("---");
        } else {
            heartRateTextView.setText("" + Data.heartRate);
        }
    }

    void writeToFile() {
        if (Data.trackPoints.isEmpty()) {
            return;
        }
        if (checkPermissionsFile()) {
            StringBuilder data = new StringBuilder("<?xml version=\"1.0\" encoding=\"utf-8\" standalone=\"yes\"?>\n" +
                    "<gpx version=\"1.1\" creator=\"FZ Tracker\" xmlns=\"http://www.topografix.com/GPX/1/1\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xsi:schemaLocation=\"http://www.topografix.com/GPX/1/1 http://www.topografix.com/GPX/1/1/gpx.xsd\">\n" +
                    "<trk>\n");
            data.append("  <trkseg>\n");
            for (TrackPoint trkpt : Data.trackPoints) {
                data.append(String.format("    <trkpt lat=\"%.8f\" lon=\"%.8f\">\n", trkpt.getLat(), trkpt.getLon()));
                data.append(String.format("      <time>%s</time>\n", trkpt.getTime()));
                if (trkpt.getPower() >= 0 || trkpt.getHeartRate() >= 0) {
                    data.append("      <extensions>\n");
                    if (trkpt.getPower() >= 0) {
                        data.append(String.format("      <cadence>%d</cadence><power>%d</power>\n", trkpt.getCadence(), trkpt.getPower()));
                    }
                    if (trkpt.getHeartRate() >= 0) {
                        data.append(String.format("        <gpxtpx:TrackPointExtension><gpxtpx:hr>%d</gpxtpx:hr></gpxtpx:TrackPointExtension>\n", trkpt.getHeartRate()));
                    }
                    data.append("      </extensions>\n");
                }
                data.append("    </trkpt>\n");
            }
            data.append("  </trkseg>\n" + "</trk>\n" + "</gpx>");
            final String fileName = Data.startTime + ".gpx";
            final File path = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
            final File file = new File(path, fileName);
            try {
                file.createNewFile();
                FileOutputStream fOut = new FileOutputStream(file);
                OutputStreamWriter myOutWriter = new OutputStreamWriter(fOut);
                myOutWriter.append(data.toString());
                myOutWriter.close();
                fOut.flush();
                fOut.close();
                Data.lastSavedTime = System.currentTimeMillis();
            } catch (Exception e) {
                Log.e("Exception", "File write failed: " + e.toString());
            }
        } else {
            requestPermissionsFile();
        }
    }

}
