package com.ongroa.fztracker;

class TrackPoint {
    private final String time;
    private final double lat;
    private final double lon;

    public TrackPoint(String time, double lat, double lon) {
        this.time = time;
        this.lat = lat;
        this.lon = lon;
    }

    public String getTime() {
        return time;
    }

    public double getLat() {
        return lat;
    }

    public double getLon() {
        return lon;
    }
}
