package com.hospital.dao;

import com.hospital.model.Patient;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Repository
public class PatientDao {

    private final JdbcTemplate jdbcTemplate;

    public PatientDao(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    private final RowMapper<Patient> patientRowMapper = (rs, rowNum) -> {
        Patient patient = new Patient();
        patient.setPatientId(rs.getLong("patient_id"));
        patient.setFullName(rs.getString("full_name"));
        Date dob = rs.getDate("date_of_birth");
        if (dob != null) {
            patient.setDateOfBirth(dob.toLocalDate());
        }
        patient.setGender(rs.getString("gender"));
        patient.setPhone(rs.getString("phone"));
        patient.setEmail(rs.getString("email"));
        patient.setAddress(rs.getString("address"));
        Timestamp ts = rs.getTimestamp("created_at");
        if (ts != null) {
            patient.setCreatedAt(ts.toLocalDateTime());
        }
        return patient;
    };

    public Optional<Patient> findById(Long patientId) {
        String sql = "SELECT patient_id, full_name, date_of_birth, gender, phone, email, address, created_at " +
                     "FROM PATIENTS WHERE patient_id = ?";
        try {
            Patient p = jdbcTemplate.queryForObject(sql, patientRowMapper, patientId);
            return Optional.ofNullable(p);
        } catch (EmptyResultDataAccessException e) {
            return Optional.empty();
        }
    }

    public List<Patient> findAll() {
        String sql = "SELECT patient_id, full_name, date_of_birth, gender, phone, email, address, created_at " +
                     "FROM PATIENTS ORDER BY patient_id DESC";
        return jdbcTemplate.query(sql, patientRowMapper);
    }

    public List<Patient> searchPatients(String term) {
        if (term == null || term.trim().isEmpty()) {
            return findAll();
        }
        term = term.trim();
        List<Object> params = new ArrayList<>();
        StringBuilder sql = new StringBuilder(
            "SELECT patient_id, full_name, date_of_birth, gender, phone, email, address, created_at FROM PATIENTS WHERE "
        );

        boolean isNumeric = term.chars().allMatch(Character::isDigit);
        if (isNumeric) {
            sql.append("(patient_id = ? OR LOWER(full_name) LIKE ? OR phone LIKE ?) ");
            params.add(Long.parseLong(term));
            params.add("%" + term.toLowerCase() + "%");
            params.add("%" + term + "%");
        } else {
            sql.append("(LOWER(full_name) LIKE ? OR phone LIKE ? OR LOWER(email) LIKE ?) ");
            params.add("%" + term.toLowerCase() + "%");
            params.add("%" + term + "%");
            params.add("%" + term.toLowerCase() + "%");
        }

        sql.append("ORDER BY patient_id DESC");
        return jdbcTemplate.query(sql.toString(), patientRowMapper, params.toArray());
    }

    public long countPatients() {
        String sql = "SELECT COUNT(*) FROM PATIENTS";
        Long count = jdbcTemplate.queryForObject(sql, Long.class);
        return count != null ? count : 0L;
    }

    public boolean hasAppointments(Long patientId) {
        String sql = "SELECT COUNT(*) FROM APPOINTMENTS WHERE patient_id = ?";
        Long count = jdbcTemplate.queryForObject(sql, Long.class, patientId);
        return count != null && count > 0;
    }

    public Long createPatient(Patient patient) {
        String sql = "INSERT INTO PATIENTS (full_name, date_of_birth, gender, phone, email, address, created_at) " +
                     "VALUES (?, ?, ?, ?, ?, ?, CURRENT_TIMESTAMP)";
        KeyHolder keyHolder = new GeneratedKeyHolder();

        jdbcTemplate.update(connection -> {
            PreparedStatement ps = connection.prepareStatement(sql, new String[]{"patient_id"});
            ps.setString(1, patient.getFullName());
            if (patient.getDateOfBirth() != null) {
                ps.setDate(2, Date.valueOf(patient.getDateOfBirth()));
            } else {
                ps.setNull(2, java.sql.Types.DATE);
            }
            ps.setString(3, patient.getGender());
            ps.setString(4, patient.getPhone());
            ps.setString(5, patient.getEmail());
            ps.setString(6, patient.getAddress());
            return ps;
        }, keyHolder);

        if (keyHolder.getKey() != null) {
            patient.setPatientId(keyHolder.getKey().longValue());
            return keyHolder.getKey().longValue();
        }
        return null;
    }

    public int updatePatient(Patient patient) {
        String sql = "UPDATE PATIENTS SET full_name = ?, date_of_birth = ?, gender = ?, phone = ?, email = ?, address = ? " +
                     "WHERE patient_id = ?";
        Date dob = patient.getDateOfBirth() != null ? Date.valueOf(patient.getDateOfBirth()) : null;
        return jdbcTemplate.update(sql,
                patient.getFullName(),
                dob,
                patient.getGender(),
                patient.getPhone(),
                patient.getEmail(),
                patient.getAddress(),
                patient.getPatientId());
    }

    public int deletePatient(Long patientId) {
        String sql = "DELETE FROM PATIENTS WHERE patient_id = ?";
        return jdbcTemplate.update(sql, patientId);
    }
}
