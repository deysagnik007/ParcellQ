package com.parceliq.dao;

import com.parceliq.db.DBConnection;
import com.parceliq.model.GuardShift;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Data Access Object: GuardDAO
 * Manages admin-assigned guard shifts and unique auto-generated guard roll numbers.
 */
public class GuardDAO {

    private final Connection connection;

    public GuardDAO() {
        this.connection = DBConnection.getConnection();
    }

    public GuardShift assignShift(String guardRollNo, String guardName, String phone, String shelterType, String shiftTime, String assignedBy) throws SQLException {
        String sql = "INSERT INTO guard_shifts (guard_roll_no, guard_name, phone, shelter_type, shift_time, assigned_by, created_at) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?)";

        long now = System.currentTimeMillis();
        int generatedId = 0;

        try (PreparedStatement ps = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, guardRollNo);
            ps.setString(2, guardName);
            ps.setString(3, phone);
            ps.setString(4, shelterType != null ? shelterType : "Main Campus Shelter");
            ps.setString(5, shiftTime);
            ps.setString(6, assignedBy != null ? assignedBy : "Admin");
            ps.setLong(7, now);
            ps.executeUpdate();

            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    generatedId = rs.getInt(1);
                }
            }
        }

        return new GuardShift(generatedId, guardRollNo, guardName, phone, shelterType, shiftTime, assignedBy, now);
    }

    public GuardShift getGuardByRollNo(String guardRollNo) throws SQLException {
        if (guardRollNo == null || guardRollNo.trim().isEmpty()) return null;
        String sql = "SELECT * FROM guard_shifts WHERE LOWER(guard_roll_no) = LOWER(?)";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, guardRollNo.trim());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapRowToGuard(rs);
                }
            }
        }
        return null;
    }

    public List<GuardShift> getAllShifts() throws SQLException {
        List<GuardShift> list = new ArrayList<>();
        String sql = "SELECT * FROM guard_shifts ORDER BY id DESC";
        try (PreparedStatement ps = connection.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                list.add(mapRowToGuard(rs));
            }
        }
        return list;
    }

    public boolean deleteShift(int id) throws SQLException {
        String sql = "DELETE FROM guard_shifts WHERE id = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, id);
            return ps.executeUpdate() > 0;
        }
    }

    private GuardShift mapRowToGuard(ResultSet rs) throws SQLException {
        String rollNo = "GRD-1001";
        try {
            rollNo = rs.getString("guard_roll_no");
        } catch (Exception ignored) {}

        return new GuardShift(
                rs.getInt("id"),
                rollNo,
                rs.getString("guard_name"),
                rs.getString("phone"),
                rs.getString("shelter_type"),
                rs.getString("shift_time"),
                rs.getString("assigned_by"),
                rs.getLong("created_at")
        );
    }
}
