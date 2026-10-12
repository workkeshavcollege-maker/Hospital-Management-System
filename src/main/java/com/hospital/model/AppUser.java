package com.hospital.model;

import java.time.LocalDateTime;

/**
 * Model representing an application user in the APP_USERS table.
 * Roles: ADMIN, RECEPTIONIST, DOCTOR.
 * Active flag: Y / N.
 */
public class AppUser {
    private Long userId;
    private String username;
    private String passwordHash;
    private String fullName;
    private String userRole;
    private String isActive;
    private LocalDateTime createdAt;

    public AppUser() {}

    public AppUser(Long userId, String username, String passwordHash, String fullName, String userRole, String isActive, LocalDateTime createdAt) {
        this.userId = userId;
        this.username = username;
        this.passwordHash = passwordHash;
        this.fullName = fullName;
        this.userRole = userRole;
        this.isActive = isActive;
        this.createdAt = createdAt;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getUserRole() {
        return userRole;
    }

    public void setUserRole(String userRole) {
        this.userRole = userRole;
    }

    public String getIsActive() {
        return isActive;
    }

    public void setIsActive(String isActive) {
        this.isActive = isActive;
    }

    public boolean isActiveUser() {
        return "Y".equalsIgnoreCase(this.isActive);
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
