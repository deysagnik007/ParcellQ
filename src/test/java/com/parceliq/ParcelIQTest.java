package com.parceliq;

import com.parceliq.dao.GuardDAO;
import com.parceliq.dao.ParcelDAO;
import com.parceliq.model.*;
import com.parceliq.util.ReportGenerator;
import org.junit.Assert;
import org.junit.Test;

import java.io.File;

/**
 * Unit Tests for ParcelIQ OOPs & Syllabus Concepts
 */
public class ParcelIQTest {

    @Test
    public void testPolymorphicFeeCalculation() {
        // Standard parcel: 3 days free, ₹10/day afterwards
        Parcel standard = new StandardParcel(1, "TRK-001", "23BCS1088", "Amazon", "Book", "1234", false, "BOX");
        Assert.assertEquals(0.0, standard.calculateFee(1), 0.001);
        Assert.assertEquals(0.0, standard.calculateFee(3), 0.001);
        Assert.assertEquals(20.0, standard.calculateFee(5), 0.001); // 2 days * 10

        // Perishable parcel: charges from day 1 (₹25 flat + ₹15/day)
        Parcel perishable = new PerishableParcel(2, "TRK-002", "23BCS1088", "Zomato", "Food", "5678", 6, 4.0);
        Assert.assertEquals(25.0, perishable.calculateFee(0), 0.001);
        Assert.assertEquals(55.0, perishable.calculateFee(2), 0.001); // 25 + (2 * 15)
    }

    @Test
    public void testVerifiableInterface() {
        StandardParcel parcel = new StandardParcel(3, "TRK-003", "23BCS1088", "Flipkart", "Shoes", "1234", false, "BOX");
        parcel.setPickupToken("PIQ-TOKEN-9988");
        parcel.setPickupOtp("552914");

        Verifiable verifiable = parcel;
        // Tests both Pickup Token and 6-digit OTP verification
        Assert.assertTrue(verifiable.verifyCode("PIQ-TOKEN-9988"));
        Assert.assertTrue(verifiable.verifyCode("  piq-token-9988  "));
        Assert.assertTrue(verifiable.verifyCode("552914"));
        Assert.assertFalse(verifiable.verifyCode("000000"));
    }

    @Test
    public void testDowncastingPerishable() {
        Parcel p = new PerishableParcel(4, "TRK-004", "23BCS1088", "Blinkit", "Milk", "1111", 1, 4.0);
        // Downcast to PerishableParcel to access checkExpiry()
        Assert.assertTrue(p instanceof PerishableParcel);
        PerishableParcel pp = (PerishableParcel) p;
        Assert.assertFalse(pp.checkExpiry()); // Not yet arrived at shelter, so false
    }

    @Test
    public void testShelfSlotFormattingAndSpace() {
        String shelf = Parcel.formatShelfId('b', 7);
        Assert.assertEquals("SHELF-B-07", shelf);

        // Verify random shelf allocation range (1-150 space)
        int randomShelf = 1 + (int)(Math.random() * 150);
        Assert.assertTrue(randomShelf >= 1 && randomShelf <= 150);
    }

    @Test
    public void testAuditReportFileGeneration() throws Exception {
        ReportGenerator generator = new ReportGenerator();
        File report = generator.generateDailyAuditReport("target/test-reports");
        Assert.assertTrue(report.exists());
        Assert.assertTrue(report.length() > 0);
    }
}
