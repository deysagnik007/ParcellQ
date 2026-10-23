package com.parceliq.model;

/**
 * Abstract Base Class: Parcel
 * Syllabus Concepts:
 * 1. Abstract Class & Contracts (public abstract double calculateFee(int daysHeld))
 * 2. Interfaces (implements Verifiable)
 * 3. Java Basics & Variables (int parcelId, String trackingNumber, boolean isCollected, double holdingFee)
 * 4. Constructors & this keyword
 * 5. Static Keyword (totalUnclaimedCount shared counter)
 * 6. Strings & Arrays (String.format for SHELF-%c-%02d coordinates)
 */
public abstract class Parcel implements Verifiable {

    // Syllabus Concept: Static Keyword - Shared live package counter across all instances
    public static int totalUnclaimedCount = 0;

    // Core delivery & logistics attributes
    protected int parcelId;
    protected String trackingNumber;
    protected String studentRegNo;
    protected String studentName;         // Student full name
    protected String carrier;             // Amazon, Flipkart, Zomato, Swiggy, Myntra, Blinkit, Speed Post, etc.
    protected String deliveryFrom;        // Same as carrier
    protected String itemName;
    protected String itemCategory;        // STANDARD, PERISHABLE, COURIER
    protected String shelfSlot;           // e.g., "SHELF-B-04"
    protected int shelfNumber;            // Space between 1 - 150 (as per spec)
    protected String pickupToken;         // Guard-issued University pickup token
    protected String pickupOtp;           // 6-digit OTP for pickup filter
    protected String deliveryPin;         // Merchant ordering PIN (shared to delivery boy)
    protected String barcodeSymbol;       // Scannable barcode symbol recognized by guard camera
    protected boolean isCollected;
    protected double holdingFee;
    protected int daysHeld;
    protected String status;              // ORDER_PLACED, IN_TRANSIT, DROPPED_AT_SHELTER, READY_FOR_PICKUP, COLLECTED
    protected long createdAt;
    protected long etaTimestamp;          // Expected time of arrival timestamp
    protected long arrivedAt;
    protected String arrivedAtTimeStr;    // Formatted time: "10:35 AM, 22 Oct"
    protected long collectedAt;
    protected String pickedUpAtTimeStr;   // Blank until picked up!
    protected boolean isCourier;

    // Social gifting attributes
    protected String giftTheme;           // BIRTHDAY, BEST_FRIEND, CHILL, LOVING, null
    protected String senderName;
    protected String recipientName;
    protected String giftMessage;

    /**
     * Syllabus Concept: Constructors & 'this' keyword
     */
    public Parcel(int parcelId, String trackingNumber, String studentRegNo, String carrier,
                  String itemName, String deliveryPin, boolean isCourier) {
        this.parcelId = parcelId;
        this.trackingNumber = trackingNumber;
        this.studentRegNo = studentRegNo;
        this.carrier = carrier;
        this.deliveryFrom = carrier;
        this.itemName = itemName;
        this.deliveryPin = deliveryPin;
        this.isCourier = isCourier;
        this.isCollected = false;
        this.holdingFee = 0.0;
        this.daysHeld = 0;
        this.status = "ORDER_PLACED";
        this.createdAt = System.currentTimeMillis();
        this.etaTimestamp = this.createdAt + (20 * 60 * 1000L);
        this.shelfSlot = "UNASSIGNED";
        this.shelfNumber = 0;
        this.pickupToken = "PENDING";
        this.pickupOtp = "";
        this.barcodeSymbol = "PIQ-BC-" + (10000 + (int)(Math.random() * 90000));
        this.arrivedAtTimeStr = "";
        this.pickedUpAtTimeStr = "";
    }

    /**
     * Full constructor for restoring state from database
     */
    public Parcel(int parcelId, String trackingNumber, String studentRegNo, String studentName,
                  String carrier, String deliveryFrom, String itemName, String shelfSlot, int shelfNumber,
                  String pickupToken, String pickupOtp, String deliveryPin, String barcodeSymbol,
                  boolean isCollected, double holdingFee, int daysHeld, String status,
                  long createdAt, long etaTimestamp, long arrivedAt, String arrivedAtTimeStr,
                  long collectedAt, String pickedUpAtTimeStr, boolean isCourier, String giftTheme,
                  String senderName, String recipientName, String giftMessage) {
        this.parcelId = parcelId;
        this.trackingNumber = trackingNumber;
        this.studentRegNo = studentRegNo;
        this.studentName = studentName;
        this.carrier = carrier;
        this.deliveryFrom = (deliveryFrom != null && !deliveryFrom.isEmpty()) ? deliveryFrom : carrier;
        this.itemName = itemName;
        this.shelfSlot = shelfSlot;
        this.shelfNumber = shelfNumber;
        this.pickupToken = pickupToken;
        this.pickupOtp = pickupOtp;
        this.deliveryPin = deliveryPin;
        this.barcodeSymbol = barcodeSymbol;
        this.isCollected = isCollected;
        this.holdingFee = holdingFee;
        this.daysHeld = daysHeld;
        this.status = status;
        this.createdAt = createdAt;
        this.etaTimestamp = etaTimestamp;
        this.arrivedAt = arrivedAt;
        this.arrivedAtTimeStr = arrivedAtTimeStr != null ? arrivedAtTimeStr : "";
        this.collectedAt = collectedAt;
        this.pickedUpAtTimeStr = pickedUpAtTimeStr != null ? pickedUpAtTimeStr : "";
        this.isCourier = isCourier;
        this.giftTheme = giftTheme;
        this.senderName = senderName;
        this.recipientName = recipientName;
        this.giftMessage = giftMessage;
    }

