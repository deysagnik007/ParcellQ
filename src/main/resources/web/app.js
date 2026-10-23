// ParcellQ - Campus Logistics & Authentication Controller
const API_BASE = "http://localhost:8080/api";

let activeRole = null;
let currentStudent = null; // { name, regNo }
let currentGuard = null;   // { guardName, guardRollNo, shiftTime }
let currentFilteredTicket = null;
let cachedStudents = [];

document.addEventListener("DOMContentLoaded", () => {
  initDropFormDefaults();
  loadRegisteredStudentsCache();
});

// ==========================================
// 1) AUTHENTICATION & ROLE MODALS
// ==========================================

// Student Modal
function openStudentLoginModal() {
  document.getElementById("studentLoginModal").classList.remove("hidden");
  document.getElementById("studentStep1").classList.remove("hidden");
  document.getElementById("studentStep2").classList.add("hidden");
  document.getElementById("studentStep3").classList.add("hidden");
  document.getElementById("stuName").value = "Aryan Sharma";
  document.getElementById("stuPassword").value = "aryan123";
  document.getElementById("stuName").focus();
}

function closeStudentLoginModal() {
  document.getElementById("studentLoginModal").classList.add("hidden");
}

function backToStudentStep1() {
  document.getElementById("studentStep1").classList.remove("hidden");
  document.getElementById("studentStep2").classList.add("hidden");
  document.getElementById("studentStep3").classList.add("hidden");
}

function openResetPasswordStep() {
  document.getElementById("studentStep1").classList.add("hidden");
  document.getElementById("studentStep2").classList.add("hidden");
  document.getElementById("studentStep3").classList.remove("hidden");
  document.getElementById("resetStuRollNo").focus();
}

async function handleStudentResetPassword() {
  const regNo = document.getElementById("resetStuRollNo").value.trim();
  const newPassword = document.getElementById("resetNewPassword").value.trim();
  const name = document.getElementById("stuName").value.trim();

  if (!regNo || !newPassword) {
    alert("Please enter both your Roll No and new password.");
    return;
  }

  try {
    const res = await fetch(`${API_BASE}/auth/student/reset-password`, {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ name, regNo, newPassword })
    });

    const data = await res.json();
    if (res.ok && data.success) {
      currentStudent = data.student;
      alert(`Password updated successfully in database!\nStudent: ${data.student.name} (Roll No: ${data.student.regNo})`);
      closeStudentLoginModal();
      enterPortalAsStudent();
    } else {
      alert(data.error || "Failed to update password.");
    }
  } catch (err) {
    alert("Connection error.");
  }
}

async function handleStudentLogin(e) {
  e.preventDefault();
  const name = document.getElementById("stuName").value.trim();
  const password = document.getElementById("stuPassword").value.trim();

  if (!name || !password) {
    alert("Please enter both Name and Password.");
    return;
  }

  try {
    const res = await fetch(`${API_BASE}/auth/student/login`, {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ name, password })
    });

    const data = await res.json();

    if (res.ok && data.success) {
      // Existing student logged in
      currentStudent = data.student;
      closeStudentLoginModal();
      enterPortalAsStudent();
    } else if (data.isNewUser) {
      // "the next instance pop out an option to write their roll no. too"
      document.getElementById("studentStep1").classList.add("hidden");
      document.getElementById("studentStep2").classList.remove("hidden");
      document.getElementById("popoutStudentName").innerText = name;
      document.getElementById("stuRollNo").value = "24BCS" + Math.floor(1000 + Math.random() * 9000);
      document.getElementById("stuRollNo").focus();
    } else {
      alert(data.error || "Login failed.");
    }
  } catch (err) {
    console.error("Student login error:", err);
    alert("Unable to connect to server.");
  }
}

async function handleStudentRegister() {
  const name = document.getElementById("stuName").value.trim();
  const password = document.getElementById("stuPassword").value.trim();
  const regNo = document.getElementById("stuRollNo").value.trim();

  if (!regNo) {
    alert("Please enter your University Roll No.");
    return;
  }

  try {
    const res = await fetch(`${API_BASE}/auth/student/register`, {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ name, password, regNo })
    });

    const data = await res.json();
    if (res.ok && data.success) {
      currentStudent = data.student;
      alert(`Account registered in database!\nStudent: ${data.student.name}\nRoll No: ${data.student.regNo}`);
      closeStudentLoginModal();
      enterPortalAsStudent();
    } else {
      alert(data.error || "Registration failed.");
    }
  } catch (err) {
    console.error("Student register error:", err);
    alert("Unable to save account.");
  }
}

