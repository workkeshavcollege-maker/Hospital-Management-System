package com.hospital.config;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Utility to load environment variables from a local .env file inside the project root
 * into Java System properties if they are not already set in the system environment.
 * Ensures seamless local development and macOS execution without external dependencies.
 */
public final class DotenvLoader {

    private static final Logger logger = LoggerFactory.getLogger(DotenvLoader.class);

    private DotenvLoader() {}

    public static void load() {
        File envFile = new File(".env");
        if (!envFile.exists() || !envFile.isFile()) {
            logger.info("No local .env file found in project root. Relying on system environment variables.");
            return;
        }

        logger.info("Loading environment configurations from local .env file: {}", envFile.getAbsolutePath());
        try (BufferedReader reader = new BufferedReader(new FileReader(envFile, StandardCharsets.UTF_8))) {
            String line;
            int loadedCount = 0;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                // Skip empty lines and comments
                if (line.isEmpty() || line.startsWith("#")) {
                    continue;
                }
                int eqIndex = line.indexOf('=');
                if (eqIndex > 0) {
                    String key = line.substring(0, eqIndex).trim();
                    String value = line.substring(eqIndex + 1).trim();

                    // Strip surrounding quotes if present
                    if ((value.startsWith("\"") && value.endsWith("\"")) ||
                        (value.startsWith("'") && value.endsWith("'"))) {
                        if (value.length() >= 2) {
                            value = value.substring(1, value.length() - 1);
                        }
                    }

                    // Only set property if not already provided in OS environment or system properties
                    if (System.getenv(key) == null && System.getProperty(key) == null) {
                        System.setProperty(key, value);
                        loadedCount++;
                    }
                }
            }
            logger.info("Loaded {} environment variables into System properties from .env", loadedCount);
        } catch (IOException e) {
            logger.warn("Could not read .env file: {}", e.getMessage());
        }
    }
}
