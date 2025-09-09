package com.virtualvet.model;


public class VetLocation {
    private String name;
    private String address;
    private String phone;
    private double latitude;
    private double longitude;
    private double distanceKm;
    private boolean isEmergencyClinic;
    private String operatingHours;
    private double rating;
    
    public VetLocation() {}
    
    public VetLocation(String name, String address, String phone, double latitude, double longitude) {
        this.name = name;
        this.address = address;
        this.phone = phone;
        this.latitude = latitude;
        this.longitude = longitude;
    }

    public VetLocation(VetLocation other) {
        this.name = other.name;
        this.address = other.address;
        this.phone = other.phone;
        this.latitude = other.latitude;
        this.longitude = other.longitude;
        this.distanceKm = other.distanceKm;
        this.isEmergencyClinic = other.isEmergencyClinic;
        this.operatingHours = other.operatingHours;
        this.rating = other.rating;
    }
    
    // Getters and Setters
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    
    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }
    
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
    
    public double getLatitude() { return latitude; }
    public void setLatitude(double latitude) { this.latitude = latitude; }
    
    public double getLongitude() { return longitude; }
    public void setLongitude(double longitude) { this.longitude = longitude; }
    
    public double getDistanceKm() { return distanceKm; }
    public void setDistanceKm(double distanceKm) { this.distanceKm = distanceKm; }
    
    public boolean isEmergencyClinic() { return isEmergencyClinic; }
    public void setEmergencyClinic(boolean emergencyClinic) { isEmergencyClinic = emergencyClinic; }
    
    public String getOperatingHours() { return operatingHours; }
    public void setOperatingHours(String operatingHours) { this.operatingHours = operatingHours; }
    
    public double getRating() { return rating; }
    public void setRating(double rating) { this.rating = rating; }
}