function enterPortalAsStudent() {
  activeRole = "STUDENT";
  document.getElementById("landingView").classList.add("hidden");
  document.getElementById("authStatus").classList.remove("hidden");
  document.getElementById("roleIndicator").innerText = `STUDENT: ${currentStudent.name} (Roll No: ${currentStudent.regNo})`;
  document.getElementById("studentWelcomeSubtext").innerText = `Personal parcel arrival notifications for Roll No: ${currentStudent.regNo}`;
  document.getElementById("studentView").classList.remove("hidden");

  fetchStudentArrivalTickets(currentStudent.regNo);
}

// Admin Modal
function openAdminLoginModal() {
  document.getElementById("adminLoginModal").classList.remove("hidden");
  document.getElementById("adminUsername").value = "admin";
  document.getElementById("adminPassword").value = "admin123";
  document.getElementById("adminPassword").focus();
}

function closeAdminLoginModal() {
  document.getElementById("adminLoginModal").classList.add("hidden");
}

async function handleAdminLogin(e) {
  e.preventDefault();
  const username = document.getElementById("adminUsername").value.trim();
  const password = document.getElementById("adminPassword").value.trim();

  try {
    const res = await fetch(`${API_BASE}/auth/admin/login`, {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ username, password })
    });

    const data = await res.json();
    if (res.ok && data.success) {
      activeRole = "ADMIN";
      closeAdminLoginModal();
      document.getElementById("landingView").classList.add("hidden");
      document.getElementById("authStatus").classList.remove("hidden");
      document.getElementById("roleIndicator").innerText = `ADMIN: ${username}`;
      document.getElementById("adminView").classList.remove("hidden");
      fetchAdminGuards();
    } else {
      alert(data.error || "Invalid Admin Credentials.");
    }
  } catch (err) {
    alert("Connection error.");
  }
}

// Guard Modal
function openGuardLoginModal() {
  document.getElementById("guardLoginModal").classList.remove("hidden");
  document.getElementById("guardRollNoInput").value = "GRD-1001";
  document.getElementById("guardRollNoInput").focus();
}

function closeGuardLoginModal() {
  document.getElementById("guardLoginModal").classList.add("hidden");
}

async function handleGuardLogin(e) {
  e.preventDefault();
  const guardRollNo = document.getElementById("guardRollNoInput").value.trim();

  if (!guardRollNo) {
    alert("Please enter your Auto-Generated Guard Roll No.");
    return;
  }

  try {
    const res = await fetch(`${API_BASE}/auth/guard/login`, {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ guardRollNo })
    });

    const data = await res.json();
    if (res.ok && data.success) {
      currentGuard = data.guard;
      activeRole = "GUARD";
      closeGuardLoginModal();
      document.getElementById("landingView").classList.add("hidden");
      document.getElementById("authStatus").classList.remove("hidden");
      document.getElementById("roleIndicator").innerText = `GUARD: ${currentGuard.guardName} (${currentGuard.guardRollNo})`;
      document.getElementById("guardDutyInfo").innerText = `Duty Shift: ${currentGuard.shiftTime} | Shelter: ${currentGuard.shelterType} | Login ID: ${currentGuard.guardRollNo}`;
      document.getElementById("guardView").classList.remove("hidden");

      initDropFormDefaults();
      fetchGuardDrops();
      loadRegisteredStudentsCache();
    } else {
      alert(data.error || "Invalid Guard Roll No ID.");
    }
  } catch (err) {
    alert("Connection error.");
  }
}

function logout() {
  activeRole = null;
  currentStudent = null;
  currentGuard = null;
  currentFilteredTicket = null;

  document.getElementById("authStatus").classList.add("hidden");
  document.getElementById("adminView").classList.add("hidden");
  document.getElementById("guardView").classList.add("hidden");
  document.getElementById("studentView").classList.add("hidden");

  document.getElementById("landingView").classList.remove("hidden");
}

