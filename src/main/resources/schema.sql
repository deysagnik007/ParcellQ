-- =======================================================
-- ParcelIQ: Smart Campus Logistics & Social Parcel Tracking
-- MySQL 8.x Database Schema
-- Syllabus Concepts: RDBMS / MySQL Tables, Foreign Keys, PreparedStatements
-- =======================================================

CREATE DATABASE IF NOT EXISTS parceliq_db;
USE parceliq_db;

-- 1. Students Table
CREATE TABLE IF NOT EXISTS students (
    reg_no VARCHAR(50) PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    phone VARCHAR(20),
    dob VARCHAR(20),
    hostel_block VARCHAR(50),
    room_number VARCHAR(50),
    reward_points INT DEFAULT 100,
    gift_tokens INT DEFAULT 2
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 2. Parcels Table
CREATE TABLE IF NOT EXISTS parcels (
    id INT AUTO_INCREMENT PRIMARY KEY,
    tracking_number VARCHAR(100) NOT NULL UNIQUE,
    student_reg_no VARCHAR(50) NOT NULL,
    carrier VARCHAR(50) NOT NULL,
    item_name VARCHAR(150) NOT NULL,
    item_category VARCHAR(30) NOT NULL,
    shelf_slot VARCHAR(50) DEFAULT 'UNASSIGNED',
    pickup_token VARCHAR(50) DEFAULT 'PENDING',
    delivery_pin VARCHAR(20) NOT NULL,
    is_collected BOOLEAN DEFAULT FALSE,
    holding_fee DOUBLE DEFAULT 0.0,
    days_held INT DEFAULT 0,
    status VARCHAR(50) DEFAULT 'ORDER_PLACED',
    created_at BIGINT,
    eta_timestamp BIGINT,
    arrived_at BIGINT DEFAULT 0,
    collected_at BIGINT DEFAULT 0,
    is_courier BOOLEAN DEFAULT FALSE,
    gift_theme VARCHAR(50) DEFAULT NULL,
    sender_name VARCHAR(100) DEFAULT NULL,
    recipient_name VARCHAR(100) DEFAULT NULL,
    gift_message TEXT DEFAULT NULL,
    packaging_type VARCHAR(50) DEFAULT 'BOX',
    max_holding_hours INT DEFAULT 24,
    is_expired BOOLEAN DEFAULT FALSE,
    cold_storage_temp DOUBLE DEFAULT 4.0,
    FOREIGN KEY (student_reg_no) REFERENCES students(reg_no) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 3. PickupLogs Table (Audit Trail)
CREATE TABLE IF NOT EXISTS pickuplogs (
    log_id INT AUTO_INCREMENT PRIMARY KEY,
    parcel_id INT NOT NULL,
    student_reg_no VARCHAR(50) NOT NULL,
    pickup_code_used VARCHAR(50) NOT NULL,
    guard_id VARCHAR(50) NOT NULL,
    verified_at BIGINT NOT NULL,
    action_status VARCHAR(50) NOT NULL,
    fee_charged DOUBLE DEFAULT 0.0
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 4. Friendships Table
CREATE TABLE IF NOT EXISTS friendships (
    user_reg_no VARCHAR(50),
    friend_reg_no VARCHAR(50),
    created_at BIGINT,
    PRIMARY KEY (user_reg_no, friend_reg_no)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 5. Rewards Table
CREATE TABLE IF NOT EXISTS rewards (
    id INT AUTO_INCREMENT PRIMARY KEY,
    student_reg_no VARCHAR(50),
    title VARCHAR(100),
    description VARCHAR(200),
    cost_points INT,
    voucher_code VARCHAR(50),
    is_redeemed BOOLEAN DEFAULT FALSE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
