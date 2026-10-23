package com.parceliq.dao;

import com.parceliq.db.DBConnection;
import com.parceliq.exception.InvalidPickupCodeException;
import com.parceliq.exception.ParcelAlreadyClaimedException;
import com.parceliq.model.Parcel;
import com.parceliq.model.PerishableParcel;
import com.parceliq.model.StandardParcel;

import java.sql.*;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Random;

/**
 * Data Access Object: ParcelDAO
 * Handles parcel creation, guard drop tickets, random shelf allocation (1-150),
 * student OTP generation, OTP lookup filter, camera barcode recognition, and claims.
 */
public class ParcelDAO {

    private final Connection connection;

    public ParcelDAO() {
        this.connection = DBConnection.getConnection();
        refreshUnclaimedCount();
    }

    /**
     * Overloaded method 1: Quick intake
     */
    public Parcel logParcel(String regNo, String carrier) throws SQLException {
        return logParcel(regNo, carrier, "Standard Campus Parcel Delivery");
    }

    /**
     * Overloaded method 2: Intake with remarks
     */
    public Parcel logParcel(String regNo, String carrier, String notes) throws SQLException {
        String trackingNo = generateTrackingNumber(carrier);
        String deliveryPin = "PIN-" + (1000 + new Random().nextInt(9000));
        boolean isCourier = carrier.equalsIgnoreCase("Speed Post") ||
                            carrier.equalsIgnoreCase("DTDC") ||
                            carrier.equalsIgnoreCase("BlueDart") ||
                            carrier.toLowerCase().contains("courier");

        Parcel parcel;
        if (carrier.equalsIgnoreCase("Zomato") ||
            carrier.equalsIgnoreCase("Swiggy") ||
            carrier.equalsIgnoreCase("Blinkit")) {
            parcel = new PerishableParcel(0, trackingNo, regNo, carrier, notes, deliveryPin, 8, 4.0);
        } else {
            parcel = new StandardParcel(0, trackingNo, regNo, carrier, notes, deliveryPin, isCourier, "BOX");
        }

        return logParcel(parcel);
    }

