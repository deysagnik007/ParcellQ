package com.parceliq.server;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.parceliq.dao.DisputeDAO;
import com.parceliq.dao.GuardDAO;
import com.parceliq.dao.ParcelDAO;
import com.parceliq.dao.StudentDAO;
import com.parceliq.exception.InvalidPickupCodeException;
import com.parceliq.exception.ParcelAlreadyClaimedException;
import com.parceliq.model.*;
import com.parceliq.util.ReportGenerator;
import com.parceliq.worker.ExpiryWatcherThread;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

import java.io.*;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.sql.SQLException;
import java.util.*;

/**
 * Embedded HTTP Server for ParcelIQ
 * Serves role-based routing (Student, Guard, Admin), Admin Guard Shift Assignment (3-hour shifts),
 * Guard DROP tickets (random shelf 1-150, OTP, stacked entries), Guard PICKUP OTP filter with Camera Barcode Scanner,
 * and Student Help Sign (Red Triangle with White Rectangle) dispute ticketing.
 */
public class ParcelHttpServer {

    private final int port;
    private final ParcelDAO parcelDAO;
    private final StudentDAO studentDAO;
    private final GuardDAO guardDAO;
    private final DisputeDAO disputeDAO;
    private final ReportGenerator reportGenerator;
    private final ExpiryWatcherThread expiryWatcherThread;
    private final Gson gson;
    private HttpServer server;

    public ParcelHttpServer(int port, ExpiryWatcherThread expiryWatcherThread) {
        this.port = port;
        this.parcelDAO = new ParcelDAO();
        this.studentDAO = new StudentDAO();
        this.guardDAO = new GuardDAO();
        this.disputeDAO = new DisputeDAO();
        this.reportGenerator = new ReportGenerator();
        this.expiryWatcherThread = expiryWatcherThread;
        this.gson = new Gson();
    }

    public void start() throws IOException {
        server = HttpServer.create(new InetSocketAddress(port), 0);

        // Static resources
        server.createContext("/", new StaticFileHandler());

        // Student & Core APIs
        server.createContext("/api/student", new StudentHandler());
        server.createContext("/api/student/update", new UpdateProfileHandler());
        server.createContext("/api/parcels", new ParcelsHandler());
        server.createContext("/api/parcels/order", new OrderPlacementHandler());
        server.createContext("/api/parcels/arrive", new ShelterArriveHandler());
        server.createContext("/api/parcels/claim", new ParcelClaimHandler());
        server.createContext("/api/shelter/stats", new ShelterStatsHandler());
        server.createContext("/api/shelter/report", new ReportHandler());
        server.createContext("/api/friends/all", new FriendsHandler());
        server.createContext("/api/friends/add", new AddFriendHandler());
        server.createContext("/api/oop-concepts", new OopConceptsHandler());

        // Admin Guard Shift Assignment APIs
        server.createContext("/api/admin/guards", new AdminGuardsHandler());
        server.createContext("/api/admin/guards/assign", new AdminAssignGuardHandler());
        server.createContext("/api/admin/guards/delete", new AdminDeleteGuardHandler());

        // Guard DROP & PICKUP APIs
        server.createContext("/api/guard/drop", new GuardDropTicketHandler());
        server.createContext("/api/guard/pickup-lookup", new GuardPickupLookupHandler());
        server.createContext("/api/guard/pickup-confirm", new GuardPickupConfirmHandler());

        // Student Help Sign Dispute APIs
        server.createContext("/api/disputes/raise", new RaiseDisputeHandler());
        server.createContext("/api/disputes", new GetDisputesHandler());

        // Authentication & User APIs
        server.createContext("/api/auth/student/login", new StudentLoginHandler());
        server.createContext("/api/auth/student/register", new StudentRegisterHandler());
        server.createContext("/api/auth/student/reset-password", new StudentResetPasswordHandler());
        server.createContext("/api/auth/admin/login", new AdminLoginHandler());
        server.createContext("/api/auth/guard/login", new GuardLoginHandler());
        server.createContext("/api/students", new StudentsListHandler());

        server.setExecutor(null);
        server.start();
        System.out.println("[ParcelHttpServer] Web Application live at http://localhost:" + port);
    }

    public void stop() {
        if (server != null) {
            server.stop(0);
        }
    }

    // Static Resource Handler (loads local file first for live updates)
    private class StaticFileHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String path = exchange.getRequestURI().getPath();
            if (path.equals("/") || path.isEmpty()) {
                path = "/index.html";
            }

