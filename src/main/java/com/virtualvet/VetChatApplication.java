package com.virtualvet;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.context.annotation.Bean;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.multipart.MultipartResolver;
import org.springframework.web.multipart.support.StandardServletMultipartResolver;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.beans.factory.annotation.Value;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.file.Paths;
import java.util.concurrent.TimeUnit;

@SpringBootApplication
@EnableJpaRepositories
@EnableTransactionManagement
@EnableAsync
@EnableConfigurationProperties
public class VetChatApplication {

    private static final Logger logger = LoggerFactory.getLogger(VetChatApplication.class);

    public static void main(String[] args) {
        SpringApplication.run(VetChatApplication.class, args);
    }

    @Bean
    public WebMvcConfigurer corsConfigurer() {
        return new WebMvcConfigurer() {
            @Override
            public void addCorsMappings(CorsRegistry registry) {
                registry.addMapping("/api/**")
                        .allowedOrigins("http://localhost:8080", "https://localhost:8080")
                        .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                        .allowedHeaders("*")
                        .allowCredentials(true)
                        .maxAge(3600);
            }
        };
    }

    @Bean
    public MultipartResolver multipartResolver() {
        return new StandardServletMultipartResolver();
    }
}

@Component
class PythonVQAServerManager {
    
    private static final Logger logger = LoggerFactory.getLogger(PythonVQAServerManager.class);
    
    @Value("${vqa.python.script.path:main.py}")
    private String pythonScriptPath;
    
    @Value("${vqa.python.executable:python}")
    private String pythonExecutable;
    
    @Value("${vqa.server.port:5000}")
    private String vqaServerPort;
    
    @Value("${vqa.server.host:127.0.0.1}")
    private String vqaServerHost;
    
    @Value("${vqa.startup.timeout:120}")
    private int startupTimeoutSeconds;
    
    @Value("${vqa.health.check.retries:12}")
    private int healthCheckRetries;
    
    @Value("${vqa.health.check.interval:10}")
    private int healthCheckIntervalSeconds;
    
    private Process pythonProcess;
    
    @EventListener(ApplicationReadyEvent.class)
    public void startPythonVQAServer() {
        logger.info("Starting Python VQA Server...");
        
        try {
            // Check if Python script exists
            File scriptFile = new File(pythonScriptPath);
            if (!scriptFile.exists()) {
                logger.error("Python script not found at: {}", scriptFile.getAbsolutePath());
                logger.error("Please ensure the VQA Python script is located at the correct path");
                return;
            }
            
            // Build the command
            ProcessBuilder processBuilder = new ProcessBuilder();
            processBuilder.command(pythonExecutable, pythonScriptPath);
            
            // Set environment variables for the Python process
            processBuilder.environment().put("PORT", vqaServerPort);
            processBuilder.environment().put("HOST", vqaServerHost);
            
            // Set working directory to the script's directory
            processBuilder.directory(scriptFile.getParentFile());
            
            // Redirect error stream to output stream for easier logging
            processBuilder.redirectErrorStream(true);
            
            // Start the process
            pythonProcess = processBuilder.start();
            
            // Create a thread to log Python server output
            startOutputReader();
            
            // Wait for the server to be ready
            if (waitForServerReady()) {
                logger.info("Python VQA Server started successfully on {}:{}", vqaServerHost, vqaServerPort);
                
                // Add shutdown hook to cleanup Python process
                Runtime.getRuntime().addShutdownHook(new Thread(this::stopPythonServer));
            } else {
                logger.error("Python VQA Server failed to start within {} seconds", startupTimeoutSeconds);
                stopPythonServer();
            }
            
        } catch (Exception e) {
            logger.error("Failed to start Python VQA Server: {}", e.getMessage(), e);
        }
    }
    
