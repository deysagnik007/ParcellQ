package com.parceliq.model;

/**
 * Subclass: StandardParcel
 * Syllabus Concepts:
 * 1. Inheritance & super keyword (extends Parcel, invokes super constructor)
 * 2. Polymorphism & Method Overriding (overrides calculateFee(int daysHeld))
 */
public class StandardParcel extends Parcel {

    private String packagingType; // BOX, ENVELOPE, CRATE

    public StandardParcel(int parcelId, String trackingNumber, String studentRegNo, String carrier,
                          String itemName, String deliveryPin, boolean isCourier, String packagingType) {
        super(parcelId, trackingNumber, studentRegNo, carrier, itemName, deliveryPin, isCourier);
        this.itemCategory = "STANDARD";
        this.packagingType = (packagingType != null && !packagingType.isEmpty()) ? packagingType : "BOX";
    }

    public StandardParcel(int parcelId, String trackingNumber, String studentRegNo, String studentName,
                          String carrier, String deliveryFrom, String itemName, String shelfSlot, int shelfNumber,
                          String pickupToken, String pickupOtp, String deliveryPin, String barcodeSymbol,
                          boolean isCollected, double holdingFee, int daysHeld, String status,
                          long createdAt, long etaTimestamp, long arrivedAt, String arrivedAtTimeStr,
                          long collectedAt, String pickedUpAtTimeStr, boolean isCourier, String giftTheme,
                          String senderName, String recipientName, String giftMessage, String packagingType) {
        super(parcelId, trackingNumber, studentRegNo, studentName, carrier, deliveryFrom, itemName, shelfSlot,
              shelfNumber, pickupToken, pickupOtp, deliveryPin, barcodeSymbol, isCollected, holdingFee, daysHeld,
              status, createdAt, etaTimestamp, arrivedAt, arrivedAtTimeStr, collectedAt, pickedUpAtTimeStr,
              isCourier, giftTheme, senderName, recipientName, giftMessage);
        this.itemCategory = "STANDARD";
        this.packagingType = packagingType;
    }

    @Override
    public double calculateFee(int daysHeld) {
        if (daysHeld <= 3) {
            return 0.0;
        }
        return (daysHeld - 3) * 10.0;
    }

    public String getPackagingType() { return packagingType; }
    public void setPackagingType(String packagingType) { this.packagingType = packagingType; }
}
