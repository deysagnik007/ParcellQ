package com.parceliq.dao;

import com.parceliq.db.DBConnection;
import com.parceliq.model.DisputeTicket;

import java.sql.*;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Random;

/**
 * Data Access Object: DisputeDAO
 * Handles student disputes raised via the Help Sign (Red Triangle with White Rectangle)
 * for Fraud, Malicious Tampering, or Missing Items.
 */
public class DisputeDAO {

    private final Connection connection;

    public DisputeDAO() {
        this.connection = DBConnection.getConnection();
    }

    public DisputeTicket raiseDispute(String studentRegNo, int parcelId, String issueType, String description) throws SQLException {
        String ticketNumber = "DSP-" + (1000 + new Random().nextInt(9000));
        long now = System.currentTimeMillis();
        SimpleDateFormat sdf = new SimpleDateFormat("hh:mm a, dd MMM");
        String formatted = sdf.format(new Date(now));

        String sql = "INSERT INTO dispute_tickets (ticket_number, student_reg_no, parcel_id, issue_type, description, status, created_at, created_at_formatted) " +
                "VALUES (?, ?, ?, ?, ?, 'UNDER_INVESTIGATION', ?, ?)";

        int id = 0;
        try (PreparedStatement ps = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, ticketNumber);
            ps.setString(2, studentRegNo);
            ps.setInt(3, parcelId);
            ps.setString(4, issueType);
            ps.setString(5, description);
            ps.setLong(6, now);
            ps.setString(7, formatted);
            ps.executeUpdate();

            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    id = rs.getInt(1);
                }
            }
        }

        return new DisputeTicket(id, ticketNumber, studentRegNo, parcelId, issueType, description, "UNDER_INVESTIGATION", now, formatted);
    }

    public List<DisputeTicket> getDisputesByStudent(String studentRegNo) throws SQLException {
        List<DisputeTicket> list = new ArrayList<>();
        String sql = "SELECT * FROM dispute_tickets WHERE student_reg_no = ? ORDER BY id DESC";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, studentRegNo);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(new DisputeTicket(
                            rs.getInt("id"),
                            rs.getString("ticket_number"),
                            rs.getString("student_reg_no"),
                            rs.getInt("parcel_id"),
                            rs.getString("issue_type"),
                            rs.getString("description"),
                            rs.getString("status"),
                            rs.getLong("created_at"),
                            rs.getString("created_at_formatted")
                    ));
                }
            }
        }
        return list;
    }

    public List<DisputeTicket> getAllDisputes() throws SQLException {
        List<DisputeTicket> list = new ArrayList<>();
        String sql = "SELECT * FROM dispute_tickets ORDER BY id DESC";
        try (PreparedStatement ps = connection.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                list.add(new DisputeTicket(
                        rs.getInt("id"),
                        rs.getString("ticket_number"),
                        rs.getString("student_reg_no"),
                        rs.getInt("parcel_id"),
                        rs.getString("issue_type"),
                        rs.getString("description"),
                        rs.getString("status"),
                        rs.getLong("created_at"),
                        rs.getString("created_at_formatted")
                ));
            }
        }
        return list;
    }
}
