package com.ongroa.fztracker;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;

import androidx.annotation.Nullable;

public class LocationUpdatesService extends Service {
    private static final int NOTIFICATION_ID = 2912884;

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {

    }
}
