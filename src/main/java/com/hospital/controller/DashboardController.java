package com.hospital.controller;

import com.hospital.model.Appointment;
import com.hospital.model.DashboardStats;
import com.hospital.service.AppointmentService;
import com.hospital.service.DashboardService;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.List;

@Controller
public class DashboardController {

    private final DashboardService dashboardService;
    private final AppointmentService appointmentService;

    public DashboardController(DashboardService dashboardService, AppointmentService appointmentService) {
        this.dashboardService = dashboardService;
        this.appointmentService = appointmentService;
    }

    @GetMapping({"/", "/dashboard"})
    public String dashboard(Authentication authentication, Model model) {
        try {
            DashboardStats stats = dashboardService.getDashboardStats();
            List<Appointment> upcoming = appointmentService.getUpcomingAppointments(8);

            model.addAttribute("stats", stats);
            model.addAttribute("upcomingAppointments", upcoming);
            model.addAttribute("username", authentication != null ? authentication.getName() : "User");
        } catch (Exception e) {
            model.addAttribute("dbErrorMessage", "Database connection issue: " + e.getMessage());
            model.addAttribute("stats", new DashboardStats());
            model.addAttribute("upcomingAppointments", List.of());
        }

        return "dashboard";
    }
}
