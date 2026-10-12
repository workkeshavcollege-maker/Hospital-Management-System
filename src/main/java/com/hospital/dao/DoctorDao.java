package com.hospital.dao;

import com.hospital.model.Doctor;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.sql.PreparedStatement;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Repository
public class DoctorDao {

    private final JdbcTemplate jdbcTemplate;

    public DoctorDao(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    private final RowMapper<Doctor> doctorRowMapper = (rs, rowNum) -> {
        Doctor doc = new Doctor();
        doc.setDoctorId(rs.getLong("doctor_id"));
        doc.setFullName(rs.getString("full_name"));
        doc.setSpecialization(rs.getString("specialization"));
        doc.setPhone(rs.getString("phone"));
        doc.setEmail(rs.getString("email"));
        BigDecimal fee = rs.getBigDecimal("consultation_fee");
        doc.setConsultationFee(fee != null ? fee : BigDecimal.ZERO);
        doc.setIsActive(rs.getString("is_active"));
        return doc;
    };

    public Optional<Doctor> findById(Long doctorId) {
        String sql = "SELECT doctor_id, full_name, specialization, phone, email, consultation_fee, is_active " +
                     "FROM DOCTORS WHERE doctor_id = ?";
        try {
            Doctor d = jdbcTemplate.queryForObject(sql, doctorRowMapper, doctorId);
            return Optional.ofNullable(d);
        } catch (EmptyResultDataAccessException e) {
            return Optional.empty();
        }
    }

    /**
     * Acquires a pessimistic lock (FOR UPDATE) on the doctor row to serialize
     * concurrent booking transactions for this doctor.
     */
    public Optional<Doctor> lockDoctorForBooking(Long doctorId) {
        String sql = "SELECT doctor_id, full_name, specialization, phone, email, consultation_fee, is_active " +
                     "FROM DOCTORS WHERE doctor_id = ? FOR UPDATE";
        try {
            Doctor d = jdbcTemplate.queryForObject(sql, doctorRowMapper, doctorId);
            return Optional.ofNullable(d);
        } catch (EmptyResultDataAccessException e) {
            return Optional.empty();
        }
    }

    public List<Doctor> findAll() {
        String sql = "SELECT doctor_id, full_name, specialization, phone, email, consultation_fee, is_active " +
                     "FROM DOCTORS ORDER BY doctor_id ASC";
        return jdbcTemplate.query(sql, doctorRowMapper);
    }

    public List<Doctor> findActiveDoctors() {
        String sql = "SELECT doctor_id, full_name, specialization, phone, email, consultation_fee, is_active " +
                     "FROM DOCTORS WHERE is_active = 'Y' ORDER BY full_name ASC";
        return jdbcTemplate.query(sql, doctorRowMapper);
    }

    public List<Doctor> searchDoctors(String term, String status) {
        StringBuilder sql = new StringBuilder(
            "SELECT doctor_id, full_name, specialization, phone, email, consultation_fee, is_active FROM DOCTORS WHERE 1=1 "
        );
        List<Object> params = new ArrayList<>();

        if (term != null && !term.trim().isEmpty()) {
            sql.append("AND (LOWER(full_name) LIKE ? OR LOWER(specialization) LIKE ? OR phone LIKE ?) ");
            String likeVal = "%" + term.trim().toLowerCase() + "%";
            params.add(likeVal);
            params.add(likeVal);
            params.add("%" + term.trim() + "%");
        }

        if (status != null && !status.trim().isEmpty() && !status.equalsIgnoreCase("ALL")) {
            sql.append("AND is_active = ? ");
            params.add(status.trim().toUpperCase());
        }

        sql.append("ORDER BY full_name ASC");
        return jdbcTemplate.query(sql.toString(), doctorRowMapper, params.toArray());
    }

    public long countActiveDoctors() {
        String sql = "SELECT COUNT(*) FROM DOCTORS WHERE is_active = 'Y'";
        Long count = jdbcTemplate.queryForObject(sql, Long.class);
        return count != null ? count : 0L;
    }

    public long countTotalDoctors() {
        String sql = "SELECT COUNT(*) FROM DOCTORS";
        Long count = jdbcTemplate.queryForObject(sql, Long.class);
        return count != null ? count : 0L;
    }

    public boolean hasAppointments(Long doctorId) {
        String sql = "SELECT COUNT(*) FROM APPOINTMENTS WHERE doctor_id = ?";
        Long count = jdbcTemplate.queryForObject(sql, Long.class, doctorId);
        return count != null && count > 0;
    }

    public Long createDoctor(Doctor doctor) {
        String sql = "INSERT INTO DOCTORS (full_name, specialization, phone, email, consultation_fee, is_active) " +
                     "VALUES (?, ?, ?, ?, ?, ?)";
        KeyHolder keyHolder = new GeneratedKeyHolder();

        jdbcTemplate.update(connection -> {
            PreparedStatement ps = connection.prepareStatement(sql, new String[]{"doctor_id"});
            ps.setString(1, doctor.getFullName());
            ps.setString(2, doctor.getSpecialization());
            ps.setString(3, doctor.getPhone());
            ps.setString(4, doctor.getEmail());
            ps.setBigDecimal(5, doctor.getConsultationFee() != null ? doctor.getConsultationFee() : BigDecimal.ZERO);
            ps.setString(6, doctor.getIsActive() != null ? doctor.getIsActive() : "Y");
            return ps;
        }, keyHolder);

        if (keyHolder.getKey() != null) {
            doctor.setDoctorId(keyHolder.getKey().longValue());
            return keyHolder.getKey().longValue();
        }
        return null;
    }

    public int updateDoctor(Doctor doctor) {
        String sql = "UPDATE DOCTORS SET full_name = ?, specialization = ?, phone = ?, email = ?, consultation_fee = ?, is_active = ? " +
                     "WHERE doctor_id = ?";
        return jdbcTemplate.update(sql,
                doctor.getFullName(),
                doctor.getSpecialization(),
                doctor.getPhone(),
                doctor.getEmail(),
                doctor.getConsultationFee(),
                doctor.getIsActive(),
                doctor.getDoctorId());
    }

    public int toggleActive(Long doctorId, String newStatus) {
        String sql = "UPDATE DOCTORS SET is_active = ? WHERE doctor_id = ?";
        return jdbcTemplate.update(sql, newStatus, doctorId);
    }
}
