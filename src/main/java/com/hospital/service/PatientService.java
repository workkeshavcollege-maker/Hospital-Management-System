package com.hospital.service;

import com.hospital.dao.PatientDao;
import com.hospital.exception.BusinessValidationException;
import com.hospital.exception.ResourceNotFoundException;
import com.hospital.model.Patient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class PatientService {

    private final PatientDao patientDao;

    public PatientService(PatientDao patientDao) {
        this.patientDao = patientDao;
    }

    public List<Patient> getAllPatients() {
        return patientDao.findAll();
    }

    public List<Patient> searchPatients(String term) {
        return patientDao.searchPatients(term);
    }

    public Patient getPatientById(Long patientId) {
        return patientDao.findById(patientId)
                .orElseThrow(() -> new ResourceNotFoundException("Patient not found with ID: " + patientId));
    }

    @Transactional
    public Long createPatient(Patient patient) {
        validatePatient(patient);
        return patientDao.createPatient(patient);
    }

    @Transactional
    public void updatePatient(Patient patient) {
        if (patient.getPatientId() == null) {
            throw new BusinessValidationException("Patient ID is required for update.");
        }
        // Verify patient exists
        getPatientById(patient.getPatientId());
        validatePatient(patient);
        patientDao.updatePatient(patient);
    }

    @Transactional
    public void deletePatient(Long patientId) {
        // Verify existence
        getPatientById(patientId);

        // Check foreign key constraint before deletion
        if (patientDao.hasAppointments(patientId)) {
            throw new BusinessValidationException(
                "Cannot delete patient #" + patientId + " because existing appointment records reference this patient. " +
                "Cancel or reassign the appointments before removing this patient record."
            );
        }

        patientDao.deletePatient(patientId);
    }

    private void validatePatient(Patient patient) {
        if (patient.getFullName() == null || patient.getFullName().trim().isEmpty()) {
            throw new BusinessValidationException("Patient full name is mandatory.");
        }
        if (patient.getPhone() == null || patient.getPhone().trim().isEmpty()) {
            throw new BusinessValidationException("Patient contact phone number is mandatory.");
        }
    }
}