            InputStream is = null;
            File localFile = new File("src/main/resources/web" + path);
            if (localFile.exists()) {
                is = new FileInputStream(localFile);
            }

            if (is == null) {
                String resourcePath = "web" + path;
                is = getClass().getClassLoader().getResourceAsStream(resourcePath);
            }

            if (is == null) {
                String response = "404 Not Found: " + path;
                exchange.sendResponseHeaders(404, response.length());
                OutputStream os = exchange.getResponseBody();
                os.write(response.getBytes());
                os.close();
                return;
            }

            String contentType = "text/plain";
            if (path.endsWith(".html")) contentType = "text/html; charset=UTF-8";
            else if (path.endsWith(".css")) contentType = "text/css; charset=UTF-8";
            else if (path.endsWith(".js")) contentType = "application/javascript; charset=UTF-8";
            else if (path.endsWith(".json")) contentType = "application/json";
            else if (path.endsWith(".png")) contentType = "image/png";
            else if (path.endsWith(".svg")) contentType = "image/svg+xml";

            byte[] bytes = is.readAllBytes();
            is.close();

            exchange.getResponseHeaders().set("Content-Type", contentType);
            exchange.sendResponseHeaders(200, bytes.length);
            OutputStream os = exchange.getResponseBody();
            os.write(bytes);
            os.close();
        }
    }

    // ================= ADMIN APIS =================

    private class AdminGuardsHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            setCorsHeaders(exchange);
            try {
                List<GuardShift> list = guardDAO.getAllShifts();
                sendJsonResponse(exchange, 200, Map.of("guards", list));
            } catch (SQLException e) {
                sendJsonResponse(exchange, 500, Map.of("error", e.getMessage()));
            }
        }
    }

    private class AdminAssignGuardHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            setCorsHeaders(exchange);
            if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
                sendJsonResponse(exchange, 405, Map.of("error", "Method not allowed"));
                return;
            }

            String body = readBody(exchange);
            JsonObject json = JsonParser.parseString(body).getAsJsonObject();

            String name = json.get("guardName").getAsString();
            String phone = json.get("phone").getAsString();
            String shelterType = json.has("shelterType") ? json.get("shelterType").getAsString() : "DROP_SHELTER";
            String shiftTime = json.get("shiftTime").getAsString();
            String assignedBy = json.has("assignedBy") ? json.get("assignedBy").getAsString() : "Admin";

            // Auto-generate unique Guard Roll No ID
            String guardRollNo = "GRD-" + (1000 + new java.util.Random().nextInt(9000));

            try {
                GuardShift shift = guardDAO.assignShift(guardRollNo, name, phone, shelterType, shiftTime, assignedBy);
                sendJsonResponse(exchange, 200, Map.of(
                        "success", true,
                        "shift", shift,
                        "guardRollNo", guardRollNo,
                        "message", "Guard assigned successfully! Unique Guard Roll No ID: " + guardRollNo
                ));
            } catch (SQLException e) {
                sendJsonResponse(exchange, 500, Map.of("error", e.getMessage()));
            }
        }
    }

    private class AdminDeleteGuardHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            setCorsHeaders(exchange);
            String query = exchange.getRequestURI().getQuery();
            int id = 0;
            if (query != null && query.contains("id=")) {
                for (String param : query.split("&")) {
                    String[] pair = param.split("=");
                    if (pair.length > 1 && pair[0].equals("id")) {
                        id = Integer.parseInt(pair[1]);
                    }
                }
            }

            try {
                boolean deleted = guardDAO.deleteShift(id);
                sendJsonResponse(exchange, 200, Map.of("success", deleted));
            } catch (SQLException e) {
                sendJsonResponse(exchange, 500, Map.of("error", e.getMessage()));
            }
        }
    }

    // ================= GUARD DROP & PICKUP APIS =================

    private class GuardDropTicketHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            setCorsHeaders(exchange);
            if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
                sendJsonResponse(exchange, 405, Map.of("error", "Method not allowed"));
                return;
            }

            String body = readBody(exchange);
            JsonObject json = JsonParser.parseString(body).getAsJsonObject();

            String studentName = json.get("studentName").getAsString().trim();
            String studentRegNo = json.has("studentRegNo") ? json.get("studentRegNo").getAsString().trim() : "";
            String deliveryFrom = json.get("deliveryFrom").getAsString().trim();
            String itemName = json.has("itemName") ? json.get("itemName").getAsString() : deliveryFrom + " Package";

            try {
                if (studentRegNo.isEmpty()) {
                    Student found = studentDAO.getStudentByName(studentName);
                    if (found != null) {
                        studentRegNo = found.getRegNo();
                    } else {
                        studentRegNo = "24ROLL-" + (1000 + new java.util.Random().nextInt(9000));
                        studentDAO.registerStudent(studentRegNo, studentName, "student123");
                    }
                } else {
                    Student existing = studentDAO.getStudentByRegNo(studentRegNo);
                    if (existing == null) {
                        studentDAO.registerStudent(studentRegNo, studentName, "student123");
                    }
                }

                Parcel created = parcelDAO.createDropTicket(studentName, studentRegNo, deliveryFrom, itemName);
                sendJsonResponse(exchange, 200, Map.of(
                        "success", true,
                        "shelfNumber", created.getShelfNumber(),
                        "studentRegNo", studentRegNo,
                        "studentName", studentName,
                        "deliveryFrom", deliveryFrom,
                        "message", "Drop ticket created! Shelf Space #" + created.getShelfNumber() + " assigned. Student " + studentName + " notified."
                ));
            } catch (SQLException e) {
                sendJsonResponse(exchange, 500, Map.of("error", e.getMessage()));
            }
        }
    }

    private class GuardPickupLookupHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            setCorsHeaders(exchange);
            String query = exchange.getRequestURI().getQuery();
            String otp = "";
            if (query != null && query.contains("otp=")) {
                for (String param : query.split("&")) {
                    String[] pair = param.split("=");
                    if (pair.length > 1 && pair[0].equals("otp")) {
                        otp = pair[1];
                    }
                }
            }

            try {
                Parcel p = parcelDAO.lookupByOtp(otp);
                if (p == null) {
                    sendJsonResponse(exchange, 404, Map.of("error", "No order found matching OTP: " + otp));
                    return;
                }
                sendJsonResponse(exchange, 200, Map.of("success", true, "parcel", p));
            } catch (SQLException e) {
                sendJsonResponse(exchange, 500, Map.of("error", e.getMessage()));
            }
        }
    }

    private class GuardPickupConfirmHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            setCorsHeaders(exchange);
            if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
                sendJsonResponse(exchange, 405, Map.of("error", "Method not allowed"));
                return;
            }

            String body = readBody(exchange);
            JsonObject json = JsonParser.parseString(body).getAsJsonObject();

            int parcelId = json.get("parcelId").getAsInt();
            String code = json.get("code").getAsString();
            String guardId = json.has("guardId") ? json.get("guardId").getAsString() : "GUARD_HOSTEL_GATE";

            try {
                boolean success = parcelDAO.confirmPickup(parcelId, code, guardId);
                Parcel p = parcelDAO.getParcelById(parcelId);
                sendJsonResponse(exchange, 200, Map.of(
                        "success", success,
                        "parcel", p,
                        "message", "Camera barcode / OTP verified! Package released at " + p.getPickedUpAtTimeStr()
                ));
            } catch (InvalidPickupCodeException e) {
                sendJsonResponse(exchange, 400, Map.of("error", e.getMessage(), "exception", "InvalidPickupCodeException"));
            } catch (ParcelAlreadyClaimedException e) {
                sendJsonResponse(exchange, 400, Map.of("error", e.getMessage(), "exception", "ParcelAlreadyClaimedException"));
            } catch (Exception e) {
                sendJsonResponse(exchange, 500, Map.of("error", e.getMessage()));
            }
        }
    }

    // ================= STUDENT DISPUTE / HELP SIGN APIS =================

    private class RaiseDisputeHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            setCorsHeaders(exchange);
            if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
                sendJsonResponse(exchange, 405, Map.of("error", "Method not allowed"));
                return;
            }

            String body = readBody(exchange);
            JsonObject json = JsonParser.parseString(body).getAsJsonObject();

            String studentReg = json.get("studentRegNo").getAsString();
            int parcelId = json.has("parcelId") ? json.get("parcelId").getAsInt() : 0;
            String issueType = json.get("issueType").getAsString();
            String description = json.get("description").getAsString();

            try {
                DisputeTicket ticket = disputeDAO.raiseDispute(studentReg, parcelId, issueType, description);
                sendJsonResponse(exchange, 200, Map.of("success", true, "ticket", ticket, "message", "Dispute ticket #" + ticket.getTicketNumber() + " registered for campus investigation."));
            } catch (SQLException e) {
                sendJsonResponse(exchange, 500, Map.of("error", e.getMessage()));
            }
        }
    }

    private class GetDisputesHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            setCorsHeaders(exchange);
            String query = exchange.getRequestURI().getQuery();
            String regNo = "23BCS1088";
            if (query != null && query.contains("regNo=")) {
                for (String param : query.split("&")) {
                    String[] pair = param.split("=");
                    if (pair.length > 1 && pair[0].equals("regNo")) {
                        regNo = pair[1];
                    }
                }
            }

            try {
                List<DisputeTicket> list = disputeDAO.getDisputesByStudent(regNo);
                sendJsonResponse(exchange, 200, Map.of("disputes", list));
            } catch (SQLException e) {
                sendJsonResponse(exchange, 500, Map.of("error", e.getMessage()));
            }
        }
    }

    // ================= CORE STUDENT & LOGISTICS HANDLERS =================

    private class StudentHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            setCorsHeaders(exchange);
            String query = exchange.getRequestURI().getQuery();
            String regNo = "23BCS1088";
            if (query != null && query.contains("regNo=")) {
                for (String param : query.split("&")) {
                    String[] pair = param.split("=");
                    if (pair.length > 1 && pair[0].equals("regNo")) {
                        regNo = pair[1];
                    }
                }
            }

            try {
                Student s = studentDAO.getStudentByRegNo(regNo);
                if (s == null) {
                    sendJsonResponse(exchange, 404, Map.of("error", "Student not found"));
                    return;
                }
                Map<String, Object> resp = new HashMap<>();
                resp.put("student", s);
                resp.put("rewards", studentDAO.getRewards(regNo));
                resp.put("friends", studentDAO.getFriends(regNo));
                sendJsonResponse(exchange, 200, resp);
            } catch (SQLException e) {
                sendJsonResponse(exchange, 500, Map.of("error", e.getMessage()));
            }
        }
    }

    private class UpdateProfileHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            setCorsHeaders(exchange);
            if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
                sendJsonResponse(exchange, 405, Map.of("error", "Method not allowed"));
                return;
            }

            String body = readBody(exchange);
            JsonObject json = JsonParser.parseString(body).getAsJsonObject();

            String regNo = json.get("regNo").getAsString();
            String name = json.get("name").getAsString();
            String phone = json.has("phone") ? json.get("phone").getAsString() : "";
            String dob = json.has("dateOfBirth") ? json.get("dateOfBirth").getAsString() : "";
            String hostelBlock = json.has("hostelBlock") ? json.get("hostelBlock").getAsString() : "";
            String roomNumber = json.has("roomNumber") ? json.get("roomNumber").getAsString() : "";

            try {
                Student s = new Student(regNo, name, phone, dob, hostelBlock, roomNumber);
                studentDAO.updateStudentProfile(s);
                sendJsonResponse(exchange, 200, Map.of("success", true, "message", "Profile updated successfully!"));
            } catch (SQLException e) {
                sendJsonResponse(exchange, 500, Map.of("error", e.getMessage()));
            }
        }
    }

    private class ParcelsHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            setCorsHeaders(exchange);
            String query = exchange.getRequestURI().getQuery();
            String regNo = null;
            boolean all = false;

            if (query != null) {
                for (String param : query.split("&")) {
                    String[] pair = param.split("=");
                    if (pair.length > 1) {
                        if (pair[0].equals("regNo")) regNo = pair[1];
                        if (pair[0].equals("all") && pair[1].equalsIgnoreCase("true")) all = true;
                    }
                }
            }

            try {
                List<Parcel> list;
                if (all) {
                    list = parcelDAO.getAllParcels();
                } else if (regNo != null && !regNo.trim().isEmpty()) {
                    list = parcelDAO.getParcelsByStudent(regNo.trim());
                } else {
                    list = new ArrayList<>();
                }
                parcelDAO.refreshUnclaimedCount();
                Map<String, Object> res = new HashMap<>();
                res.put("parcels", list);
                res.put("totalUnclaimedCount", Parcel.totalUnclaimedCount);
                sendJsonResponse(exchange, 200, res);
            } catch (SQLException e) {
                sendJsonResponse(exchange, 500, Map.of("error", e.getMessage()));
            }
        }
    }

    private class OrderPlacementHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            setCorsHeaders(exchange);
            if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
                sendJsonResponse(exchange, 405, Map.of("error", "Method not allowed"));
                return;
            }

            String body = readBody(exchange);
            JsonObject json = JsonParser.parseString(body).getAsJsonObject();

            String regNo = json.has("studentRegNo") ? json.get("studentRegNo").getAsString() : "23BCS1088";
            String carrier = json.has("carrier") ? json.get("carrier").getAsString() : "Amazon";
            String itemName = json.has("itemName") ? json.get("itemName").getAsString() : "Online Purchase";
            boolean isCourier = json.has("isCourier") && json.get("isCourier").getAsBoolean();
            String giftTheme = json.has("giftTheme") && !json.get("giftTheme").isJsonNull() ? json.get("giftTheme").getAsString() : null;
            String senderName = json.has("senderName") && !json.get("senderName").isJsonNull() ? json.get("senderName").getAsString() : null;
            String recipientName = json.has("recipientName") && !json.get("recipientName").isJsonNull() ? json.get("recipientName").getAsString() : null;
            String giftMessage = json.has("giftMessage") && !json.get("giftMessage").isJsonNull() ? json.get("giftMessage").getAsString() : null;
            String recipientRegNo = json.has("recipientRegNo") && !json.get("recipientRegNo").isJsonNull() ? json.get("recipientRegNo").getAsString() : regNo;

            String trackingNo = (carrier.substring(0, Math.min(3, carrier.length())).toUpperCase()) + "-" +
                    (10000 + new Random().nextInt(90000)) + "-IN";
            String deliveryPin = "PIN-" + (1000 + new Random().nextInt(9000));

            Parcel parcel;
            if (carrier.equalsIgnoreCase("Zomato") || carrier.equalsIgnoreCase("Swiggy") || carrier.equalsIgnoreCase("Blinkit")) {
                parcel = new PerishableParcel(0, trackingNo, recipientRegNo, carrier, itemName, deliveryPin, 6, 4.0);
            } else {
                parcel = new StandardParcel(0, trackingNo, recipientRegNo, carrier, itemName, deliveryPin, isCourier, "BOX");
            }

            parcel.setStudentName(recipientName != null ? recipientName : "Aarav Sharma");
            parcel.setGiftTheme(giftTheme);
            parcel.setSenderName(senderName);
            parcel.setRecipientName(recipientName);
            parcel.setGiftMessage(giftMessage);

            try {
                Parcel saved = parcelDAO.logParcel(parcel);
                sendJsonResponse(exchange, 200, Map.of(
                        "success", true,
                        "parcel", saved,
                        "message", "Order successfully placed and synchronized with " + carrier + "!"
                ));
            } catch (SQLException e) {
                sendJsonResponse(exchange, 500, Map.of("error", e.getMessage()));
            }
        }
    }

    private class ShelterArriveHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            setCorsHeaders(exchange);
            if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
                sendJsonResponse(exchange, 405, Map.of("error", "Method not allowed"));
                return;
            }

            String body = readBody(exchange);
            JsonObject json = JsonParser.parseString(body).getAsJsonObject();

            int parcelId = json.get("parcelId").getAsInt();
            String deliveryPin = json.has("deliveryPin") ? json.get("deliveryPin").getAsString() : "";

            try {
                Parcel updated = parcelDAO.arriveAtShelter(parcelId, deliveryPin);
                sendJsonResponse(exchange, 200, Map.of(
                        "success", true,
                        "parcel", updated,
                        "message", "Package arrived at shelter! Shelf: " + updated.getShelfSlot() + " | Token: " + updated.getPickupToken() + " | OTP: " + updated.getPickupOtp()
                ));
            } catch (ParcelAlreadyClaimedException e) {
                sendJsonResponse(exchange, 400, Map.of("error", e.getMessage(), "type", "ParcelAlreadyClaimedException"));
            } catch (Exception e) {
                sendJsonResponse(exchange, 400, Map.of("error", e.getMessage()));
            }
        }
    }

    private class ParcelClaimHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            setCorsHeaders(exchange);
            if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
                sendJsonResponse(exchange, 405, Map.of("error", "Method not allowed"));
                return;
            }

            String body = readBody(exchange);
            JsonObject json = JsonParser.parseString(body).getAsJsonObject();

            int parcelId = json.get("parcelId").getAsInt();
            String pickupToken = json.get("pickupToken").getAsString();
            String guardId = json.has("guardId") ? json.get("guardId").getAsString() : "GUARD_HOSTEL_GATE";

            try {
                boolean success = parcelDAO.confirmPickup(parcelId, pickupToken, guardId);
                sendJsonResponse(exchange, 200, Map.of(
                        "success", success,
                        "message", "Verification passed! Package released to student. +50 Reward points awarded!"
                ));
            } catch (InvalidPickupCodeException e) {
                sendJsonResponse(exchange, 400, Map.of(
                        "error", e.getMessage(),
                        "exception", "InvalidPickupCodeException",
                        "attemptedCode", e.getAttemptedCode()
                ));
            } catch (ParcelAlreadyClaimedException e) {
                sendJsonResponse(exchange, 400, Map.of(
                        "error", e.getMessage(),
                        "exception", "ParcelAlreadyClaimedException",
                        "parcelId", e.getParcelId()
                ));
            } catch (Exception e) {
                sendJsonResponse(exchange, 500, Map.of("error", e.getMessage()));
            }
        }
    }

    private class ShelterStatsHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            setCorsHeaders(exchange);
            parcelDAO.refreshUnclaimedCount();
            Map<String, Object> map = new HashMap<>();
            map.put("totalUnclaimedCount", Parcel.totalUnclaimedCount);
            map.put("expiryWorkerRunning", expiryWatcherThread.isRunning());
            map.put("expiredItemsFound", expiryWatcherThread.getExpiredFoundCount());
            map.put("activeDbEngine", com.parceliq.db.DBConnection.getActiveDbType());
            sendJsonResponse(exchange, 200, map);
        }
    }

    private class ReportHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            setCorsHeaders(exchange);
            try {
                File report = reportGenerator.generateDailyAuditReport(".");
                String content = ReportGenerator.readReportContent(report);
                sendJsonResponse(exchange, 200, Map.of(
                        "success", true,
                        "filename", report.getName(),
                        "path", report.getAbsolutePath(),
                        "content", content
                ));
            } catch (Exception e) {
                sendJsonResponse(exchange, 500, Map.of("error", e.getMessage()));
            }
        }
    }

    private class FriendsHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            setCorsHeaders(exchange);
            try {
                List<Student> students = studentDAO.getAllStudents();
                sendJsonResponse(exchange, 200, Map.of("students", students));
            } catch (SQLException e) {
                sendJsonResponse(exchange, 500, Map.of("error", e.getMessage()));
            }
        }
    }

    private class AddFriendHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            setCorsHeaders(exchange);
            if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
                sendJsonResponse(exchange, 405, Map.of("error", "Method not allowed"));
                return;
            }
            String body = readBody(exchange);
            JsonObject json = JsonParser.parseString(body).getAsJsonObject();
            String userRegNo = json.get("userRegNo").getAsString();
            String friendRegNo = json.get("friendRegNo").getAsString();

            try {
                boolean added = studentDAO.addFriend(userRegNo, friendRegNo);
                sendJsonResponse(exchange, 200, Map.of("success", added));
            } catch (SQLException e) {
                sendJsonResponse(exchange, 500, Map.of("error", e.getMessage()));
            }
        }
    }

    private class OopConceptsHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            setCorsHeaders(exchange);
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("project", "ParcelIQ - Campus Shelter Logistics & Social Parcel Tracking");
            map.put("syllabusConcepts", List.of(
                    Map.of("concept", "Java Basics & Variables", "codeRef", "Parcel.java: parcelId, trackingNumber, isCollected, holdingFee, shelfNumber (1-150)"),
                    Map.of("concept", "Strings & Arrays", "codeRef", "Parcel.java: String.format(\"SHELF-%c-%02d\", aisle, slot)"),
                    Map.of("concept", "Wrapper Classes", "codeRef", "Integer.parseInt(parcelId), Double.valueOf(fee) in handlers"),
                    Map.of("concept", "Constructors & 'this'", "codeRef", "Parcel.java: this.trackingNumber = trackingNumber; this.studentRegNo = ..."),
                    Map.of("concept", "Static Keyword", "codeRef", "Parcel.totalUnclaimedCount & DBConnection.getConnection()"),
                    Map.of("concept", "Method Overloading", "codeRef", "GuardDAO: assignShift(guardName, phone, shiftTime) vs assignShift(guardName, phone, shelterType, shiftTime, assignedBy) & ParcelDAO.logParcel(...)"),
                    Map.of("concept", "Inheritance & 'super'", "codeRef", "StandardParcel & PerishableParcel extending Parcel and calling super(...)"),
                    Map.of("concept", "Polymorphism & Overriding", "codeRef", "calculateFee(daysHeld) overridden in StandardParcel (free 3 days) vs PerishableParcel (day 1 cold fee)"),
                    Map.of("concept", "Abstract Class", "codeRef", "public abstract class Parcel { public abstract double calculateFee(int daysHeld); }"),
                    Map.of("concept", "Interfaces", "codeRef", "public interface Verifiable { boolean verifyCode(String inputCode); }"),
                    Map.of("concept", "Type Casting / Downcasting", "codeRef", "ExpiryWatcherThread: ((PerishableParcel) p).checkExpiry()"),
                    Map.of("concept", "Exception Handling", "codeRef", "Custom exceptions InvalidPickupCodeException & ParcelAlreadyClaimedException"),
                    Map.of("concept", "File I/O & Byte Streams", "codeRef", "ReportGenerator.java: FileOutputStream / FileWriter generating daily_audit_report.txt"),
                    Map.of("concept", "Multithreading", "codeRef", "ExpiryWatcherThread: background thread with while(true) and Thread.sleep()"),
                    Map.of("concept", "RDBMS / MySQL", "codeRef", "MySQL tables students, parcels, pickuplogs, guard_shifts, dispute_tickets accessed via JDBC PreparedStatement & ResultSet")
            ));
            sendJsonResponse(exchange, 200, map);
        }
    }

    // ================= AUTHENTICATION & USER APIS =================

    private class StudentLoginHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            setCorsHeaders(exchange);
            if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
                sendJsonResponse(exchange, 405, Map.of("error", "Method not allowed"));
                return;
            }

            String body = readBody(exchange);
            JsonObject json = JsonParser.parseString(body).getAsJsonObject();
            String name = json.has("name") ? json.get("name").getAsString().trim() : "";
            String password = json.has("password") ? json.get("password").getAsString().trim() : "";

            try {
                Student s = studentDAO.getStudentByName(name);
                if (s != null) {
                    // If student was auto-created during a guard drop with default placeholder 'student123',
                    // automatically adopt and set the password the student chooses on their first login!
                    if ("student123".equals(s.getPassword())) {
                        studentDAO.updatePassword(s.getRegNo(), password);
                        s.setPassword(password);
                        sendJsonResponse(exchange, 200, Map.of(
                                "success", true,
                                "student", s,
                                "message", "Welcome " + name + "! Your password has been successfully saved."
                        ));
                    } else if (s.getPassword().equals(password)) {
                        sendJsonResponse(exchange, 200, Map.of("success", true, "student", s));
                    } else {
                        sendJsonResponse(exchange, 401, Map.of("success", false, "error", "Incorrect password for student " + name));
                    }
                } else {
                    // Student does not exist -> trigger 2nd step: Roll No pop out!
                    sendJsonResponse(exchange, 200, Map.of(
                            "success", false,
                            "isNewUser", true,
                            "name", name,
                            "message", "Welcome " + name + "! Please enter your Roll No to complete registration."
                    ));
                }
            } catch (SQLException e) {
                sendJsonResponse(exchange, 500, Map.of("error", e.getMessage()));
            }
        }
    }

    private class StudentResetPasswordHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            setCorsHeaders(exchange);
            if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
                sendJsonResponse(exchange, 405, Map.of("error", "Method not allowed"));
                return;
            }

            String body = readBody(exchange);
            JsonObject json = JsonParser.parseString(body).getAsJsonObject();
            String name = json.has("name") ? json.get("name").getAsString().trim() : "";
            String regNo = json.has("regNo") ? json.get("regNo").getAsString().trim() : "";
            String newPassword = json.has("newPassword") ? json.get("newPassword").getAsString().trim() : "";

            try {
                Student s = studentDAO.getStudentByRegNo(regNo);
                if (s == null) {
                    s = studentDAO.getStudentByName(name);
                }

                if (s != null) {
                    studentDAO.updatePassword(s.getRegNo(), newPassword);
                    s.setPassword(newPassword);
                    sendJsonResponse(exchange, 200, Map.of("success", true, "student", s, "message", "Password successfully updated!"));
                } else {
                    sendJsonResponse(exchange, 404, Map.of("success", false, "error", "Student record not found for Roll No: " + regNo));
                }
            } catch (SQLException e) {
                sendJsonResponse(exchange, 500, Map.of("error", e.getMessage()));
            }
        }
    }

    private class StudentRegisterHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            setCorsHeaders(exchange);
            if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
                sendJsonResponse(exchange, 405, Map.of("error", "Method not allowed"));
                return;
            }

            String body = readBody(exchange);
            JsonObject json = JsonParser.parseString(body).getAsJsonObject();
            String name = json.get("name").getAsString().trim();
            String password = json.get("password").getAsString().trim();
            String regNo = json.get("regNo").getAsString().trim();

            try {
                Student existing = studentDAO.getStudentByRegNo(regNo);
                if (existing != null) {
                    existing.setName(name);
                    existing.setPassword(password);
                    studentDAO.updateStudentProfile(existing);
                    sendJsonResponse(exchange, 200, Map.of("success", true, "student", existing));
                } else {
                    Student created = studentDAO.registerStudent(regNo, name, password);
                    sendJsonResponse(exchange, 200, Map.of("success", true, "student", created));
                }
            } catch (SQLException e) {
                sendJsonResponse(exchange, 500, Map.of("error", e.getMessage()));
            }
        }
    }

    private class AdminLoginHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            setCorsHeaders(exchange);
            if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
                sendJsonResponse(exchange, 405, Map.of("error", "Method not allowed"));
                return;
            }

            String body = readBody(exchange);
            JsonObject json = JsonParser.parseString(body).getAsJsonObject();
            String username = json.has("username") ? json.get("username").getAsString().trim() : "";
            String password = json.has("password") ? json.get("password").getAsString().trim() : "";

            if (username.equalsIgnoreCase("admin") && password.equals("admin123")) {
                sendJsonResponse(exchange, 200, Map.of("success", true, "role", "ADMIN", "user", "admin"));
            } else {
                sendJsonResponse(exchange, 401, Map.of("success", false, "error", "Invalid credentials. Admin username must be 'admin' and password 'admin123'."));
            }
        }
    }

    private class GuardLoginHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            setCorsHeaders(exchange);
            if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
                sendJsonResponse(exchange, 405, Map.of("error", "Method not allowed"));
                return;
            }

            String body = readBody(exchange);
            JsonObject json = JsonParser.parseString(body).getAsJsonObject();
            String guardRollNo = json.has("guardRollNo") ? json.get("guardRollNo").getAsString().trim() : "";

            try {
                GuardShift shift = guardDAO.getGuardByRollNo(guardRollNo);
                if (shift != null) {
                    sendJsonResponse(exchange, 200, Map.of("success", true, "guard", shift));
                } else {
                    sendJsonResponse(exchange, 401, Map.of("success", false, "error", "Invalid Guard Roll No: '" + guardRollNo + "'. Only guards assigned a Roll No by the Admin can log in."));
                }
            } catch (SQLException e) {
                sendJsonResponse(exchange, 500, Map.of("error", e.getMessage()));
            }
        }
    }

    private class StudentsListHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            setCorsHeaders(exchange);
            try {
                List<Student> list = studentDAO.getAllStudents();
                sendJsonResponse(exchange, 200, Map.of("students", list));
            } catch (SQLException e) {
                sendJsonResponse(exchange, 500, Map.of("error", e.getMessage()));
            }
        }
    }

    private void setCorsHeaders(HttpExchange exchange) {
        exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
        exchange.getResponseHeaders().set("Access-Control-Allow-Methods", "GET, POST, DELETE, OPTIONS");
        exchange.getResponseHeaders().set("Access-Control-Allow-Headers", "Content-Type");
    }

    private String readBody(HttpExchange exchange) throws IOException {
        try (InputStream is = exchange.getRequestBody();
             ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            byte[] buf = new byte[1024];
            int read;
            while ((read = is.read(buf)) != -1) {
                baos.write(buf, 0, read);
            }
            return baos.toString(StandardCharsets.UTF_8);
        }
    }

    private void sendJsonResponse(HttpExchange exchange, int statusCode, Object data) throws IOException {
        String json = gson.toJson(data);
        byte[] bytes = json.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/json; charset=UTF-8");
        exchange.sendResponseHeaders(statusCode, bytes.length);
        OutputStream os = exchange.getResponseBody();
        os.write(bytes);
        os.close();
    }
}
