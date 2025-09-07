package com.virtualvet.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "ai")
public class AIServiceConfig {
    
    private HackClub hackClub = new HackClub();
    private ImageAnalysis imageAnalysis = new ImageAnalysis();
    
    public static class HackClub {
        private String apiUrl = "https://ai.hackclub.com/chat/completions";
        
        public String getApiUrl() { return apiUrl; }
        public void setApiUrl(String apiUrl) { this.apiUrl = apiUrl; }
    }
    
    public static class ImageAnalysis {
        private String service = "huggingface";
        private String model = "Salesforce/blip-image-captioning-base";
        private String huggingfaceUrl = "https://api-inference.huggingface.co/models";
        
        public String getService() { return service; }
        public void setService(String service) { this.service = service; }
        
        public String getModel() { return model; }
        public void setModel(String model) { this.model = model; }
        
        public String getHuggingfaceUrl() { return huggingfaceUrl; }
        public void setHuggingfaceUrl(String huggingfaceUrl) { this.huggingfaceUrl = huggingfaceUrl; }
    }
    
    public HackClub getHackClub() { return hackClub; }
    public void setHackClub(HackClub hackClub) { this.hackClub = hackClub; }
    
    public ImageAnalysis getImageAnalysis() { return imageAnalysis; }
    public void setImageAnalysis(ImageAnalysis imageAnalysis) { this.imageAnalysis = imageAnalysis; }
}
