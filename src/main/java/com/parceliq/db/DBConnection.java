package com.parceliq.db;

import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Properties;

/**
 * Singleton Database Connection Manager
 * Syllabus Concepts:
 * 1. Static Keyword & Singleton Pattern: DBConnection.getConnection()
 * 2. RDBMS / MySQL Integration: Supports standard MySQL 8.x via JDBC PreparedStatement / ResultSet
 * 3. Graceful Fallback: Seamlessly boots in-memory H2 in MySQL Compatibility Mode if MySQL is unconfigured.
 */
public class DBConnection {

    private static Connection connection = null;
    private static String activeDbType = "UNKNOWN";

    private DBConnection() {}

    public static synchronized Connection getConnection() {
        try {
            if (connection == null || connection.isClosed()) {
                initConnection();
            }
        } catch (SQLException e) {
            System.err.println("[DBConnection] Error checking connection state: " + e.getMessage());
            initConnection();
        }
        return connection;
    }

    private static void initConnection() {
        Properties props = new Properties();
        try (InputStream in = DBConnection.class.getClassLoader().getResourceAsStream("application.properties")) {
            if (in != null) props.load(in);
        } catch (Exception ignored) {}

        String mysqlUrl = props.getProperty("db.mysql.url", "jdbc:mysql://localhost:3306/parceliq_db?createDatabaseIfNotExist=true&useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC");
        String mysqlUser = props.getProperty("db.mysql.user", "root");
        String mysqlPassword = props.getProperty("db.mysql.password", "root");

        // Attempt 1: Try MySQL Connection
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            connection = DriverManager.getConnection(mysqlUrl, mysqlUser, mysqlPassword);
            activeDbType = "MySQL 8.x";
            System.out.println("[DBConnection] Successfully connected to live MySQL Database (" + mysqlUrl + ")");
            initializeSchema();
            seedDefaultData();
            return;
        } catch (Exception e) {
            System.out.println("[DBConnection] Notice: MySQL direct connection not reachable (" + e.getMessage() + ")");
            System.out.println("[DBConnection] Seamlessly activating embedded in-memory RDBMS engine (H2 MySQL Compatibility Mode)...");
        }