// ==========================================
// 2) ADMIN: ASSIGN GUARD & AUTO-GENERATE ROLL NO
// ==========================================
async function fetchAdminGuards() {
  try {
    const res = await fetch(`${API_BASE}/admin/guards`);
    const data = await res.json();
    const guards = data.guards || [];
    const tbody = document.getElementById("guardRosterTbody");
    tbody.innerHTML = "";

    if (!guards || guards.length === 0) {
      tbody.innerHTML = `<tr><td colspan="5" style="text-align:center; color:#94a3b8;">No guards currently assigned.</td></tr>`;
      return;
    }

    guards.forEach(g => {
      const tr = document.createElement("tr");
      tr.innerHTML = `
        <td><strong style="color:#0284c7; font-family:monospace; font-size:1.05rem;">${escapeHtml(g.guardRollNo || ('GRD-' + g.id))}</strong></td>
        <td>${escapeHtml(g.guardName)}</td>
        <td>${escapeHtml(g.phone)}</td>
        <td><span style="color:#2563eb; font-weight:700;">${escapeHtml(g.shiftTime)}</span></td>
        <td>${escapeHtml(g.shelterType)}</td>
      `;
      tbody.appendChild(tr);
    });
  } catch (err) {
    console.error("Error fetching guards roster:", err);
  }
}

async function submitGuardShift(e) {
  e.preventDefault();
  const payload = {
    guardName: document.getElementById("guardInputName").value.trim(),
    phone: document.getElementById("guardInputPhone").value.trim(),
    shiftTime: document.getElementById("shiftSlotSelect").value,
    shelterType: document.getElementById("shelterLocationInput").value.trim()
  };

  try {
    const res = await fetch(`${API_BASE}/admin/guards/assign`, {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify(payload)
    });

    const data = await res.json();
    if (res.ok) {
      alert(`✅ Guard Shift Assigned Successfully!\n\n` +
            `• Guard Name: ${payload.guardName}\n` +
            `• Phone: ${payload.phone}\n` +
            `• Shift: ${payload.shiftTime}\n` +
            `• AUTO-GENERATED GUARD ROLL NO: ${data.guardRollNo}\n\n` +
            `👉 Note: The guard must use this Roll No (${data.guardRollNo}) to log into the Guard Station.`);

      document.getElementById("guardInputName").value = "";
      document.getElementById("guardInputPhone").value = "";
      fetchAdminGuards();
    } else {
      alert("Failed to assign guard shift.");
    }
  } catch (err) {
    console.error("Error assigning shift:", err);
    alert("Connection error when assigning shift.");
  }
}

// ==========================================
// 3 & 4) GUARD: DROP & PICK UP OPTIONS
// ==========================================
function showGuardTab(tab) {
  const dropSec = document.getElementById("guardDropSection");
  const pickupSec = document.getElementById("guardPickupSection");
  const tabDropBtn = document.getElementById("tabDropBtn");
  const tabPickupBtn = document.getElementById("tabPickupBtn");

  if (tab === "DROP") {
    dropSec.classList.remove("hidden");
    pickupSec.classList.add("hidden");
    tabDropBtn.classList.add("active");
    tabPickupBtn.classList.remove("active");
    initDropFormDefaults();
    fetchGuardDrops();
  } else {
    dropSec.classList.add("hidden");
    pickupSec.classList.remove("hidden");
    tabDropBtn.classList.remove("active");
    tabPickupBtn.classList.add("active");
  }
}

function initDropFormDefaults() {
  const srNoInput = document.getElementById("dropSrNo");
  const arrivedAtInput = document.getElementById("dropArrivedAt");
  const pickedUpAtInput = document.getElementById("dropPickedUpAt");

  if (srNoInput) {
    const randDigits = Math.floor(100000 + Math.random() * 900000);
    srNoInput.value = `SR-${randDigits}`;
  }

  if (arrivedAtInput) {
    const now = new Date();
    arrivedAtInput.value = now.toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' }) + ", Today";
  }

  if (pickedUpAtInput) {
    pickedUpAtInput.value = "-- (Blank until picked up) --";
  }
}

async function loadRegisteredStudentsCache() {
  try {
    const res = await fetch(`${API_BASE}/students`);
    const data = await res.json();
    cachedStudents = data.students || [];

    const datalist = document.getElementById("registeredStudentsList");
    if (datalist) {
      datalist.innerHTML = "";
      cachedStudents.forEach(s => {
        const opt = document.createElement("option");
        opt.value = s.regNo;
        opt.label = `${s.name} (${s.regNo})`;
        datalist.appendChild(opt);
      });
    }
  } catch (err) {
    console.error("Error loading students list:", err);
  }
}

