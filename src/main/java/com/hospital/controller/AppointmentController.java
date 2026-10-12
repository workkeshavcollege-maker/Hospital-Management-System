package com.hospital.controller;

import com.hospital.exception.AppointmentConflictException;
import com.hospital.exception.BusinessValidationException;
import com.hospital.model.Appointment;
import com.hospital.model.AppointmentFilter;
import com.hospital.service.AppointmentService;
import com.hospital.service.DoctorService;
import com.hospital.service.PatientService;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Controller
@RequestMapping("/appointments")
public class AppointmentController {

    private final AppointmentService appointmentService;
    private final DoctorService doctorService;
    private final PatientService patientService;

    public AppointmentController(AppointmentService appointmentService, DoctorService doctorService, PatientService patientService) {
        this.appointmentService = appointmentService;
        this.doctorService = doctorService;
        this.patientService = patientService;
    }

    @GetMapping
    public String listAppointments(@ModelAttribute("filter") AppointmentFilter filter, Model model) {
        List<Appointment> appointments = appointmentService.filterAppointments(filter);
        model.addAttribute("appointments", appointments);
        model.addAttribute("doctors", doctorService.getAllDoctors());
        model.addAttribute("patients", patientService.getAllPatients());
        return "appointments/list";
    }

    @GetMapping("/book")
    public String bookForm(@RequestParam(value = "patientId", required = false) Long patientId,
                           @RequestParam(value = "doctorId", required = false) Long doctorId,
                           Model model) {
        Appointment appt = new Appointment();
        if (patientId != null) appt.setPatientId(patientId);
        if (doctorId != null) appt.setDoctorId(doctorId);
        appt.setAppointmentDatetime(LocalDateTime.now().plusHours(1).withMinute(0).withSecond(0));

        model.addAttribute("appointment", appt);
        model.addAttribute("activeDoctors", doctorService.getActiveDoctors());
        model.addAttribute("patients", patientService.getAllPatients());
        return "appointments/book";
    }

    @PostMapping("/book")
    public String processBooking(@Valid @ModelAttribute("appointment") Appointment appointment,
                                 BindingResult bindingResult,
                                 RedirectAttributes redirectAttributes,
                                 Model model) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("activeDoctors", doctorService.getActiveDoctors());
            model.addAttribute("patients", patientService.getAllPatients());
            return "appointments/book";
        }

        try {
            Long newId = appointmentService.bookAppointment(appointment);
            redirectAttributes.addFlashAttribute("successMessage", "Appointment successfully booked with ID #" + newId + ".");
            return "redirect:/appointments";
        } catch (AppointmentConflictException | BusinessValidationException e) {
            model.addAttribute("errorMessage", e.getMessage());
            model.addAttribute("activeDoctors", doctorService.getActiveDoctors());
            model.addAttribute("patients", patientService.getAllPatients());
            return "appointments/book";
        } catch (Exception e) {
            model.addAttribute("errorMessage", "Error booking appointment: " + e.getMessage());
            model.addAttribute("activeDoctors", doctorService.getActiveDoctors());
            model.addAttribute("patients", patientService.getAllPatients());
            return "appointments/book";
        }
    }

    @GetMapping("/{id}/reschedule")
    public String rescheduleForm(@PathVariable("id") Long id, Model model) {
        Appointment existing = appointmentService.getAppointmentById(id);
        model.addAttribute("appointment", existing);
        return "appointments/reschedule";
    }

    @PostMapping("/{id}/reschedule")
    public String processReschedule(@PathVariable("id") Long id,
                                    @RequestParam("newDateTime") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime newDateTime,
                                    @RequestParam(value = "reason", required = false) String reason,
                                    RedirectAttributes redirectAttributes,
                                    Model model) {
        try {
            appointmentService.rescheduleAppointment(id, newDateTime, reason);
            redirectAttributes.addFlashAttribute("successMessage", "Appointment #" + id + " rescheduled successfully.");
            return "redirect:/appointments";
        } catch (AppointmentConflictException | BusinessValidationException e) {
            Appointment existing = appointmentService.getAppointmentById(id);
            model.addAttribute("appointment", existing);
            model.addAttribute("errorMessage", e.getMessage());
            return "appointments/reschedule";
        } catch (Exception e) {
            Appointment existing = appointmentService.getAppointmentById(id);
            model.addAttribute("appointment", existing);
            model.addAttribute("errorMessage", "Could not reschedule: " + e.getMessage());
            return "appointments/reschedule";
        }
    }

    @PostMapping("/{id}/status")
    public String updateStatus(@PathVariable("id") Long id,
                               @RequestParam("status") String status,
                               RedirectAttributes redirectAttributes) {
        try {
            appointmentService.updateStatus(id, status);
            redirectAttributes.addFlashAttribute("successMessage", "Appointment #" + id + " marked as " + status + ".");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Error updating status: " + e.getMessage());
        }
        return "redirect:/appointments";
    }

    @PostMapping("/{id}/delete")
    public String deleteAppointment(@PathVariable("id") Long id, RedirectAttributes redirectAttributes) {
        try {
            appointmentService.deleteAppointment(id);
            redirectAttributes.addFlashAttribute("successMessage", "Appointment #" + id + " deleted.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Error deleting appointment: " + e.getMessage());
        }
        return "redirect:/appointments";
    }

    @GetMapping("/report")
    public String dailyReport(@RequestParam(value = "reportDate", required = false)
                              @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate reportDate,
                              Model model) {
        LocalDate targetDate = (reportDate != null) ? reportDate : LocalDate.now();
        List<Appointment> reportList = appointmentService.getDailyReport(targetDate);

        BigDecimal totalRevenue = BigDecimal.ZERO;
        long completedCount = 0;
        long scheduledCount = 0;
        long cancelledCount = 0;

        for (Appointment a : reportList) {
            if ("COMPLETED".equalsIgnoreCase(a.getStatus())) {
                completedCount++;
                if (a.getDoctorFee() != null) {
                    totalRevenue = totalRevenue.add(a.getDoctorFee());
                }
            } else if ("SCHEDULED".equalsIgnoreCase(a.getStatus())) {
                scheduledCount++;
            } else if ("CANCELLED".equalsIgnoreCase(a.getStatus())) {
                cancelledCount++;
            }
        }

        model.addAttribute("reportDate", targetDate);
        model.addAttribute("appointments", reportList);
        model.addAttribute("totalAppointments", reportList.size());
        model.addAttribute("completedCount", completedCount);
        model.addAttribute("scheduledCount", scheduledCount);
        model.addAttribute("cancelledCount", cancelledCount);
        model.addAttribute("totalRevenue", totalRevenue);

        return "appointments/report";
    }
}
