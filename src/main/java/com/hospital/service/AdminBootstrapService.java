package com.hospital.service;

import com.hospital.dao.UserDao;
import com.hospital.model.AppUser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

/**
 * Service to safely initialize the primary Administrator account if APP_USERS is empty.
 * Never overrides existing credentials.
 * Hashes passwords with BCrypt before persistence.
 */
@Service
public class AdminBootstrapService implements ApplicationRunner {

    private static final Logger logger = LoggerFactory.getLogger(AdminBootstrapService.class);

    private final UserDao userDao;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.bootstrap.username:admin}")
    private String bootstrapUsername;

    @Value("${app.bootstrap.password:Admin@123}")
    private String bootstrapPassword;

    public AdminBootstrapService(UserDao userDao, PasswordEncoder passwordEncoder) {
        this.userDao = userDao;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(ApplicationArguments args) {
        try {
            long totalUsers = userDao.countUsers();
            if (totalUsers == 0) {
                logger.info("APP_USERS table is empty. Bootstrapping initial administrator account...");

                AppUser adminUser = new AppUser();
                adminUser.setUsername(bootstrapUsername != null && !bootstrapUsername.isBlank() ? bootstrapUsername : "admin");
                adminUser.setPasswordHash(passwordEncoder.encode(bootstrapPassword != null && !bootstrapPassword.isBlank() ? bootstrapPassword : "Admin@123"));
                adminUser.setFullName("Chief Administrator");
                adminUser.setUserRole("ADMIN");
                adminUser.setIsActive("Y");

                userDao.createUser(adminUser);
                logger.info("Bootstrap complete: Initial administrator '{}' successfully created.", adminUser.getUsername());
            } else {
                long adminCount = userDao.countAdmins();
                logger.info("APP_USERS already contains {} user(s) (including {} active admin(s)). Bootstrap skipped.", totalUsers, adminCount);
            }
        } catch (Exception e) {
            logger.warn("Could not execute admin bootstrap on database startup: {}. Please verify Oracle connection settings.", e.getMessage());
        }
    }
}