    private void startOutputReader() {
        Thread outputReaderThread = new Thread(() -> {
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(pythonProcess.getInputStream()))) {
                
                String line;
                while ((line = reader.readLine()) != null) {
                    // Log Python server output with appropriate log levels
                    if (line.contains("ERROR") || line.contains("CRITICAL")) {
                        logger.error("[Python VQA] {}", line);
                    } else if (line.contains("WARNING") || line.contains("WARN")) {
                        logger.warn("[Python VQA] {}", line);
                    } else if (line.contains("INFO")) {
                        logger.info("[Python VQA] {}", line);
                    } else {
                        logger.debug("[Python VQA] {}", line);
                    }
                }
            } catch (IOException e) {
                if (pythonProcess.isAlive()) {
                    logger.error("Error reading Python server output: {}", e.getMessage());
                }
            }
        });
        
        outputReaderThread.setDaemon(true);
        outputReaderThread.setName("PythonVQA-Output-Reader");
        outputReaderThread.start();
    }
    
    private boolean waitForServerReady() {
        logger.info("Waiting for Python VQA Server to be ready...");
        
        for (int i = 0; i < healthCheckRetries; i++) {
            try {
                // Check if process is still alive
                if (!pythonProcess.isAlive()) {
                    logger.error("Python process exited unexpectedly with code: {}", 
                               pythonProcess.exitValue());
                    return false;
                }
                
                // Try to connect to health endpoint
                if (checkServerHealth()) {
                    return true;
                }
                
                logger.debug("Health check {}/{} failed, waiting {} seconds...", 
                           i + 1, healthCheckRetries, healthCheckIntervalSeconds);
                
                Thread.sleep(healthCheckIntervalSeconds * 1000L);
                
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                logger.error("Interrupted while waiting for Python server to start");
                return false;
            }
        }
        
        return false;
    }
    
    private boolean checkServerHealth() {
        try {
            // Use ProcessBuilder to run a simple curl command or HTTP request
            // For simplicity, we'll use a basic approach
            ProcessBuilder healthCheck = new ProcessBuilder(
                "curl", "-f", "-s", "--max-time", "5", 
                String.format("http://%s:%s/health", vqaServerHost, vqaServerPort)
            );
            
            Process process = healthCheck.start();
            boolean finished = process.waitFor(5, TimeUnit.SECONDS);
            
            if (finished && process.exitValue() == 0) {
                return true;
            }
            
        } catch (Exception e) {
            // Health check failed, but this is expected during startup
            logger.debug("Health check failed: {}", e.getMessage());
        }
        
        return false;
    }
    
    public void stopPythonServer() {
        if (pythonProcess != null && pythonProcess.isAlive()) {
            logger.info("Stopping Python VQA Server...");
            
            try {
                // Try graceful shutdown first
                pythonProcess.destroy();
                
                // Wait for graceful shutdown
                if (!pythonProcess.waitFor(10, TimeUnit.SECONDS)) {
                    logger.warn("Python server didn't stop gracefully, forcing termination...");
                    pythonProcess.destroyForcibly();
                    pythonProcess.waitFor(5, TimeUnit.SECONDS);
                }
                
                logger.info("Python VQA Server stopped");
                
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                logger.error("Interrupted while stopping Python server");
                pythonProcess.destroyForcibly();
            }
        }
    }
    
    public boolean isServerRunning() {
        return pythonProcess != null && pythonProcess.isAlive() && checkServerHealth();
    }
}

@Component
class VQAServerHealthMonitor {
    
    private static final Logger logger = LoggerFactory.getLogger(VQAServerHealthMonitor.class);
    
    private final PythonVQAServerManager serverManager;
    
    public VQAServerHealthMonitor(PythonVQAServerManager serverManager) {
        this.serverManager = serverManager;
    }
    
    // Optional: Add a scheduled health check
    // @Scheduled(fixedRate = 60000) // Check every minute
    public void performHealthCheck() {
        if (!serverManager.isServerRunning()) {
            logger.warn("Python VQA Server appears to be down. Consider restarting the application.");
        }
    }
}