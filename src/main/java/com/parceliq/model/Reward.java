package com.parceliq.model;

/**
 * Model: Reward
 * Represents reward vouchers and perks earned from tracking packages and couriers.
 */
public class Reward {
    private int id;
    private String studentRegNo;
    private String title;
    private String description;
    private int costPoints;
    private String voucherCode;
    private boolean isRedeemed;

    public Reward(int id, String studentRegNo, String title, String description, int costPoints, String voucherCode, boolean isRedeemed) {
        this.id = id;
        this.studentRegNo = studentRegNo;
        this.title = title;
        this.description = description;
        this.costPoints = costPoints;
        this.voucherCode = voucherCode;
        this.isRedeemed = isRedeemed;
    }

    public int getId() { return id; }
    public String getStudentRegNo() { return studentRegNo; }
    public String getTitle() { return title; }
    public String getDescription() { return description; }
    public int getCostPoints() { return costPoints; }
    public String getVoucherCode() { return voucherCode; }
    public boolean isRedeemed() { return isRedeemed; }
    public void setRedeemed(boolean redeemed) { isRedeemed = redeemed; }
}
