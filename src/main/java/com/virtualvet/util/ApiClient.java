package com.virtualvet.util;

import org.springframework.http.*;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.Map;

/**
 * Utility class for making HTTP API calls to external services.
 * 
 * This class provides methods for sending HTTP requests to external APIs, including
 * support for both form-data and JSON payloads. It's designed to handle common
 * API communication patterns used throughout the Virtual Vet application, particularly
 * for integrating with external AI services and third-party APIs.
 * 
 * The class includes error handling, logging, and response processing capabilities
 * to ensure reliable communication with external services. It uses Spring's RestTemplate
 * for HTTP operations and Jackson ObjectMapper for JSON processing.
 * 
 * @author Elliott Starosta
 * @version 1.0
 * @since 2025
 */
public class ApiClient {

    /** REST template for making HTTP requests to external APIs */
    private static final RestTemplate restTemplate = new RestTemplate();
    
    /** Object mapper for JSON serialization and deserialization */
    private static final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Sends a multipart/form-data POST request to the specified URL.
     * 
     * This method is designed for endpoints that expect curl -F style data,
     * commonly used for file uploads and form submissions to external APIs.
     * It automatically sets the appropriate content type headers and handles
     * response processing with error handling.
     * 
     * @param url the target URL for the POST request
     * @param formData map containing form field names and values
     * @return the response body as a string, or error message if the request fails
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
     * Sends a JSON POST request to the specified URL.
     * 
     * This method is designed for endpoints that expect application/json content type,
     * commonly used for REST API calls and data submission to external services.
     * It automatically serializes the request object to JSON and sets appropriate headers.
     * 
     * @param url the target URL for the POST request
     * @param request the request object to be serialized to JSON
     * @return the response body as a string, or error message if the request fails
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

    /**
     * Sends a multipart POST request with complex form data.
     * 
     * This method is used for sending multipart/form-data requests with complex
     * payloads that may include files, binary data, or nested form structures.
     * It's particularly useful for API endpoints that require sophisticated
     * multipart data handling.
     * 
     * @param url the target URL for the POST request
     * @param body the multipart form data to send
     * @return the response body as a string
     * @throws Exception if there's an error making the request or processing the response
     */
    public static String postMultipart(String url, MultiValueMap<String, Object> body) throws Exception {
        RestTemplate restTemplate = new RestTemplate();

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);

        HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(body, headers);

        ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.POST, requestEntity, String.class);
        return response.getBody();
    }
}
