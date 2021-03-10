package com.ongroa.fztracker;

import android.os.Environment;
import android.util.Log;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

public class Util {
    public static String millisecondsToHuman(final long seconds) {
        SimpleDateFormat dateFormat;
        if (seconds <= 3600000) {
            dateFormat = new SimpleDateFormat("mm:ss");
        } else {
            dateFormat = new SimpleDateFormat("HH:mm:ss");
        }
        dateFormat.setTimeZone(TimeZone.getTimeZone("GMT"));
        Date time = new Date(seconds);
        return dateFormat.format(time);
    }

}
