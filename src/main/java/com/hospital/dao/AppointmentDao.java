package com.hospital.dao;

import com.hospital.model.Appointment;
import com.hospital.model.AppointmentFilter;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.sql.PreparedStatement;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Repository
public class AppointmentDao {

    private final JdbcTemplate jdbcTemplate;

    public AppointmentDao(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    private final RowMapper<Appointment> appointmentWithDetailsRowMapper = (rs, rowNum) -> {
        Appointment a = new Appointment();
        a.setAppointmentId(rs.getLong("appointment_id"));
        a.setPatientId(rs.getLong("patient_id"));
        a.setDoctorId(rs.getLong("doctor_id"));

        Timestamp ts = rs.getTimestamp("appointment_datetime");
        if (ts != null) {
            a.setAppointmentDatetime(ts.toLocalDateTime());
        }

        a.setReason(rs.getString("reason"));
        a.setStatus(rs.getString("status"));

        Timestamp createdTs = rs.getTimestamp("created_at");
        if (createdTs != null) {
            a.setCreatedAt(createdTs.toLocalDateTime());
        }

        // Joined fields
        a.setPatientName(rs.getString("patient_name"));
        a.setPatientPhone(rs.getString("patient_phone"));
        a.setDoctorName(rs.getString("doctor_name"));
        a.setDoctorSpecialization(rs.getString("doctor_specialization"));
        a.setDoctorFee(rs.getBigDecimal("consultation_fee"));

        return a;
    };

    public Optional<Appointment> findById(Long appointmentId) {
        String sql = "SELECT a.appointment_id, a.patient_id, a.doctor_id, a.appointment_datetime, a.reason, a.status, a.created_at, " +
                     "p.full_name AS patient_name, p.phone AS patient_phone, " +
                     "d.full_name AS doctor_name, d.specialization AS doctor_specialization, d.consultation_fee " +
                     "FROM APPOINTMENTS a " +
                     "JOIN PATIENTS p ON a.patient_id = p.patient_id " +
                     "JOIN DOCTORS d ON a.doctor_id = d.doctor_id " +
                     "WHERE a.appointment_id = ?";
        try {
            Appointment a = jdbcTemplate.queryForObject(sql, appointmentWithDetailsRowMapper, appointmentId);
            return Optional.ofNullable(a);
        } catch (EmptyResultDataAccessException e) {
            return Optional.empty();
        }
    }

    public List<Appointment> findAllWithDetails() {
        String sql = "SELECT a.appointment_id, a.patient_id, a.doctor_id, a.appointment_datetime, a.reason, a.status, a.created_at, " +
                     "p.full_name AS patient_name, p.phone AS patient_phone, " +
                     "d.full_name AS doctor_name, d.specialization AS doctor_specialization, d.consultation_fee " +
                     "FROM APPOINTMENTS a " +
                     "JOIN PATIENTS p ON a.patient_id = p.patient_id " +
                     "JOIN DOCTORS d ON a.doctor_id = d.doctor_id " +
                     "ORDER BY a.appointment_datetime DESC";
        return jdbcTemplate.query(sql, appointmentWithDetailsRowMapper);
    }

    public List<Appointment> findUpcomingAppointments(int limit) {
        String sql = "SELECT a.appointment_id, a.patient_id, a.doctor_id, a.appointment_datetime, a.reason, a.status, a.created_at, " +
                     "p.full_name AS patient_name, p.phone AS patient_phone, " +
                     "d.full_name AS doctor_name, d.specialization AS doctor_specialization, d.consultation_fee " +
                     "FROM APPOINTMENTS a " +
                     "JOIN PATIENTS p ON a.patient_id = p.patient_id " +
                     "JOIN DOCTORS d ON a.doctor_id = d.doctor_id " +
                     "WHERE a.appointment_datetime >= CURRENT_TIMESTAMP " +
                     "ORDER BY a.appointment_datetime ASC FETCH FIRST ? ROWS ONLY";
        return jdbcTemplate.query(sql, appointmentWithDetailsRowMapper, limit);
    }

    public List<Appointment> findAppointmentsByPatient(Long patientId) {
        String sql = "SELECT a.appointment_id, a.patient_id, a.doctor_id, a.appointment_datetime, a.reason, a.status, a.created_at, " +
                     "p.full_name AS patient_name, p.phone AS patient_phone, " +
                     "d.full_name AS doctor_name, d.specialization AS doctor_specialization, d.consultation_fee " +
                     "FROM APPOINTMENTS a " +
                     "JOIN PATIENTS p ON a.patient_id = p.patient_id " +
                     "JOIN DOCTORS d ON a.doctor_id = d.doctor_id " +
                     "WHERE a.patient_id = ? " +
                     "ORDER BY a.appointment_datetime DESC";
        return jdbcTemplate.query(sql, appointmentWithDetailsRowMapper, patientId);
    }

    public List<Appointment> findAppointmentsByDoctor(Long doctorId) {
        String sql = "SELECT a.appointment_id, a.patient_id, a.doctor_id, a.appointment_datetime, a.reason, a.status, a.created_at, " +
                     "p.full_name AS patient_name, p.phone AS patient_phone, " +
                     "d.full_name AS doctor_name, d.specialization AS doctor_specialization, d.consultation_fee " +
                     "FROM APPOINTMENTS a " +
                     "JOIN PATIENTS p ON a.patient_id = p.patient_id " +
                     "JOIN DOCTORS d ON a.doctor_id = d.doctor_id " +
                     "WHERE a.doctor_id = ? " +
                     "ORDER BY a.appointment_datetime DESC";
        return jdbcTemplate.query(sql, appointmentWithDetailsRowMapper, doctorId);
    }

    public List<Appointment> filterAppointments(AppointmentFilter filter) {
        StringBuilder sql = new StringBuilder(
            "SELECT a.appointment_id, a.patient_id, a.doctor_id, a.appointment_datetime, a.reason, a.status, a.created_at, " +
            "p.full_name AS patient_name, p.phone AS patient_phone, " +
            "d.full_name AS doctor_name, d.specialization AS doctor_specialization, d.consultation_fee " +
            "FROM APPOINTMENTS a " +
            "JOIN PATIENTS p ON a.patient_id = p.patient_id " +
            "JOIN DOCTORS d ON a.doctor_id = d.doctor_id " +
            "WHERE 1=1 "
        );
        List<Object> params = new ArrayList<>();

        if (filter.getDoctorId() != null) {
            sql.append("AND a.doctor_id = ? ");
            params.add(filter.getDoctorId());
        }

        if (filter.getPatientId() != null) {
            sql.append("AND a.patient_id = ? ");
            params.add(filter.getPatientId());
        }

        if (filter.getStatus() != null && !filter.getStatus().trim().isEmpty() && !filter.getStatus().equalsIgnoreCase("ALL")) {
            sql.append("AND a.status = ? ");
            params.add(filter.getStatus().trim().toUpperCase());
        }

        if (filter.getStartDate() != null) {
            sql.append("AND a.appointment_datetime >= ? ");
            params.add(Timestamp.valueOf(filter.getStartDate().atStartOfDay()));
        }

        if (filter.getEndDate() != null) {
            sql.append("AND a.appointment_datetime <= ? ");
            params.add(Timestamp.valueOf(filter.getEndDate().atTime(23, 59, 59)));
        }

        sql.append("ORDER BY a.appointment_datetime DESC");
        return jdbcTemplate.query(sql.toString(), appointmentWithDetailsRowMapper, params.toArray());
    }

    public List<Appointment> findDailyReport(LocalDate reportDate) {
        String sql = "SELECT a.appointment_id, a.patient_id, a.doctor_id, a.appointment_datetime, a.reason, a.status, a.created_at, " +
                     "p.full_name AS patient_name, p.phone AS patient_phone, " +
                     "d.full_name AS doctor_name, d.specialization AS doctor_specialization, d.consultation_fee " +
                     "FROM APPOINTMENTS a " +
                     "JOIN PATIENTS p ON a.patient_id = p.patient_id " +
                     "JOIN DOCTORS d ON a.doctor_id = d.doctor_id " +
                     "WHERE a.appointment_datetime >= ? AND a.appointment_datetime <= ? " +
                     "ORDER BY a.appointment_datetime ASC";
        Timestamp start = Timestamp.valueOf(reportDate.atStartOfDay());
        Timestamp end = Timestamp.valueOf(reportDate.atTime(23, 59, 59));
        return jdbcTemplate.query(sql, appointmentWithDetailsRowMapper, start, end);
    }

    public long countTotalAppointments() {
        String sql = "SELECT COUNT(*) FROM APPOINTMENTS";
        Long count = jdbcTemplate.queryForObject(sql, Long.class);
        return count != null ? count : 0L;
    }

    public long countByStatus(String status) {
        String sql = "SELECT COUNT(*) FROM APPOINTMENTS WHERE status = ?";
        Long count = jdbcTemplate.queryForObject(sql, Long.class, status);
        return count != null ? count : 0L;
    }

    public long countTodayAppointments() {
        String sql = "SELECT COUNT(*) FROM APPOINTMENTS WHERE a.appointment_datetime >= ? AND a.appointment_datetime <= ?";
        LocalDate today = LocalDate.now();
        Timestamp start = Timestamp.valueOf(today.atStartOfDay());
        Timestamp end = Timestamp.valueOf(today.atTime(23, 59, 59));
        Long count = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM APPOINTMENTS WHERE appointment_datetime >= ? AND appointment_datetime <= ?",
            Long.class, start, end
        );
        return count != null ? count : 0L;
    }

