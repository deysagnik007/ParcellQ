# 📦 ParcellQ - University Parcel & Shelter Logistics Management System

**ParcellQ** is a campus parcel intake, shelter space allocation, and pickup verification system built using **Object-Oriented Programming (OOP) in Java** and backed by **RDBMS / MySQL** (with automated in-memory H2 fallback in MySQL compatibility mode).

The platform coordinates parcel deliveries from online platforms (*Amazon, Flipkart, Blinkit, Zomato, Swiggy, Myntra, BlueDart, Speed Post, etc.*) to university shelter points, automatically assigning secure physical shelf coordinates and generating secret pickup verification tokens.

---

## 🌟 Key Features

### 1. 🚪 3-Way Role Gateway
The system greets users with three clear access options:
- 🎓 **Login as Student**: Unique personal order notifications, shelf space tracking (1–150), secret OTP retrieval, and issue dispute ticket filing.
- 🛡️ **Login as Guard**: Guard station with **DROP** and **PICK UP** operational modes.
- ⚙️ **Login as Admin**: 3-hour shelter shift assignment and automatic generation of unique Guard Roll No IDs.

---

### 2. ⚙️ Admin Portal: 3-Hour Shift Assignment & Auto Guard Roll No
- Administrators assign campus security guards to the pickup & drop shelter.
- Configurable **3-hour shift slots**:
  * `06:00 AM - 09:00 AM (Morning Shift)`
  * `09:00 AM - 12:00 PM (Forenoon Shift)`
  * `12:00 PM - 03:00 PM (Afternoon Shift)`
  * `03:00 PM - 06:00 PM (Evening Shift)`
  * `06:00 PM - 09:00 PM (Night Shift 1)`
  * `09:00 PM - 12:00 AM (Night Shift 2)`
- **Auto-Generated Guard Roll No ID**:
  * Upon assigning a shift, the system auto-generates a unique Guard Roll No ID (e.g. `GRD-2739`).
  * The guard uses this Roll No ID as their unique login credential to enter the Guard Station.
- Live duty roster table displays all assigned guards, phone numbers, shifts, and shelter locations.

---

### 3. 🛡️ Guard Shelter Station

#### A. 📦 DROP Option (Incoming Parcel Intake)
- Auto-generates **Sr. No.** (e.g., `SR-440023`).
- Guard enters **Student's Roll No** (auto-fills/validates registered student name) and **Delivery From** (*Amazon, Flipkart, Swiggy, Blinkit, etc.*).
- **Random Shelf Space (1–150)**: Randomly generates a shelf number between 1 and 150 where the physical parcel must be placed.
- **Secret 6-Digit OTP**: Generates a secure OTP sent privately to the student. The OTP is strictly hidden from the guard at drop time to prevent theft and maintain verification integrity.
- **Picked Up At**: Stays blank until the student arrives and collects the order.
- Stacked drop table shows all arrived shelter packages chronologically.

#### B. 🔑 PICK UP Option (OTP Filter & Barcode Scanner)
- The student visits the shelter and recites their secret **OTP**.
- As the guard types the OTP, the system **automatically filters out whose order it is** and displays their drop ticket.
- **Camera Barcode Recognition 📷**:
  * Right beside the ticket is the Camera Scanner option.
  * Simulates optical barcode detection (`PIQ-BC-xxxxx`).
  * Recognizing the barcode marks the package `COLLECTED`, sets the exact collection timestamp, and updates the database.

---

### 4. 🎓 Student Portal & Security Help Sign

- **Unique Personal Page**:
  * Login with **Name** and **Password**.
  * First-time student logins pop out a prompt to register their **University Roll No**.
  * Each student sees **only their own arrived orders** linked to their Roll No.
  * **No Dummy Orders**: Displays a clean empty state until a guard creates a drop ticket for their Roll No.
- **Arrival Notification Ticket**:
  * Shows carrier, *Dropped off at* timing, *Picked up at* timing (or pending status), allocated *Shelf Space # (1-150)*, and the secret *OTP*.
- **Help Sign (Red Triangle with White Rectangle ⚠️)**:
  * Marked with the red hazard triangle containing a white rectangle.
  * Allows students to raise incident tickets for:
    1. 🚨 **Fraudulent Order / Incorrect Item**
    2. ⚠️ **Malicious Tampering / Damaged Seal**
    3. 📦 **Missing Item from Order**
  * Incident reports are logged in the MySQL `dispute_tickets` table for security investigation.

---

## 🗄️ Database Architecture (MySQL / RDBMS)

The backend utilizes JDBC `PreparedStatement` queries with parameterized inputs to guard against SQL injection:

| Table | Purpose |
| :--- | :--- |
| **`students`** | University student accounts (`reg_no` PK, `name`, `password`, `phone`, `hostel_block`, `room_number`) |
| **`guard_shifts`** | Admin-assigned shifts (`guard_roll_no` UNIQUE, `guard_name`, `phone`, `shift_time`, `shelter_type`) |
| **`parcels`** | Parcel intake states (`tracking_number`, `student_reg_no`, `shelf_number` (1-150), `pickup_otp`, `barcode_symbol`, `arrived_at_str`, `picked_up_at_str`, `is_collected`) |
| **`dispute_tickets`** | Security tickets raised via the Help Sign (`ticket_number`, `student_reg_no`, `parcel_id`, `issue_type`, `description`) |
| **`pickuplogs`** | Audit trail recording guard ID, verification code, timestamp, and release status |

---

## 💻 Tech Stack

- **Backend**: Java 17, Java HTTP Server (`com.sun.net.httpserver`)
- **Database**: MySQL 8.x / Embedded H2 in MySQL Compatibility Mode via JDBC
- **Serialization**: Google Gson 2.10.1
- **Frontend**: HTML5, Vanilla JavaScript (ES6+), CSS3 Modern Flex/Grid Layout
- **Build Tool**: Apache Maven 3.x

---

## 🚀 Getting Started

### Prerequisites
- Java JDK 17 or higher
- Apache Maven 3.6+

### Run Locally
```bash
# Clone the repository
git clone https://github.com/deysagnik007/ParcellQ.git
cd ParcellQ

# Compile the application
mvn compile

# Launch the server
mvn exec:java
```

Once started, open your browser at:
```text
http://localhost:8080
```

---

## 🔑 Pre-Configured Demo Credentials

| Role | Login Identifier | Password / Code |
| :--- | :--- | :--- |
| **Admin** | `admin` | `admin123` |
| **Guard** | Auto-Generated Roll No ID (e.g. `GRD-1001` or newly generated from Admin) | *Roll No acts as login credential* |
| **Student (Existing)** | `Aryan Sharma` | `aryan123` *(Roll No: `23BCS1088`)* |
| **Student (New)** | *Any new name* | *Any password (Roll No pop-out will prompt on first login)* |