function autoFillStudentName() {
  const rollNo = document.getElementById("dropStudentRollNo").value.trim().toLowerCase();
  const matched = cachedStudents.find(s => s.regNo.toLowerCase() === rollNo);
  if (matched) {
    document.getElementById("dropStudentName").value = matched.name;
  }
}

async function submitGuardDrop(e) {
  e.preventDefault();
  const srNo = document.getElementById("dropSrNo").value;
  const studentRollNo = document.getElementById("dropStudentRollNo").value.trim();
  const studentName = document.getElementById("dropStudentName").value.trim();
  const deliveryFrom = document.getElementById("dropDeliveryFrom").value.trim();

  const payload = {
    srNo: srNo,
    studentRegNo: studentRollNo,
    studentName: studentName,
    deliveryFrom: deliveryFrom,
    itemName: `${deliveryFrom} Parcel`
  };

  try {
    const res = await fetch(`${API_BASE}/guard/drop`, {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify(payload)
    });

    const data = await res.json();
    if (res.ok) {
      alert(`✅ Drop Ticket Created Successfully!\n\n` +
            `• Student: ${studentName} (Roll No: ${data.studentRegNo})\n` +
            `• Place Parcel On: Shelf Space #${data.shelfNumber} (1-150)\n` +
            `• Status: Notification dispatched to student with secret pickup OTP.`);

      document.getElementById("dropStudentRollNo").value = "";
      document.getElementById("dropStudentName").value = "";
      document.getElementById("dropDeliveryFrom").value = "";
      initDropFormDefaults();
      fetchGuardDrops();
      loadRegisteredStudentsCache();
    } else {
      alert("Failed to create drop ticket: " + (data.error || "Unknown error"));
    }
  } catch (err) {
    console.error("Drop submission error:", err);
    alert("Network error creating drop ticket.");
  }
}

async function fetchGuardDrops() {
  try {
    const res = await fetch(`${API_BASE}/parcels?all=true`);
    const data = await res.json();
    const parcels = data.parcels || [];
    const tbody = document.getElementById("guardDropsTbody");
    tbody.innerHTML = "";

    if (parcels.length === 0) {
      tbody.innerHTML = `<tr><td colspan="8" style="text-align:center; color:#94a3b8;">No dropped parcels logged yet.</td></tr>`;
      return;
    }

    parcels.forEach(p => {
      const tr = document.createElement("tr");
      const isClaimed = p.isCollected || p.status === "COLLECTED";
      const pickedDisplay = isClaimed && p.pickedUpAtTimeStr 
        ? `<span style="color:#059669; font-weight:600;">${p.pickedUpAtTimeStr}</span>`
        : `<span style="color:#94a3b8; font-style:italic;">-- (Blank until picked up) --</span>`;

      tr.innerHTML = `
        <td><strong>${escapeHtml(p.srNo || ('SR-' + p.parcelId))}</strong></td>
        <td><span style="font-family:monospace; font-weight:700; color:#0284c7;">${escapeHtml(p.studentRegNo || '23BCS1088')}</span></td>
        <td>${escapeHtml(p.studentName || p.recipientName || 'Student')}</td>
        <td>${escapeHtml(p.deliveryFrom || p.senderCarrier || 'Courier')}</td>
        <td>${escapeHtml(p.arrivedAtTimeStr || '10:00 AM')}</td>
        <td>${pickedDisplay}</td>
        <td><strong style="color:#2563eb;">Shelf Space #${p.shelfNumber || p.shelfSlot} (1-150)</strong></td>
        <td>${isClaimed ? '✅ Picked Up' : '🟡 Arrived at Shelter'}</td>
      `;
      tbody.appendChild(tr);
    });
  } catch (err) {
    console.error("Error fetching guard drops:", err);
  }
}