    /**
     * Checks if any conflicting appointment exists for the doctor within +/- 30 minutes.
     * Overlapping interval rule: appointment_datetime > (targetTime - 30 mins) AND appointment_datetime < (targetTime + 30 mins).
     * Excludes CANCELLED appointments. If rescheduling, excludes the existing appointment ID.
     */
    public boolean checkConflict30Minutes(Long doctorId, LocalDateTime targetTime, Long excludeAppointmentId) {
        Timestamp startWindow = Timestamp.valueOf(targetTime.minusMinutes(30));
        Timestamp endWindow = Timestamp.valueOf(targetTime.plusMinutes(30));

        StringBuilder sql = new StringBuilder(
            "SELECT COUNT(*) FROM APPOINTMENTS " +
            "WHERE doctor_id = ? " +
            "AND status != 'CANCELLED' " +
            "AND appointment_datetime > ? " +
            "AND appointment_datetime < ? "
        );
        List<Object> params = new ArrayList<>();
        params.add(doctorId);
        params.add(startWindow);
        params.add(endWindow);

        if (excludeAppointmentId != null) {
            sql.append("AND appointment_id != ? ");
            params.add(excludeAppointmentId);
        }

        Long count = jdbcTemplate.queryForObject(sql.toString(), Long.class, params.toArray());
        return count != null && count > 0;
    }

