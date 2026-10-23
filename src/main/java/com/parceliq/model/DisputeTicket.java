package com.parceliq.model;

/**
 * Model: DisputeTicket
 * Represents a student-raised issue triggered by the Help Sign (Red Triangle with White Rectangle)
 * Categories: FRAUD, MALICIOUS_TAMPERING, MISSING_ITEM
 */
public class DisputeTicket {
    private int id;
    private String ticketNumber;   // e.g. "DSP-9102"
    private String studentRegNo;
    private int parcelId;
    private String issueType;      // "FRAUD", "MALICIOUS_TAMPERING", "MISSING_ITEM"
    private String description;
    private String status;         // "UNDER_INVESTIGATION", "RESOLVED", "REFUND_INITIATED"
    private long createdAt;
    private String createdAtFormatted;

    public DisputeTicket(int id, String ticketNumber, String studentRegNo, int parcelId,
                         String issueType, String description, String status, long createdAt,
                         String createdAtFormatted) {
        this.id = id;
        this.ticketNumber = ticketNumber;
        this.studentRegNo = studentRegNo;
        this.parcelId = parcelId;
        this.issueType = issueType;
        this.description = description;
        this.status = status;
        this.createdAt = createdAt;
        this.createdAtFormatted = createdAtFormatted;
    }

    public int getId() { return id; }
    public String getTicketNumber() { return ticketNumber; }
    public String getStudentRegNo() { return studentRegNo; }
    public int getParcelId() { return parcelId; }
    public String getIssueType() { return issueType; }
    public String getDescription() { return description; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public long getCreatedAt() { return createdAt; }
    public String getCreatedAtFormatted() { return createdAtFormatted; }
}