    /**
     * Overloaded method 3: Full Parcel insertion
     */
    public Parcel logParcel(Parcel parcel) throws SQLException {
        String sql = "INSERT INTO parcels (" +
                "tracking_number, student_reg_no, student_name, carrier, delivery_from, item_name, " +
                "item_category, shelf_slot, shelf_number, pickup_token, pickup_otp, delivery_pin, " +
                "barcode_symbol, is_collected, holding_fee, days_held, status, created_at, eta_timestamp, " +
                "arrived_at, arrived_at_str, collected_at, picked_up_at_str, is_courier, gift_theme, " +
                "sender_name, recipient_name, gift_message, packaging_type, max_holding_hours, " +
                "is_expired, cold_storage_temp" +
                ") VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

        try (PreparedStatement ps = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, parcel.getTrackingNumber());
            ps.setString(2, parcel.getStudentRegNo());
            ps.setString(3, parcel.getStudentName() != null ? parcel.getStudentName() : "Student " + parcel.getStudentRegNo());
            ps.setString(4, parcel.getCarrier());
            ps.setString(5, parcel.getDeliveryFrom() != null ? parcel.getDeliveryFrom() : parcel.getCarrier());
            ps.setString(6, parcel.getItemName());
            ps.setString(7, parcel.getItemCategory());
            ps.setString(8, parcel.getShelfSlot());
            ps.setInt(9, parcel.getShelfNumber());
            ps.setString(10, parcel.getPickupToken());
            ps.setString(11, parcel.getPickupOtp());
            ps.setString(12, parcel.getDeliveryPin());
            ps.setString(13, parcel.getBarcodeSymbol());
            ps.setBoolean(14, parcel.isCollected());
            ps.setDouble(15, parcel.getHoldingFee());
            ps.setInt(16, parcel.getDaysHeld());
            ps.setString(17, parcel.getStatus());
            ps.setLong(18, parcel.getCreatedAt());
            ps.setLong(19, parcel.getEtaTimestamp());
            ps.setLong(20, parcel.getArrivedAt());
            ps.setString(21, parcel.getArrivedAtTimeStr() != null ? parcel.getArrivedAtTimeStr() : "");
            ps.setLong(22, parcel.getCollectedAt());
            ps.setString(23, parcel.getPickedUpAtTimeStr() != null ? parcel.getPickedUpAtTimeStr() : "");
            ps.setBoolean(24, parcel.isCourier());
            ps.setString(25, parcel.getGiftTheme());
            ps.setString(26, parcel.getSenderName());
            ps.setString(27, parcel.getRecipientName());
            ps.setString(28, parcel.getGiftMessage());

            if (parcel instanceof StandardParcel) {
                ps.setString(29, ((StandardParcel) parcel).getPackagingType());
                ps.setInt(30, 72);
                ps.setBoolean(31, false);
                ps.setDouble(32, 25.0);
            } else if (parcel instanceof PerishableParcel) {
                PerishableParcel pp = (PerishableParcel) parcel;
                ps.setString(29, "COLD_PACK");
                ps.setInt(30, pp.getMaxHoldingHours());
                ps.setBoolean(31, pp.isExpired());
                ps.setDouble(32, pp.getColdStorageTemp());
            } else {
                ps.setString(29, "STANDARD");
                ps.setInt(30, 48);
                ps.setBoolean(31, false);
                ps.setDouble(32, 20.0);
            }

            ps.executeUpdate();

            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    parcel.setParcelId(rs.getInt(1));
                }
            }
        }

        refreshUnclaimedCount();
        return parcel;
    }

    /**
     * Guard DROP option: Creates a drop ticket.
     * Auto Sr. No, Student's Name, Delivery From, Arrived At, Picked Up At (stays blank!).
     * Randomly generates a shelf number between 1 - 150 space.
     * Generates a 6-digit OTP for student pickup.
     */
    public Parcel createDropTicket(String studentName, String studentRegNo, String deliveryFrom, String itemName) throws SQLException {
        long now = System.currentTimeMillis();
        SimpleDateFormat sdf = new SimpleDateFormat("hh:mm a, dd MMM");
        String arrivedAtStr = sdf.format(new Date(now));

        // Randomly generate shelf number between 1 - 150 space
        int shelfNumber = 1 + new Random().nextInt(150);
        char aisle = (char) ('A' + (shelfNumber % 4));
        String shelfSlot = String.format("SHELF-%c-%02d (Space #%d)", aisle, (shelfNumber % 30) + 1, shelfNumber);

        // Generate 6-digit OTP
        String otp = String.format("%06d", 100000 + new Random().nextInt(900000));
        String pickupToken = "PIQ-TOKEN-" + (1000 + new Random().nextInt(9000));
        String trackingNo = (deliveryFrom.substring(0, Math.min(3, deliveryFrom.length())).toUpperCase()) +
                "-" + (10000 + new Random().nextInt(90000)) + "-IN";
        String barcodeSymbol = "PIQ-BC-" + (10000 + new Random().nextInt(90000));

        boolean isPerishable = deliveryFrom.equalsIgnoreCase("Zomato") ||
                               deliveryFrom.equalsIgnoreCase("Swiggy") ||
                               deliveryFrom.equalsIgnoreCase("Blinkit");
        boolean isCourier = deliveryFrom.toLowerCase().contains("post") ||
                            deliveryFrom.toLowerCase().contains("courier") ||
                            deliveryFrom.toLowerCase().contains("bluedart");

        Parcel parcel;
        if (isPerishable) {
            parcel = new PerishableParcel(0, trackingNo, studentRegNo, deliveryFrom, itemName, "PIN-" + (1000 + new Random().nextInt(9000)), 6, 4.0);
        } else {
            parcel = new StandardParcel(0, trackingNo, studentRegNo, deliveryFrom, itemName, "PIN-" + (1000 + new Random().nextInt(9000)), isCourier, "BOX");
        }

        parcel.setStudentName(studentName);
        parcel.setDeliveryFrom(deliveryFrom);
        parcel.setShelfNumber(shelfNumber);
        parcel.setShelfSlot(shelfSlot);
        parcel.setPickupOtp(otp);
        parcel.setPickupToken(pickupToken);
        parcel.setBarcodeSymbol(barcodeSymbol);
        parcel.setArrivedAt(now);
        parcel.setArrivedAtTimeStr(arrivedAtStr);
        parcel.setPickedUpAtTimeStr(""); // STAYS BLANK UNTIL PICKED UP!
        parcel.setStatus("READY_FOR_PICKUP");

        return logParcel(parcel);
    }

    /**
     * Guard PICKUP option: OTP filter
     * Automatically filters out whose order it is when the guard enters the student's OTP!
     */
    public Parcel lookupByOtp(String otp) throws SQLException {
        if (otp == null || otp.trim().isEmpty()) return null;
        String trimmed = otp.trim();
        String sql = "SELECT * FROM parcels WHERE pickup_otp = ? OR pickup_token = ? OR barcode_symbol = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, trimmed);
            ps.setString(2, trimmed);
            ps.setString(3, trimmed);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapResultSetToParcel(rs);
                }
            }
        }
        return null;
    }

    /**
     * Guard PICKUP option: Confirm pickup with camera barcode recognition / OTP.
     * Sets pickedUpAtTimeStr with current time, marks collected, logs in RDBMS.
     */
    public boolean confirmPickup(int parcelId, String inputCode, String guardId)
            throws SQLException, InvalidPickupCodeException, ParcelAlreadyClaimedException {

        Parcel p = getParcelById(parcelId);
        if (p == null) {
            throw new IllegalArgumentException("Parcel with ID #" + parcelId + " not found.");
        }

        // Prevent double claim
        if (p.isCollected() || (p.getPickedUpAtTimeStr() != null && !p.getPickedUpAtTimeStr().isEmpty())) {
            logPickupAttempt(parcelId, p.getStudentRegNo(), inputCode, guardId, "FAILED_ALREADY_CLAIMED", 0.0);
            throw new ParcelAlreadyClaimedException("Parcel has already been collected and claimed!", p.getParcelId(), p.getTrackingNumber());
        }

        // Verify via Verifiable interface
        if (!p.verifyCode(inputCode)) {
            logPickupAttempt(parcelId, p.getStudentRegNo(), inputCode, guardId, "FAILED_INVALID_CODE", 0.0);
            throw new InvalidPickupCodeException("Code verification mismatch! OTP or barcode does not match ticket.", inputCode, p.getTrackingNumber());
        }

        long now = System.currentTimeMillis();
        SimpleDateFormat sdf = new SimpleDateFormat("hh:mm a, dd MMM");
        String pickedUpStr = sdf.format(new Date(now));

        int daysHeld = 0;
        if (p.getArrivedAt() > 0) {
            daysHeld = (int) Math.max(0, (now - p.getArrivedAt()) / (1000 * 60 * 60 * 24));
        }
        double fee = p.calculateFee(daysHeld);

        String sql = "UPDATE parcels SET is_collected = TRUE, status = 'COLLECTED', collected_at = ?, picked_up_at_str = ?, holding_fee = ?, days_held = ? WHERE id = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setLong(1, now);
            ps.setString(2, pickedUpStr);
            ps.setDouble(3, fee);
            ps.setInt(4, daysHeld);
            ps.setInt(5, parcelId);
            ps.executeUpdate();
        }

        logPickupAttempt(parcelId, p.getStudentRegNo(), inputCode, guardId, "SUCCESS_CLAIMED", fee);
        addRewardPoints(p.getStudentRegNo(), 50);
        refreshUnclaimedCount();
        return true;
    }

    public Parcel arriveAtShelter(int parcelId, String guardDeliveryPinInput) throws Exception {
        Parcel p = getParcelById(parcelId);
        if (p == null) throw new IllegalArgumentException("Parcel #" + parcelId + " not found.");
        if (p.isCollected()) {
            throw new ParcelAlreadyClaimedException("Parcel is already collected.", p.getParcelId(), p.getTrackingNumber());
        }

        int shelfNumber = 1 + new Random().nextInt(150);
        char aisle = (char) ('A' + (shelfNumber % 4));
        String shelfSlot = String.format("SHELF-%c-%02d (Space #%d)", aisle, (shelfNumber % 30) + 1, shelfNumber);
        String pickupToken = "PIQ-TOKEN-" + (1000 + new Random().nextInt(9000));
        String otp = String.format("%06d", 100000 + new Random().nextInt(900000));
        long now = System.currentTimeMillis();
        SimpleDateFormat sdf = new SimpleDateFormat("hh:mm a, dd MMM");
        String arrivedStr = sdf.format(new Date(now));

        String sql = "UPDATE parcels SET status = 'READY_FOR_PICKUP', shelf_slot = ?, shelf_number = ?, pickup_token = ?, pickup_otp = ?, arrived_at = ?, arrived_at_str = ? WHERE id = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, shelfSlot);
            ps.setInt(2, shelfNumber);
            ps.setString(3, pickupToken);
            ps.setString(4, otp);
            ps.setLong(5, now);
            ps.setString(6, arrivedStr);
            ps.setInt(7, parcelId);
            ps.executeUpdate();
        }

        p.setStatus("READY_FOR_PICKUP");
        p.setShelfSlot(shelfSlot);
        p.setShelfNumber(shelfNumber);
        p.setPickupToken(pickupToken);
        p.setPickupOtp(otp);
        p.setArrivedAt(now);
        p.setArrivedAtTimeStr(arrivedStr);

        refreshUnclaimedCount();
        return p;
    }

    public List<Parcel> getParcelsByStudent(String regNo) throws SQLException {
        String sql = "SELECT * FROM parcels WHERE student_reg_no = ? ORDER BY id DESC";
        List<Parcel> list = new ArrayList<>();
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, regNo);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapResultSetToParcel(rs));
                }
            }
        }
        return list;
    }

    public List<Parcel> getAllParcels() throws SQLException {
        String sql = "SELECT * FROM parcels ORDER BY id DESC";
        List<Parcel> list = new ArrayList<>();
        try (PreparedStatement ps = connection.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                list.add(mapResultSetToParcel(rs));
            }
        }
        return list;
    }

    public Parcel getParcelById(int id) throws SQLException {
        String sql = "SELECT * FROM parcels WHERE id = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapResultSetToParcel(rs);
                }
            }
        }
        return null;
    }

    public void addRewardPoints(String regNo, int points) {
        String sql = "UPDATE students SET reward_points = reward_points + ?, gift_tokens = gift_tokens + (? / 50) WHERE reg_no = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, points);
            ps.setInt(2, points);
            ps.setString(3, regNo);
            ps.executeUpdate();
        } catch (SQLException e) {
            System.err.println("[ParcelDAO] Error updating rewards: " + e.getMessage());
        }
    }

    public void refreshUnclaimedCount() {
        String sql = "SELECT COUNT(*) FROM parcels WHERE is_collected = FALSE";
        try (Statement st = connection.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            if (rs.next()) {
                Parcel.totalUnclaimedCount = rs.getInt(1);
            }
        } catch (SQLException e) {
            System.err.println("[ParcelDAO] Error refreshing unclaimed count: " + e.getMessage());
        }
    }

    private void logPickupAttempt(int parcelId, String regNo, String code, String guardId, String actionStatus, double fee) {
        String sql = "INSERT INTO pickuplogs (parcel_id, student_reg_no, pickup_code_used, guard_id, verified_at, action_status, fee_charged) " +
                     "VALUES (?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, parcelId);
            ps.setString(2, regNo);
            ps.setString(3, code);
            ps.setString(4, guardId != null ? guardId : "GUARD_DESK");
            ps.setLong(5, System.currentTimeMillis());
            ps.setString(6, actionStatus);
            ps.setDouble(7, fee);
            ps.executeUpdate();
        } catch (SQLException e) {
            System.err.println("[ParcelDAO] Warning logging pickup: " + e.getMessage());
        }
    }

    private Parcel mapResultSetToParcel(ResultSet rs) throws SQLException {
        int id = rs.getInt("id");
        String trackingNo = rs.getString("tracking_number");
        String regNo = rs.getString("student_reg_no");
        String studentName = rs.getString("student_name");
        String carrier = rs.getString("carrier");
        String deliveryFrom = rs.getString("delivery_from");
        String itemName = rs.getString("item_name");
        String category = rs.getString("item_category");
        String shelfSlot = rs.getString("shelf_slot");
        int shelfNumber = rs.getInt("shelf_number");
        String pickupToken = rs.getString("pickup_token");
        String pickupOtp = rs.getString("pickup_otp");
        String deliveryPin = rs.getString("delivery_pin");
        String barcodeSymbol = rs.getString("barcode_symbol");
        boolean isCollected = rs.getBoolean("is_collected");
        double holdingFee = rs.getDouble("holding_fee");
        int daysHeld = rs.getInt("days_held");
        String status = rs.getString("status");
        long createdAt = rs.getLong("created_at");
        long etaTimestamp = rs.getLong("eta_timestamp");
        long arrivedAt = rs.getLong("arrived_at");
        String arrivedAtStr = rs.getString("arrived_at_str");
        long collectedAt = rs.getLong("collected_at");
        String pickedUpAtStr = rs.getString("picked_up_at_str");
        boolean isCourier = rs.getBoolean("is_courier");
        String giftTheme = rs.getString("gift_theme");
        String senderName = rs.getString("sender_name");
        String recipientName = rs.getString("recipient_name");
        String giftMessage = rs.getString("gift_message");

        if ("PERISHABLE".equalsIgnoreCase(category)) {
            int maxHoldingHours = rs.getInt("max_holding_hours");
            boolean isExpired = rs.getBoolean("is_expired");
            double coldStorageTemp = rs.getDouble("cold_storage_temp");
            return new PerishableParcel(id, trackingNo, regNo, studentName, carrier, deliveryFrom, itemName,
                    shelfSlot, shelfNumber, pickupToken, pickupOtp, deliveryPin, barcodeSymbol, isCollected,
                    holdingFee, daysHeld, status, createdAt, etaTimestamp, arrivedAt, arrivedAtStr,
                    collectedAt, pickedUpAtStr, isCourier, giftTheme, senderName, recipientName, giftMessage,
                    maxHoldingHours, isExpired, coldStorageTemp);
        } else {
            String packagingType = rs.getString("packaging_type");
            return new StandardParcel(id, trackingNo, regNo, studentName, carrier, deliveryFrom, itemName,
                    shelfSlot, shelfNumber, pickupToken, pickupOtp, deliveryPin, barcodeSymbol, isCollected,
                    holdingFee, daysHeld, status, createdAt, etaTimestamp, arrivedAt, arrivedAtStr,
                    collectedAt, pickedUpAtStr, isCourier, giftTheme, senderName, recipientName, giftMessage,
                    packagingType);
        }
    }

    private String generateTrackingNumber(String carrier) {
        String prefix = carrier.substring(0, Math.min(3, carrier.length())).toUpperCase();
        int randomCode = 10000 + new Random().nextInt(90000);
        return prefix + "-" + randomCode + "-IN";
    }
}
