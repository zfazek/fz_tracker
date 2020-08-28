package com.ongroa.fztracker;

class TrackPoint {
    private final String time;
    private final double lat;
    private final double lon;
    private long cadence;
    private long power;

    public TrackPoint(String time, double lat, double lon) {
        this.time = time;
        this.lat = lat;
        this.lon = lon;
        this.cadence = 0;
        this.power = 0;
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

    public long getCadence() {
        return cadence;
    }

    public long getPower() {
        return power;
    }

    public void setCadenceAndPower(long cadence, long power) {
        this.cadence = cadence;
        this.power = power;
    }
}
