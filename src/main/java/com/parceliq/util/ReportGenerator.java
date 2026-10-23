package com.parceliq.util;

import com.parceliq.dao.ParcelDAO;
import com.parceliq.model.Parcel;
import com.parceliq.model.PerishableParcel;
import com.parceliq.model.StandardParcel;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

/**
 * Utility: ReportGenerator
 * Syllabus Concepts:
 * 1. File I/O & Byte Streams: Uses FileOutputStream and FileWriter / BufferedWriter
 *    to generate daily_audit_report.txt for university hostel authority and logistics staff.
 * 2. Demonstrates Byte Stream reading (FileInputStream) to calculate file bytes & stream output.
 */
public class ReportGenerator {

    private final ParcelDAO parcelDAO;
    public static final String REPORT_FILENAME = "daily_audit_report.txt";

    public ReportGenerator() {
        this.parcelDAO = new ParcelDAO();
    }

    /**
     * Syllabus Concept: File I/O & Byte Streams
     * Generates a physical audit text file using FileOutputStream and character writers.
     * @param targetDirectory Directory where the report will be written.
     * @return File object pointing to the written report.
     * @throws IOException If file writing fails.
     */
    public File generateDailyAuditReport(String targetDirectory) throws IOException {
        File dir = (targetDirectory != null && !targetDirectory.isEmpty()) ? new File(targetDirectory) : new File(".");
        if (!dir.exists()) {
            dir.mkdirs();
        }
        File reportFile = new File(dir, REPORT_FILENAME);

        List<Parcel> parcels;
        try {
            parcels = parcelDAO.getAllParcels();
        } catch (Exception e) {
            throw new IOException("Failed to load parcels for audit: " + e.getMessage(), e);
        }

        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        String timestamp = sdf.format(new Date());

        // Using FileOutputStream (Byte Stream) wrapped in OutputStreamWriter for proper charset handling
        try (FileOutputStream fos = new FileOutputStream(reportFile);
             OutputStreamWriter osw = new OutputStreamWriter(fos, StandardCharsets.UTF_8);
             BufferedWriter writer = new BufferedWriter(osw)) {

            writer.write("========================================================================\n");
            writer.write("           PARCELIQ - CAMPUS SHELTER DAILY AUDIT DISPATCH REPORT         \n");
            writer.write("========================================================================\n");
            writer.write("Generated At: " + timestamp + "\n");
            writer.write("Total Packages In Log: " + parcels.size() + "\n");
            writer.write("Current Unclaimed Count (Static Counter): " + Parcel.totalUnclaimedCount + "\n");
            writer.write("========================================================================\n\n");

            writer.write(String.format("%-6s | %-15s | %-12s | %-12s | %-12s | %-14s | %-8s | %s\n",
                    "ID", "TRACKING #", "CARRIER", "STUDENT", "SHELF", "TOKEN", "STATUS", "FEE (INR)"));
            writer.write("--------------------------------------------------------------------------------------------------------\n");

            double totalHoldingFees = 0.0;
            int claimedCount = 0;
            int pendingCount = 0;
            int perishableCount = 0;

            for (Parcel p : parcels) {
                if (p.isCollected()) {
                    claimedCount++;
                } else {
                    pendingCount++;
                }
                if (p instanceof PerishableParcel) {
                    perishableCount++;
                }
                totalHoldingFees += p.getHoldingFee();

                writer.write(String.format("%-6d | %-15s | %-12s | %-12s | %-12s | %-14s | %-8s | INR %.2f\n",
                        p.getParcelId(),
                        p.getTrackingNumber(),
                        p.getCarrier(),
                        p.getStudentRegNo(),
                        p.getShelfSlot(),
                        p.getPickupToken(),
                        p.isCollected() ? "CLAIMED" : p.getStatus(),
                        p.getHoldingFee()));
            }

            writer.write("--------------------------------------------------------------------------------------------------------\n");
            writer.write(String.format("SUMMARY: Claimed: %d | Pending in Shelter: %d | Perishables: %d | Total Fees: INR %.2f\n",
                    claimedCount, pendingCount, perishableCount, totalHoldingFees));
            writer.write("========================================================================\n");
            writer.write("End of Shift Audit Log - Verified by Chief Warden & Shelter Guard Desk\n");
            writer.flush();
        }

        System.out.println("[ReportGenerator] Successfully generated audit report at: " + reportFile.getAbsolutePath());
        return reportFile;
    }

    /**
     * Reads report content back via Byte Stream (FileInputStream)
     */
    public static String readReportContent(File reportFile) throws IOException {
        if (!reportFile.exists()) {
            return "No report generated yet.";
        }
        try (FileInputStream fis = new FileInputStream(reportFile);
             ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            byte[] buffer = new byte[1024];
            int bytesRead;
            while ((bytesRead = fis.read(buffer)) != -1) {
                baos.write(buffer, 0, bytesRead);
            }
            return baos.toString(StandardCharsets.UTF_8.name());
        }
    }
}
