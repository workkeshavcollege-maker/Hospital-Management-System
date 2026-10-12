package com.hospital.service;

import com.hospital.dao.DoctorDao;
import com.hospital.exception.BusinessValidationException;
import com.hospital.exception.ResourceNotFoundException;
import com.hospital.model.Doctor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
public class DoctorService {

    private final DoctorDao doctorDao;

    public DoctorService(DoctorDao doctorDao) {
        this.doctorDao = doctorDao;
    }

    public List<Doctor> getAllDoctors() {
        return doctorDao.findAll();
    }

    public List<Doctor> getActiveDoctors() {
        return doctorDao.findActiveDoctors();
    }

    public List<Doctor> searchDoctors(String term, String status) {
        return doctorDao.searchDoctors(term, status);
    }

    public Doctor getDoctorById(Long doctorId) {
        return doctorDao.findById(doctorId)
                .orElseThrow(() -> new ResourceNotFoundException("Doctor not found with ID: " + doctorId));
    }

    @Transactional
    public Long createDoctor(Doctor doctor) {
        validateDoctor(doctor);
        return doctorDao.createDoctor(doctor);
    }

    @Transactional
    public void updateDoctor(Doctor doctor) {
        if (doctor.getDoctorId() == null) {
            throw new BusinessValidationException("Doctor ID is required for updating information.");
        }
        getDoctorById(doctor.getDoctorId());
        validateDoctor(doctor);
        doctorDao.updateDoctor(doctor);
    }

    @Transactional
    public void toggleActive(Long doctorId) {
        Doctor doc = getDoctorById(doctorId);
        String newStatus = "Y".equalsIgnoreCase(doc.getIsActive()) ? "N" : "Y";
        doctorDao.toggleActive(doctorId, newStatus);
    }

    private void validateDoctor(Doctor doctor) {
        if (doctor.getFullName() == null || doctor.getFullName().trim().isEmpty()) {
            throw new BusinessValidationException("Doctor full name is mandatory.");
        }
        if (doctor.getSpecialization() == null || doctor.getSpecialization().trim().isEmpty()) {
            throw new BusinessValidationException("Doctor specialization is mandatory.");
        }
        if (doctor.getConsultationFee() != null && doctor.getConsultationFee().compareTo(BigDecimal.ZERO) < 0) {
            throw new BusinessValidationException("Consultation fee must be non-negative.");
        }
    }
}
