package com.hospital.dao;

import com.hospital.model.AppUser;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.sql.PreparedStatement;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.List;
import java.util.Optional;

@Repository
public class UserDao {

    private final JdbcTemplate jdbcTemplate;

    public UserDao(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    private final RowMapper<AppUser> userRowMapper = (rs, rowNum) -> {
        AppUser user = new AppUser();
        user.setUserId(rs.getLong("user_id"));
        user.setUsername(rs.getString("username"));
        user.setPasswordHash(rs.getString("password_hash"));
        user.setFullName(rs.getString("full_name"));
        user.setUserRole(rs.getString("user_role"));
        user.setIsActive(rs.getString("is_active"));
        Timestamp ts = rs.getTimestamp("created_at");
        if (ts != null) {
            user.setCreatedAt(ts.toLocalDateTime());
        }
        return user;
    };

    public Optional<AppUser> findByUsername(String username) {
        String sql = "SELECT user_id, username, password_hash, full_name, user_role, is_active, created_at " +
                     "FROM APP_USERS WHERE username = ?";
        try {
            AppUser user = jdbcTemplate.queryForObject(sql, userRowMapper, username);
            return Optional.ofNullable(user);
        } catch (EmptyResultDataAccessException e) {
            return Optional.empty();
        }
    }

    public Optional<AppUser> findById(Long userId) {
        String sql = "SELECT user_id, username, password_hash, full_name, user_role, is_active, created_at " +
                     "FROM APP_USERS WHERE user_id = ?";
        try {
            AppUser user = jdbcTemplate.queryForObject(sql, userRowMapper, userId);
            return Optional.ofNullable(user);
        } catch (EmptyResultDataAccessException e) {
            return Optional.empty();
        }
    }

    public List<AppUser> findAll() {
        String sql = "SELECT user_id, username, password_hash, full_name, user_role, is_active, created_at " +
                     "FROM APP_USERS ORDER BY user_id ASC";
        return jdbcTemplate.query(sql, userRowMapper);
    }

    public long countUsers() {
        String sql = "SELECT COUNT(*) FROM APP_USERS";
        Long count = jdbcTemplate.queryForObject(sql, Long.class);
        return count != null ? count : 0L;
    }

    public long countAdmins() {
        String sql = "SELECT COUNT(*) FROM APP_USERS WHERE user_role = 'ADMIN' AND is_active = 'Y'";
        Long count = jdbcTemplate.queryForObject(sql, Long.class);
        return count != null ? count : 0L;
    }

    public int createUser(AppUser user) {
        String sql = "INSERT INTO APP_USERS (username, password_hash, full_name, user_role, is_active, created_at) " +
                     "VALUES (?, ?, ?, ?, ?, CURRENT_TIMESTAMP)";
        KeyHolder keyHolder = new GeneratedKeyHolder();

        int affected = jdbcTemplate.update(connection -> {
            PreparedStatement ps = connection.prepareStatement(sql, new String[]{"user_id"});
            ps.setString(1, user.getUsername());
            ps.setString(2, user.getPasswordHash());
            ps.setString(3, user.getFullName());
            ps.setString(4, user.getUserRole() != null ? user.getUserRole() : "RECEPTIONIST");
            ps.setString(5, user.getIsActive() != null ? user.getIsActive() : "Y");
            return ps;
        }, keyHolder);

        if (keyHolder.getKey() != null) {
            user.setUserId(keyHolder.getKey().longValue());
        }
        return affected;
    }

    public int updateUserStatus(Long userId, String isActive) {
        String sql = "UPDATE APP_USERS SET is_active = ? WHERE user_id = ?";
        return jdbcTemplate.update(sql, isActive, userId);
    }

    public int updatePassword(Long userId, String newPasswordHash) {
        String sql = "UPDATE APP_USERS SET password_hash = ? WHERE user_id = ?";
        return jdbcTemplate.update(sql, newPasswordHash, userId);
    }
}