    public Long createAppointment(Appointment a) {
        String sql = "INSERT INTO APPOINTMENTS (patient_id, doctor_id, appointment_datetime, reason, status, created_at) " +
                     "VALUES (?, ?, ?, ?, ?, CURRENT_TIMESTAMP)";
        KeyHolder keyHolder = new GeneratedKeyHolder();

        jdbcTemplate.update(connection -> {
            PreparedStatement ps = connection.prepareStatement(sql, new String[]{"appointment_id"});
            ps.setLong(1, a.getPatientId());
            ps.setLong(2, a.getDoctorId());
            ps.setTimestamp(3, Timestamp.valueOf(a.getAppointmentDatetime()));
            ps.setString(4, a.getReason());
            ps.setString(5, a.getStatus() != null ? a.getStatus() : "SCHEDULED");
            return ps;
        }, keyHolder);

        if (keyHolder.getKey() != null) {
            a.setAppointmentId(keyHolder.getKey().longValue());
            return keyHolder.getKey().longValue();
        }
        return null;
    }

    public int updateStatus(Long appointmentId, String newStatus) {
        String sql = "UPDATE APPOINTMENTS SET status = ? WHERE appointment_id = ?";
        return jdbcTemplate.update(sql, newStatus, appointmentId);
    }

    public int rescheduleAppointment(Long appointmentId, LocalDateTime newDateTime, String reason) {
        String sql = "UPDATE APPOINTMENTS SET appointment_datetime = ?, reason = ? WHERE appointment_id = ?";
        return jdbcTemplate.update(sql, Timestamp.valueOf(newDateTime), reason, appointmentId);
    }

    public int deleteAppointment(Long appointmentId) {
        String sql = "DELETE FROM APPOINTMENTS WHERE appointment_id = ?";
        return jdbcTemplate.update(sql, appointmentId);
    }
}
