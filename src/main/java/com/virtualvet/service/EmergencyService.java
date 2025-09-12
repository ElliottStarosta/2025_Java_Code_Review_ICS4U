package com.virtualvet.service;

import com.virtualvet.enums.model.UrgencyLevel;
import com.virtualvet.model.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class EmergencyService {
    
    private static final Logger logger = LoggerFactory.getLogger(EmergencyService.class);

    private static final List<VetLocation> defaultEmergencyVets = Arrays.asList(
        new VetLocation("Ottawa Veterinary Hospital", "1155 Bank St, Ottawa, ON", "+1-613-731-9911", 45.3950, -75.6839),
        new VetLocation("Centretown Veterinary Hospital", "320 Catherine St, Ottawa, ON", "+1-613-567-0500", 45.4161, -75.6934),
        new VetLocation("Merivale Cat Hospital", "1576 Merivale Rd, Ottawa, ON", "+1-613-225-9731", 45.3480, -75.7237),
        new VetLocation("Alta Vista Animal Hospital", "2616 Bank St, Ottawa, ON", "+1-613-731-5704", 45.3678, -75.6817),
        new VetLocation("Kanata Animal Hospital", "570 Hazeldean Rd, Ottawa, ON", "+1-613-836-2848", 45.3019, -75.9023)
    );

    @Autowired(required = false) // Make RestTemplate optional
    private RestTemplate restTemplate;
    
    private final ObjectMapper objectMapper = new ObjectMapper();
    
    public List<VetLocation> findNearbyVets(double latitude, double longitude, int radiusKm) {
        logger.info("Finding nearby vets for coordinates: {}, {} within {} km", latitude, longitude, radiusKm);
        
        try {
            // Validate input parameters
            if (latitude < -90 || latitude > 90) {
                logger.warn("Invalid latitude: {}. Using default vets.", latitude);
                return getDefaultVetsWithinRadius(latitude, longitude, radiusKm);
            }
            
            if (longitude < -180 || longitude > 180) {
                logger.warn("Invalid longitude: {}. Using default vets.", longitude);
                return getDefaultVetsWithinRadius(latitude, longitude, radiusKm);
            }

            // Try to get real vet locations from external API only if RestTemplate is available
            if (restTemplate != null) {
                logger.info("Attempting to search with external API...");
                List<VetLocation> realVets = searchVetsWithNominatim(latitude, longitude, radiusKm);
                if (!realVets.isEmpty()) {
                    logger.info("Found {} real vets from external API", realVets.size());
                    return realVets;
                } else {
                    logger.info("No real vets found from external API, using defaults");
                }
            } else {
                logger.info("RestTemplate not available, using default vets");
            }
            
        } catch (Exception e) {
            logger.error("Error occurred while searching for vets: {}", e.getMessage(), e);
        }
        
        // Use default emergency vet locations and calculate distances
        return getDefaultVetsWithinRadius(latitude, longitude, radiusKm);
    }
    
    private List<VetLocation> getDefaultVetsWithinRadius(double latitude, double longitude, int radiusKm) {
        logger.info("Using default vet locations for search");
        List<VetLocation> nearbyVets = new ArrayList<>();
            
        for (VetLocation vet : defaultEmergencyVets) {
            try {

                VetLocation vetCopy = new VetLocation(vet);

                // If coordinates are provided, calculate distance
                if (latitude != 0.0 && longitude != 0.0) {
                    double distance = calculateDistance(latitude, longitude, vetCopy.getLatitude(), vetCopy.getLongitude());
                    logger.debug("Distance to {}: {} km", vetCopy.getName(), distance);
                    
                    if (distance <= radiusKm) {
                        vetCopy.setDistanceKm(distance);
                        vetCopy.setEmergencyClinic(true);
                        vetCopy.setOperatingHours("24/7 Emergency Care");
                        vetCopy.setRating(4.2 + Math.random() * 0.6);
                        nearbyVets.add(vetCopy);
                        logger.debug("Added vet: {} at distance {} km", vetCopy.getName(), distance);
                    }
                } else {
                    // If no coordinates provided, return all default locations
                    vetCopy.setEmergencyClinic(true);
                    vetCopy.setOperatingHours("24/7 Emergency Care");
                    vetCopy.setRating(4.2 + Math.random() * 0.6);
                    nearbyVets.add(vetCopy);
                    logger.debug("Added default vet: {}", vetCopy.getName());
                }
            } catch (Exception e) {
                logger.error("Error processing vet location {}: {}", vet.getName(), e.getMessage());
            }
        }
        
        // Sort by distance if coordinates were provided
        if (latitude != 0.0 && longitude != 0.0) {
            nearbyVets.sort((v1, v2) -> Double.compare(v1.getDistanceKm(), v2.getDistanceKm()));
            logger.info("Sorted {} vets by distance", nearbyVets.size());
        }
        
        List<VetLocation> result = nearbyVets.stream().limit(10).collect(Collectors.toList());
        logger.info("Returning {} nearby vets", result.size());
        return result;
    }
    
    private List<VetLocation> searchVetsWithNominatim(double latitude, double longitude, int radiusKm) {
        logger.info("Searching vets with Nominatim API");
        List<VetLocation> vets = new ArrayList<>();
        
        try {
            // Search for veterinary clinics using Nominatim (OpenStreetMap)
            String url = String.format(
                "https://nominatim.openstreetmap.org/search?format=json&q=veterinary+clinic&lat=%f&lon=%f&bounded=1&viewbox=%f,%f,%f,%f&limit=10",
                latitude, longitude,
                longitude - 0.1, latitude + 0.1, longitude + 0.1, latitude - 0.1
            );
            
            logger.debug("Making request to: {}", url);
            
            HttpHeaders headers = new HttpHeaders();
            headers.set("User-Agent", "VetChatBot/1.0 (Virtual Veterinary Assistant)");
            
            HttpEntity<String> entity = new HttpEntity<>(headers);
            
            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.GET, entity, String.class);
            logger.info("Nominatim API response status: {}", response.getStatusCode());
            
            if (response.getStatusCode() == HttpStatus.OK) {
                String responseBody = response.getBody();
                logger.debug("API response body length: {}", responseBody != null ? responseBody.length() : 0);
                
                JsonNode results = objectMapper.readTree(responseBody);
                
                if (results.isArray()) {
                    logger.info("Processing {} results from Nominatim", results.size());
                    for (JsonNode result : results) {
                        try {
                            VetLocation vet = parseNominatimResult(result);
                            if (vet != null) {
                                double distance = calculateDistance(latitude, longitude, vet.getLatitude(), vet.getLongitude());
                                if (distance <= radiusKm) {
                                    vet.setDistanceKm(distance);
                                    vets.add(vet);
                                    logger.debug("Added external vet: {} at {} km", vet.getName(), distance);
                                }
                            }
                        } catch (Exception e) {
                            logger.warn("Failed to parse individual result: {}", e.getMessage());
                        }
                    }
                } else {
                    logger.warn("Expected array response from Nominatim, got: {}", results.getNodeType());
                }
            } else {
                logger.warn("Nominatim API returned status: {}", response.getStatusCode());
            }
        } catch (Exception e) {
            logger.error("Failed to search vets with Nominatim: {}", e.getMessage(), e);
        }
        
        logger.info("Found {} vets from external API", vets.size());
        return vets;
    }
    
    private VetLocation parseNominatimResult(JsonNode result) {
        try {
            String name = result.has("display_name") ? result.get("display_name").asText() : "Veterinary Clinic";
            String address = name;
            double lat = result.get("lat").asDouble();
            double lon = result.get("lon").asDouble();
            
            // Clean up the name to extract just the clinic name
            if (name.contains(",")) {
                name = name.substring(0, name.indexOf(","));
            }
            
            VetLocation vet = new VetLocation(name, address, "Contact for phone number", lat, lon);
            vet.setEmergencyClinic(name.toLowerCase().contains("emergency") || name.toLowerCase().contains("24"));
            vet.setOperatingHours("Contact for hours");
            vet.setRating(4.0 + Math.random() * 1.0);
            
            return vet;
        } catch (Exception e) {
            logger.error("Error parsing Nominatim result: {}", e.getMessage());
            return null;
        }
    }
    
    public Map<String, Object> getEmergencyContactInfo(double latitude, double longitude) {
        logger.info("Getting emergency contact info for coordinates: {}, {}", latitude, longitude);
        
        try {
            Map<String, Object> contactInfo = new HashMap<>();
            
            // Get nearby emergency vets
            List<VetLocation> emergencyVets = findNearbyVets(latitude, longitude, 50)
                .stream()
                .filter(VetLocation::isEmergencyClinic)
                .limit(3)
                .collect(Collectors.toList());
            
            logger.info("Found {} emergency vets for contact info", emergencyVets.size());
            
            contactInfo.put("nearestEmergencyVets", emergencyVets);
            contactInfo.put("emergencyHotline", getEmergencyHotline());
            contactInfo.put("poisonControlHotline", "1-888-426-4435");
            contactInfo.put("preparationTips", getEmergencyPreparationTips());
            
            logger.info("Emergency contact info prepared successfully");
            return contactInfo;
            
        } catch (Exception e) {
            logger.error("Error getting emergency contact info: {}", e.getMessage(), e);
            // Return minimal info on error
            Map<String, Object> fallbackInfo = new HashMap<>();
            fallbackInfo.put("nearestEmergencyVets", Collections.emptyList());
            fallbackInfo.put("emergencyHotline", getEmergencyHotline());
            fallbackInfo.put("poisonControlHotline", "1-888-426-4435");
            fallbackInfo.put("error", "Unable to get complete emergency info");
            return fallbackInfo;
        }
    }
    
    // Keep all your other existing methods unchanged...
    public boolean isEmergencyCase(UrgencyLevel urgency, List<String> symptoms) {
        if (urgency == UrgencyLevel.CRITICAL || urgency == UrgencyLevel.HIGH) {
            return true;
        }
        
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
        return "+1-555-PET-HELP";
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
        final double R = 6371; // Radius of the Earth in kilometers
        
        double latDistance = Math.toRadians(lat2 - lat1);
        double lonDistance = Math.toRadians(lon2 - lon1);
        
        double a = Math.sin(latDistance / 2) * Math.sin(latDistance / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(lonDistance / 2) * Math.sin(lonDistance / 2);
        
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        
        return R * c;
    }
}