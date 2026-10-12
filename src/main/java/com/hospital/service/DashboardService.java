package com.hospital.service;

import com.hospital.dao.AppointmentDao;
import com.hospital.dao.DoctorDao;
import com.hospital.dao.PatientDao;
import com.hospital.model.DashboardStats;
import org.springframework.stereotype.Service;

@Service
public class DashboardService {

    private final PatientDao patientDao;
    private final DoctorDao doctorDao;
    private final AppointmentDao appointmentDao;

    public DashboardService(PatientDao patientDao, DoctorDao doctorDao, AppointmentDao appointmentDao) {
        this.patientDao = patientDao;
        this.doctorDao = doctorDao;
        this.appointmentDao = appointmentDao;
    }

    public DashboardStats getDashboardStats() {
        DashboardStats stats = new DashboardStats();
        stats.setTotalPatients(patientDao.countPatients());
        stats.setTotalActiveDoctors(doctorDao.countActiveDoctors());
        stats.setTotalAppointments(appointmentDao.countTotalAppointments());
        stats.setScheduledAppointments(appointmentDao.countByStatus("SCHEDULED"));
        stats.setCompletedAppointments(appointmentDao.countByStatus("COMPLETED"));
        stats.setCancelledAppointments(appointmentDao.countByStatus("CANCELLED"));
        stats.setTodayAppointments(appointmentDao.countTodayAppointments());
        return stats;
    }
}
