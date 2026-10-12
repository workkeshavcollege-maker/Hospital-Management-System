package com.hospital.controller;

import com.hospital.model.Appointment;
import com.hospital.model.Doctor;
import com.hospital.service.AppointmentService;
import com.hospital.service.DoctorService;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
@RequestMapping("/doctors")
public class DoctorController {

    private final DoctorService doctorService;
    private final AppointmentService appointmentService;

    public DoctorController(DoctorService doctorService, AppointmentService appointmentService) {
        this.doctorService = doctorService;
        this.appointmentService = appointmentService;
    }

    @GetMapping
    public String listDoctors(@RequestParam(value = "term", required = false) String term,
                              @RequestParam(value = "status", required = false) String status,
                              Model model) {
        List<Doctor> doctors = doctorService.searchDoctors(term, status);
        model.addAttribute("doctors", doctors);
        model.addAttribute("term", term);
        model.addAttribute("status", status != null ? status : "ALL");
        return "doctors/list";
    }

    @GetMapping("/new")
    public String newDoctorForm(Model model) {
        model.addAttribute("doctor", new Doctor());
        model.addAttribute("isEdit", false);
        return "doctors/form";
    }

    @PostMapping("/new")
    public String createDoctor(@Valid @ModelAttribute("doctor") Doctor doctor,
                               BindingResult bindingResult,
                               RedirectAttributes redirectAttributes,
                               Model model) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("isEdit", false);
            return "doctors/form";
        }

        try {
            Long newId = doctorService.createDoctor(doctor);
            redirectAttributes.addFlashAttribute("successMessage", "Doctor '" + doctor.getFullName() + "' registered successfully with ID #" + newId + ".");
            return "redirect:/doctors";
        } catch (Exception e) {
            model.addAttribute("errorMessage", "Error registering doctor: " + e.getMessage());
            model.addAttribute("isEdit", false);
            return "doctors/form";
        }
    }

    @GetMapping("/{id}")
    public String viewDoctor(@PathVariable("id") Long id, Model model) {
        Doctor doctor = doctorService.getDoctorById(id);
        List<Appointment> appointments = appointmentService.getAppointmentsByDoctor(id);

        model.addAttribute("doctor", doctor);
        model.addAttribute("appointments", appointments);
        return "doctors/view";
    }

    @GetMapping("/{id}/edit")
    public String editDoctorForm(@PathVariable("id") Long id, Model model) {
        Doctor doctor = doctorService.getDoctorById(id);
        model.addAttribute("doctor", doctor);
        model.addAttribute("isEdit", true);
        return "doctors/form";
    }

    @PostMapping("/{id}/edit")
    public String updateDoctor(@PathVariable("id") Long id,
                               @Valid @ModelAttribute("doctor") Doctor doctor,
                               BindingResult bindingResult,
                               RedirectAttributes redirectAttributes,
                               Model model) {
        doctor.setDoctorId(id);
        if (bindingResult.hasErrors()) {
            model.addAttribute("isEdit", true);
            return "doctors/form";
        }

        try {
            doctorService.updateDoctor(doctor);
            redirectAttributes.addFlashAttribute("successMessage", "Dr. " + doctor.getFullName() + "'s details updated successfully.");
            return "redirect:/doctors/" + id;
        } catch (Exception e) {
            model.addAttribute("errorMessage", "Error updating doctor: " + e.getMessage());
            model.addAttribute("isEdit", true);
            return "doctors/form";
        }
    }

    @PostMapping("/{id}/toggle-status")
    public String toggleStatus(@PathVariable("id") Long id, RedirectAttributes redirectAttributes) {
        try {
            doctorService.toggleActive(id);
            Doctor doctor = doctorService.getDoctorById(id);
            String state = doctor.isActiveDoctor() ? "ACTIVATED" : "DEACTIVATED";
            redirectAttributes.addFlashAttribute("successMessage", "Dr. " + doctor.getFullName() + " status updated to " + state + ".");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Could not toggle status: " + e.getMessage());
        }
        return "redirect:/doctors";
    }
}
