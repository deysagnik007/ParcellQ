package com.parceliq.dao;

import com.parceliq.db.DBConnection;
import com.parceliq.model.Reward;
import com.parceliq.model.Student;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * Data Access Object: StudentDAO
 * Manages student profiles, authentication credentials (name, password, roll no/reg_no).
 */
public class StudentDAO {

    private final Connection connection;

    public StudentDAO() {
        this.connection = DBConnection.getConnection();
    }

    public Student getStudentByRegNo(String regNo) throws SQLException {
        String sql = "SELECT * FROM students WHERE LOWER(reg_no) = LOWER(?)";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, regNo);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    Student s = mapRowToStudent(rs);
                    s.setFriendRegNos(getFriends(regNo));
                    return s;
                }
            }
        }
        return null;
    }

    public Student getStudentByName(String name) throws SQLException {
        String sql = "SELECT * FROM students WHERE LOWER(name) = LOWER(?)";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, name);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    Student s = mapRowToStudent(rs);
                    s.setFriendRegNos(getFriends(s.getRegNo()));
                    return s;
                }
            }
        }
        return null;
    }

    public Student authenticate(String name, String password) throws SQLException {
        String sql = "SELECT * FROM students WHERE LOWER(name) = LOWER(?) AND password = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, name);
            ps.setString(2, password);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    Student s = mapRowToStudent(rs);
                    s.setFriendRegNos(getFriends(s.getRegNo()));
                    return s;
                }
            }
        }
        return null;
    }

    public Student registerStudent(String regNo, String name, String password) throws SQLException {
        String sql = "INSERT INTO students (reg_no, name, password, phone, dob, hostel_block, room_number, reward_points, gift_tokens) " +
                "VALUES (?, ?, ?, '', '', '', '', 100, 2)";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, regNo);
            ps.setString(2, name);
            ps.setString(3, password);
            ps.executeUpdate();
        }
        return new Student(regNo, name, password);
    }

    public void updatePassword(String regNo, String newPassword) throws SQLException {
        String sql = "UPDATE students SET password = ? WHERE LOWER(reg_no) = LOWER(?)";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, newPassword);
            ps.setString(2, regNo);
            ps.executeUpdate();
        }
    }

    public void updateStudentProfile(Student student) throws SQLException {
        String sql = "UPDATE students SET name = ?, phone = ?, dob = ?, hostel_block = ?, room_number = ? WHERE reg_no = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, student.getName());
            ps.setString(2, student.getPhone());
            ps.setString(3, student.getDateOfBirth());
            ps.setString(4, student.getHostelBlock());
            ps.setString(5, student.getRoomNumber());
            ps.setString(6, student.getRegNo());
            ps.executeUpdate();
        }
    }

    public List<Student> getAllStudents() throws SQLException {
        List<Student> list = new ArrayList<>();
        String sql = "SELECT * FROM students ORDER BY name ASC";
        try (PreparedStatement ps = connection.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                list.add(mapRowToStudent(rs));
            }
        }
        return list;
    }

    public List<String> getFriends(String regNo) {
        List<String> friends = new ArrayList<>();
        String sql = "SELECT friend_reg_no FROM friendships WHERE user_reg_no = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, regNo);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    friends.add(rs.getString("friend_reg_no"));
                }
            }
        } catch (SQLException e) {
            System.err.println("[StudentDAO] Error loading friends: " + e.getMessage());
        }
        return friends;
    }

    public boolean addFriend(String userRegNo, String friendRegNo) throws SQLException {
        if (userRegNo.equalsIgnoreCase(friendRegNo)) return false;
        String sql = "INSERT INTO friendships (user_reg_no, friend_reg_no, created_at) VALUES (?, ?, ?)";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, userRegNo);
            ps.setString(2, friendRegNo);
            ps.setLong(3, System.currentTimeMillis());
            ps.executeUpdate();
            return true;
        } catch (SQLException e) {
            return false;
        }
    }

    public List<Reward> getRewards(String regNo) {
        List<Reward> list = new ArrayList<>();
        String sql = "SELECT * FROM rewards WHERE student_reg_no = ? AND is_redeemed = FALSE";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, regNo);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(new Reward(
                            rs.getInt("id"),
                            rs.getString("student_reg_no"),
                            rs.getString("title"),
                            rs.getString("description"),
                            rs.getInt("cost_points"),
                            rs.getString("voucher_code"),
                            rs.getBoolean("is_redeemed")
                    ));
                }
            }
        } catch (SQLException e) {
            System.err.println("[StudentDAO] Error loading rewards: " + e.getMessage());
        }
        return list;
    }

    private Student mapRowToStudent(ResultSet rs) throws SQLException {
        String pass = "password123";
        try {
            pass = rs.getString("password");
        } catch (Exception ignored) {}

        return new Student(
                rs.getString("reg_no"),
                rs.getString("name"),
                pass,
                rs.getString("phone"),
                rs.getString("dob"),
                rs.getString("hostel_block"),
                rs.getString("room_number"),
                rs.getInt("reward_points"),
                rs.getInt("gift_tokens")
        );
    }
}
