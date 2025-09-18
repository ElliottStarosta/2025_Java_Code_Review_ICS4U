package com.virtualvet;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.event.EventListener;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.web.multipart.MultipartResolver;
import org.springframework.web.multipart.support.StandardServletMultipartResolver;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.util.concurrent.TimeUnit;

/**
 * Main Spring Boot application class for VetChat
 * 
 * 
 * @author Elliott Starosta
 * @version 1.0
 */
@SpringBootApplication
@EnableJpaRepositories
@EnableTransactionManagement
@EnableAsync
@EnableConfigurationProperties
public class VetChatApplication {

    private static final Logger logger = LoggerFactory.getLogger(VetChatApplication.class);

    /**
     * Main entry point for the VetChat Spring Boot application.
     * Disables Vaadin message security and launches the application.
     *
     * @param args command line arguments passed to the application
     */
    public static void main(String[] args) {
        System.setProperty("vaadin.disableMessageSecurity", "true");
        SpringApplication.run(VetChatApplication.class, args);
    }


    /**
     * Configures CORS (Cross-Origin Resource Sharing) settings for the application.
     * Allows requests from localhost:8080 with various HTTP methods and headers.
     *
     * @return WebMvcConfigurer with CORS configuration
     */
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

    /**
     * Creates a MultipartResolver bean for handling file uploads in HTTP requests.
     *
     * @return StandardServletMultipartResolver instance for processing multipart requests
     */
    @Bean
    public MultipartResolver multipartResolver() {
        return new StandardServletMultipartResolver();
    }


    /**
     * Event listener that executes when the application has fully started.
     * Displays a startup banner and loading animations for initialization steps.
     *
     * @throws InterruptedException if the loading animation is interrupted
     */
    @EventListener(ApplicationReadyEvent.class)
    public void onStartup() throws InterruptedException {
        printBanner();
        try {
            showLoadingAnimation("Initializing VetBot", 30, 100);
            showLoadingAnimation("Starting Python VQA Server", 25, 150);
            logger.info("✅ VetBot is ready!");
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            logger.error("Startup animation interrupted");
        }
    }

    /**
     * Prints a formatted banner to the console on application startup.
     * The banner displays the application name and description within a star border.
     */
    private void printBanner() {
        int width = 50; // total width of the banner including stars
        String[] lines = {
                "VetChat Application",
                "Your Virtual Veterinary Assistant Awaits!",
                "Spring Boot Application"
        };

        // print top border
        System.out.println("*".repeat(width));

        // print empty line
        System.out.println("*" + " ".repeat(width - 2) + "*");

        // print centered lines
        for (String line : lines) {
            int padding = (width - 2 - line.length()) / 2;
            String formattedLine = "*" + " ".repeat(padding) + line;
            formattedLine += " ".repeat(width - 2 - formattedLine.length() + 1) + "*";
            System.out.println(formattedLine);
        }

        // print empty line
        System.out.println("*" + " ".repeat(width - 2) + "*");

        // print bottom border
        System.out.println("*".repeat(width));
    }

    /**
     * Displays a loading animation in the console with a progress bar.
     *
     * @param message the text to display before the progress bar
     * @param steps the number of progress steps to display
     * @param delayMillis the delay between each progress step in milliseconds
     * @throws InterruptedException if the animation thread is interrupted
     */
    private void showLoadingAnimation(String message, int steps, int delayMillis) throws InterruptedException {
        System.out.print(message + ": [");
        for (int i = 0; i < steps; i++) {
            System.out.print("=");
            Thread.sleep(delayMillis);
        }
        System.out.println("]");
    }
}

