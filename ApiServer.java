// ============================================================
// API Server — Bridges dashboard.html ↔ MySQL Database
// Uses JDK built-in com.sun.net.httpserver (zero extra deps)
// Run:  java -cp ".;lib/mysql-connector-j-9.6.0.jar" ApiServer
// Then open http://localhost:8080 in your browser
// ============================================================

import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpExchange;

import java.io.*;
import java.net.InetSocketAddress;
import java.nio.file.*;
import java.sql.*;
import java.util.*;

public class ApiServer {

    // ── Port ──
    static final int PORT = 8080;

    public static void main(String[] args) throws Exception {
        // Establish DB connection on startup
        DBConnection.getConnection();
        System.out.println("[API] Database connected.");

        HttpServer server = HttpServer.create(new InetSocketAddress(PORT), 0);

        // Serve dashboard.html at root
        server.createContext("/", new DashboardHandler());

        // REST API endpoints
        server.createContext("/api/incidents", new IncidentsHandler());
        server.createContext("/api/incidents/all", new AllIncidentsHandler());
        server.createContext("/api/ambulances", new AmbulancesHandler());
        server.createContext("/api/responders", new RespondersHandler());
        server.createContext("/api/dispatches", new DispatchesHandler());
        server.createContext("/api/stats", new StatsHandler());
        server.createContext("/api/dispatch", new NewDispatchHandler());
        server.createContext("/api/hospitals", new HospitalsHandler());
        server.createContext("/api/history", new HistoryHandler());
        server.createContext("/api/zones", new ZonesHandler());
        server.createContext("/api/patients", new PatientsHandler());
        server.createContext("/api/triage", new TriageHandler());
        server.createContext("/api/equipment", new EquipmentHandler());

        server.setExecutor(null);
        server.start();

        System.out.println("╔══════════════════════════════════════════╗");
        System.out.println("║  API Server running on port " + PORT + "          ║");
        System.out.println("║  Open http://localhost:" + PORT + " in browser  ║");
        System.out.println("╚══════════════════════════════════════════╝");
    }

    // ── Helper: send JSON response with CORS headers ──
    static void sendJson(HttpExchange ex, int code, String json) throws IOException {
        ex.getResponseHeaders().set("Content-Type", "application/json; charset=UTF-8");
        ex.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
        ex.getResponseHeaders().set("Access-Control-Allow-Methods", "GET, POST, OPTIONS");
        ex.getResponseHeaders().set("Access-Control-Allow-Headers", "Content-Type");
        byte[] bytes = json.getBytes("UTF-8");
        ex.sendResponseHeaders(code, bytes.length);
        ex.getResponseBody().write(bytes);
        ex.getResponseBody().close();
    }

    // ── Helper: escape string for JSON ──
    static String esc(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }

    // ── Helper: read POST body ──
    static String readBody(HttpExchange ex) throws IOException {
        try (BufferedReader br = new BufferedReader(new InputStreamReader(ex.getRequestBody(), "UTF-8"))) {
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = br.readLine()) != null) sb.append(line);
            return sb.toString();
        }
    }

    // ── Helper: simple JSON field extractor ──
    static String jsonVal(String json, String key) {
        String search = "\"" + key + "\"";
        int idx = json.indexOf(search);
        if (idx < 0) return "";
        int colon = json.indexOf(":", idx);
        if (colon < 0) return "";
        int start = json.indexOf("\"", colon + 1);
        if (start < 0) return "";
        int end = json.indexOf("\"", start + 1);
        if (end < 0) return "";
        return json.substring(start + 1, end);
    }

    static int jsonIntVal(String json, String key) {
        String search = "\"" + key + "\"";
        int idx = json.indexOf(search);
        if (idx < 0) return 0;
        int colon = json.indexOf(":", idx);
        if (colon < 0) return 0;
        // skip whitespace
        int i = colon + 1;
        while (i < json.length() && (json.charAt(i) == ' ' || json.charAt(i) == '\t')) i++;
        StringBuilder num = new StringBuilder();
        while (i < json.length() && Character.isDigit(json.charAt(i))) {
            num.append(json.charAt(i));
            i++;
        }
        if (num.length() == 0) return 0;
        return Integer.parseInt(num.toString());
    }

    // ═══════════════════════════════════════════════════════════
    //  HANDLER: Serve dashboard.html
    // ═══════════════════════════════════════════════════════════
    static class DashboardHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange ex) throws IOException {
            String path = ex.getRequestURI().getPath();
            // Only serve root path; let API handlers handle /api/*
            if (path.startsWith("/api/")) return;

            try {
                byte[] html = Files.readAllBytes(Paths.get("dashboard.html"));
                ex.getResponseHeaders().set("Content-Type", "text/html; charset=UTF-8");
                ex.sendResponseHeaders(200, html.length);
                ex.getResponseBody().write(html);
            } catch (Exception e) {
                String msg = "dashboard.html not found. Place it in the same directory as ApiServer.";
                ex.sendResponseHeaders(404, msg.length());
                ex.getResponseBody().write(msg.getBytes());
            }
            ex.getResponseBody().close();
        }
    }

    // ═══════════════════════════════════════════════════════════
    //  GET /api/incidents — Active incidents from DB
    // ═══════════════════════════════════════════════════════════
    static class IncidentsHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange ex) throws IOException {
            if ("OPTIONS".equals(ex.getRequestMethod())) { sendJson(ex, 204, ""); return; }
            try {
                Connection conn = DBConnection.getConnection();
                String sql = """
                    SELECT i.incident_id, i.caller_name, i.caller_phone,
                           i.incident_type, i.severity, i.description,
                           i.incident_address, i.status, i.call_time,
                           COALESCE(a.vehicle_no, '—') AS vehicle_no,
                           COALESCE(r.full_name, '—')   AS responder_name
                    FROM incidents i
                    LEFT JOIN dispatch_log d  ON d.incident_id = i.incident_id
                    LEFT JOIN ambulances   a  ON a.ambulance_id = d.ambulance_id
                    LEFT JOIN responders   r  ON r.responder_id = d.responder_id
                    WHERE i.status NOT IN ('Resolved','Cancelled')
                    ORDER BY FIELD(i.severity,'Critical','High','Medium','Low'), i.call_time ASC
                """;
                StringBuilder sb = new StringBuilder("[");
                try (Statement st = conn.createStatement(); ResultSet rs = st.executeQuery(sql)) {
                    boolean first = true;
                    while (rs.next()) {
                        if (!first) sb.append(",");
                        first = false;
                        Timestamp callTime = rs.getTimestamp("call_time");
                        String timeStr = callTime != null
                                ? String.format("%02d:%02d", callTime.getHours(), callTime.getMinutes())
                                : "—";
                        sb.append(String.format(
                            "{\"id\":\"INC-%03d\",\"dbId\":%d,\"type\":\"%s\",\"severity\":\"%s\","
                          + "\"address\":\"%s\",\"amb\":\"%s\",\"resp\":\"%s\","
                          + "\"time\":\"%s\",\"status\":\"%s\",\"caller\":\"%s\",\"phone\":\"%s\",\"desc\":\"%s\"}",
                            rs.getInt("incident_id"), rs.getInt("incident_id"),
                            esc(rs.getString("incident_type")),
                            esc(rs.getString("severity")),
                            esc(rs.getString("incident_address")),
                            esc(rs.getString("vehicle_no")),
                            esc(rs.getString("responder_name")),
                            timeStr,
                            esc(rs.getString("status")),
                            esc(rs.getString("caller_name")),
                            esc(rs.getString("caller_phone")),
                            esc(rs.getString("description"))
                        ));
                    }
                }
                sb.append("]");
                sendJson(ex, 200, sb.toString());
            } catch (SQLException e) {
                sendJson(ex, 500, "{\"error\":\"" + esc(e.getMessage()) + "\"}");
            }
        }
    }

    // ═══════════════════════════════════════════════════════════
    //  GET /api/ambulances — All ambulances from DB
    // ═══════════════════════════════════════════════════════════
    static class AmbulancesHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange ex) throws IOException {
            if ("OPTIONS".equals(ex.getRequestMethod())) { sendJson(ex, 204, ""); return; }
            try {
                Connection conn = DBConnection.getConnection();
                String sql = "SELECT ambulance_id, vehicle_no, vehicle_type, status, zone_id FROM ambulances ORDER BY ambulance_id";
                StringBuilder sb = new StringBuilder("[");
                try (Statement st = conn.createStatement(); ResultSet rs = st.executeQuery(sql)) {
                    boolean first = true;
                    while (rs.next()) {
                        if (!first) sb.append(",");
                        first = false;
                        sb.append(String.format(
                            "{\"id\":%d,\"no\":\"%s\",\"type\":\"%s\",\"status\":\"%s\",\"zoneId\":%d}",
                            rs.getInt("ambulance_id"),
                            esc(rs.getString("vehicle_no")),
                            esc(rs.getString("vehicle_type")),
                            esc(rs.getString("status")),
                            rs.getInt("zone_id")
                        ));
                    }
                }
                sb.append("]");
                sendJson(ex, 200, sb.toString());
            } catch (SQLException e) {
                sendJson(ex, 500, "{\"error\":\"" + esc(e.getMessage()) + "\"}");
            }
        }
    }

    // ═══════════════════════════════════════════════════════════
    //  GET /api/responders — All responders from DB
    // ═══════════════════════════════════════════════════════════
    static class RespondersHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange ex) throws IOException {
            if ("OPTIONS".equals(ex.getRequestMethod())) { sendJson(ex, 204, ""); return; }
            try {
                Connection conn = DBConnection.getConnection();
                String sql = "SELECT responder_id, full_name, role, phone, status FROM responders ORDER BY responder_id";
                StringBuilder sb = new StringBuilder("[");
                try (Statement st = conn.createStatement(); ResultSet rs = st.executeQuery(sql)) {
                    boolean first = true;
                    while (rs.next()) {
                        if (!first) sb.append(",");
                        first = false;
                        sb.append(String.format(
                            "{\"id\":%d,\"name\":\"%s\",\"role\":\"%s\",\"phone\":\"%s\",\"status\":\"%s\"}",
                            rs.getInt("responder_id"),
                            esc(rs.getString("full_name")),
                            esc(rs.getString("role")),
                            esc(rs.getString("phone")),
                            esc(rs.getString("status"))
                        ));
                    }
                }
                sb.append("]");
                sendJson(ex, 200, sb.toString());
            } catch (SQLException e) {
                sendJson(ex, 500, "{\"error\":\"" + esc(e.getMessage()) + "\"}");
            }
        }
    }

    // ═══════════════════════════════════════════════════════════
    //  GET /api/dispatches — All dispatch logs from DB
    // ═══════════════════════════════════════════════════════════
    static class DispatchesHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange ex) throws IOException {
            if ("OPTIONS".equals(ex.getRequestMethod())) { sendJson(ex, 204, ""); return; }
            try {
                Connection conn = DBConnection.getConnection();
                String sql = """
                    SELECT d.dispatch_id, d.incident_id, d.ambulance_id,
                           d.dispatched_at, d.arrived_scene_at,
                           d.response_time_min, d.status,
                           COALESCE(a.vehicle_no, '—') AS vehicle_no
                    FROM dispatch_log d
                    LEFT JOIN ambulances a ON a.ambulance_id = d.ambulance_id
                    ORDER BY d.dispatched_at DESC
                    LIMIT 20
                """;
                StringBuilder sb = new StringBuilder("[");
                try (Statement st = conn.createStatement(); ResultSet rs = st.executeQuery(sql)) {
                    boolean first = true;
                    while (rs.next()) {
                        if (!first) sb.append(",");
                        first = false;
                        Timestamp dispatched = rs.getTimestamp("dispatched_at");
                        Timestamp arrived = rs.getTimestamp("arrived_scene_at");
                        String dispStr = dispatched != null
                                ? String.format("%02d:%02d", dispatched.getHours(), dispatched.getMinutes())
                                : "—";
                        String arrStr = arrived != null
                                ? String.format("%02d:%02d", arrived.getHours(), arrived.getMinutes())
                                : "—";
                        double respMin = rs.getDouble("response_time_min");
                        String respMinStr = rs.wasNull() ? "—" : String.format("%.1f", respMin);

                        sb.append(String.format(
                            "{\"id\":\"D-%03d\",\"incident\":\"INC-%03d\",\"amb\":\"%s\","
                          + "\"dispatched\":\"%s\",\"arrived\":\"%s\",\"respMin\":\"%s\",\"status\":\"%s\"}",
                            rs.getInt("dispatch_id"),
                            rs.getInt("incident_id"),
                            esc(rs.getString("vehicle_no")),
                            dispStr, arrStr, respMinStr,
                            esc(rs.getString("status"))
                        ));
                    }
                }
                sb.append("]");
                sendJson(ex, 200, sb.toString());
            } catch (SQLException e) {
                sendJson(ex, 500, "{\"error\":\"" + esc(e.getMessage()) + "\"}");
            }
        }
    }

    // ═══════════════════════════════════════════════════════════
    //  GET /api/stats — KPI numbers for the dashboard
    // ═══════════════════════════════════════════════════════════
    static class StatsHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange ex) throws IOException {
            if ("OPTIONS".equals(ex.getRequestMethod())) { sendJson(ex, 204, ""); return; }
            try {
                Connection conn = DBConnection.getConnection();

                int critical = 0, dispatched = 0, resolved = 0, pending = 0;
                int availAmb = 0, onDuty = 0;
                double avgResp = 0;

                // Critical active
                try (ResultSet rs = conn.createStatement().executeQuery(
                        "SELECT COUNT(*) AS c FROM incidents WHERE severity='Critical' AND status NOT IN ('Resolved','Cancelled')")) {
                    if (rs.next()) critical = rs.getInt("c");
                }

                // Dispatched today
                try (ResultSet rs = conn.createStatement().executeQuery(
                        "SELECT COUNT(*) AS c FROM dispatch_log WHERE DATE(dispatched_at) = CURDATE()")) {
                    if (rs.next()) dispatched = rs.getInt("c");
                }

                // Resolved today
                try (ResultSet rs = conn.createStatement().executeQuery(
                        "SELECT COUNT(*) AS c FROM incidents WHERE status='Resolved' AND DATE(resolved_at) = CURDATE()")) {
                    if (rs.next()) resolved = rs.getInt("c");
                }

                // Avg response time
                try (ResultSet rs = conn.createStatement().executeQuery(
                        "SELECT ROUND(AVG(response_time_min),1) AS a FROM dispatch_log WHERE response_time_min IS NOT NULL")) {
                    if (rs.next()) { avgResp = rs.getDouble("a"); if (rs.wasNull()) avgResp = 0; }
                }

                // Available ambulances
                try (ResultSet rs = conn.createStatement().executeQuery(
                        "SELECT COUNT(*) AS c FROM ambulances WHERE status='Available'")) {
                    if (rs.next()) availAmb = rs.getInt("c");
                }

                // Responders on duty
                try (ResultSet rs = conn.createStatement().executeQuery(
                        "SELECT COUNT(*) AS c FROM responders WHERE status IN ('Available','On Duty')")) {
                    if (rs.next()) onDuty = rs.getInt("c");
                }

                // Pending incidents
                try (ResultSet rs = conn.createStatement().executeQuery(
                        "SELECT COUNT(*) AS c FROM incidents WHERE status='Pending'")) {
                    if (rs.next()) pending = rs.getInt("c");
                }

                // Active incidents count (for sidebar badge)
                int activeCount = 0;
                try (ResultSet rs = conn.createStatement().executeQuery(
                        "SELECT COUNT(*) AS c FROM incidents WHERE status NOT IN ('Resolved','Cancelled')")) {
                    if (rs.next()) activeCount = rs.getInt("c");
                }

                String json = String.format(
                    "{\"critical\":%d,\"dispatched\":%d,\"resolved\":%d,\"avgResp\":%.1f,"
                  + "\"availAmb\":%d,\"onDuty\":%d,\"pending\":%d,\"activeCount\":%d}",
                    critical, dispatched, resolved, avgResp,
                    availAmb, onDuty, pending, activeCount
                );
                sendJson(ex, 200, json);
            } catch (SQLException e) {
                sendJson(ex, 500, "{\"error\":\"" + esc(e.getMessage()) + "\"}");
            }
        }
    }

    // ═══════════════════════════════════════════════════════════
    //  POST /api/dispatch — Create incident + auto-dispatch
    // ═══════════════════════════════════════════════════════════
    static class NewDispatchHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange ex) throws IOException {
            if ("OPTIONS".equals(ex.getRequestMethod())) { sendJson(ex, 204, ""); return; }
            if (!"POST".equals(ex.getRequestMethod())) {
                sendJson(ex, 405, "{\"error\":\"POST only\"}");
                return;
            }
            try {
                String body = readBody(ex);
                Connection conn = DBConnection.getConnection();

                // Check if this is a quick dispatch for an existing incident
                boolean isQuick = body.contains("\"quickDispatch\"") && body.contains("true");
                int existingIncidentId = jsonIntVal(body, "incidentId");

                if (isQuick && existingIncidentId > 0) {
                    // ── QUICK DISPATCH: dispatch an existing pending incident ──
                    int ambId  = jsonIntVal(body, "ambulanceId");
                    int respId = jsonIntVal(body, "responderId");

                    int dispatchId = -1;
                    if (ambId > 0) {
                        String dispSql = """
                            INSERT INTO dispatch_log (incident_id, ambulance_id, responder_id, status)
                            VALUES (?, ?, ?, 'Dispatched')
                        """;
                        try (PreparedStatement ps = conn.prepareStatement(dispSql, Statement.RETURN_GENERATED_KEYS)) {
                            ps.setInt(1, existingIncidentId);
                            ps.setInt(2, ambId);
                            ps.setInt(3, respId > 0 ? respId : 0);
                            ps.executeUpdate();
                            ResultSet keys = ps.getGeneratedKeys();
                            if (keys.next()) dispatchId = keys.getInt(1);
                        }

                        try (PreparedStatement ps = conn.prepareStatement(
                                "UPDATE ambulances SET status='Dispatched' WHERE ambulance_id=?")) {
                            ps.setInt(1, ambId);
                            ps.executeUpdate();
                        }
                        try (PreparedStatement ps = conn.prepareStatement(
                                "UPDATE incidents SET status='Dispatched' WHERE incident_id=?")) {
                            ps.setInt(1, existingIncidentId);
                            ps.executeUpdate();
                        }
                        if (respId > 0) {
                            try (PreparedStatement ps = conn.prepareStatement(
                                    "UPDATE responders SET status='On Duty' WHERE responder_id=?")) {
                                ps.setInt(1, respId);
                                ps.executeUpdate();
                            }
                        }
                    }

                    System.out.printf("[API] Quick dispatch: Incident #%d → Dispatch #%d%n", existingIncidentId, dispatchId);
                    sendJson(ex, 200, String.format(
                        "{\"ok\":true,\"incidentId\":%d,\"dispatchId\":%d}", existingIncidentId, dispatchId));
                    return;
                }

                // ── NEW INCIDENT + DISPATCH ──
                String caller  = jsonVal(body, "caller");
                String phone   = jsonVal(body, "phone");
                String type    = jsonVal(body, "type");
                String severity= jsonVal(body, "severity");
                String address = jsonVal(body, "address");
                String desc    = jsonVal(body, "description");
                int    ambId   = jsonIntVal(body, "ambulanceId");
                int    respId  = jsonIntVal(body, "responderId");

                if (caller.isBlank()) caller = "Unknown Caller";
                if (phone.isBlank())  phone  = "N/A";
                if (address.isBlank()) address = "Unknown Location";

                // Create incident
                String insSql = """
                    INSERT INTO incidents
                      (caller_name, caller_phone, incident_type, severity,
                       description, incident_address, status)
                    VALUES (?, ?, ?, ?, ?, ?, 'Pending')
                """;
                int incidentId;
                try (PreparedStatement ps = conn.prepareStatement(insSql, Statement.RETURN_GENERATED_KEYS)) {
                    ps.setString(1, caller);
                    ps.setString(2, phone);
                    ps.setString(3, type);
                    ps.setString(4, severity);
                    ps.setString(5, desc);
                    ps.setString(6, address);
                    ps.executeUpdate();
                    ResultSet keys = ps.getGeneratedKeys();
                    if (!keys.next()) { sendJson(ex, 500, "{\"error\":\"Failed to create incident\"}"); return; }
                    incidentId = keys.getInt(1);
                }

                // Auto-dispatch if ambulance selected
                int dispatchId = -1;
                if (ambId > 0) {
                    String dispSql = """
                        INSERT INTO dispatch_log (incident_id, ambulance_id, responder_id, status)
                        VALUES (?, ?, ?, 'Dispatched')
                    """;
                    try (PreparedStatement ps = conn.prepareStatement(dispSql, Statement.RETURN_GENERATED_KEYS)) {
                        ps.setInt(1, incidentId);
                        ps.setInt(2, ambId);
                        ps.setInt(3, respId > 0 ? respId : 0);
                        ps.executeUpdate();
                        ResultSet keys = ps.getGeneratedKeys();
                        if (keys.next()) dispatchId = keys.getInt(1);
                    }

                    // Update ambulance status
                    try (PreparedStatement ps = conn.prepareStatement(
                            "UPDATE ambulances SET status='Dispatched' WHERE ambulance_id=?")) {
                        ps.setInt(1, ambId);
                        ps.executeUpdate();
                    }

                    // Update incident status
                    try (PreparedStatement ps = conn.prepareStatement(
                            "UPDATE incidents SET status='Dispatched' WHERE incident_id=?")) {
                        ps.setInt(1, incidentId);
                        ps.executeUpdate();
                    }

                    // Update responder status
                    if (respId > 0) {
                        try (PreparedStatement ps = conn.prepareStatement(
                                "UPDATE responders SET status='On Duty' WHERE responder_id=?")) {
                            ps.setInt(1, respId);
                            ps.executeUpdate();
                        }
                    }
                }

                System.out.printf("[API] Incident #%d created, Dispatch #%d%n", incidentId, dispatchId);
                sendJson(ex, 200, String.format(
                    "{\"ok\":true,\"incidentId\":%d,\"dispatchId\":%d}", incidentId, dispatchId));

            } catch (Exception e) {
                sendJson(ex, 500, "{\"error\":\"" + esc(e.getMessage()) + "\"}");
            }
        }
    }

    // ═══════════════════════════════════════════════════════════
    //  GET /api/incidents/all — ALL incidents (including resolved)
    // ═══════════════════════════════════════════════════════════
    static class AllIncidentsHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange ex) throws IOException {
            if ("OPTIONS".equals(ex.getRequestMethod())) { sendJson(ex, 204, ""); return; }
            try {
                Connection conn = DBConnection.getConnection();
                String sql = """
                    SELECT i.incident_id, i.caller_name, i.caller_phone,
                           i.incident_type, i.severity, i.description,
                           i.incident_address, i.status, i.call_time, i.resolved_at,
                           COALESCE(a.vehicle_no, '—') AS vehicle_no,
                           COALESCE(r.full_name, '—')   AS responder_name
                    FROM incidents i
                    LEFT JOIN dispatch_log d  ON d.incident_id = i.incident_id
                    LEFT JOIN ambulances   a  ON a.ambulance_id = d.ambulance_id
                    LEFT JOIN responders   r  ON r.responder_id = d.responder_id
                    ORDER BY i.call_time DESC
                    LIMIT 100
                """;
                StringBuilder sb = new StringBuilder("[");
                try (Statement st = conn.createStatement(); ResultSet rs = st.executeQuery(sql)) {
                    boolean first = true;
                    while (rs.next()) {
                        if (!first) sb.append(",");
                        first = false;
                        Timestamp callTime = rs.getTimestamp("call_time");
                        String timeStr = callTime != null
                                ? String.format("%tF %tR", callTime, callTime)
                                : "—";
                        Timestamp resolvedAt = rs.getTimestamp("resolved_at");
                        String resolvedStr = resolvedAt != null
                                ? String.format("%tF %tR", resolvedAt, resolvedAt)
                                : "—";
                        sb.append(String.format(
                            "{\"id\":\"INC-%03d\",\"dbId\":%d,\"type\":\"%s\",\"severity\":\"%s\","
                          + "\"address\":\"%s\",\"amb\":\"%s\",\"resp\":\"%s\","
                          + "\"time\":\"%s\",\"resolvedAt\":\"%s\",\"status\":\"%s\",\"caller\":\"%s\",\"phone\":\"%s\",\"desc\":\"%s\"}",
                            rs.getInt("incident_id"), rs.getInt("incident_id"),
                            esc(rs.getString("incident_type")),
                            esc(rs.getString("severity")),
                            esc(rs.getString("incident_address")),
                            esc(rs.getString("vehicle_no")),
                            esc(rs.getString("responder_name")),
                            timeStr, resolvedStr,
                            esc(rs.getString("status")),
                            esc(rs.getString("caller_name")),
                            esc(rs.getString("caller_phone")),
                            esc(rs.getString("description"))
                        ));
                    }
                }
                sb.append("]");
                sendJson(ex, 200, sb.toString());
            } catch (SQLException e) {
                sendJson(ex, 500, "{\"error\":\"" + esc(e.getMessage()) + "\"}");
            }
        }
    }

    // ═══════════════════════════════════════════════════════════
    //  GET /api/hospitals — All hospitals from DB
    // ═══════════════════════════════════════════════════════════
    static class HospitalsHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange ex) throws IOException {
            if ("OPTIONS".equals(ex.getRequestMethod())) { sendJson(ex, 204, ""); return; }
            try {
                Connection conn = DBConnection.getConnection();
                String sql = """
                    SELECT h.hospital_id, h.name, h.address, h.phone,
                           h.total_beds, h.available_beds, h.trauma_center,
                           COALESCE(z.zone_name,'—') AS zone_name
                    FROM hospitals h
                    LEFT JOIN zones z ON z.zone_id = h.zone_id
                    ORDER BY h.hospital_id
                """;
                StringBuilder sb = new StringBuilder("[");
                try (Statement st = conn.createStatement(); ResultSet rs = st.executeQuery(sql)) {
                    boolean first = true;
                    while (rs.next()) {
                        if (!first) sb.append(",");
                        first = false;
                        sb.append(String.format(
                            "{\"id\":%d,\"name\":\"%s\",\"address\":\"%s\",\"phone\":\"%s\","
                          + "\"totalBeds\":%d,\"availBeds\":%d,\"trauma\":%b,\"zone\":\"%s\"}",
                            rs.getInt("hospital_id"),
                            esc(rs.getString("name")),
                            esc(rs.getString("address")),
                            esc(rs.getString("phone")),
                            rs.getInt("total_beds"),
                            rs.getInt("available_beds"),
                            rs.getBoolean("trauma_center"),
                            esc(rs.getString("zone_name"))
                        ));
                    }
                }
                sb.append("]");
                sendJson(ex, 200, sb.toString());
            } catch (SQLException e) {
                sendJson(ex, 500, "{\"error\":\"" + esc(e.getMessage()) + "\"}");
            }
        }
    }

    // ═══════════════════════════════════════════════════════════
    //  GET /api/history — Audit log / DB change history
    // ═══════════════════════════════════════════════════════════
    static class HistoryHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange ex) throws IOException {
            if ("OPTIONS".equals(ex.getRequestMethod())) { sendJson(ex, 204, ""); return; }
            try {
                Connection conn = DBConnection.getConnection();
                String sql = """
                    SELECT log_id, table_name, record_id, action,
                           changed_by, changed_at, old_values, new_values
                    FROM audit_log
                    ORDER BY changed_at DESC
                    LIMIT 100
                """;
                StringBuilder sb = new StringBuilder("[");
                try (Statement st = conn.createStatement(); ResultSet rs = st.executeQuery(sql)) {
                    boolean first = true;
                    while (rs.next()) {
                        if (!first) sb.append(",");
                        first = false;
                        Timestamp changedAt = rs.getTimestamp("changed_at");
                        String timeStr = changedAt != null
                                ? String.format("%tF %tR", changedAt, changedAt)
                                : "—";
                        String oldVals = rs.getString("old_values");
                        String newVals = rs.getString("new_values");
                        sb.append(String.format(
                            "{\"id\":%d,\"table\":\"%s\",\"recordId\":%d,\"action\":\"%s\","
                          + "\"changedBy\":\"%s\",\"changedAt\":\"%s\","
                          + "\"oldValues\":\"%s\",\"newValues\":\"%s\"}",
                            rs.getInt("log_id"),
                            esc(rs.getString("table_name")),
                            rs.getInt("record_id"),
                            esc(rs.getString("action")),
                            esc(rs.getString("changed_by")),
                            timeStr,
                            esc(oldVals != null ? oldVals : ""),
                            esc(newVals != null ? newVals : "")
                        ));
                    }
                }
                sb.append("]");
                sendJson(ex, 200, sb.toString());
            } catch (SQLException e) {
                sendJson(ex, 500, "{\"error\":\"" + esc(e.getMessage()) + "\"}");
            }
        }
    }

    // ═══════════════════════════════════════════════════════════
    //  GET /api/zones — All zones from DB
    // ═══════════════════════════════════════════════════════════
    static class ZonesHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange ex) throws IOException {
            if ("OPTIONS".equals(ex.getRequestMethod())) { sendJson(ex, 204, ""); return; }
            try {
                Connection conn = DBConnection.getConnection();
                String sql = "SELECT zone_id, zone_name, region, latitude, longitude, created_at FROM zones ORDER BY zone_id";
                StringBuilder sb = new StringBuilder("[");
                try (Statement st = conn.createStatement(); ResultSet rs = st.executeQuery(sql)) {
                    boolean first = true;
                    while (rs.next()) {
                        if (!first) sb.append(",");
                        first = false;
                        sb.append(String.format(
                            "{\"id\":%d,\"name\":\"%s\",\"region\":\"%s\",\"lat\":%s,\"lng\":%s}",
                            rs.getInt("zone_id"),
                            esc(rs.getString("zone_name")),
                            esc(rs.getString("region")),
                            rs.getObject("latitude") != null ? rs.getBigDecimal("latitude").toPlainString() : "null",
                            rs.getObject("longitude") != null ? rs.getBigDecimal("longitude").toPlainString() : "null"
                        ));
                    }
                }
                sb.append("]");
                sendJson(ex, 200, sb.toString());
            } catch (SQLException e) {
                sendJson(ex, 500, "{\"error\":\"" + esc(e.getMessage()) + "\"}");
            }
        }
    }

    // ═══════════════════════════════════════════════════════════
    //  GET /api/patients — All patients from DB
    // ═══════════════════════════════════════════════════════════
    static class PatientsHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange ex) throws IOException {
            if ("OPTIONS".equals(ex.getRequestMethod())) { sendJson(ex, 204, ""); return; }
            try {
                Connection conn = DBConnection.getConnection();
                String sql = "SELECT patient_id, full_name, dob, gender, blood_group, phone, address, allergies, medical_history FROM patients ORDER BY patient_id";
                StringBuilder sb = new StringBuilder("[");
                try (Statement st = conn.createStatement(); ResultSet rs = st.executeQuery(sql)) {
                    boolean first = true;
                    while (rs.next()) {
                        if (!first) sb.append(",");
                        first = false;
                        sb.append(String.format(
                            "{\"id\":%d,\"name\":\"%s\",\"dob\":\"%s\",\"gender\":\"%s\","
                          + "\"bloodGroup\":\"%s\",\"phone\":\"%s\",\"address\":\"%s\","
                          + "\"allergies\":\"%s\",\"medicalHistory\":\"%s\"}",
                            rs.getInt("patient_id"),
                            esc(rs.getString("full_name")),
                            esc(rs.getString("dob") != null ? rs.getString("dob") : "—"),
                            esc(rs.getString("gender")),
                            esc(rs.getString("blood_group") != null ? rs.getString("blood_group") : "—"),
                            esc(rs.getString("phone") != null ? rs.getString("phone") : "—"),
                            esc(rs.getString("address") != null ? rs.getString("address") : "—"),
                            esc(rs.getString("allergies") != null ? rs.getString("allergies") : "None"),
                            esc(rs.getString("medical_history") != null ? rs.getString("medical_history") : "None")
                        ));
                    }
                }
                sb.append("]");
                sendJson(ex, 200, sb.toString());
            } catch (SQLException e) {
                sendJson(ex, 500, "{\"error\":\"" + esc(e.getMessage()) + "\"}");
            }
        }
    }

    // ═══════════════════════════════════════════════════════════
    //  GET /api/triage — All triage records from DB
    // ═══════════════════════════════════════════════════════════
    static class TriageHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange ex) throws IOException {
            if ("OPTIONS".equals(ex.getRequestMethod())) { sendJson(ex, 204, ""); return; }
            try {
                Connection conn = DBConnection.getConnection();
                String sql = """
                    SELECT t.triage_id, t.dispatch_id, t.triage_level,
                           t.chief_complaint, t.vitals_bp, t.vitals_pulse,
                           t.vitals_spo2, t.vitals_temp, t.gcs_score,
                           t.treatment_notes, t.recorded_at,
                           COALESCE(p.full_name, '—') AS patient_name
                    FROM triage_records t
                    LEFT JOIN patients p ON p.patient_id = t.patient_id
                    ORDER BY t.recorded_at DESC
                    LIMIT 100
                """;
                StringBuilder sb = new StringBuilder("[");
                try (Statement st = conn.createStatement(); ResultSet rs = st.executeQuery(sql)) {
                    boolean first = true;
                    while (rs.next()) {
                        if (!first) sb.append(",");
                        first = false;
                        Timestamp recordedAt = rs.getTimestamp("recorded_at");
                        String timeStr = recordedAt != null
                                ? String.format("%tF %tR", recordedAt, recordedAt)
                                : "—";
                        sb.append(String.format(
                            "{\"id\":%d,\"dispatchId\":%d,\"level\":\"%s\","
                          + "\"complaint\":\"%s\",\"bp\":\"%s\",\"pulse\":%d,"
                          + "\"spo2\":\"%.1f\",\"temp\":\"%.1f\",\"gcs\":%d,"
                          + "\"notes\":\"%s\",\"time\":\"%s\",\"patient\":\"%s\"}",
                            rs.getInt("triage_id"),
                            rs.getInt("dispatch_id"),
                            esc(rs.getString("triage_level")),
                            esc(rs.getString("chief_complaint") != null ? rs.getString("chief_complaint") : "—"),
                            esc(rs.getString("vitals_bp") != null ? rs.getString("vitals_bp") : "—"),
                            rs.getInt("vitals_pulse"),
                            rs.getDouble("vitals_spo2"),
                            rs.getDouble("vitals_temp"),
                            rs.getInt("gcs_score"),
                            esc(rs.getString("treatment_notes") != null ? rs.getString("treatment_notes") : "—"),
                            timeStr,
                            esc(rs.getString("patient_name"))
                        ));
                    }
                }
                sb.append("]");
                sendJson(ex, 200, sb.toString());
            } catch (SQLException e) {
                sendJson(ex, 500, "{\"error\":\"" + esc(e.getMessage()) + "\"}");
            }
        }
    }

    // ═══════════════════════════════════════════════════════════
    //  GET /api/equipment — All equipment from DB
    // ═══════════════════════════════════════════════════════════
    static class EquipmentHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange ex) throws IOException {
            if ("OPTIONS".equals(ex.getRequestMethod())) { sendJson(ex, 204, ""); return; }
            try {
                Connection conn = DBConnection.getConnection();
                String sql = """
                    SELECT e.equipment_id, e.name, e.category, e.quantity,
                           e.last_checked, e.expiry_date, e.status,
                           COALESCE(a.vehicle_no, '—') AS vehicle_no
                    FROM equipment e
                    LEFT JOIN ambulances a ON a.ambulance_id = e.ambulance_id
                    ORDER BY e.equipment_id
                """;
                StringBuilder sb = new StringBuilder("[");
                try (Statement st = conn.createStatement(); ResultSet rs = st.executeQuery(sql)) {
                    boolean first = true;
                    while (rs.next()) {
                        if (!first) sb.append(",");
                        first = false;
                        sb.append(String.format(
                            "{\"id\":%d,\"name\":\"%s\",\"category\":\"%s\",\"qty\":%d,"
                          + "\"lastChecked\":\"%s\",\"expiry\":\"%s\",\"status\":\"%s\",\"vehicle\":\"%s\"}",
                            rs.getInt("equipment_id"),
                            esc(rs.getString("name")),
                            esc(rs.getString("category") != null ? rs.getString("category") : "—"),
                            rs.getInt("quantity"),
                            esc(rs.getString("last_checked") != null ? rs.getString("last_checked") : "—"),
                            esc(rs.getString("expiry_date") != null ? rs.getString("expiry_date") : "—"),
                            esc(rs.getString("status")),
                            esc(rs.getString("vehicle_no"))
                        ));
                    }
                }
                sb.append("]");
                sendJson(ex, 200, sb.toString());
            } catch (SQLException e) {
                sendJson(ex, 500, "{\"error\":\"" + esc(e.getMessage()) + "\"}");
            }
        }
    }
}
