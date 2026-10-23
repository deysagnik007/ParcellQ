package com.parceliq.worker;

import com.parceliq.dao.ParcelDAO;
import com.parceliq.db.DBConnection;
import com.parceliq.model.Parcel;
import com.parceliq.model.PerishableParcel;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.List;

/**
 * Background Worker: ExpiryWatcherThread
 * Syllabus Concepts:
 * 1. Multithreading: Inherits from java.lang.Thread, executes an asynchronous background worker loop with Thread.sleep()
 * 2. Type Casting & Downcasting: ((PerishableParcel) p).checkExpiry()
 * 3. Real-world purpose: Monitors perishable food orders (Zomato, Swiggy, Blinkit) held in hostel shelter
 *    cold-lockers. Flags spoiled items for hygiene audits and alerts students.
 */
public class ExpiryWatcherThread extends Thread {

    private final ParcelDAO parcelDAO;
    private final long checkIntervalMs;
    private volatile boolean running = true;
    private int expiredFoundCount = 0;

    public ExpiryWatcherThread(long checkIntervalMs) {
        super("ParcelIQ-ExpiryWatcher-Worker");
        this.parcelDAO = new ParcelDAO();
        this.checkIntervalMs = checkIntervalMs > 0 ? checkIntervalMs : 15000;
        setDaemon(true); // Daemon thread will not block application shutdown
    }

    public ExpiryWatcherThread() {
        this(15000);
    }

    @Override
    public void run() {
        System.out.println("[ExpiryWatcherThread] Background thread started. Monitoring perishable parcels every " +
                (checkIntervalMs / 1000) + " seconds.");

        while (running) {
            try {
                inspectPerishables();
                Thread.sleep(checkIntervalMs);
            } catch (InterruptedException e) {
                System.out.println("[ExpiryWatcherThread] Worker interrupted, terminating gracefully.");
                break;
            } catch (Exception e) {
                System.err.println("[ExpiryWatcherThread] Exception during inspection cycle: " + e.getMessage());
            }
        }
    }

    /**
     * Inspects active parcels in the database.
     * Syllabus Concept: Downcasting base Parcel to PerishableParcel.
     */
    public synchronized void inspectPerishables() {
        try {
            List<Parcel> allParcels = parcelDAO.getAllParcels();
            Connection conn = DBConnection.getConnection();

            for (Parcel p : allParcels) {
                // Check if parcel is perishable and still uncollected at shelter
                if (p instanceof PerishableParcel && !p.isCollected() && "READY_FOR_PICKUP".equals(p.getStatus())) {

                    // Syllabus Concept: Downcasting base Parcel object to access subclass specific behavior
                    PerishableParcel perishable = (PerishableParcel) p;
                    boolean isExpired = perishable.checkExpiry();

                    if (isExpired && !perishable.isExpired()) {
                        expiredFoundCount++;
                        System.out.printf("[ExpiryWatcherThread] ALERT: Parcel #%d (%s for %s) marked EXPIRED/OVERDUE%n",
                                perishable.getParcelId(), perishable.getItemName(), perishable.getStudentRegNo());

                        // Update database flag
                        String sql = "UPDATE parcels SET is_expired = TRUE, status = 'OVERDUE_PERISHABLE' WHERE id = ?";
                        try (PreparedStatement ps = conn.prepareStatement(sql)) {
                            ps.setInt(1, perishable.getParcelId());
                            ps.executeUpdate();
                        }
                    }
                }
            }
        } catch (SQLException e) {
            System.err.println("[ExpiryWatcherThread] Database error during cycle: " + e.getMessage());
        }
    }

    public void stopWatching() {
        this.running = false;
        this.interrupt();
    }

    public boolean isRunning() {
        return running;
    }

    public int getExpiredFoundCount() {
        return expiredFoundCount;
    }
}
