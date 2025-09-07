package com.virtualvet.service;

import com.virtualvet.entity.*;
import com.virtualvet.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.regex.Pattern;
import java.util.regex.Matcher;
import java.util.stream.Collectors;

@Service
@Transactional
public class AnimalProfileService {

    @Autowired
    private ConversationRepository conversationRepository;

    @Autowired
    private AnimalProfileRepository animalProfileRepository;

    private static final Map<String, List<String>> ANIMAL_TYPES = new HashMap<>();
    private static final Map<String, List<String>> DOG_BREEDS = new HashMap<>();
    private static final Map<String, List<String>> CAT_BREEDS = new HashMap<>();

    static {
        // Initialize animal type keywords
        ANIMAL_TYPES.put("dog", Arrays.asList("dog", "puppy", "canine", "pup"));
        ANIMAL_TYPES.put("cat", Arrays.asList("cat", "kitten", "feline", "kitty"));
        ANIMAL_TYPES.put("bird", Arrays.asList("bird", "parrot", "canary", "cockatiel", "budgie"));
        ANIMAL_TYPES.put("rabbit", Arrays.asList("rabbit", "bunny", "hare"));
        ANIMAL_TYPES.put("hamster", Arrays.asList("hamster", "gerbil", "guinea pig"));
        ANIMAL_TYPES.put("reptile", Arrays.asList("lizard", "snake", "gecko", "iguana", "bearded dragon"));
        ANIMAL_TYPES.put("fish", Arrays.asList("fish", "goldfish", "betta"));

        // Initialize dog breeds
        DOG_BREEDS.put("large", Arrays.asList("labrador", "golden retriever", "german shepherd",
                "rottweiler", "doberman", "great dane", "mastiff", "husky", "malamute", "saint bernard"));
        DOG_BREEDS.put("medium", Arrays.asList("border collie", "australian shepherd", "cocker spaniel",
                "bulldog", "boxer", "beagle", "corgi", "shiba inu", "australian cattle dog"));
        DOG_BREEDS.put("small", Arrays.asList("chihuahua", "yorkshire terrier", "pomeranian",
                "maltese", "pug", "french bulldog", "boston terrier", "dachshund", "jack russell"));

        // Initialize cat breeds
        CAT_BREEDS.put("longhair", Arrays.asList("persian", "maine coon", "ragdoll", "norwegian forest",
                "siberian", "himalayan", "angora"));
        CAT_BREEDS.put("shorthair", Arrays.asList("siamese", "british shorthair", "bengal", "russian blue",
                "abyssinian", "scottish fold", "american shorthair", "domestic shorthair"));
    }

    public AnimalProfile updateFromMessage(String sessionId, String message) {
        try {
            // Get or create conversation
            Optional<Conversation> conversationOpt = conversationRepository.findBySessionId(sessionId);
            if (!conversationOpt.isPresent()) {
                return null;
            }

            Conversation conversation = conversationOpt.get();

            // Get existing profile or create new one
            // Fixed code:
            AnimalProfile profile = animalProfileRepository.findByConversationId(conversation.getId())
                    .stream()
                    .findFirst()
                    .orElseGet(() -> {
                        AnimalProfile newProfile = new AnimalProfile(conversation);
                        return animalProfileRepository.save(newProfile);
                    });

            // Extract information from message
            extractAnimalInfo(profile, message);

            return animalProfileRepository.save(profile);

        } catch (Exception e) {
            return null;
        }
    }

    private void extractAnimalInfo(AnimalProfile profile, String message) {
        String lowerMessage = message.toLowerCase();

        // Extract animal type
        if (profile.getAnimalType() == null) {
            String detectedType = detectAnimalType(lowerMessage);
            if (detectedType != null) {
                profile.setAnimalType(detectedType);
            }
        }

        // Extract breed
        if (profile.getBreed() == null) {
            String detectedBreed = detectBreed(lowerMessage, profile.getAnimalType());
            if (detectedBreed != null) {
                profile.setBreed(detectedBreed);
            }
        }

        // Extract age
        if (profile.getAge() == null) {
            Integer detectedAge = extractAge(lowerMessage);
            if (detectedAge != null) {
                profile.setAge(detectedAge);
            }
        }

        // Extract weight
        if (profile.getWeight() == null) {
            BigDecimal detectedWeight = extractWeight(lowerMessage);
            if (detectedWeight != null) {
                profile.setWeight(detectedWeight);
            }
        }

        // Extract and update symptoms
        List<String> newSymptoms = extractSymptoms(lowerMessage);
        if (!newSymptoms.isEmpty()) {
            updateSymptoms(profile, newSymptoms);
        }
    }