// PICK UP OPTION: OTP FILTER & CAMERA SCANNER
async function filterOrderByOtp() {
  const otpInput = document.getElementById("filterOtpInput");
  const otp = otpInput.value.trim();

  if (otp.length < 6) return;

  try {
    const res = await fetch(`${API_BASE}/guard/pickup-lookup?otp=${encodeURIComponent(otp)}`);
    if (!res.ok) {
      document.getElementById("pickupTicketDetails").classList.add("hidden");
      alert("No active shelter drop found matching OTP: " + otp);
      return;
    }

    const data = await res.json();
    const parcel = data.parcel;
    currentFilteredTicket = parcel;

    // Display the filtered ticket created during drop
    document.getElementById("pickupTicketDetails").classList.remove("hidden");
    document.getElementById("pkSrNo").innerText = parcel.srNo || (`SR-${parcel.parcelId}`);
    document.getElementById("pkStudentRegNo").innerText = parcel.studentRegNo || "23BCS1088";
    document.getElementById("pkStudentName").innerText = parcel.studentName || parcel.recipientName || "Student";
    document.getElementById("pkDeliveryFrom").innerText = parcel.deliveryFrom || parcel.senderCarrier || "Courier";
    document.getElementById("pkArrivedAt").innerText = parcel.arrivedAtTimeStr || "10:00 AM";
    document.getElementById("pkPickedUpAt").innerText = parcel.pickedUpAtTimeStr || "-- (Pending Collection) --";
    document.getElementById("pkShelfNumber").innerText = `Shelf Space #${parcel.shelfNumber || parcel.shelfSlot} (1-150)`;
    document.getElementById("pkBarcodeTarget").innerText = parcel.barcodeSymbol || `PIQ-BC-${parcel.pickupOtp || otp}`;

    const isCollected = parcel.isCollected || parcel.status === "COLLECTED";
    const badge = document.getElementById("ticketBadge");
    const scanBtn = document.getElementById("btnScanBarcode");

    if (isCollected) {
      badge.innerText = "ALREADY COLLECTED";
      badge.style.backgroundColor = "#dcfce7";
      badge.style.color = "#15803d";
      scanBtn.disabled = true;
      scanBtn.innerText = "Order Already Released";
      scanBtn.style.opacity = "0.6";
    } else {
      badge.innerText = "AWAITING PICKUP";
      badge.style.backgroundColor = "#dbeafe";
      badge.style.color = "#1e40af";
      scanBtn.disabled = false;
      scanBtn.innerText = "📷 Scan Barcode & Complete Pick-Up";
      scanBtn.style.opacity = "1";
    }
  } catch (err) {
    console.error("OTP filter error:", err);
  }
}

async function executeBarcodeCameraScan() {
  if (!currentFilteredTicket) return;

  const barcode = currentFilteredTicket.barcodeSymbol || `PIQ-BC-${currentFilteredTicket.pickupOtp}`;
  const payload = {
    parcelId: currentFilteredTicket.parcelId,
    code: barcode,
    guardId: currentGuard ? currentGuard.guardRollNo : "GUARD_STATION"
  };

  try {
    const res = await fetch(`${API_BASE}/guard/pickup-confirm`, {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify(payload)
    });

    const data = await res.json();
    if (res.ok) {
      alert(`📷 Camera recognized barcode: ${barcode}\n\n` +
            `Verification passed!\nParcel released to ${currentFilteredTicket.studentName}.\nPicked up at: ${data.parcel ? data.parcel.pickedUpAtTimeStr : 'Just Now'}`);

      document.getElementById("pkPickedUpAt").innerText = data.parcel ? data.parcel.pickedUpAtTimeStr : "Just Now";
      const badge = document.getElementById("ticketBadge");
      badge.innerText = "COLLECTED";
      badge.style.backgroundColor = "#dcfce7";
      badge.style.color = "#15803d";

      const scanBtn = document.getElementById("btnScanBarcode");
      scanBtn.disabled = true;
      scanBtn.innerText = "Order Handed Over ✅";
      scanBtn.style.opacity = "0.6";
    } else {
      alert("Barcode scan verification failed: " + (data.error || "Unknown"));
    }
  } catch (err) {
    console.error("Camera barcode confirm error:", err);
  }
}

