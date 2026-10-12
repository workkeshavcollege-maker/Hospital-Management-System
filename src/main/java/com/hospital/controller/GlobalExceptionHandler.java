package com.hospital.controller;

import com.hospital.exception.BusinessValidationException;
import com.hospital.exception.ResourceNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import java.sql.SQLException;

@ControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(ResourceNotFoundException.class)
    public String handleNotFound(ResourceNotFoundException ex, Model model) {
        logger.warn("Resource not found: {}", ex.getMessage());
        model.addAttribute("errorCode", "404 - Not Found");
        model.addAttribute("errorMessage", ex.getMessage());
        return "error";
    }

    @ExceptionHandler(BusinessValidationException.class)
    public String handleValidation(BusinessValidationException ex, Model model) {
        logger.warn("Business validation error: {}", ex.getMessage());
        model.addAttribute("errorCode", "Validation Error");
        model.addAttribute("errorMessage", ex.getMessage());
        return "error";
    }

    @ExceptionHandler({DataAccessException.class, SQLException.class})
    public String handleDatabaseException(Exception ex, Model model) {
        logger.error("Database operation failed: {}", ex.getMessage(), ex);
        model.addAttribute("errorCode", "Database Error");
        model.addAttribute("errorMessage", 
            "An error occurred while communicating with the Oracle database. " +
            "Please check network connectivity, database schema, or constraint rules.");
        return "error";
    }

    @ExceptionHandler(Exception.class)
    public String handleGeneralException(Exception ex, Model model) {
        logger.error("Unhandled exception: {}", ex.getMessage(), ex);
        model.addAttribute("errorCode", "Application Error");
        model.addAttribute("errorMessage", "An unexpected system error occurred: " + ex.getMessage());
        return "error";
    }
}