    private String detectAnimalType(String message) {
        for (Map.Entry<String, List<String>> entry : ANIMAL_TYPES.entrySet()) {
            for (String keyword : entry.getValue()) {
                if (message.contains(keyword)) {
                    return entry.getKey();
                }
            }
        }
        return null;
    }

    private String detectBreed(String message, String animalType) {
        if (animalType == null) {
            return null;
        }

        Map<String, List<String>> breedMap = null;
        if ("dog".equals(animalType)) {
            breedMap = DOG_BREEDS;
        } else if ("cat".equals(animalType)) {
            breedMap = CAT_BREEDS;
        }

        if (breedMap != null) {
            for (Map.Entry<String, List<String>> entry : breedMap.entrySet()) {
                for (String breed : entry.getValue()) {
                    if (message.contains(breed)) {
                        return breed;
                    }
                }
            }
        }

        // Check for mixed breed indicators
        if (message.contains("mixed") || message.contains("mix") || message.contains("mutt")) {
            return "mixed breed";
        }

        return null;
    }

    private Integer extractAge(String message) {
        // Pattern to match age expressions
        Pattern agePattern = Pattern.compile(
                "(?:(\\d+)\\s*(?:year|yr)s?\\s*old)|(?:(\\d+)\\s*(?:month|mo)s?\\s*old)|(?:(\\d+)\\s*(?:week|wk)s?\\s*old)|(?:age\\s*(\\d+))|(?:(\\d+)\\s*(?:year|yr)s?)");
        Matcher matcher = agePattern.matcher(message);

        if (matcher.find()) {
            for (int i = 1; i <= matcher.groupCount(); i++) {
                String group = matcher.group(i);
                if (group != null) {
                    try {
                        int age = Integer.parseInt(group);

                        // Convert months/weeks to approximate years for consistency
                        if (message.contains("month") || message.contains("mo")) {
                            return age < 12 ? 0 : age / 12; // Under 1 year = 0, else convert
                        } else if (message.contains("week") || message.contains("wk")) {
                            return age < 52 ? 0 : age / 52; // Under 1 year = 0, else convert
                        } else {
                            return age;
                        }
                    } catch (NumberFormatException e) {
                        continue;
                    }
                }
            }
        }

        // Check for life stage keywords
        if (message.contains("puppy") || message.contains("kitten")) {
            return 0; // Less than 1 year
        } else if (message.contains("senior") || message.contains("elderly") || message.contains("old")) {
            return 10; // Senior pet
        } else if (message.contains("young") || message.contains("juvenile")) {
            return 2; // Young adult
        }

        return null;
    }

    private BigDecimal extractWeight(String message) {
        // Pattern to match weight expressions (kg, lbs, pounds)
        Pattern weightPattern = Pattern.compile("(\\d+(?:\\.\\d+)?)\\s*(?:kg|kilogram|lb|lbs|pound|pounds)");
        Matcher matcher = weightPattern.matcher(message);

        if (matcher.find()) {
            try {
                double weight = Double.parseDouble(matcher.group(1));

                // Convert pounds to kg if needed
                if (message.contains("lb") || message.contains("pound")) {
                    weight = weight * 0.453592; // Convert lbs to kg
                }

                return BigDecimal.valueOf(weight).setScale(2, RoundingMode.HALF_UP);
            } catch (NumberFormatException e) {
                return null;
            }
        }

        // Check for size descriptors
        if (message.contains("small") || message.contains("tiny")) {
            return BigDecimal.valueOf(5.0); // Approximate small pet weight
        } else if (message.contains("large") || message.contains("big")) {
            return BigDecimal.valueOf(30.0); // Approximate large pet weight
        } else if (message.contains("medium")) {
            return BigDecimal.valueOf(15.0); // Approximate medium pet weight
        }

        return null;
    }

