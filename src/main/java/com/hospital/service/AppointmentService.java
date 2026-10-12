package com.hospital.service;

import com.hospital.dao.AppointmentDao;
import com.hospital.dao.DoctorDao;
import com.hospital.dao.PatientDao;
import com.hospital.exception.AppointmentConflictException;
import com.hospital.exception.BusinessValidationException;
import com.hospital.exception.ResourceNotFoundException;
import com.hospital.model.Appointment;
import com.hospital.model.AppointmentFilter;
import com.hospital.model.Doctor;
import com.hospital.model.Patient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Set;

@Service
public class AppointmentService {

    private static final Set<String> ALLOWED_STATUSES = Set.of("SCHEDULED", "COMPLETED", "CANCELLED", "NO_SHOW");
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("dd-MMM-yyyy HH:mm");

    private final AppointmentDao appointmentDao;
    private final DoctorDao doctorDao;
    private final PatientDao patientDao;

    public AppointmentService(AppointmentDao appointmentDao, DoctorDao doctorDao, PatientDao patientDao) {
        this.appointmentDao = appointmentDao;
        this.doctorDao = doctorDao;
        this.patientDao = patientDao;
    }

    public List<Appointment> getAllAppointments() {
        return appointmentDao.findAllWithDetails();
    }

    public Appointment getAppointmentById(Long appointmentId) {
        return appointmentDao.findById(appointmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Appointment not found with ID: " + appointmentId));
    }

    public List<Appointment> getUpcomingAppointments(int limit) {
        return appointmentDao.findUpcomingAppointments(limit);
    }

    public List<Appointment> getAppointmentsByPatient(Long patientId) {
        return appointmentDao.findAppointmentsByPatient(patientId);
    }

    public List<Appointment> getAppointmentsByDoctor(Long doctorId) {
        return appointmentDao.findAppointmentsByDoctor(doctorId);
    }

    public List<Appointment> filterAppointments(AppointmentFilter filter) {
        return appointmentDao.filterAppointments(filter);
    }

    public List<Appointment> getDailyReport(LocalDate reportDate) {
        LocalDate date = (reportDate != null) ? reportDate : LocalDate.now();
        return appointmentDao.findDailyReport(date);
    }

    /**
     * Books an appointment with transactional row-level concurrency lock on the Doctor record,
     * verifying doctor active state, patient existence, and strict 30-minute slot non-overlap.
     */
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public Long bookAppointment(Appointment appointment) {
        if (appointment.getPatientId() == null) {
            throw new BusinessValidationException("A patient must be selected.");
        }
        if (appointment.getDoctorId() == null) {
            throw new BusinessValidationException("A doctor must be selected.");
        }
        if (appointment.getAppointmentDatetime() == null) {
            throw new BusinessValidationException("Appointment date and time must be provided.");
        }

        // Verify patient exists
        Patient patient = patientDao.findById(appointment.getPatientId())
                .orElseThrow(() -> new ResourceNotFoundException("Selected patient #" + appointment.getPatientId() + " does not exist."));

        // Acquire pessimistic lock on the Doctor record to serialize concurrent bookings for this doctor
        Doctor doctor = doctorDao.lockDoctorForBooking(appointment.getDoctorId())
                .orElseThrow(() -> new ResourceNotFoundException("Selected doctor #" + appointment.getDoctorId() + " does not exist."));

        // Verify doctor is currently active
        if (!doctor.isActiveDoctor()) {
            throw new BusinessValidationException("Cannot book appointment: Dr. " + doctor.getFullName() + " is currently inactive.");
        }

        // Check for 30-minute interval conflict
        boolean conflict = appointmentDao.checkConflict30Minutes(
                appointment.getDoctorId(),
                appointment.getAppointmentDatetime(),
                null
        );

        if (conflict) {
            String timeStr = appointment.getAppointmentDatetime().format(FORMATTER);
            throw new AppointmentConflictException(
                "Scheduling conflict: Dr. " + doctor.getFullName() + " already has an appointment within 30 minutes of " +
                timeStr + ". Consecutive appointments require at least 30 minutes separation."
            );
        }

        if (appointment.getStatus() == null || appointment.getStatus().isBlank()) {
            appointment.setStatus("SCHEDULED");
        }

        return appointmentDao.createAppointment(appointment);
    }

    /**
     * Reschedules an existing appointment, verifying 30-minute slot availability.
     */
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public void rescheduleAppointment(Long appointmentId, LocalDateTime newDateTime, String reason) {
        Appointment existing = getAppointmentById(appointmentId);

        if (newDateTime == null) {
            throw new BusinessValidationException("New appointment date and time is required.");
        }

        // Lock doctor row
        Doctor doctor = doctorDao.lockDoctorForBooking(existing.getDoctorId())
                .orElseThrow(() -> new ResourceNotFoundException("Associated doctor #" + existing.getDoctorId() + " not found."));

        // Check 30-minute overlap excluding the current appointment
        boolean conflict = appointmentDao.checkConflict30Minutes(
                existing.getDoctorId(),
                newDateTime,
                appointmentId
        );

        if (conflict) {
            String timeStr = newDateTime.format(FORMATTER);
            throw new AppointmentConflictException(
                "Cannot reschedule: Dr. " + doctor.getFullName() + " already has a conflicting appointment within 30 minutes of " + timeStr + "."
            );
        }

        String updatedReason = (reason != null && !reason.isBlank()) ? reason : existing.getReason();
        appointmentDao.rescheduleAppointment(appointmentId, newDateTime, updatedReason);
    }

    @Transactional
    public void updateStatus(Long appointmentId, String newStatus) {
        if (newStatus == null || !ALLOWED_STATUSES.contains(newStatus.toUpperCase())) {
            throw new BusinessValidationException("Invalid appointment status: " + newStatus + ". Allowed: " + ALLOWED_STATUSES);
        }
        getAppointmentById(appointmentId);
        appointmentDao.updateStatus(appointmentId, newStatus.toUpperCase());
    }

    @Transactional
    public void deleteAppointment(Long appointmentId) {
        getAppointmentById(appointmentId);
        appointmentDao.deleteAppointment(appointmentId);
    }
}