    /**
     * Syllabus Concept: Abstract Method & Polymorphic Contract
     */
    public abstract double calculateFee(int daysHeld);

    /**
     * Syllabus Concept: Strings & Arrays
     */
    public static String formatShelfId(char aisle, int slot) {
        return String.format("SHELF-%c-%02d", Character.toUpperCase(aisle), slot);
    }

    /**
     * Syllabus Concept: Interface Implementation (Verifiable)
     * Matches either the Pickup Token or the 6-digit OTP code!
     */
    @Override
    public boolean verifyCode(String inputCode) {
        if (inputCode == null) return false;
        String trimmed = inputCode.trim();
        if (this.pickupOtp != null && this.pickupOtp.equalsIgnoreCase(trimmed)) {
            return true;
        }
        if (this.pickupToken != null && this.pickupToken.trim().equalsIgnoreCase(trimmed)) {
            return true;
        }
        if (this.barcodeSymbol != null && this.barcodeSymbol.trim().equalsIgnoreCase(trimmed)) {
            return true;
        }
        return false;
    }

    // Getters and Setters
    public int getParcelId() { return parcelId; }
    public void setParcelId(int parcelId) { this.parcelId = parcelId; }

    public String getTrackingNumber() { return trackingNumber; }
    public void setTrackingNumber(String trackingNumber) { this.trackingNumber = trackingNumber; }

    public String getStudentRegNo() { return studentRegNo; }
    public void setStudentRegNo(String studentRegNo) { this.studentRegNo = studentRegNo; }

    public String getStudentName() { return studentName; }
    public void setStudentName(String studentName) { this.studentName = studentName; }

    public String getCarrier() { return carrier; }
    public void setCarrier(String carrier) { this.carrier = carrier; this.deliveryFrom = carrier; }

    public String getDeliveryFrom() { return deliveryFrom; }
    public void setDeliveryFrom(String deliveryFrom) { this.deliveryFrom = deliveryFrom; this.carrier = deliveryFrom; }

    public String getItemName() { return itemName; }
    public void setItemName(String itemName) { this.itemName = itemName; }

    public String getItemCategory() { return itemCategory; }
    public void setItemCategory(String itemCategory) { this.itemCategory = itemCategory; }

    public String getShelfSlot() { return shelfSlot; }
    public void setShelfSlot(String shelfSlot) { this.shelfSlot = shelfSlot; }

    public int getShelfNumber() { return shelfNumber; }
    public void setShelfNumber(int shelfNumber) { this.shelfNumber = shelfNumber; }

    public String getPickupToken() { return pickupToken; }
    public void setPickupToken(String pickupToken) { this.pickupToken = pickupToken; }

    public String getPickupOtp() { return pickupOtp; }
    public void setPickupOtp(String pickupOtp) { this.pickupOtp = pickupOtp; }

    public String getDeliveryPin() { return deliveryPin; }
    public void setDeliveryPin(String deliveryPin) { this.deliveryPin = deliveryPin; }

    public String getBarcodeSymbol() { return barcodeSymbol; }
    public void setBarcodeSymbol(String barcodeSymbol) { this.barcodeSymbol = barcodeSymbol; }

    public boolean isCollected() { return isCollected; }
    public void setCollected(boolean collected) { isCollected = collected; }

    public double getHoldingFee() { return holdingFee; }
    public void setHoldingFee(double holdingFee) { this.holdingFee = holdingFee; }

    public int getDaysHeld() { return daysHeld; }
    public void setDaysHeld(int daysHeld) { this.daysHeld = daysHeld; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public long getCreatedAt() { return createdAt; }
    public void setCreatedAt(long createdAt) { this.createdAt = createdAt; }

    public long getEtaTimestamp() { return etaTimestamp; }
    public void setEtaTimestamp(long etaTimestamp) { this.etaTimestamp = etaTimestamp; }

    public long getArrivedAt() { return arrivedAt; }
    public void setArrivedAt(long arrivedAt) { this.arrivedAt = arrivedAt; }

    public String getArrivedAtTimeStr() { return arrivedAtTimeStr; }
    public void setArrivedAtTimeStr(String arrivedAtTimeStr) { this.arrivedAtTimeStr = arrivedAtTimeStr; }

    public long getCollectedAt() { return collectedAt; }
    public void setCollectedAt(long collectedAt) { this.collectedAt = collectedAt; }

    public String getPickedUpAtTimeStr() { return pickedUpAtTimeStr; }
    public void setPickedUpAtTimeStr(String pickedUpAtTimeStr) { this.pickedUpAtTimeStr = pickedUpAtTimeStr; }

    public boolean isCourier() { return isCourier; }
    public void setCourier(boolean courier) { isCourier = courier; }

    public String getGiftTheme() { return giftTheme; }
    public void setGiftTheme(String giftTheme) { this.giftTheme = giftTheme; }

    public String getSenderName() { return senderName; }
    public void setSenderName(String senderName) { this.senderName = senderName; }

    public String getRecipientName() { return recipientName; }
    public void setRecipientName(String recipientName) { this.recipientName = recipientName; }

    public String getGiftMessage() { return giftMessage; }
    public void setGiftMessage(String giftMessage) { this.giftMessage = giftMessage; }
}
