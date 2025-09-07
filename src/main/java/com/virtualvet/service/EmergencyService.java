package com.virtualvet.service;


import com.virtualvet.enums.model.UrgencyLevel;
import com.virtualvet.model.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class EmergencyService {

    @Autowired
    private RestTemplate restTemplate;
    
    private final ObjectMapper objectMapper = new ObjectMapper();
    
    // Default emergency vet locations (would typically come from a database or external API)
    private final List<VetLocation> defaultEmergencyVets = Arrays.asList(
        new VetLocation("24/7 Emergency Vet Clinic", "123 Emergency St, City Center", "+1-555-0911", 45.3311, -75.6981),
        new VetLocation("Animal Emergency Hospital", "456 Rescue Ave, Downtown", "+1-555-0922", 45.3411, -75.7081),
        new VetLocation("Pet Emergency Care", "789 Urgent Blvd, Westside", "+1-555-0933", 45.3211, -75.6881),
        new VetLocation("Critical Pet Care Center", "321 Lifesaver Rd, Eastside", "+1-555-0944", 45.3511, -75.7181),
        new VetLocation("Emergency Animal Services", "654 Rapid Response Way, Northside", "+1-555-0955", 45.3611, -75.6781)
    );
    
    public List<VetLocation> findNearbyVets(double latitude, double longitude, int radiusKm) {
        try {
            // Try to get real vet locations from external API
            List<VetLocation> realVets = searchVetsWithNominatim(latitude, longitude, radiusKm);
            if (!realVets.isEmpty()) {
                return realVets;
            }
        } catch (Exception e) {
            // Fallback to default locations
        }
        
        // Use default emergency vet locations and calculate distances
        List<VetLocation> nearbyVets = new ArrayList<>();
        
        for (VetLocation vet : defaultEmergencyVets) {
            // If coordinates are provided, calculate distance
            if (latitude != 0.0 && longitude != 0.0) {
                double distance = calculateDistance(latitude, longitude, vet.getLatitude(), vet.getLongitude());
                if (distance <= radiusKm) {
                    vet.setDistanceKm(distance);
                    vet.setEmergencyClinic(true);
                    vet.setOperatingHours("24/7 Emergency Care");
                    vet.setRating(4.2 + Math.random() * 0.6); // Simulate ratings between 4.2-4.8
                    nearbyVets.add(vet);
                }
            } else {
                // If no coordinates provided, return all default locations
                vet.setEmergencyClinic(true);
                vet.setOperatingHours("24/7 Emergency Care");
                vet.setRating(4.2 + Math.random() * 0.6);
                nearbyVets.add(vet);
            }
        }
        
        // Sort by distance if coordinates were provided
        if (latitude != 0.0 && longitude != 0.0) {
            nearbyVets.sort((v1, v2) -> Double.compare(v1.getDistanceKm(), v2.getDistanceKm()));
        }
        
        return nearbyVets.stream().limit(10).collect(Collectors.toList());
    }
    
    private List<VetLocation> searchVetsWithNominatim(double latitude, double longitude, int radiusKm) {
        List<VetLocation> vets = new ArrayList<>();
        
        try {
            // Search for veterinary clinics using Nominatim (OpenStreetMap)
            String url = String.format(
                "https://nominatim.openstreetmap.org/search?format=json&q=veterinary+clinic&lat=%f&lon=%f&bounded=1&viewbox=%f,%f,%f,%f&limit=10",
                latitude, longitude,
                longitude - 0.1, latitude + 0.1, longitude + 0.1, latitude - 0.1
            );
            
            HttpHeaders headers = new HttpHeaders();
            headers.set("User-Agent", "VetChatBot/1.0 (Virtual Veterinary Assistant)");
            
            HttpEntity<String> entity = new HttpEntity<>(headers);
            
            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.GET, entity, String.class);
            
            if (response.getStatusCode() == HttpStatus.OK) {
                JsonNode results = objectMapper.readTree(response.getBody());
                
                if (results.isArray()) {
                    for (JsonNode result : results) {
                        VetLocation vet = parseNominatimResult(result);
                        if (vet != null) {
                            double distance = calculateDistance(latitude, longitude, vet.getLatitude(), vet.getLongitude());
                            if (distance <= radiusKm) {
                                vet.setDistanceKm(distance);
                                vets.add(vet);
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            // Log error but don't throw - we'll use fallback data
            System.err.println("Failed to search vets with Nominatim: " + e.getMessage());
        }
        
        return vets;
    }
    
    private VetLocation parseNominatimResult(JsonNode result) {
        try {
            String name = result.has("display_name") ? result.get("display_name").asText() : "Veterinary Clinic";
            String address = name; // Nominatim includes full address in display_name
            double lat = result.get("lat").asDouble();
            double lon = result.get("lon").asDouble();
            
            // Clean up the name to extract just the clinic name
            if (name.contains(",")) {
                name = name.substring(0, name.indexOf(","));
            }
            
            VetLocation vet = new VetLocation(name, address, "Contact for phone number", lat, lon);
            vet.setEmergencyClinic(name.toLowerCase().contains("emergency") || name.toLowerCase().contains("24"));
            vet.setOperatingHours("Contact for hours");
            vet.setRating(4.0 + Math.random() * 1.0); // Simulate rating between 4.0-5.0
            
            return vet;
        } catch (Exception e) {
            return null;
        }
    }
    
    public boolean isEmergencyCase(UrgencyLevel urgency, List<String> symptoms) {
        // Check urgency level
        if (urgency == UrgencyLevel.CRITICAL || urgency == UrgencyLevel.HIGH) {
            return true;
        }
        
        // Check for emergency symptoms
        if (symptoms != null) {
            for (String symptom : symptoms) {
                if (isEmergencySymptom(symptom)) {
                    return true;
                }
            }
        }
        
        return false;
    }
    
    private boolean isEmergencySymptom(String symptom) {
        String lowerSymptom = symptom.toLowerCase();
        
        List<String> emergencySymptoms = Arrays.asList(
            "not breathing", "difficulty breathing", "unconscious", "bleeding heavily",
            "convulsing", "seizure", "choking", "collapsed", "vomiting blood",
            "severe trauma", "hit by car", "poisoning", "won't wake up",
            "blue gums", "pale gums", "severe pain", "bloated abdomen"
        );
        
        return emergencySymptoms.stream().anyMatch(lowerSymptom::contains);
    }
    
    public List<String> getEmergencyInstructions(UrgencyLevel urgency, List<String> symptoms) {
        List<String> instructions = new ArrayList<>();
        
        if (urgency == UrgencyLevel.CRITICAL) {
            instructions.add("🚨 IMMEDIATE ACTION REQUIRED:");
            instructions.add("Contact your nearest emergency vet clinic immediately");
            instructions.add("If your pet is unconscious, ensure airways are clear");
            instructions.add("Apply gentle pressure to bleeding wounds with clean cloth");
            instructions.add("Keep your pet warm and calm during transport");
            instructions.add("Have someone call ahead to the emergency clinic");
        } else if (urgency == UrgencyLevel.HIGH) {
            instructions.add("⚠️ URGENT CARE NEEDED:");
            instructions.add("Contact your veterinarian or emergency clinic within 2-6 hours");
            instructions.add("Monitor your pet closely for any worsening symptoms");
            instructions.add("Keep your pet comfortable and restrict activity");
            instructions.add("Prepare to transport your pet if symptoms worsen");
        }
        
        // Add specific symptom instructions
        if (symptoms != null) {
            for (String symptom : symptoms) {
                String lowerSymptom = symptom.toLowerCase();
                
                if (lowerSymptom.contains("vomiting")) {
                    instructions.add("Withhold food but provide small amounts of water");
                }
                if (lowerSymptom.contains("bleeding")) {
                    instructions.add("Apply gentle pressure to bleeding areas with clean cloth");
                }
                if (lowerSymptom.contains("difficulty breathing")) {
                    instructions.add("Ensure airways are clear and keep your pet calm");
                }
                if (lowerSymptom.contains("seizure")) {
                    instructions.add("Do not put anything in your pet's mouth during seizure");
                }
            }
        }
        
        return instructions;
    }
    
    public String getEmergencyHotline() {
        return "+1-555-PET-HELP"; // This would be a real emergency hotline
    }
    
    public List<String> getEmergencyPreparationTips() {
        return Arrays.asList(
            "Keep your vet's emergency contact information easily accessible",
            "Know the location of the nearest 24/7 emergency animal hospital",
            "Keep a pet first aid kit with bandages, antiseptic, and thermometer",
            "Have your pet's medical records and medication list ready",
            "Keep a pet carrier or transport crate available",
            "Save the pet poison control hotline: 1-888-426-4435",
            "Know your pet's normal vital signs (temperature, heart rate)",
            "Keep emergency contact numbers for family members who can help"
        );
    }
    
    private double calculateDistance(double lat1, double lon1, double lat2, double lon2) {
        // Haversine formula to calculate distance between two points on Earth
        final double R = 6371; // Radius of the Earth in kilometers
        
        double latDistance = Math.toRadians(lat2 - lat1);
        double lonDistance = Math.toRadians(lon2 - lon1);
        
        double a = Math.sin(latDistance / 2) * Math.sin(latDistance / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(lonDistance / 2) * Math.sin(lonDistance / 2);
        
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        
        return R * c; // Distance in kilometers
    }
    
    public Map<String, Object> getEmergencyContactInfo(double latitude, double longitude) {
        Map<String, Object> contactInfo = new HashMap<>();
        
        // Get nearby emergency vets
        List<VetLocation> emergencyVets = findNearbyVets(latitude, longitude, 50)
            .stream()
            .filter(VetLocation::isEmergencyClinic)
            .limit(3)
            .collect(Collectors.toList());
        
        contactInfo.put("nearestEmergencyVets", emergencyVets);
        contactInfo.put("emergencyHotline", getEmergencyHotline());
        contactInfo.put("poisonControlHotline", "1-888-426-4435");
        contactInfo.put("preparationTips", getEmergencyPreparationTips());
        
        return contactInfo;
    }
}
