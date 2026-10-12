package com.hospital.service;

import com.hospital.dao.UserDao;
import com.hospital.exception.BusinessValidationException;
import com.hospital.exception.ResourceNotFoundException;
import com.hospital.model.AppUser;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;

@Service
public class UserService {

    private static final Set<String> ALLOWED_ROLES = Set.of("ADMIN", "RECEPTIONIST", "DOCTOR");

    private final UserDao userDao;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserDao userDao, PasswordEncoder passwordEncoder) {
        this.userDao = userDao;
        this.passwordEncoder = passwordEncoder;
    }

    public List<AppUser> getAllUsers() {
        return userDao.findAll();
    }

    public AppUser getUserById(Long userId) {
        return userDao.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with ID: " + userId));
    }

    @Transactional
    public Long createUser(String username, String rawPassword, String fullName, String role) {
        if (username == null || username.trim().isEmpty()) {
            throw new BusinessValidationException("Username is required.");
        }
        if (rawPassword == null || rawPassword.trim().length() < 6) {
            throw new BusinessValidationException("Password must be at least 6 characters long.");
        }
        if (fullName == null || fullName.trim().isEmpty()) {
            throw new BusinessValidationException("Full name is required.");
        }
        if (role == null || !ALLOWED_ROLES.contains(role.toUpperCase())) {
            throw new BusinessValidationException("Role must be one of: " + ALLOWED_ROLES);
        }

        if (userDao.findByUsername(username.trim()).isPresent()) {
            throw new BusinessValidationException("Username '" + username + "' is already taken.");
        }

        AppUser user = new AppUser();
        user.setUsername(username.trim());
        user.setPasswordHash(passwordEncoder.encode(rawPassword));
        user.setFullName(fullName.trim());
        user.setUserRole(role.toUpperCase());
        user.setIsActive("Y");

        userDao.createUser(user);
        return user.getUserId();
    }

    @Transactional
    public void toggleUserStatus(Long userId) {
        AppUser user = getUserById(userId);
        String newStatus = "Y".equalsIgnoreCase(user.getIsActive()) ? "N" : "Y";
        userDao.updateUserStatus(userId, newStatus);
    }

    @Transactional
    public void resetPassword(Long userId, String newRawPassword) {
        if (newRawPassword == null || newRawPassword.trim().length() < 6) {
            throw new BusinessValidationException("Password must be at least 6 characters long.");
        }
        getUserById(userId);
        String hash = passwordEncoder.encode(newRawPassword);
        userDao.updatePassword(userId, hash);
    }
}
