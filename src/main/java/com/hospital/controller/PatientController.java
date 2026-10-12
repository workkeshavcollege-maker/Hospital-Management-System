package com.hospital.controller;

import com.hospital.exception.BusinessValidationException;
import com.hospital.model.Appointment;
import com.hospital.model.Patient;
import com.hospital.service.AppointmentService;
import com.hospital.service.PatientService;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
@RequestMapping("/patients")
public class PatientController {

    private final PatientService patientService;
    private final AppointmentService appointmentService;

    public PatientController(PatientService patientService, AppointmentService appointmentService) {
        this.patientService = patientService;
        this.appointmentService = appointmentService;
    }

    @GetMapping
    public String listPatients(@RequestParam(value = "term", required = false) String term, Model model) {
        List<Patient> patients = (term != null && !term.isBlank())
                ? patientService.searchPatients(term)
                : patientService.getAllPatients();

        model.addAttribute("patients", patients);
        model.addAttribute("term", term);
        return "patients/list";
    }

    @GetMapping("/new")
    public String newPatientForm(Model model) {
        model.addAttribute("patient", new Patient());
        model.addAttribute("isEdit", false);
        return "patients/form";
    }

    @PostMapping("/new")
    public String createPatient(@Valid @ModelAttribute("patient") Patient patient,
                                BindingResult bindingResult,
                                RedirectAttributes redirectAttributes,
                                Model model) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("isEdit", false);
            return "patients/form";
        }

        try {
            Long newId = patientService.createPatient(patient);
            redirectAttributes.addFlashAttribute("successMessage", "Patient '" + patient.getFullName() + "' successfully registered with ID #" + newId + ".");
            return "redirect:/patients";
        } catch (Exception e) {
            model.addAttribute("errorMessage", "Error saving patient: " + e.getMessage());
            model.addAttribute("isEdit", false);
            return "patients/form";
        }
    }

    @GetMapping("/{id}")
    public String viewPatient(@PathVariable("id") Long id, Model model) {
        Patient patient = patientService.getPatientById(id);
        List<Appointment> appointments = appointmentService.getAppointmentsByPatient(id);

        model.addAttribute("patient", patient);
        model.addAttribute("appointments", appointments);
        return "patients/view";
    }

    @GetMapping("/{id}/edit")
    public String editPatientForm(@PathVariable("id") Long id, Model model) {
        Patient patient = patientService.getPatientById(id);
        model.addAttribute("patient", patient);
        model.addAttribute("isEdit", true);
        return "patients/form";
    }

    @PostMapping("/{id}/edit")
    public String updatePatient(@PathVariable("id") Long id,
                                @Valid @ModelAttribute("patient") Patient patient,
                                BindingResult bindingResult,
                                RedirectAttributes redirectAttributes,
                                Model model) {
        patient.setPatientId(id);
        if (bindingResult.hasErrors()) {
            model.addAttribute("isEdit", true);
            return "patients/form";
        }

        try {
            patientService.updatePatient(patient);
            redirectAttributes.addFlashAttribute("successMessage", "Patient #" + id + " updated successfully.");
            return "redirect:/patients/" + id;
        } catch (Exception e) {
            model.addAttribute("errorMessage", "Error updating patient: " + e.getMessage());
            model.addAttribute("isEdit", true);
            return "patients/form";
        }
    }

    @PostMapping("/{id}/delete")
    public String deletePatient(@PathVariable("id") Long id, RedirectAttributes redirectAttributes) {
        try {
            patientService.deletePatient(id);
            redirectAttributes.addFlashAttribute("successMessage", "Patient #" + id + " was deleted successfully.");
        } catch (BusinessValidationException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Could not delete patient: " + e.getMessage());
        }
        return "redirect:/patients";
    }
}