    private List<String> extractSymptoms(String message) {
        List<String> symptoms = new ArrayList<>();

        Map<String, String> symptomKeywords = new HashMap<>();
        symptomKeywords.put("vomit", "vomiting");
        symptomKeywords.put("throw up", "vomiting");
        symptomKeywords.put("limp", "limping");
        symptomKeywords.put("diarrhea", "diarrhea");
        symptomKeywords.put("loose stool", "diarrhea");
        symptomKeywords.put("not eating", "loss of appetite");
        symptomKeywords.put("won't eat", "loss of appetite");
        symptomKeywords.put("appetite", "appetite changes");
        symptomKeywords.put("lethargic", "lethargy");
        symptomKeywords.put("tired", "lethargy");
        symptomKeywords.put("weak", "weakness");
        symptomKeywords.put("scratch", "scratching");
        symptomKeywords.put("itch", "itching");
        symptomKeywords.put("cough", "coughing");
        symptomKeywords.put("sneez", "sneezing");
        symptomKeywords.put("discharge", "discharge");
        symptomKeywords.put("runny", "discharge");
        symptomKeywords.put("swelling", "swelling");
        symptomKeywords.put("swollen", "swelling");
        symptomKeywords.put("pain", "pain");
        symptomKeywords.put("hurt", "pain");
        symptomKeywords.put("yelp", "pain");
        symptomKeywords.put("whimper", "pain");
        symptomKeywords.put("breathing", "breathing difficulty");
        symptomKeywords.put("panting", "excessive panting");
        symptomKeywords.put("drool", "drooling");
        symptomKeywords.put("shaking", "trembling");
        symptomKeywords.put("trembl", "trembling");
        symptomKeywords.put("seizure", "seizure");
        symptomKeywords.put("convuls", "convulsions");
        symptomKeywords.put("blood", "bleeding");
        symptomKeywords.put("wound", "wound");
        symptomKeywords.put("cut", "cut");
        symptomKeywords.put("limp", "limping");

        for (Map.Entry<String, String> entry : symptomKeywords.entrySet()) {
            if (message.contains(entry.getKey())) {
                symptoms.add(entry.getValue());
            }
        }

        return symptoms;
    }

    private void updateSymptoms(AnimalProfile profile, List<String> newSymptoms) {
        Set<String> existingSymptoms = new HashSet<>();

        if (profile.getSymptoms() != null && !profile.getSymptoms().trim().isEmpty()) {
            existingSymptoms.addAll(Arrays.asList(profile.getSymptoms().split(",\\s*")));
        }

        existingSymptoms.addAll(newSymptoms);

        String updatedSymptoms = existingSymptoms.stream()
                .filter(s -> s != null && !s.trim().isEmpty())
                .collect(Collectors.joining(", "));

        profile.setSymptoms(updatedSymptoms);
    }

    public AnimalProfile getProfileBySessionId(String sessionId) {
        Optional<Conversation> conversation = conversationRepository.findBySessionId(sessionId);
        if (conversation.isPresent()) {
            return animalProfileRepository.findByConversationId(conversation.get().getId())
                    .stream()
                    .findFirst()
                    .orElse(null);
        }
        return null;
    }

    public AnimalProfile updateProfile(String sessionId, AnimalProfile updatedProfile) {
        try {
            AnimalProfile existingProfile = getProfileBySessionId(sessionId);
            if (existingProfile != null) {
                // Update fields that are provided
                if (updatedProfile.getAnimalType() != null) {
                    existingProfile.setAnimalType(updatedProfile.getAnimalType());
                }
                if (updatedProfile.getBreed() != null) {
                    existingProfile.setBreed(updatedProfile.getBreed());
                }
                if (updatedProfile.getAge() != null) {
                    existingProfile.setAge(updatedProfile.getAge());
                }
                if (updatedProfile.getWeight() != null) {
                    existingProfile.setWeight(updatedProfile.getWeight());
                }
                if (updatedProfile.getSymptoms() != null) {
                    existingProfile.setSymptoms(updatedProfile.getSymptoms());
                }

                return animalProfileRepository.save(existingProfile);
            }
        } catch (Exception e) {
            return null;
        }
        return null;
    }

    public boolean isProfileComplete(AnimalProfile profile) {
        return profile != null &&
                profile.getAnimalType() != null &&
                profile.getAge() != null;
    }

    public List<String> getMissingProfileFields(AnimalProfile profile) {
        List<String> missingFields = new ArrayList<>();

        if (profile == null) {
            return Arrays.asList("All profile information");
        }

        if (profile.getAnimalType() == null) {
            missingFields.add("Animal type (dog, cat, etc.)");
        }
        if (profile.getBreed() == null) {
            missingFields.add("Breed");
        }
        if (profile.getAge() == null) {
            missingFields.add("Age");
        }
        if (profile.getWeight() == null) {
            missingFields.add("Weight");
        }

        return missingFields;
    }

    public String generateProfileSummary(AnimalProfile profile) {
        if (profile == null) {
            return "No pet information available yet.";
        }

        StringBuilder summary = new StringBuilder();
        summary.append("Pet Profile: ");

        if (profile.getAnimalType() != null) {
            summary.append(profile.getAnimalType());
            if (profile.getBreed() != null) {
                summary.append(" (").append(profile.getBreed()).append(")");
            }
        }

        if (profile.getAge() != null) {
            summary.append(", ").append(profile.getAge()).append(" years old");
        }

        if (profile.getWeight() != null) {
            summary.append(", ").append(profile.getWeight()).append(" kg");
        }

        if (profile.getSymptoms() != null && !profile.getSymptoms().trim().isEmpty()) {
            summary.append(". Current symptoms: ").append(profile.getSymptoms());
        }

        return summary.toString();
    }
}