/**
 * Component responsible for managing the Python VQA (Visual Question Answering) server process.
 * Handles starting, monitoring, and stopping the external Python server.
 */
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

    /**
     * Event listener that starts the Python VQA server when the application is ready.
     * Configures the process environment, starts the server, and sets up output reading.
     */
    @EventListener(ApplicationReadyEvent.class)
    public void startPythonVQAServer() {
        logger.info("Starting Python VQA Server...");

        try {
            File scriptFile = new File(pythonScriptPath);
            if (!scriptFile.exists()) {
                logger.error("Python script not found at: {}", scriptFile.getAbsolutePath());
                return;
            }

            ProcessBuilder processBuilder = new ProcessBuilder(pythonExecutable, pythonScriptPath);
            processBuilder.environment().put("PORT", vqaServerPort);
            processBuilder.environment().put("HOST", vqaServerHost);
            processBuilder.directory(scriptFile.getParentFile());
            processBuilder.redirectErrorStream(true);

            pythonProcess = processBuilder.start();

            startOutputReader();

            if (waitForServerReady()) {
                logger.info("Python VQA Server started successfully on {}:{}", vqaServerHost, vqaServerPort);
                Runtime.getRuntime().addShutdownHook(new Thread(this::stopPythonServer));
            } else {
                logger.error("Python VQA Server failed to start within {} seconds", startupTimeoutSeconds);
                stopPythonServer();
            }

        } catch (Exception e) {
            logger.error("Failed to start Python VQA Server: {}", e.getMessage(), e);
        }
    }

    /**
     * Starts a background thread to read and log output from the Python server process.
     * Categorizes log messages based on their severity level.
     */
    private void startOutputReader() {
        Thread outputReaderThread = new Thread(() -> {
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(pythonProcess.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
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

    /**
     * Waits for the Python server to become ready by performing health checks at intervals.
     *
     * @return true if the server becomes ready within the configured timeout, false otherwise
     */
    private boolean waitForServerReady() {
        logger.info("Waiting for Python VQA Server to be ready...");

        for (int i = 0; i < healthCheckRetries; i++) {
            try {
                if (!pythonProcess.isAlive()) {
                    logger.error("Python process exited unexpectedly with code: {}", pythonProcess.exitValue());
                    return false;
                }

                if (checkServerHealth()) {
                    return true;
                }

                logger.debug("Health check {}/{} failed, waiting {} seconds...", i + 1, healthCheckRetries,
                        healthCheckIntervalSeconds);
                Thread.sleep(healthCheckIntervalSeconds * 1000L);

            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                logger.error("Interrupted while waiting for Python server to start");
                return false;
            }
        }

        return false;
    }

    /**
     * Performs a health check on the Python server by making an HTTP request to its health endpoint.
     *
     * @return true if the health check succeeds (HTTP 200 response), false otherwise
     */
    private boolean checkServerHealth() {
        try {
            ProcessBuilder healthCheck = new ProcessBuilder(
                    "curl", "-f", "-s", "--max-time", "5",
                    String.format("http://%s:%s/health", vqaServerHost, vqaServerPort));

            Process process = healthCheck.start();
            boolean finished = process.waitFor(5, TimeUnit.SECONDS);

            return finished && process.exitValue() == 0;

        } catch (Exception e) {
            logger.debug("Health check failed: {}", e.getMessage());
        }

        return false;
    }

    /**
     * Stops the Python server process gracefully, with a fallback to forced termination.
     * Attempts graceful shutdown first, then forces termination if necessary.
     */
    public void stopPythonServer() {
        if (pythonProcess != null && pythonProcess.isAlive()) {
            logger.info("Stopping Python VQA Server...");
            try {
                pythonProcess.destroy();
                if (!pythonProcess.waitFor(10, TimeUnit.SECONDS)) {
                    logger.warn("Python server didn't stop gracefully, forcing termination...");
                    pythonProcess.destroyForcibly();
                    pythonProcess.waitFor(5, TimeUnit.SECONDS);
                }
                logger.info("Python VQA Server stopped");
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                pythonProcess.destroyForcibly();
                logger.error("Interrupted while stopping Python server");
            }
        }
    }

    /**
     * Checks if the Python server is currently running and responsive.
     *
     * @return true if the server process is alive and health checks succeed, false otherwise
     */
    public boolean isServerRunning() {
        return pythonProcess != null && pythonProcess.isAlive() && checkServerHealth();
    }
}

/**
 * Component that periodically monitors the health of the Python VQA server.
 * Runs scheduled health checks and logs warnings if the server appears to be down.
 */
@Component
class VQAServerHealthMonitor {

    private static final Logger logger = LoggerFactory.getLogger(VQAServerHealthMonitor.class);
    private final PythonVQAServerManager serverManager;

    /**
     * Constructs a VQAServerHealthMonitor with the specified server manager.
     *
     * @param serverManager the PythonVQAServerManager instance to monitor
     */
    public VQAServerHealthMonitor(PythonVQAServerManager serverManager) {
        this.serverManager = serverManager;
    }

    /**
     * Scheduled method that performs health checks on the Python VQA server.
     * Runs every 60 seconds and logs warnings if the server is not responsive.
     */
    @Scheduled(fixedRate = 60000)
    public void performHealthCheck() {
        if (!serverManager.isServerRunning()) {
            logger.warn("Python VQA Server appears to be down. Consider restarting the application.");
        }
    }
}