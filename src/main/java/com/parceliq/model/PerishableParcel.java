package com.parceliq.model;

/**
 * Subclass: PerishableParcel
 * Syllabus Concepts:
 * 1. Inheritance & super keyword (extends Parcel, invokes super constructor)
 * 2. Polymorphism & Method Overriding (overrides calculateFee(int daysHeld))
 * 3. Type Casting / Downcasting (((PerishableParcel) p).checkExpiry())
 */
public class PerishableParcel extends Parcel {

    private int maxHoldingHours;          // Maximum hours before spoiled
    private boolean isExpired;
    private double coldStorageTemp;       // e.g., 4.0 °C

    public PerishableParcel(int parcelId, String trackingNumber, String studentRegNo, String carrier,
                            String itemName, String deliveryPin, int maxHoldingHours, double coldStorageTemp) {
        super(parcelId, trackingNumber, studentRegNo, carrier, itemName, deliveryPin, false);
        this.itemCategory = "PERISHABLE";
        this.maxHoldingHours = maxHoldingHours > 0 ? maxHoldingHours : 12;
        this.isExpired = false;
        this.coldStorageTemp = coldStorageTemp;
    }

    public PerishableParcel(int parcelId, String trackingNumber, String studentRegNo, String studentName,
                            String carrier, String deliveryFrom, String itemName, String shelfSlot, int shelfNumber,
                            String pickupToken, String pickupOtp, String deliveryPin, String barcodeSymbol,
                            boolean isCollected, double holdingFee, int daysHeld, String status,
                            long createdAt, long etaTimestamp, long arrivedAt, String arrivedAtTimeStr,
                            long collectedAt, String pickedUpAtTimeStr, boolean isCourier, String giftTheme,
                            String senderName, String recipientName, String giftMessage, int maxHoldingHours,
                            boolean isExpired, double coldStorageTemp) {
        super(parcelId, trackingNumber, studentRegNo, studentName, carrier, deliveryFrom, itemName, shelfSlot,
              shelfNumber, pickupToken, pickupOtp, deliveryPin, barcodeSymbol, isCollected, holdingFee, daysHeld,
              status, createdAt, etaTimestamp, arrivedAt, arrivedAtTimeStr, collectedAt, pickedUpAtTimeStr,
              isCourier, giftTheme, senderName, recipientName, giftMessage);
        this.itemCategory = "PERISHABLE";
        this.maxHoldingHours = maxHoldingHours;
        this.isExpired = isExpired;
        this.coldStorageTemp = coldStorageTemp;
    }

    @Override
    public double calculateFee(int daysHeld) {
        return 25.0 + (daysHeld * 15.0);
    }

    public boolean checkExpiry() {
        if (isCollected) {
            return false;
        }
        if (arrivedAt <= 0) {
            return false;
        }
        long hoursElapsed = (System.currentTimeMillis() - arrivedAt) / (1000 * 60 * 60);
        if (hoursElapsed >= maxHoldingHours) {
            this.isExpired = true;
            return true;
        }
        return false;
    }

    public int getMaxHoldingHours() { return maxHoldingHours; }
    public void setMaxHoldingHours(int maxHoldingHours) { this.maxHoldingHours = maxHoldingHours; }

    public boolean isExpired() { return isExpired; }
    public void setExpired(boolean expired) { isExpired = expired; }

    public double getColdStorageTemp() { return coldStorageTemp; }
    public void setColdStorageTemp(double coldStorageTemp) { this.coldStorageTemp = coldStorageTemp; }
}
