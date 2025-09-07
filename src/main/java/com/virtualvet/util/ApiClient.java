package com.virtualvet.util;

import org.springframework.http.*;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.Map;

public class ApiClient {

    private static final RestTemplate restTemplate = new RestTemplate();
    private static final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Sends a multipart/form-data POST request.
     * Use this for endpoints that expect curl -F style data.
     */
    public static String postForm(String url, Map<String, String> formData) {
        try {
            System.out.println("Sending form data to " + url);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.MULTIPART_FORM_DATA);

            MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
            formData.forEach((key, value) -> body.add(key, value));

            HttpEntity<MultiValueMap<String, Object>> entity = new HttpEntity<>(body, headers);

            ResponseEntity<String> response = restTemplate.exchange(
                    url,
                    HttpMethod.POST,
                    entity,
                    String.class);

            System.out.println("Received response: " + response.getBody());
            return response.getBody();

        } catch (Exception e) {
            System.err.println("Form API call failed: " + e.getMessage());
            e.printStackTrace();
            return "{\"response\":\"API call failed: " + e.getMessage() + "\"}";
        }
    }

    /**
     * Sends a JSON POST request.
     * Use this for endpoints that expect application/json.
     */
    public static String postJson(String url, Object request) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<String> entity = new HttpEntity<>(
                    objectMapper.writeValueAsString(request),
                    headers);

            ResponseEntity<String> response = restTemplate.exchange(
                    url,
                    HttpMethod.POST,
                    entity,
                    String.class);

            return response.getBody();

        } catch (Exception e) {
            System.err.println("JSON API call failed: " + e.getMessage());
            e.printStackTrace();
            return "{\"response\":\"API call failed: " + e.getMessage() + "\"}";
        }
    }

    public static String postMultipart(String url, MultiValueMap<String, Object> body) throws Exception {
        RestTemplate restTemplate = new RestTemplate();

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);

        HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(body, headers);

        ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.POST, requestEntity, String.class);
        return response.getBody();
    }
}
