package com.ongroa.fztracker;

class TrackPoint {
    final long millis;
    private final String time;
    private final double lat;
    private final double lon;
    private int cadence;
    private int power;
    private int heartRate;

    public TrackPoint(long millis, String time, double lat, double lon) {
        this.millis = millis;
        this.time = time;
        this.lat = lat;
        this.lon = lon;
        this.cadence = 0;
        this.power = 0;
        this.heartRate = 0;
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

    public int getCadence() {
        return cadence;
    }

    public int getPower() {
        return power;
    }

    public int getHeartRate() {
        return heartRate;
    }

    public void setCadenceAndPower(int cadence, int power) {
        this.cadence = cadence;
        this.power = power;
    }

    public void setHeartRate(int heartRate) {
        this.heartRate = heartRate;
    }
}
