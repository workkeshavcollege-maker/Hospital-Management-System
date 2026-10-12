package com.hospital.config;

import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

import java.io.File;

/**
 * Handles Oracle-specific connection setup such as auto-discovering
 * local Oracle Cloud / Autonomous FreeSQL Wallets placed in the project root.
 */
@Configuration
public class OracleConnectionConfig {

    private static final Logger logger = LoggerFactory.getLogger(OracleConnectionConfig.class);

    @Value("${spring.datasource.url}")
    private String jdbcUrl;

    @Value("${spring.datasource.username}")
    private String dbUsername;

    @PostConstruct
    public void setupOracleWalletAndLog() {
        // Auto-detect project-root wallet directory if TNS_ADMIN is not explicitly set
        String currentTns = System.getProperty("oracle.net.tns_admin");
        if (currentTns == null || currentTns.isBlank()) {
            String envTns = System.getenv("ORACLE_TNS_ADMIN");
            if (envTns != null && !envTns.isBlank()) {
                System.setProperty("oracle.net.tns_admin", envTns);
                logger.info("Configured oracle.net.tns_admin from ORACLE_TNS_ADMIN environment variable: {}", envTns);
            } else {
                File defaultWallet = new File("wallet");
                if (defaultWallet.exists() && defaultWallet.isDirectory()) {
                    System.setProperty("oracle.net.tns_admin", defaultWallet.getAbsolutePath());
                    logger.info("Auto-detected local wallet directory in project root. Set oracle.net.tns_admin to: {}", defaultWallet.getAbsolutePath());
                }
            }
        }

        logger.info("Initializing Oracle JDBC connection pool for user '{}' with URL: {}", dbUsername, maskJdbcUrl(jdbcUrl));
    }

    private String maskJdbcUrl(String url) {
        if (url == null) return "null";
        // Ensure passwords inside connection strings are masked if present
        return url.replaceAll("(?i)(password\\s*=\\s*)([^&;?\\s]+)", "$1****");
    }
}
