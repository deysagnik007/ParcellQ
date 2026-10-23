package com.parceliq;

import com.parceliq.dao.ParcelDAO;
import com.parceliq.db.DBConnection;
import com.parceliq.model.Parcel;
import com.parceliq.model.PerishableParcel;
import com.parceliq.model.StandardParcel;
import com.parceliq.server.ParcelHttpServer;
import com.parceliq.util.ReportGenerator;
import com.parceliq.worker.ExpiryWatcherThread;

import java.io.InputStream;
import java.util.Properties;

/**
 * Runner / Application Controller: Main
 * Launches ParcelIQ backend services, initializes RDBMS, starts background worker thread,
 * and launches embedded web server.
 */
public class Main {

    public static void main(String[] args) {
        System.out.println("==========================================================================");
        System.out.println("                  PARCELIQ : CAMPUS LOGISTICS & SOCIAL TRACKER            ");
        System.out.println("                      Core Logic: OOPs in Java & RDBMS                    ");
        System.out.println("==========================================================================");

        // 1. Initialize RDBMS Database Connection (MySQL / Embedded H2 Mode)
        System.out.println("[Main] 1. Initializing RDBMS Connection...");
        DBConnection.getConnection();
        System.out.println("[Main] -> Active RDBMS Engine: " + DBConnection.getActiveDbType());

        // 2. Syllabus Concepts Verification / Smoke Demonstration
        System.out.println("[Main] 2. Verifying OOPs Syllabus Architecture...");
        demonstrateOopConcepts();

        // 3. Start Background Multithreading Worker (ExpiryWatcherThread)
        System.out.println("[Main] 3. Launching Background Worker (Multithreading)...");
        ExpiryWatcherThread expiryWatcher = new ExpiryWatcherThread(15000);
        expiryWatcher.start();

        // 4. Test File I/O & Byte Streams Report Generation
        System.out.println("[Main] 4. Testing File I/O & Byte Streams (ReportGenerator)...");
        try {
            ReportGenerator reporter = new ReportGenerator();
            reporter.generateDailyAuditReport(".");
        } catch (Exception e) {
            System.err.println("[Main] Notice during initial report generation: " + e.getMessage());
        }

        // 5. Start Embedded HTTP Web Server
        int port = 8080;
        Properties props = new Properties();
        try (InputStream in = Main.class.getClassLoader().getResourceAsStream("application.properties")) {
            if (in != null) props.load(in);
            port = Integer.parseInt(props.getProperty("server.port", "8080"));
        } catch (Exception ignored) {}

        System.out.println("[Main] 5. Starting Web Server on port " + port + "...");
        ParcelHttpServer server = new ParcelHttpServer(port, expiryWatcher);
        try {
            server.start();
            System.out.println("==========================================================================");
            System.out.println("  >>> PARCELIQ WEB APPLICATION IS RUNNING SUCCESSFULLY! <<<");
            System.out.println("  >>> Open your browser at: http://localhost:" + port + " <<<");
            System.out.println("==========================================================================");
        } catch (Exception e) {
            System.err.println("[Main] Failed to start HTTP server: " + e.getMessage());
            e.printStackTrace();
        }

        // Keep process running
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println("\n[Main] Shutting down ParcelIQ gracefully...");
            expiryWatcher.stopWatching();
            server.stop();
        }));
    }

    /**
     * Demonstrates core OOP concepts required in the syllabus screenshots:
     * - Inheritance & Polymorphic method calculateFee()
     * - Downcasting: ((PerishableParcel) p).checkExpiry()
     * - Interface Verifiable
     * - Static variables & methods
     */
    private static void demonstrateOopConcepts() {
        // Polymorphism demo
        Parcel standard = new StandardParcel(101, "AMZ-TEST-01", "23BCS1088", "Aarav Sharma", "Amazon", "Amazon", "Books",
                "SHELF-A-01", 24, "PIQ-TOKEN-1111", "111100", "PIN-1234", "PIQ-BC-101", false, 0.0, 5, "READY_FOR_PICKUP",
                System.currentTimeMillis(), System.currentTimeMillis() + 600000, System.currentTimeMillis() - 86400000L * 5,
                "10:00 AM, Yesterday", 0, "", false, null, null, null, null, "BOX");

        Parcel perishable = new PerishableParcel(102, "ZOM-TEST-02", "23BCS1088", "Aarav Sharma", "Zomato", "Zomato", "Pizza",
                "SHELF-B-02", 72, "PIQ-TOKEN-2222", "222200", "PIN-5678", "PIQ-BC-102", false, 0.0, 2, "READY_FOR_PICKUP",
                System.currentTimeMillis(), System.currentTimeMillis() + 600000, System.currentTimeMillis() - 86400000L * 2,
                "12:30 PM, Yesterday", 0, "", false, null, null, null, null, 4, false, 4.0);

        System.out.println("   [OOP Demo] StandardParcel fee (5 days held, 3 free): INR " + standard.calculateFee(5));
        System.out.println("   [OOP Demo] PerishableParcel fee (2 days held, cold storage): INR " + perishable.calculateFee(2));

        // Downcasting demo
        if (perishable instanceof PerishableParcel) {
            PerishableParcel pp = (PerishableParcel) perishable;
            System.out.println("   [OOP Demo] Type Casting Downcast: ((PerishableParcel) p).checkExpiry() = " + pp.checkExpiry());
        }

        // Interface Verifiable demo
        System.out.println("   [OOP Demo] Verifiable.verifyCode('PIQ-TOKEN-1111') = " + standard.verifyCode("PIQ-TOKEN-1111"));
        System.out.println("   [OOP Demo] Static totalUnclaimedCount = " + Parcel.totalUnclaimedCount);
    }
}
