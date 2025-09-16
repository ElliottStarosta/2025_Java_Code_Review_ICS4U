package com.virtualvet.model;


/**
 * Model class representing a veterinary clinic or hospital location in the Virtual Vet application.
 * 
 * This class encapsulates comprehensive information about veterinary facilities including
 * contact details, geographical coordinates, operating hours, ratings, and emergency
 * service availability. It is used for location-based searches, emergency referrals,
 * and providing pet owners with nearby veterinary resources.
 * 
 * The class includes multiple constructors for different use cases, including a copy
 * constructor for creating location instances from existing data. It provides
 * complete geographical and operational information necessary for veterinary
 * location services and emergency assistance features.
 * 
 * @author Elliott Starosta
 * @version 1.0
 * @since 2025
 */
public class VetLocation {
    /**
     * Name of the veterinary clinic or hospital.
     * The official business name as it appears to the public.
     */
    private String name;
    
    /**
     * Complete street address of the veterinary facility.
     * Includes street number, street name, city, state, and postal code
     * for accurate location identification and navigation.
     */
    private String address;
    
    /**
     * Primary contact phone number for the veterinary facility.
     * Used for emergency contact and appointment scheduling.
     */
    private String phone;
    
    /**
     * Latitude coordinate of the veterinary facility location.
     * Used for geographical calculations, distance measurements,
     * and mapping applications.
     */
    private double latitude;
    
    /**
     * Longitude coordinate of the veterinary facility location.
     * Used for geographical calculations, distance measurements,
     * and mapping applications.
     */
    private double longitude;
    
    /**
     * Distance from the user's location to this veterinary facility in kilometers.
     * Calculated based on geographical coordinates and used for
     * proximity-based sorting and recommendations.
     */
    private double distanceKm;
    
    /**
     * Flag indicating whether this facility provides emergency veterinary services.
     * Determines if the clinic is available for urgent or emergency situations
     * outside of normal operating hours.
     */
    private boolean isEmergencyClinic;
    
    /**
     * Operating hours of the veterinary facility.
     * Describes when the clinic is open for appointments and consultations,
     * typically including days of the week and time ranges.
     */
    private String operatingHours;
    
    /**
     * Average rating of the veterinary facility (typically 0.0 to 5.0).
     * Represents customer satisfaction and service quality based on
     * reviews and feedback from pet owners.
     */
    private double rating;
    
    /**
     * Default constructor that creates an empty VetLocation instance.
     * Initializes a new location without any preset values.
     */
    public VetLocation() {}
    
    /**
     * Constructor that creates a VetLocation with essential contact and location information.
     * 
     * @param name the name of the veterinary facility
     * @param address the complete street address
     * @param phone the primary contact phone number
     * @param latitude the latitude coordinate of the location
     * @param longitude the longitude coordinate of the location
     */
    public VetLocation(String name, String address, String phone, double latitude, double longitude) {
        this.name = name;
        this.address = address;
        this.phone = phone;
        this.latitude = latitude;
        this.longitude = longitude;
    }

    /**
     * Copy constructor that creates a VetLocation from an existing VetLocation instance.
     * 
     * @param other the VetLocation instance to copy all properties from
     */
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
    
    /**
     * Gets the name of the veterinary facility.
     * 
     * @return the facility name string
     */
    public String getName() { return name; }
    
    /**
     * Sets the name of the veterinary facility.
     * 
     * @param name the facility name to set
     */
    public void setName(String name) { this.name = name; }
    
    /**
     * Gets the complete street address of the veterinary facility.
     * 
     * @return the address string
     */
    public String getAddress() { return address; }
    
    /**
     * Sets the complete street address of the veterinary facility.
     * 
     * @param address the address string to set
     */
    public void setAddress(String address) { this.address = address; }
    
    /**
     * Gets the primary contact phone number for the veterinary facility.
     * 
     * @return the phone number string
     */
    public String getPhone() { return phone; }
    
    /**
     * Sets the primary contact phone number for the veterinary facility.
     * 
     * @param phone the phone number string to set
     */
    public void setPhone(String phone) { this.phone = phone; }
    
    /**
     * Gets the latitude coordinate of the veterinary facility location.
     * 
     * @return the latitude coordinate as a double
     */
    public double getLatitude() { return latitude; }
    
    /**
     * Sets the latitude coordinate of the veterinary facility location.
     * 
     * @param latitude the latitude coordinate to set
     */
    public void setLatitude(double latitude) { this.latitude = latitude; }
    
    /**
     * Gets the longitude coordinate of the veterinary facility location.
     * 
     * @return the longitude coordinate as a double
     */
    public double getLongitude() { return longitude; }
    
    /**
     * Sets the longitude coordinate of the veterinary facility location.
     * 
     * @param longitude the longitude coordinate to set
     */
    public void setLongitude(double longitude) { this.longitude = longitude; }
    
    /**
     * Gets the distance from the user's location to this veterinary facility in kilometers.
     * 
     * @return the distance in kilometers as a double
     */
    public double getDistanceKm() { return distanceKm; }
    
    /**
     * Sets the distance from the user's location to this veterinary facility in kilometers.
     * 
     * @param distanceKm the distance in kilometers to set
     */
    public void setDistanceKm(double distanceKm) { this.distanceKm = distanceKm; }
    
    /**
     * Checks if this veterinary facility provides emergency services.
     * 
     * @return true if this is an emergency clinic, false otherwise
     */
    public boolean isEmergencyClinic() { return isEmergencyClinic; }
    
    /**
     * Sets whether this veterinary facility provides emergency services.
     * 
     * @param emergencyClinic true if this is an emergency clinic, false otherwise
     */
    public void setEmergencyClinic(boolean emergencyClinic) { isEmergencyClinic = emergencyClinic; }
    
    /**
     * Gets the operating hours of the veterinary facility.
     * 
     * @return the operating hours description string
     */
    public String getOperatingHours() { return operatingHours; }
    
    /**
     * Sets the operating hours of the veterinary facility.
     * 
     * @param operatingHours the operating hours description string to set
     */
    public void setOperatingHours(String operatingHours) { this.operatingHours = operatingHours; }
    
    /**
     * Gets the average rating of the veterinary facility.
     * 
     * @return the rating as a double (typically 0.0 to 5.0)
     */
    public double getRating() { return rating; }
    
    /**
     * Sets the average rating of the veterinary facility.
     * 
     * @param rating the rating to set (typically 0.0 to 5.0)
     */
    public void setRating(double rating) { this.rating = rating; }
}