        // Attempt 2: Embedded H2 in MySQL Compatibility Mode
        try {
            Class.forName("org.h2.Driver");
            String h2Url = "jdbc:h2:mem:parceliq_db;MODE=MySQL;DATABASE_TO_LOWER=TRUE;DEFAULT_NULL_ORDERING=HIGH;DB_CLOSE_DELAY=-1";
            connection = DriverManager.getConnection(h2Url, "sa", "");
            activeDbType = "Embedded H2 (MySQL Mode)";
            System.out.println("[DBConnection] Successfully initialized Embedded H2 RDBMS in MySQL mode.");
            initializeSchema();
            seedDefaultData();
        } catch (Exception e) {
            System.err.println("[DBConnection] Fatal: Failed to initialize database: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private static void initializeSchema() {
        if (connection == null) return;

        try (Statement stmt = connection.createStatement()) {
            // 1. Students Table (with username, password & roll no / reg_no)
            stmt.execute("CREATE TABLE IF NOT EXISTS students (" +
                    "reg_no VARCHAR(50) PRIMARY KEY, " +
                    "name VARCHAR(100) NOT NULL, " +
                    "password VARCHAR(100) NOT NULL, " +
                    "phone VARCHAR(20), " +
                    "dob VARCHAR(20), " +
                    "hostel_block VARCHAR(50), " +
                    "room_number VARCHAR(50), " +
                    "reward_points INT DEFAULT 100, " +
                    "gift_tokens INT DEFAULT 2" +
                    ")");

            // Migration safety: Ensure password column exists if table previously created
            try {
                stmt.execute("ALTER TABLE students ADD COLUMN IF NOT EXISTS password VARCHAR(100) DEFAULT 'password123'");
            } catch (Exception ignored) {}

            // 2. Parcels Table
            stmt.execute("CREATE TABLE IF NOT EXISTS parcels (" +
                    "id INT AUTO_INCREMENT PRIMARY KEY, " +
                    "tracking_number VARCHAR(100) NOT NULL UNIQUE, " +
                    "student_reg_no VARCHAR(50) NOT NULL, " +
                    "student_name VARCHAR(100) DEFAULT 'Student', " +
                    "carrier VARCHAR(50) NOT NULL, " +
                    "delivery_from VARCHAR(50) NOT NULL, " +
                    "item_name VARCHAR(150) NOT NULL, " +
                    "item_category VARCHAR(30) NOT NULL, " +
                    "shelf_slot VARCHAR(50) DEFAULT 'UNASSIGNED', " +
                    "shelf_number INT DEFAULT 0, " +
                    "pickup_token VARCHAR(50) DEFAULT 'PENDING', " +
                    "pickup_otp VARCHAR(20) DEFAULT '', " +
                    "delivery_pin VARCHAR(20) NOT NULL, " +
                    "barcode_symbol VARCHAR(50) DEFAULT '', " +
                    "is_collected BOOLEAN DEFAULT FALSE, " +
                    "holding_fee DOUBLE DEFAULT 0.0, " +
                    "days_held INT DEFAULT 0, " +
                    "status VARCHAR(50) DEFAULT 'ORDER_PLACED', " +
                    "created_at BIGINT, " +
                    "eta_timestamp BIGINT, " +
                    "arrived_at BIGINT DEFAULT 0, " +
                    "arrived_at_str VARCHAR(50) DEFAULT '', " +
                    "collected_at BIGINT DEFAULT 0, " +
                    "picked_up_at_str VARCHAR(50) DEFAULT '', " +
                    "is_courier BOOLEAN DEFAULT FALSE, " +
                    "gift_theme VARCHAR(50), " +
                    "sender_name VARCHAR(100), " +
                    "recipient_name VARCHAR(100), " +
                    "gift_message TEXT, " +
                    "packaging_type VARCHAR(50), " +
                    "max_holding_hours INT DEFAULT 24, " +
                    "is_expired BOOLEAN DEFAULT FALSE, " +
                    "cold_storage_temp DOUBLE DEFAULT 4.0, " +
                    "FOREIGN KEY (student_reg_no) REFERENCES students(reg_no)" +
                    ")");

            // 3. PickupLogs Table
            stmt.execute("CREATE TABLE IF NOT EXISTS pickuplogs (" +
                    "log_id INT AUTO_INCREMENT PRIMARY KEY, " +
                    "parcel_id INT NOT NULL, " +
                    "student_reg_no VARCHAR(50) NOT NULL, " +
                    "pickup_code_used VARCHAR(50) NOT NULL, " +
                    "guard_id VARCHAR(50) NOT NULL, " +
                    "verified_at BIGINT NOT NULL, " +
                    "action_status VARCHAR(50) NOT NULL, " +
                    "fee_charged DOUBLE DEFAULT 0.0" +
                    ")");

            // 4. Friendships Table
            stmt.execute("CREATE TABLE IF NOT EXISTS friendships (" +
                    "user_reg_no VARCHAR(50), " +
                    "friend_reg_no VARCHAR(50), " +
                    "created_at BIGINT, " +
                    "PRIMARY KEY (user_reg_no, friend_reg_no)" +
                    ")");

            // 5. Rewards Table
            stmt.execute("CREATE TABLE IF NOT EXISTS rewards (" +
                    "id INT AUTO_INCREMENT PRIMARY KEY, " +
                    "student_reg_no VARCHAR(50), " +
                    "title VARCHAR(100), " +
                    "description VARCHAR(200), " +
                    "cost_points INT, " +
                    "voucher_code VARCHAR(50), " +
                    "is_redeemed BOOLEAN DEFAULT FALSE" +
                    ")");

            // 6. Guard Shifts Table (with auto-generated unique guard_roll_no)
            stmt.execute("CREATE TABLE IF NOT EXISTS guard_shifts (" +
                    "id INT AUTO_INCREMENT PRIMARY KEY, " +
                    "guard_roll_no VARCHAR(50) NOT NULL UNIQUE, " +
                    "guard_name VARCHAR(100) NOT NULL, " +
                    "phone VARCHAR(30) NOT NULL, " +
                    "shelter_type VARCHAR(50) NOT NULL, " +
                    "shift_time VARCHAR(100) NOT NULL, " +
                    "assigned_by VARCHAR(50) DEFAULT 'Admin', " +
                    "created_at BIGINT NOT NULL" +
                    ")");

            try {
                stmt.execute("ALTER TABLE guard_shifts ADD COLUMN IF NOT EXISTS guard_roll_no VARCHAR(50) DEFAULT 'GRD-1001'");
            } catch (Exception ignored) {}

            // 7. Dispute Tickets Table (Help Sign: Red Triangle with White Rectangle)
            stmt.execute("CREATE TABLE IF NOT EXISTS dispute_tickets (" +
                    "id INT AUTO_INCREMENT PRIMARY KEY, " +
                    "ticket_number VARCHAR(50) NOT NULL UNIQUE, " +
                    "student_reg_no VARCHAR(50) NOT NULL, " +
                    "parcel_id INT NOT NULL, " +
                    "issue_type VARCHAR(50) NOT NULL, " +
                    "description TEXT NOT NULL, " +
                    "status VARCHAR(50) DEFAULT 'UNDER_INVESTIGATION', " +
                    "created_at BIGINT NOT NULL, " +
                    "created_at_formatted VARCHAR(50) NOT NULL" +
                    ")");

            System.out.println("[DBConnection] Schema verified: students, parcels, pickuplogs, guard_shifts, dispute_tickets.");
        } catch (SQLException e) {
            System.err.println("[DBConnection] Schema initialization warning: " + e.getMessage());
        }
    }

    private static void seedDefaultData() {
        try (Statement stmt = connection.createStatement()) {
            // Seed Default Student Accounts with Password & Roll No
            stmt.execute("MERGE INTO students (reg_no, name, password, phone, dob, hostel_block, room_number, reward_points, gift_tokens) KEY(reg_no) VALUES (" +
                    "'23BCS1088', 'Aryan Sharma', 'aryan123', '+91 98765 43210', '2004-06-15', 'Hostel Block-A', 'Room 304', 350, 4)");
            stmt.execute("MERGE INTO students (reg_no, name, password, phone, dob, hostel_block, room_number, reward_points, gift_tokens) KEY(reg_no) VALUES (" +
                    "'23BCS1092', 'Diya Patel', 'diya123', '+91 98111 22334', '2004-09-22', 'Hostel Block-C', 'Room 112', 200, 3)");

            // Seed Admin Assigned Guard Shifts with unique auto-generated guard roll numbers
            stmt.execute("MERGE INTO guard_shifts (id, guard_roll_no, guard_name, phone, shelter_type, shift_time, assigned_by, created_at) KEY(id) VALUES (" +
                    "1, 'GRD-1001', 'Officer Ramesh Kumar', '+91 98765 11223', 'Main Campus Shelter', '09:00 AM - 12:00 PM (Forenoon Shift)', 'Admin', 1700000000000)");
            stmt.execute("MERGE INTO guard_shifts (id, guard_roll_no, guard_name, phone, shelter_type, shift_time, assigned_by, created_at) KEY(id) VALUES (" +
                    "2, 'GRD-1002', 'Officer Vikram Singh', '+91 98765 22334', 'Main Campus Shelter', '12:00 PM - 03:00 PM (Afternoon Shift)', 'Admin', 1700000000000)");

            // Crucial: Clear all dummy parcels so students see clean page unless guard creates an arrival ticket
            stmt.execute("DELETE FROM parcels WHERE status != 'USER_CREATED_ORDER'");

            System.out.println("[DBConnection] Seeded default accounts. Cleared dummy order tickets.");
        } catch (SQLException e) {
            System.err.println("[DBConnection] Error during demo seed: " + e.getMessage());
        }
    }

    public static String getActiveDbType() {
        return activeDbType;
    }
}