// ==========================================
// 5) STUDENT UNIQUE NOTIFICATION PAGE & HELP SIGN
// ==========================================
async function fetchStudentArrivalTickets(regNo) {
  try {
    const res = await fetch(`${API_BASE}/parcels?regNo=${encodeURIComponent(regNo)}`);
    const data = await res.json();
    const parcels = data.parcels || [];
    const container = document.getElementById("studentTicketsGrid");
    const disputeSelect = document.getElementById("disputeParcelSelect");

    container.innerHTML = "";
    disputeSelect.innerHTML = "";

    // "I should not see any dummy order notification unless the guard asssign an notification ticket"
    if (parcels.length === 0) {
      container.innerHTML = `
        <div class="empty-orders-card">
          <div class="empty-icon">📦</div>
          <h3>No Arrived Orders Yet</h3>
          <p>Hello <strong>${escapeHtml(currentStudent.name)}</strong> (Roll No: <strong>${escapeHtml(regNo)}</strong>)! When a campus guard drops your parcel at the shelter, your arrival ticket, shelf location (1–150), and secret pickup OTP will appear here.</p>
        </div>
      `;
      return;
    }

    parcels.forEach(p => {
      const isClaimed = p.isCollected || p.status === "COLLECTED";
      const card = document.createElement("div");
      card.className = "student-ticket-item";

      card.innerHTML = `
        <div class="ticket-top-row">
          <h3>📦 ${escapeHtml(p.deliveryFrom || p.senderCarrier || 'Incoming Parcel')}</h3>
          <span class="otp-tag">OTP: ${p.pickupOtp || '------'}</span>
        </div>
        <p style="margin: 0.5rem 0; font-size: 0.95rem;">
          <strong>Sr No:</strong> ${escapeHtml(p.srNo || ('SR-' + p.parcelId))} | 
          <strong>Shelter Space:</strong> <span style="color:#2563eb; font-weight:700;">Shelf #${p.shelfNumber || p.shelfSlot} (1-150)</span>
        </p>
        <div class="ticket-timing-info">
          <div>⏱️ <strong>Dropped off at:</strong> ${escapeHtml(p.arrivedAtTimeStr || '10:00 AM')}</div>
          <div>⏱️ <strong>Picked up at:</strong> ${isClaimed ? ('<span style="color:#059669; font-weight:700;">' + escapeHtml(p.pickedUpAtTimeStr) + '</span>') : '<span style="color:#d97706; font-style:italic;">-- (Pending pickup by you) --</span>'}</div>
        </div>
      `;
      container.appendChild(card);

      // Populate dispute select
      const opt = document.createElement("option");
      opt.value = p.parcelId;
      opt.innerText = `${p.srNo || ('SR-' + p.parcelId)} - ${p.deliveryFrom || 'Parcel'} (Shelf #${p.shelfNumber || p.shelfSlot})`;
      disputeSelect.appendChild(opt);
    });
  } catch (err) {
    console.error("Error fetching student tickets:", err);
  }
}

// HELP SIGN / DISPUTE MODAL
function openDisputeModal() {
  document.getElementById("disputeModal").classList.remove("hidden");
}

function closeDisputeModal() {
  document.getElementById("disputeModal").classList.add("hidden");
}

async function submitDispute(e) {
  e.preventDefault();
  const selectElem = document.getElementById("disputeParcelSelect");
  if (!selectElem.value) {
    alert("No active parcel available to raise dispute for.");
    return;
  }

  const parcelId = parseInt(selectElem.value);
  const issueType = document.getElementById("disputeIssueType").value;
  const description = document.getElementById("disputeComments").value.trim();

  const payload = {
    studentRegNo: currentStudent ? currentStudent.regNo : "23BCS1088",
    parcelId: parcelId,
    issueType: issueType,
    description: description
  };

  try {
    const res = await fetch(`${API_BASE}/disputes/raise`, {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify(payload)
    });

    const data = await res.json();
    if (res.ok) {
      alert(`⚠️ Incident Ticket Raised Successfully!\nTicket ID: #${data.ticket ? data.ticket.ticketNumber : 'TK-991'}\nCampus Security & Shelter Supervisor have been notified of this ${issueType} report.`);
      document.getElementById("disputeComments").value = "";
      closeDisputeModal();
    } else {
      alert("Failed to submit ticket: " + (data.error || "Unknown error"));
    }
  } catch (err) {
    console.error("Dispute error:", err);
    alert("Connection error when submitting dispute.");
  }
}

// Helper utility
function escapeHtml(str) {
  if (!str) return "";
  return String(str)
    .replace(/&/g, "&amp;")
    .replace(/</g, "&lt;")
    .replace(/>/g, "&gt;")
    .replace(/"/g, "&quot;")
    .replace(/'/g, "&#039;");
}
