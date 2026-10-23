package com.parceliq.model;

/**
 * Model: GuardShift
 * Represents a security guard assigned by the Administrator to a 3-hour shift
 * at either the Pickup Shelter or Drop Shelter.
 * Includes an auto-generated unique Guard Roll No / ID used for guard authentication.
 */
public class GuardShift {
    private int id;
    private String guardRollNo; // e.g. "GRD-4891" (auto-generated unique credential)
    private String guardName;
    private String phone;
    private String shelterType; // "PICKUP_SHELTER" or "DROP_SHELTER"
    private String shiftTime;   // e.g. "09:00 AM - 12:00 PM (3 Hours)"
    private String assignedBy;  // "Admin"
    private long createdAt;

    public GuardShift(int id, String guardRollNo, String guardName, String phone, String shelterType, String shiftTime, String assignedBy, long createdAt) {
        this.id = id;
        this.guardRollNo = guardRollNo;
        this.guardName = guardName;
        this.phone = phone;
        this.shelterType = shelterType;
        this.shiftTime = shiftTime;
        this.assignedBy = assignedBy;
        this.createdAt = createdAt;
    }

    public GuardShift(int id, String guardName, String phone, String shelterType, String shiftTime, String assignedBy, long createdAt) {
        this(id, "GRD-" + (1000 + (id % 9000)), guardName, phone, shelterType, shiftTime, assignedBy, createdAt);
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getGuardRollNo() { return guardRollNo; }
    public void setGuardRollNo(String guardRollNo) { this.guardRollNo = guardRollNo; }

    public String getGuardName() { return guardName; }
    public void setGuardName(String guardName) { this.guardName = guardName; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public String getShelterType() { return shelterType; }
    public void setShelterType(String shelterType) { this.shelterType = shelterType; }

    public String getShiftTime() { return shiftTime; }
    public void setShiftTime(String shiftTime) { this.shiftTime = shiftTime; }

    public String getAssignedBy() { return assignedBy; }
    public void setAssignedBy(String assignedBy) { this.assignedBy = assignedBy; }

    public long getCreatedAt() { return createdAt; }
    public void setCreatedAt(long createdAt) { this.createdAt = createdAt; }
}
