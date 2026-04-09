// ============================================================
// Emergency Healthcare Response & Dispatch Tracking System
// Java JDBC Backend  — Single-file demo (split into packages in prod)
// Dependencies: MySQL Connector/J 9.x
// ============================================================

import java.sql.*;
import java.util.*;

// ─────────────────────────────────────────────────────────────
//  ADDED: Interface — defines the 4 standard CRUD operations
//         that every DAO must support (Abstraction via interface)
// ─────────────────────────────────────────────────────────────
interface CrudOperations<T> {
    // INSERT
    boolean insert(T entity) throws SQLException;
    // SELECT
    T selectById(int id) throws SQLException;
    // UPDATE
    boolean updateStatus(int id, String newStatus) throws SQLException;
    // DELETE
    boolean delete(int id) throws SQLException;
}

// ─────────────────────────────────────────────────────────────
//  ADDED: Abstract base class — shared validation logic and
//         common protected helper used by all DAO subclasses
//         (Abstraction + Inheritance base)
// ─────────────────────────────────────────────────────────────
abstract class BaseDAO {

    // ADDED: protected — accessible to all subclass DAOs but not outside
    protected Connection getConn() throws SQLException {
        return DBConnection.getConnection();
    }

    // ADDED: Validation — shared method for non-null / non-blank strings
    protected void validateNotBlank(String value, String fieldName) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException("Validation failed: '" + fieldName + "' must not be blank.");
        }
    }

    // ADDED: Validation — shared method for positive integer IDs
    protected void validatePositiveId(int id, String fieldName) {
        if (id <= 0) {
            throw new IllegalArgumentException("Validation failed: '" + fieldName + "' must be a positive number.");
        }
    }

    // ADDED: abstract method — forces every subclass to describe itself
    //        (Polymorphism — each DAO returns a different description)
    public abstract String getDaoName();
}

// ─────────────────────────────────────────────────────────────
//  1. DATABASE CONNECTION MANAGER
// ─────────────────────────────────────────────────────────────
class DBConnection {
    // ADDED: private — credentials hidden (Encapsulation)
    private static final String URL = "jdbc:mysql://localhost:3306/emergency_dispatch";
    private static final String USER = "root";
    private static final String PASSWORD = "Ss*160922";

    private static Connection connection;

    public static Connection getConnection() throws SQLException {
        if (connection == null || connection.isClosed()) {
            try {
                Class.forName("com.mysql.cj.jdbc.Driver");
                connection = DriverManager.getConnection(URL, USER, PASSWORD);
                System.out.println("[DB] Connection established.");
            } catch (ClassNotFoundException e) {
                throw new SQLException("MySQL JDBC Driver not found.", e);
            }
        }
        return connection;
    }

    public static void closeConnection() {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
                System.out.println("[DB] Connection closed.");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}

// ─────────────────────────────────────────────────────────────
// 2. MODELS  — ENCAPSULATION applied: fields now private,
//              exposed via public getters and setters
// ─────────────────────────────────────────────────────────────
class Incident {
    // ADDED: private fields (Encapsulation)
    private int incidentId;
    private String callerName;
    private String callerPhone;
    private String incidentType;
    private String severity;
    private String description;
    private String address;
    private String status;
    private Timestamp callTime;

    // ADDED: public getters and setters
    public int getIncidentId()              { return incidentId; }
    public void setIncidentId(int v)        { this.incidentId = v; }
    public String getCallerName()           { return callerName; }
    public void setCallerName(String v)     { this.callerName = v; }
    public String getCallerPhone()          { return callerPhone; }
    public void setCallerPhone(String v)    { this.callerPhone = v; }
    public String getIncidentType()         { return incidentType; }
    public void setIncidentType(String v)   { this.incidentType = v; }
    public String getSeverity()             { return severity; }
    public void setSeverity(String v)       { this.severity = v; }
    public String getDescription()          { return description; }
    public void setDescription(String v)    { this.description = v; }
    public String getAddress()              { return address; }
    public void setAddress(String v)        { this.address = v; }
    public String getStatus()               { return status; }
    public void setStatus(String v)         { this.status = v; }
    public Timestamp getCallTime()          { return callTime; }
    public void setCallTime(Timestamp v)    { this.callTime = v; }

    @Override
    public String toString() {
        return String.format("[Incident #%d | %s | %s | %s | %s]",
                incidentId, incidentType, severity, status, address);
    }
}

class Ambulance {
    // ADDED: private fields (Encapsulation)
    private int ambulanceId;
    private String vehicleNo;
    private String vehicleType;
    private String status;
    private int zoneId;

    // ADDED: public getters and setters
    public int getAmbulanceId()             { return ambulanceId; }
    public void setAmbulanceId(int v)       { this.ambulanceId = v; }
    public String getVehicleNo()            { return vehicleNo; }
    public void setVehicleNo(String v)      { this.vehicleNo = v; }
    public String getVehicleType()          { return vehicleType; }
    public void setVehicleType(String v)    { this.vehicleType = v; }
    public String getStatus()               { return status; }
    public void setStatus(String v)         { this.status = v; }
    public int getZoneId()                  { return zoneId; }
    public void setZoneId(int v)            { this.zoneId = v; }

    @Override
    public String toString() {
        return String.format("[Ambulance #%d | %s | %s | %s]",
                ambulanceId, vehicleNo, vehicleType, status);
    }
}

class Responder {
    // ADDED: private fields (Encapsulation)
    private int responderId;
    private String fullName;
    private String role;
    private String phone;
    private String status;

    // ADDED: public getters and setters
    public int getResponderId()             { return responderId; }
    public void setResponderId(int v)       { this.responderId = v; }
    public String getFullName()             { return fullName; }
    public void setFullName(String v)       { this.fullName = v; }
    public String getRole()                 { return role; }
    public void setRole(String v)           { this.role = v; }
    public String getPhone()                { return phone; }
    public void setPhone(String v)          { this.phone = v; }
    public String getStatus()               { return status; }
    public void setStatus(String v)         { this.status = v; }

    @Override
    public String toString() {
        return String.format("[Responder #%d | %s | %s | %s]",
                responderId, fullName, role, status);
    }
}

class DispatchLog {
    // ADDED: private fields (Encapsulation)
    private int dispatchId;
    private int incidentId;
    private int ambulanceId;
    private int responderId;
    private Timestamp dispatchedAt;
    private String status;

    // ADDED: public getters and setters
    public int getDispatchId()              { return dispatchId; }
    public void setDispatchId(int v)        { this.dispatchId = v; }
    public int getIncidentId()              { return incidentId; }
    public void setIncidentId(int v)        { this.incidentId = v; }
    public int getAmbulanceId()             { return ambulanceId; }
    public void setAmbulanceId(int v)       { this.ambulanceId = v; }
    public int getResponderId()             { return responderId; }
    public void setResponderId(int v)       { this.responderId = v; }
    public Timestamp getDispatchedAt()      { return dispatchedAt; }
    public void setDispatchedAt(Timestamp v){ this.dispatchedAt = v; }
    public String getStatus()               { return status; }
    public void setStatus(String v)         { this.status = v; }

    @Override
    public String toString() {
        return String.format("[Dispatch #%d | Incident:%d | Ambulance:%d | %s | %s]",
                dispatchId, incidentId, ambulanceId, status, dispatchedAt);
    }
}

// ─────────────────────────────────────────────────────────────
// 3. INCIDENT DAO
//    ADDED: extends BaseDAO (Inheritance)
//           implements CrudOperations (Interface / Polymorphism)
// ─────────────────────────────────────────────────────────────
class IncidentDAO extends BaseDAO implements CrudOperations<Incident> {

    // ADDED: Polymorphism — concrete implementation of abstract method
    @Override
    public String getDaoName() { return "IncidentDAO"; }

    // ADDED: Interface method — INSERT  (Validation added)
    @Override
    public boolean insert(Incident inc) throws SQLException {
        validateNotBlank(inc.getCallerName(),   "callerName");   // ADDED: Validation
        validateNotBlank(inc.getCallerPhone(),  "callerPhone");  // ADDED: Validation
        validateNotBlank(inc.getIncidentType(), "incidentType"); // ADDED: Validation
        validateNotBlank(inc.getSeverity(),     "severity");     // ADDED: Validation
        validateNotBlank(inc.getAddress(),      "address");      // ADDED: Validation
        return createIncident(inc) > 0;
    }

    // ADDED: Interface method — SELECT by ID
    @Override
    public Incident selectById(int id) throws SQLException {
        validatePositiveId(id, "incidentId"); // ADDED: Validation
        return getById(id);
    }

    // ADDED: Interface method — UPDATE (overrides the interface, delegates below)
    @Override
    public boolean updateStatus(int incidentId, String newStatus) throws SQLException {
        validatePositiveId(incidentId, "incidentId"); // ADDED: Validation
        validateNotBlank(newStatus, "newStatus");      // ADDED: Validation
        String sql = "UPDATE incidents SET status = ? WHERE incident_id = ?";
        Connection conn = getConn(); // uses protected helper from BaseDAO
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, newStatus);
            ps.setInt(2, incidentId);
            int rows = ps.executeUpdate();
            System.out.printf("[Incident #%d] Status → %s%n", incidentId, newStatus);
            return rows > 0;
        }
    }

    // ADDED: Interface method — DELETE
    @Override
    public boolean delete(int incidentId) throws SQLException {
        validatePositiveId(incidentId, "incidentId"); // ADDED: Validation
        return deleteIncident(incidentId);
    }

    // ── original methods kept exactly as-is ──────────────────

    public int createIncident(Incident inc) throws SQLException {
        String sql = """
                    INSERT INTO incidents
                      (caller_name, caller_phone, incident_type, severity,
                       description, incident_address, status)
                    VALUES (?, ?, ?, ?, ?, ?, 'Pending')
                """;
        Connection conn = DBConnection.getConnection();
        try (PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, inc.getCallerName());
            ps.setString(2, inc.getCallerPhone());
            ps.setString(3, inc.getIncidentType());
            ps.setString(4, inc.getSeverity());
            ps.setString(5, inc.getDescription());
            ps.setString(6, inc.getAddress());
            ps.executeUpdate();
            ResultSet rs = ps.getGeneratedKeys();
            if (rs.next()) {
                int id = rs.getInt(1);
                System.out.println("[Incident] Created with ID: " + id);
                return id;
            }
        }
        return -1;
    }

    public List<Incident> getActiveIncidents() throws SQLException {
        List<Incident> list = new ArrayList<>();
        String sql = """
                    SELECT incident_id, caller_name, caller_phone,
                           incident_type, severity, description,
                           incident_address, status, call_time
                    FROM incidents
                    WHERE status NOT IN ('Resolved','Cancelled')
                    ORDER BY
                      FIELD(severity,'Critical','High','Medium','Low'),
                      call_time ASC
                """;
        Connection conn = DBConnection.getConnection();
        try (Statement st = conn.createStatement();
                ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                Incident i = new Incident();
                i.setIncidentId(rs.getInt("incident_id"));
                i.setCallerName(rs.getString("caller_name"));
                i.setCallerPhone(rs.getString("caller_phone"));
                i.setIncidentType(rs.getString("incident_type"));
                i.setSeverity(rs.getString("severity"));
                i.setDescription(rs.getString("description"));
                i.setAddress(rs.getString("incident_address"));
                i.setStatus(rs.getString("status"));
                i.setCallTime(rs.getTimestamp("call_time"));
                list.add(i);
            }
        }
        return list;
    }

    public Incident getById(int id) throws SQLException {
        String sql = "SELECT * FROM incidents WHERE incident_id = ?";
        Connection conn = DBConnection.getConnection();
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                Incident i = new Incident();
                i.setIncidentId(rs.getInt("incident_id"));
                i.setCallerName(rs.getString("caller_name"));
                i.setCallerPhone(rs.getString("caller_phone"));
                i.setIncidentType(rs.getString("incident_type"));
                i.setSeverity(rs.getString("severity"));
                i.setDescription(rs.getString("description"));
                i.setAddress(rs.getString("incident_address"));
                i.setStatus(rs.getString("status"));
                i.setCallTime(rs.getTimestamp("call_time"));
                return i;
            }
        }
        return null;
    }

    public boolean deleteIncident(int incidentId) throws SQLException {
        String sql = "DELETE FROM incidents WHERE incident_id = ?";
        Connection conn = DBConnection.getConnection();
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, incidentId);
            return ps.executeUpdate() > 0;
        }
    }
}

// ─────────────────────────────────────────────────────────────
// 4. AMBULANCE DAO
//    ADDED: extends BaseDAO (Inheritance)
//           implements CrudOperations (Interface / Polymorphism)
// ─────────────────────────────────────────────────────────────
class AmbulanceDAO extends BaseDAO implements CrudOperations<Ambulance> {

    // ADDED: Polymorphism — concrete implementation of abstract method
    @Override
    public String getDaoName() { return "AmbulanceDAO"; }

    // ADDED: Interface method — INSERT (stub; ambulances are pre-registered in DB)
    @Override
    public boolean insert(Ambulance entity) throws SQLException {
        validateNotBlank(entity.getVehicleNo(), "vehicleNo"); // ADDED: Validation
        // Ambulance insert not exposed in menu but satisfies interface contract
        String sql = "INSERT INTO ambulances (vehicle_no, vehicle_type, status, zone_id) VALUES (?,?,?,?)";
        try (PreparedStatement ps = getConn().prepareStatement(sql)) {
            ps.setString(1, entity.getVehicleNo());
            ps.setString(2, entity.getVehicleType());
            ps.setString(3, entity.getStatus() != null ? entity.getStatus() : "Available");
            ps.setInt(4, entity.getZoneId());
            return ps.executeUpdate() > 0;
        }
    }

    // ADDED: Interface method — SELECT by ID
    @Override
    public Ambulance selectById(int id) throws SQLException {
        validatePositiveId(id, "ambulanceId"); // ADDED: Validation
        String sql = "SELECT * FROM ambulances WHERE ambulance_id = ?";
        try (PreparedStatement ps = getConn().prepareStatement(sql)) {
            ps.setInt(1, id);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                Ambulance a = new Ambulance();
                a.setAmbulanceId(rs.getInt("ambulance_id"));
                a.setVehicleNo(rs.getString("vehicle_no"));
                a.setVehicleType(rs.getString("vehicle_type"));
                a.setStatus(rs.getString("status"));
                a.setZoneId(rs.getInt("zone_id"));
                return a;
            }
        }
        return null;
    }

    // ADDED: Interface method — UPDATE
    @Override
    public boolean updateStatus(int ambulanceId, String newStatus) throws SQLException {
        validatePositiveId(ambulanceId, "ambulanceId"); // ADDED: Validation
        validateNotBlank(newStatus, "newStatus");        // ADDED: Validation
        String sql = "UPDATE ambulances SET status = ? WHERE ambulance_id = ?";
        Connection conn = DBConnection.getConnection();
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, newStatus);
            ps.setInt(2, ambulanceId);
            int rows = ps.executeUpdate();
            System.out.printf("[Ambulance #%d] Status → %s%n", ambulanceId, newStatus);
            return rows > 0;
        }
    }

    // ADDED: Interface method — DELETE
    @Override
    public boolean delete(int ambulanceId) throws SQLException {
        validatePositiveId(ambulanceId, "ambulanceId"); // ADDED: Validation
        return deleteAmbulance(ambulanceId);
    }

    // ── original methods kept exactly as-is ──────────────────

    public List<Ambulance> getAvailableAmbulances() throws SQLException {
        List<Ambulance> list = new ArrayList<>();
        String sql = "SELECT * FROM ambulances WHERE status = 'Available'";
        Connection conn = DBConnection.getConnection();
        try (Statement st = conn.createStatement();
                ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                Ambulance a = new Ambulance();
                a.setAmbulanceId(rs.getInt("ambulance_id"));
                a.setVehicleNo(rs.getString("vehicle_no"));
                a.setVehicleType(rs.getString("vehicle_type"));
                a.setStatus(rs.getString("status"));
                a.setZoneId(rs.getInt("zone_id"));
                list.add(a);
            }
        }
        return list;
    }

    public boolean updateLocation(int ambulanceId, double lat, double lon) throws SQLException {
        String sql = "UPDATE ambulances SET last_latitude=?, last_longitude=? WHERE ambulance_id=?";
        Connection conn = DBConnection.getConnection();
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setDouble(1, lat);
            ps.setDouble(2, lon);
            ps.setInt(3, ambulanceId);
            return ps.executeUpdate() > 0;
        }
    }

    public boolean deleteAmbulance(int ambulanceId) throws SQLException {
        String sql = "DELETE FROM ambulances WHERE ambulance_id = ?";
        Connection conn = DBConnection.getConnection();
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, ambulanceId);
            return ps.executeUpdate() > 0;
        }
    }
}

// ─────────────────────────────────────────────────────────────
// 5. RESPONDER DAO
//    ADDED: extends BaseDAO (Inheritance)
//           implements CrudOperations (Interface / Polymorphism)
// ─────────────────────────────────────────────────────────────
class ResponderDAO extends BaseDAO implements CrudOperations<Responder> {

    // ADDED: Polymorphism — concrete implementation of abstract method
    @Override
    public String getDaoName() { return "ResponderDAO"; }

    // ADDED: Interface method — INSERT
    @Override
    public boolean insert(Responder entity) throws SQLException {
        validateNotBlank(entity.getFullName(), "fullName"); // ADDED: Validation
        validateNotBlank(entity.getRole(),     "role");     // ADDED: Validation
        String sql = "INSERT INTO responders (full_name, role, phone, status) VALUES (?,?,?,?)";
        try (PreparedStatement ps = getConn().prepareStatement(sql)) {
            ps.setString(1, entity.getFullName());
            ps.setString(2, entity.getRole());
            ps.setString(3, entity.getPhone());
            ps.setString(4, entity.getStatus() != null ? entity.getStatus() : "Available");
            return ps.executeUpdate() > 0;
        }
    }

    // ADDED: Interface method — SELECT by ID
    @Override
    public Responder selectById(int id) throws SQLException {
        validatePositiveId(id, "responderId"); // ADDED: Validation
        String sql = "SELECT * FROM responders WHERE responder_id = ?";
        try (PreparedStatement ps = getConn().prepareStatement(sql)) {
            ps.setInt(1, id);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                Responder r = new Responder();
                r.setResponderId(rs.getInt("responder_id"));
                r.setFullName(rs.getString("full_name"));
                r.setRole(rs.getString("role"));
                r.setPhone(rs.getString("phone"));
                r.setStatus(rs.getString("status"));
                return r;
            }
        }
        return null;
    }

    // ADDED: Interface method — UPDATE
    @Override
    public boolean updateStatus(int responderId, String newStatus) throws SQLException {
        validatePositiveId(responderId, "responderId"); // ADDED: Validation
        validateNotBlank(newStatus, "newStatus");        // ADDED: Validation
        String sql = "UPDATE responders SET status = ? WHERE responder_id = ?";
        Connection conn = DBConnection.getConnection();
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, newStatus);
            ps.setInt(2, responderId);
            return ps.executeUpdate() > 0;
        }
    }

    // ADDED: Interface method — DELETE
    @Override
    public boolean delete(int responderId) throws SQLException {
        validatePositiveId(responderId, "responderId"); // ADDED: Validation
        return deleteResponder(responderId);
    }

    // ── original methods kept exactly as-is ──────────────────

    public List<Responder> getAvailableResponders() throws SQLException {
        List<Responder> list = new ArrayList<>();
        String sql = "SELECT * FROM responders WHERE status = 'Available'";
        Connection conn = DBConnection.getConnection();
        try (Statement st = conn.createStatement();
                ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                Responder r = new Responder();
                r.setResponderId(rs.getInt("responder_id"));
                r.setFullName(rs.getString("full_name"));
                r.setRole(rs.getString("role"));
                r.setPhone(rs.getString("phone"));
                r.setStatus(rs.getString("status"));
                list.add(r);
            }
        }
        return list;
    }

    public boolean deleteResponder(int responderId) throws SQLException {
        String sql = "DELETE FROM responders WHERE responder_id = ?";
        Connection conn = DBConnection.getConnection();
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, responderId);
            return ps.executeUpdate() > 0;
        }
    }
}

// ─────────────────────────────────────────────────────────────
// 6. DISPATCH DAO
//    ADDED: extends BaseDAO (Inheritance)
//           implements CrudOperations (Interface / Polymorphism)
// ─────────────────────────────────────────────────────────────
class DispatchDAO extends BaseDAO implements CrudOperations<DispatchLog> {

    // ADDED: Polymorphism — concrete implementation of abstract method
    @Override
    public String getDaoName() { return "DispatchDAO"; }

    // ADDED: Interface method — INSERT
    @Override
    public boolean insert(DispatchLog entity) throws SQLException {
        validatePositiveId(entity.getIncidentId(),  "incidentId");  // ADDED: Validation
        validatePositiveId(entity.getAmbulanceId(), "ambulanceId"); // ADDED: Validation
        return createDispatch(entity.getIncidentId(), entity.getAmbulanceId(), entity.getResponderId()) > 0;
    }

    // ADDED: Interface method — SELECT by ID
    @Override
    public DispatchLog selectById(int id) throws SQLException {
        validatePositiveId(id, "dispatchId"); // ADDED: Validation
        String sql = "SELECT * FROM dispatch_log WHERE dispatch_id = ?";
        try (PreparedStatement ps = getConn().prepareStatement(sql)) {
            ps.setInt(1, id);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                DispatchLog d = new DispatchLog();
                d.setDispatchId(rs.getInt("dispatch_id"));
                d.setIncidentId(rs.getInt("incident_id"));
                d.setAmbulanceId(rs.getInt("ambulance_id"));
                d.setResponderId(rs.getInt("responder_id"));
                d.setDispatchedAt(rs.getTimestamp("dispatched_at"));
                d.setStatus(rs.getString("status"));
                return d;
            }
        }
        return null;
    }

    // ADDED: Interface method — UPDATE
    @Override
    public boolean updateStatus(int dispatchId, String newStatus) throws SQLException {
        validatePositiveId(dispatchId, "dispatchId"); // ADDED: Validation
        validateNotBlank(newStatus, "newStatus");      // ADDED: Validation
        String sql = "UPDATE dispatch_log SET status = ? WHERE dispatch_id = ?";
        try (PreparedStatement ps = getConn().prepareStatement(sql)) {
            ps.setString(1, newStatus);
            ps.setInt(2, dispatchId);
            return ps.executeUpdate() > 0;
        }
    }

    // ADDED: Interface method — DELETE
    @Override
    public boolean delete(int dispatchId) throws SQLException {
        validatePositiveId(dispatchId, "dispatchId"); // ADDED: Validation
        return deleteDispatch(dispatchId);
    }

    // ── original methods kept exactly as-is ──────────────────

    public int createDispatch(int incidentId, int ambulanceId, int responderId)
            throws SQLException {
        String sql = """
                    INSERT INTO dispatch_log
                      (incident_id, ambulance_id, responder_id, status)
                    VALUES (?, ?, ?, 'Dispatched')
                """;
        Connection conn = DBConnection.getConnection();
        try (PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, incidentId);
            ps.setInt(2, ambulanceId);
            ps.setInt(3, responderId);
            ps.executeUpdate();
            ResultSet rs = ps.getGeneratedKeys();
            if (rs.next()) {
                int id = rs.getInt(1);
                System.out.println("[Dispatch] Log created with ID: " + id);
                return id;
            }
        }
        return -1;
    }

    public boolean markArrivedScene(int dispatchId) throws SQLException {
        String sql = """
                    UPDATE dispatch_log
                    SET arrived_scene_at = NOW(), status = 'At Scene'
                    WHERE dispatch_id = ?
                """;
        Connection conn = DBConnection.getConnection();
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, dispatchId);
            boolean ok = ps.executeUpdate() > 0;
            if (ok)
                System.out.printf("[Dispatch #%d] Arrived at scene.%n", dispatchId);
            return ok;
        }
    }

    public boolean markTransporting(int dispatchId, int hospitalId) throws SQLException {
        String sql = """
                    UPDATE dispatch_log
                    SET departed_scene_at = NOW(), hospital_id = ?, status = 'Transporting'
                    WHERE dispatch_id = ?
                """;
        Connection conn = DBConnection.getConnection();
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, hospitalId);
            ps.setInt(2, dispatchId);
            return ps.executeUpdate() > 0;
        }
    }

    public boolean markCompleted(int dispatchId) throws SQLException {
        String sql = """
                    UPDATE dispatch_log
                    SET arrived_hospital_at = NOW(), status = 'Completed'
                    WHERE dispatch_id = ?
                """;
        Connection conn = DBConnection.getConnection();
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, dispatchId);
            return ps.executeUpdate() > 0;
        }
    }

    public List<DispatchLog> getAllDispatches() throws SQLException {
        List<DispatchLog> list = new ArrayList<>();
        String sql = """
                    SELECT dispatch_id, incident_id, ambulance_id,
                           responder_id, dispatched_at, status
                    FROM dispatch_log
                    ORDER BY dispatched_at DESC
                """;
        Connection conn = DBConnection.getConnection();
        try (Statement st = conn.createStatement();
                ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                DispatchLog d = new DispatchLog();
                d.setDispatchId(rs.getInt("dispatch_id"));
                d.setIncidentId(rs.getInt("incident_id"));
                d.setAmbulanceId(rs.getInt("ambulance_id"));
                d.setResponderId(rs.getInt("responder_id"));
                d.setDispatchedAt(rs.getTimestamp("dispatched_at"));
                d.setStatus(rs.getString("status"));
                list.add(d);
            }
        }
        return list;
    }

    public boolean deleteDispatch(int dispatchId) throws SQLException {
        String sql = "DELETE FROM dispatch_log WHERE dispatch_id = ?";
        Connection conn = DBConnection.getConnection();
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, dispatchId);
            return ps.executeUpdate() > 0;
        }
    }

    public void printResponseReport() throws SQLException {
        String sql = """
                    SELECT
                      i.incident_type,
                      COUNT(d.dispatch_id) AS total_dispatches,
                      ROUND(AVG(d.response_time_min), 2) AS avg_response_min,
                      ROUND(MIN(d.response_time_min), 2) AS min_response_min,
                      ROUND(MAX(d.response_time_min), 2) AS max_response_min
                    FROM dispatch_log d
                    JOIN incidents i ON d.incident_id = i.incident_id
                    WHERE d.arrived_scene_at IS NOT NULL
                    GROUP BY i.incident_type
                    ORDER BY avg_response_min ASC
                """;
        Connection conn = DBConnection.getConnection();
        try (Statement st = conn.createStatement();
                ResultSet rs = st.executeQuery(sql)) {
            System.out.println("\n=== Response Time Report ===");
            System.out.printf("%-20s %8s %12s %10s %10s%n",
                    "Type", "Total", "Avg(min)", "Min(min)", "Max(min)");
            System.out.println("-".repeat(65));
            while (rs.next()) {
                System.out.printf("%-20s %8d %12.2f %10.2f %10.2f%n",
                        rs.getString("incident_type"),
                        rs.getInt("total_dispatches"),
                        rs.getDouble("avg_response_min"),
                        rs.getDouble("min_response_min"),
                        rs.getDouble("max_response_min"));
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────
// 7. DISPATCH SERVICE (Orchestrator)
// ─────────────────────────────────────────────────────────────
class DispatchService {
    private final IncidentDAO incidentDAO = new IncidentDAO();
    private final AmbulanceDAO ambulanceDAO = new AmbulanceDAO();
    private final ResponderDAO responderDAO = new ResponderDAO();
    private final DispatchDAO dispatchDAO = new DispatchDAO();

    public int dispatchForIncident(Incident inc) throws SQLException {
        int incidentId = incidentDAO.createIncident(inc);
        if (incidentId < 0)
            throw new SQLException("Failed to create incident.");

        List<Ambulance> ambulances = ambulanceDAO.getAvailableAmbulances();
        List<Responder> responders = responderDAO.getAvailableResponders();

        if (ambulances.isEmpty()) {
            System.out.println("[WARNING] No ambulances available! Queueing incident.");
            return -1;
        }

        Ambulance chosenAmbulance = ambulances.get(0);
        Responder chosenResponder = responders.isEmpty() ? null : responders.get(0);
        int responderId = (chosenResponder != null) ? chosenResponder.getResponderId() : 0;

        int dispatchId = dispatchDAO.createDispatch(incidentId, chosenAmbulance.getAmbulanceId(), responderId);

        ambulanceDAO.updateStatus(chosenAmbulance.getAmbulanceId(), "Dispatched");
        incidentDAO.updateStatus(incidentId, "Dispatched");
        if (chosenResponder != null)
            responderDAO.updateStatus(chosenResponder.getResponderId(), "On Duty");

        System.out.printf("[Service] Incident #%d dispatched → Ambulance %s, Responder: %s%n",
                incidentId,
                chosenAmbulance.getVehicleNo(),
                chosenResponder != null ? chosenResponder.getFullName() : "None");

        return dispatchId;
    }
}

// ─────────────────────────────────────────────────────────────
// 8. MAIN — Demo Entry Point
// ─────────────────────────────────────────────────────────────
public class EmergencyDispatchApp {
    static Scanner sc = new Scanner(System.in);
    static IncidentDAO incidentDAO = new IncidentDAO();
    static AmbulanceDAO ambulanceDAO = new AmbulanceDAO();
    static ResponderDAO responderDAO = new ResponderDAO();
    static DispatchDAO dispatchDAO = new DispatchDAO();
    static DispatchService service = new DispatchService();

    public static void main(String[] args) {
        try {
            DBConnection.getConnection();
            while (true) {
                System.out.println("""
                        \n╔══ EMERGENCY DISPATCH SYSTEM ══╗
                        ║ 1. Incidents                  ║
                        ║ 2. Ambulances                 ║
                        ║ 3. Responders                 ║
                        ║ 4. Dispatch                   ║
                        ║ 5. Patients                   ║
                        ║ 6. Equipment                  ║
                        ║ 7. Triage Records             ║
                        ║ 8. Reports                    ║
                        ║ 9. View Tables                ║
                        ║ 0. Exit                       ║
                        ╚═══════════════════════════════╝""");
                System.out.print("Choice: ");
                int choice = sc.nextInt();
                switch (choice) {
                    case 1 -> incidentMenu();
                    case 2 -> ambulanceMenu();
                    case 3 -> responderMenu();
                    case 4 -> dispatchMenu();
                    case 5 -> patientMenu();
                    case 6 -> equipmentMenu();
                    case 7 -> triageMenu();
                    case 8 -> reportsMenu();
                    case 9 -> viewTablesMenu();
                    case 0 -> {
                        DBConnection.closeConnection();
                        return;
                    }
                    default -> System.out.println("Invalid choice.");
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // ── INCIDENT MENU ──────────────────────────────────────
    static void incidentMenu() throws SQLException {
        while (true) {
            System.out.println("""
                    \n--- INCIDENTS ---
                    1. Log New Incident
                    2. View Active Incidents
                    3. View by ID
                    4. Update Status
                    5. Delete Incident
                    0. Back""");
            System.out.print("Choice: ");
            switch (sc.nextInt()) {
                case 1 -> {
                    Incident inc = new Incident();
                    sc.nextLine();
                    System.out.print("Caller Name: ");
                    inc.setCallerName(sc.nextLine());
                    System.out.print("Phone: ");
                    inc.setCallerPhone(sc.nextLine());
                    System.out.print("Type (Cardiac Arrest/Respiratory/Trauma/...): ");
                    inc.setIncidentType(sc.nextLine());
                    System.out.print("Severity (Critical/High/Medium/Low): ");
                    inc.setSeverity(sc.nextLine());
                    System.out.print("Description: ");
                    inc.setDescription(sc.nextLine());
                    System.out.print("Address: ");
                    inc.setAddress(sc.nextLine());
                    try {
                        incidentDAO.insert(inc); // ADDED: uses interface method with validation
                    } catch (IllegalArgumentException e) {
                        System.out.println("[Validation Error] " + e.getMessage());
                    }
                }
                case 2 -> incidentDAO.getActiveIncidents().forEach(System.out::println);
                case 3 -> {
                    System.out.print("Incident ID: ");
                    int id = sc.nextInt();
                    try {
                        Incident i = incidentDAO.selectById(id); // ADDED: uses interface method with validation
                        System.out.println(i != null ? i : "Not found.");
                    } catch (IllegalArgumentException e) {
                        System.out.println("[Validation Error] " + e.getMessage());
                    }
                }
                case 4 -> {
                    System.out.print("Incident ID: ");
                    int id = sc.nextInt();
                    sc.nextLine();
                    System.out.print("New Status (Pending/Dispatched/Resolved/Cancelled): ");
                    String status = sc.nextLine();
                    try {
                        incidentDAO.updateStatus(id, status); // ADDED: validation inside
                    } catch (IllegalArgumentException e) {
                        System.out.println("[Validation Error] " + e.getMessage());
                    }
                }
                case 5 -> {
                    System.out.print("Incident ID: ");
                    int id = sc.nextInt();
                    System.out.print("Confirm delete? (y/n): ");
                    if (sc.next().equalsIgnoreCase("y")) {
                        try {
                            System.out.println(incidentDAO.delete(id) ? "Deleted." : "Not found."); // ADDED: interface method
                        } catch (IllegalArgumentException e) {
                            System.out.println("[Validation Error] " + e.getMessage());
                        }
                    }
                }
                case 0 -> { return; }
                default -> System.out.println("Invalid choice.");
            }
        }
    }

    // ── AMBULANCE MENU ─────────────────────────────────────
    static void ambulanceMenu() throws SQLException {
        while (true) {
            System.out.println("""
                    \n--- AMBULANCES ---
                    1. View Available
                    2. Update Status
                    3. Update GPS Location
                    4. Delete Ambulance
                    0. Back""");
            System.out.print("Choice: ");
            switch (sc.nextInt()) {
                case 1 -> ambulanceDAO.getAvailableAmbulances().forEach(System.out::println);
                case 2 -> {
                    System.out.print("Ambulance ID: ");
                    int id = sc.nextInt();
                    sc.nextLine();
                    System.out.print("New Status (Available/Dispatched/Maintenance): ");
                    String status = sc.nextLine();
                    try {
                        ambulanceDAO.updateStatus(id, status); // ADDED: validation inside
                    } catch (IllegalArgumentException e) {
                        System.out.println("[Validation Error] " + e.getMessage());
                    }
                }
                case 3 -> {
                    System.out.print("Ambulance ID: ");
                    int id = sc.nextInt();
                    System.out.print("Latitude: ");
                    double lat = sc.nextDouble();
                    System.out.print("Longitude: ");
                    double lon = sc.nextDouble();
                    ambulanceDAO.updateLocation(id, lat, lon);
                    System.out.println("Location updated.");
                }
                case 4 -> {
                    System.out.print("Ambulance ID: ");
                    int id = sc.nextInt();
                    System.out.print("Confirm delete? (y/n): ");
                    if (sc.next().equalsIgnoreCase("y")) {
                        try {
                            System.out.println(ambulanceDAO.delete(id) ? "Deleted." : "Not found."); // ADDED: interface method
                        } catch (IllegalArgumentException e) {
                            System.out.println("[Validation Error] " + e.getMessage());
                        }
                    }
                }
                case 0 -> { return; }
                default -> System.out.println("Invalid choice.");
            }
        }
    }

    // ── RESPONDER MENU ─────────────────────────────────────
    static void responderMenu() throws SQLException {
        while (true) {
            System.out.println("""
                    \n--- RESPONDERS ---
                    1. View Available
                    2. Update Status
                    3. Delete Responder
                    0. Back""");
            System.out.print("Choice: ");
            switch (sc.nextInt()) {
                case 1 -> responderDAO.getAvailableResponders().forEach(System.out::println);
                case 2 -> {
                    System.out.print("Responder ID: ");
                    int id = sc.nextInt();
                    sc.nextLine();
                    System.out.print("New Status (Available/On Duty/Off Duty): ");
                    String status = sc.nextLine();
                    try {
                        responderDAO.updateStatus(id, status); // ADDED: validation inside
                    } catch (IllegalArgumentException e) {
                        System.out.println("[Validation Error] " + e.getMessage());
                    }
                }
                case 3 -> {
                    System.out.print("Responder ID: ");
                    int id = sc.nextInt();
                    System.out.print("Confirm delete? (y/n): ");
                    if (sc.next().equalsIgnoreCase("y")) {
                        try {
                            System.out.println(responderDAO.delete(id) ? "Deleted." : "Not found."); // ADDED: interface method
                        } catch (IllegalArgumentException e) {
                            System.out.println("[Validation Error] " + e.getMessage());
                        }
                    }
                }
                case 0 -> { return; }
                default -> System.out.println("Invalid choice.");
            }
        }
    }

    // ── DISPATCH MENU ──────────────────────────────────────
    static void dispatchMenu() throws SQLException {
        while (true) {
            System.out.println("""
                    \n--- DISPATCH ---
                    1. Auto-Dispatch for New Incident
                    2. View All Dispatch Logs
                    3. Mark Arrived at Scene
                    4. Mark Transporting to Hospital
                    5. Mark Completed
                    6. Delete Dispatch Log
                    0. Back""");
            System.out.print("Choice: ");
            switch (sc.nextInt()) {
                case 1 -> {
                    Incident inc = new Incident();
                    sc.nextLine();
                    System.out.print("Caller Name: ");
                    inc.setCallerName(sc.nextLine());
                    System.out.print("Phone: ");
                    inc.setCallerPhone(sc.nextLine());
                    System.out.print("Type: ");
                    inc.setIncidentType(sc.nextLine());
                    System.out.print("Severity: ");
                    inc.setSeverity(sc.nextLine());
                    System.out.print("Description: ");
                    inc.setDescription(sc.nextLine());
                    System.out.print("Address: ");
                    inc.setAddress(sc.nextLine());
                    int dId = service.dispatchForIncident(inc);
                    if (dId > 0)
                        System.out.println("Dispatch ID: " + dId);
                }
                case 2 -> dispatchDAO.getAllDispatches().forEach(System.out::println);
                case 3 -> {
                    System.out.print("Dispatch ID: ");
                    dispatchDAO.markArrivedScene(sc.nextInt());
                }
                case 4 -> {
                    System.out.print("Dispatch ID: ");
                    int dId = sc.nextInt();
                    System.out.print("Hospital ID: ");
                    int hId = sc.nextInt();
                    dispatchDAO.markTransporting(dId, hId);
                }
                case 5 -> {
                    System.out.print("Dispatch ID: ");
                    dispatchDAO.markCompleted(sc.nextInt());
                }
                case 6 -> {
                    System.out.print("Dispatch ID: ");
                    int id = sc.nextInt();
                    System.out.print("Confirm delete? (y/n): ");
                    if (sc.next().equalsIgnoreCase("y")) {
                        try {
                            System.out.println(dispatchDAO.delete(id) ? "Deleted." : "Not found."); // ADDED: interface method
                        } catch (IllegalArgumentException e) {
                            System.out.println("[Validation Error] " + e.getMessage());
                        }
                    }
                }
                case 0 -> { return; }
                default -> System.out.println("Invalid choice.");
            }
        }
    }

    // ── PATIENT MENU ───────────────────────────────────────
    static void patientMenu() throws SQLException {
        Connection conn = DBConnection.getConnection();
        while (true) {
            System.out.println("""
                    \n--- PATIENTS ---
                    1. Add Patient
                    2. View All Patients
                    3. View by ID
                    4. Update Patient
                    5. Delete Patient
                    0. Back""");
            System.out.print("Choice: ");
            switch (sc.nextInt()) {
                case 1 -> {
                    sc.nextLine();
                    System.out.print("Full Name: ");
                    String name = sc.nextLine();
                    System.out.print("DOB (YYYY-MM-DD): ");
                    String dob = sc.nextLine();
                    System.out.print("Gender: ");
                    String gender = sc.nextLine();
                    System.out.print("Blood Group: ");
                    String blood = sc.nextLine();
                    System.out.print("Phone: ");
                    String phone = sc.nextLine();
                    System.out.print("Address: ");
                    String addr = sc.nextLine();
                    System.out.print("Allergies: ");
                    String allerg = sc.nextLine();
                    System.out.print("Medical History: ");
                    String hist = sc.nextLine();
                    // ADDED: Validation for required patient fields
                    if (name.isBlank() || phone.isBlank()) {
                        System.out.println("[Validation Error] Name and Phone are required.");
                        break;
                    }
                    String sql = "INSERT INTO patients (full_name,dob,gender,blood_group,phone,address,allergies,medical_history) VALUES (?,?,?,?,?,?,?,?)";
                    try (PreparedStatement ps = conn.prepareStatement(sql)) {
                        ps.setString(1, name);
                        ps.setString(2, dob);
                        ps.setString(3, gender);
                        ps.setString(4, blood);
                        ps.setString(5, phone);
                        ps.setString(6, addr);
                        ps.setString(7, allerg);
                        ps.setString(8, hist);
                        ps.executeUpdate();
                        System.out.println("Patient added.");
                    }
                }
                case 2 -> {
                    try (ResultSet rs = conn.createStatement().executeQuery("SELECT * FROM patients")) {
                        while (rs.next())
                            System.out.printf("[Patient #%d | %s | %s | %s | %s]%n",
                                    rs.getInt("patient_id"), rs.getString("full_name"),
                                    rs.getString("blood_group"), rs.getString("phone"),
                                    rs.getString("address"));
                    }
                }
                case 3 -> {
                    System.out.print("Patient ID: ");
                    int id = sc.nextInt();
                    // ADDED: Validation
                    if (id <= 0) { System.out.println("[Validation Error] ID must be positive."); break; }
                    try (PreparedStatement ps = conn.prepareStatement("SELECT * FROM patients WHERE patient_id=?")) {
                        ps.setInt(1, id);
                        ResultSet rs = ps.executeQuery();
                        if (rs.next())
                            System.out.printf("[Patient #%d | %s | DOB:%s | %s | %s | %s | Allergies:%s]%n",
                                    rs.getInt("patient_id"), rs.getString("full_name"),
                                    rs.getString("dob"), rs.getString("gender"),
                                    rs.getString("blood_group"), rs.getString("phone"),
                                    rs.getString("allergies"));
                        else
                            System.out.println("Not found.");
                    }
                }
                case 4 -> {
                    System.out.print("Patient ID: ");
                    int id = sc.nextInt();
                    sc.nextLine();
                    System.out.print("New Phone: ");
                    String phone = sc.nextLine();
                    System.out.print("New Address: ");
                    String addr = sc.nextLine();
                    // ADDED: Validation
                    if (phone.isBlank()) { System.out.println("[Validation Error] Phone must not be blank."); break; }
                    try (PreparedStatement ps = conn.prepareStatement(
                            "UPDATE patients SET phone=?, address=? WHERE patient_id=?")) {
                        ps.setString(1, phone);
                        ps.setString(2, addr);
                        ps.setInt(3, id);
                        System.out.println(ps.executeUpdate() > 0 ? "Updated." : "Not found.");
                    }
                }
                case 5 -> {
                    System.out.print("Patient ID: ");
                    int id = sc.nextInt();
                    System.out.print("Confirm delete? (y/n): ");
                    if (sc.next().equalsIgnoreCase("y")) {
                        try (PreparedStatement ps = conn.prepareStatement("DELETE FROM patients WHERE patient_id=?")) {
                            ps.setInt(1, id);
                            System.out.println(ps.executeUpdate() > 0 ? "Deleted." : "Not found.");
                        }
                    }
                }
                case 0 -> { return; }
                default -> System.out.println("Invalid choice.");
            }
        }
    }

    // ── EQUIPMENT MENU ─────────────────────────────────────
    static void equipmentMenu() throws SQLException {
        Connection conn = DBConnection.getConnection();
        while (true) {
            System.out.println("""
                    \n--- EQUIPMENT ---
                    1. Add Equipment
                    2. View All Equipment
                    3. View by Ambulance ID
                    4. Update Status
                    5. Delete Equipment
                    0. Back""");
            System.out.print("Choice: ");
            switch (sc.nextInt()) {
                case 1 -> {
                    sc.nextLine();
                    System.out.print("Name: ");
                    String name = sc.nextLine();
                    System.out.print("Category: ");
                    String cat = sc.nextLine();
                    System.out.print("Quantity: ");
                    int qty = sc.nextInt();
                    System.out.print("Ambulance ID: ");
                    int ambId = sc.nextInt();
                    sc.nextLine();
                    System.out.print("Expiry (YYYY-MM-DD): ");
                    String exp = sc.nextLine();
                    // ADDED: Validation
                    if (name.isBlank() || cat.isBlank()) {
                        System.out.println("[Validation Error] Name and Category are required.");
                        break;
                    }
                    if (qty <= 0) {
                        System.out.println("[Validation Error] Quantity must be positive.");
                        break;
                    }
                    String sql = "INSERT INTO equipment (name,category,quantity,ambulance_id,last_checked,expiry_date,status) VALUES (?,?,?,?,CURDATE(),?,'Available')";
                    try (PreparedStatement ps = conn.prepareStatement(sql)) {
                        ps.setString(1, name);
                        ps.setString(2, cat);
                        ps.setInt(3, qty);
                        ps.setInt(4, ambId);
                        ps.setString(5, exp);
                        ps.executeUpdate();
                        System.out.println("Equipment added.");
                    }
                }
                case 2 -> {
                    try (ResultSet rs = conn.createStatement().executeQuery("SELECT * FROM equipment")) {
                        while (rs.next())
                            System.out.printf("[Equip #%d | %s | %s | Qty:%d | Amb#%d | %s]%n",
                                    rs.getInt("equipment_id"), rs.getString("name"),
                                    rs.getString("category"), rs.getInt("quantity"),
                                    rs.getInt("ambulance_id"), rs.getString("status"));
                    }
                }
                case 3 -> {
                    System.out.print("Ambulance ID: ");
                    int id = sc.nextInt();
                    try (PreparedStatement ps = conn.prepareStatement("SELECT * FROM equipment WHERE ambulance_id=?")) {
                        ps.setInt(1, id);
                        ResultSet rs = ps.executeQuery();
                        while (rs.next())
                            System.out.printf("[Equip #%d | %s | Qty:%d | %s]%n",
                                    rs.getInt("equipment_id"), rs.getString("name"),
                                    rs.getInt("quantity"), rs.getString("status"));
                    }
                }
                case 4 -> {
                    System.out.print("Equipment ID: ");
                    int id = sc.nextInt();
                    sc.nextLine();
                    System.out.print("New Status (Available/In Use/Expired): ");
                    String st = sc.nextLine();
                    // ADDED: Validation
                    if (st.isBlank()) { System.out.println("[Validation Error] Status must not be blank."); break; }
                    try (PreparedStatement ps = conn
                            .prepareStatement("UPDATE equipment SET status=? WHERE equipment_id=?")) {
                        ps.setString(1, st);
                        ps.setInt(2, id);
                        System.out.println(ps.executeUpdate() > 0 ? "Updated." : "Not found.");
                    }
                }
                case 5 -> {
                    System.out.print("Equipment ID: ");
                    int id = sc.nextInt();
                    System.out.print("Confirm delete? (y/n): ");
                    if (sc.next().equalsIgnoreCase("y")) {
                        try (PreparedStatement ps = conn
                                .prepareStatement("DELETE FROM equipment WHERE equipment_id=?")) {
                            ps.setInt(1, id);
                            System.out.println(ps.executeUpdate() > 0 ? "Deleted." : "Not found.");
                        }
                    }
                }
                case 0 -> { return; }
                default -> System.out.println("Invalid choice.");
            }
        }
    }

    // ── TRIAGE MENU ────────────────────────────────────────
    static void triageMenu() throws SQLException {
        Connection conn = DBConnection.getConnection();
        while (true) {
            System.out.println("""
                    \n--- TRIAGE RECORDS ---
                    1. Add Triage Record
                    2. View All Records
                    3. View by Dispatch ID
                    4. Update Notes
                    5. Delete Triage Record
                    0. Back""");
            System.out.print("Choice: ");
            switch (sc.nextInt()) {
                case 1 -> {
                    sc.nextLine();
                    System.out.print("Dispatch ID: ");
                    int dId = Integer.parseInt(sc.nextLine());
                    System.out.print("Patient ID: ");
                    int pId = Integer.parseInt(sc.nextLine());
                    System.out.print("Triage Level (Immediate/Delayed/Minor): ");
                    String lvl = sc.nextLine();
                    System.out.print("Chief Complaint: ");
                    String complaint = sc.nextLine();
                    System.out.print("BP (e.g. 120/80): ");
                    String bp = sc.nextLine();
                    System.out.print("Pulse: ");
                    int pulse = Integer.parseInt(sc.nextLine());
                    System.out.print("SpO2: ");
                    double spo2 = Double.parseDouble(sc.nextLine());
                    System.out.print("Temp: ");
                    double temp = Double.parseDouble(sc.nextLine());
                    System.out.print("GCS Score: ");
                    int gcs = Integer.parseInt(sc.nextLine());
                    System.out.print("Treatment Notes: ");
                    String notes = sc.nextLine();
                    // ADDED: Validation
                    if (lvl.isBlank() || complaint.isBlank()) {
                        System.out.println("[Validation Error] Triage Level and Chief Complaint are required.");
                        break;
                    }
                    if (pulse <= 0 || spo2 <= 0) {
                        System.out.println("[Validation Error] Pulse and SpO2 must be positive values.");
                        break;
                    }
                    String sql = "INSERT INTO triage_records (dispatch_id,patient_id,triage_level,chief_complaint,vitals_bp,vitals_pulse,vitals_spo2,vitals_temp,gcs_score,treatment_notes) VALUES (?,?,?,?,?,?,?,?,?,?)";
                    try (PreparedStatement ps = conn.prepareStatement(sql)) {
                        ps.setInt(1, dId);
                        ps.setInt(2, pId);
                        ps.setString(3, lvl);
                        ps.setString(4, complaint);
                        ps.setString(5, bp);
                        ps.setInt(6, pulse);
                        ps.setDouble(7, spo2);
                        ps.setDouble(8, temp);
                        ps.setInt(9, gcs);
                        ps.setString(10, notes);
                        ps.executeUpdate();
                        System.out.println("Triage record added.");
                    }
                }
                case 2 -> {
                    try (ResultSet rs = conn.createStatement().executeQuery("SELECT * FROM triage_records")) {
                        while (rs.next())
                            System.out.printf("[Triage #%d | Dispatch#%d | Patient#%d | %s | %s]%n",
                                    rs.getInt("triage_id"), rs.getInt("dispatch_id"),
                                    rs.getInt("patient_id"), rs.getString("triage_level"),
                                    rs.getString("chief_complaint"));
                    }
                }
                case 3 -> {
                    System.out.print("Dispatch ID: ");
                    int id = sc.nextInt();
                    try (PreparedStatement ps = conn
                            .prepareStatement("SELECT * FROM triage_records WHERE dispatch_id=?")) {
                        ps.setInt(1, id);
                        ResultSet rs = ps.executeQuery();
                        while (rs.next())
                            System.out.printf("[Triage #%d | %s | BP:%s | Pulse:%d | SpO2:%.1f | GCS:%d]%n",
                                    rs.getInt("triage_id"), rs.getString("triage_level"),
                                    rs.getString("vitals_bp"), rs.getInt("vitals_pulse"),
                                    rs.getDouble("vitals_spo2"), rs.getInt("gcs_score"));
                    }
                }
                case 4 -> {
                    System.out.print("Triage ID: ");
                    int id = sc.nextInt();
                    sc.nextLine();
                    System.out.print("New Treatment Notes: ");
                    String notes = sc.nextLine();
                    // ADDED: Validation
                    if (notes.isBlank()) { System.out.println("[Validation Error] Notes must not be blank."); break; }
                    try (PreparedStatement ps = conn
                            .prepareStatement("UPDATE triage_records SET treatment_notes=? WHERE triage_id=?")) {
                        ps.setString(1, notes);
                        ps.setInt(2, id);
                        System.out.println(ps.executeUpdate() > 0 ? "Updated." : "Not found.");
                    }
                }
                case 5 -> {
                    System.out.print("Triage ID: ");
                    int id = sc.nextInt();
                    System.out.print("Confirm delete? (y/n): ");
                    if (sc.next().equalsIgnoreCase("y")) {
                        try (PreparedStatement ps = conn
                                .prepareStatement("DELETE FROM triage_records WHERE triage_id=?")) {
                            ps.setInt(1, id);
                            System.out.println(ps.executeUpdate() > 0 ? "Deleted." : "Not found.");
                        }
                    }
                }
                case 0 -> { return; }
                default -> System.out.println("Invalid choice.");
            }
        }
    }

    // ── REPORTS MENU ───────────────────────────────────────
    static void reportsMenu() throws SQLException {
        while (true) {
            System.out.println("""
                    \n--- REPORTS ---
                    1. Response Time Report
                    2. Active Incidents Summary
                    0. Back""");
            System.out.print("Choice: ");
            switch (sc.nextInt()) {
                case 1 -> dispatchDAO.printResponseReport();
                case 2 -> incidentDAO.getActiveIncidents().forEach(System.out::println);
                case 0 -> { return; }
                default -> System.out.println("Invalid choice.");
            }
        }
    }

    // ── VIEW TABLES MENU ───────────────────────────────────
    static void viewTablesMenu() throws SQLException {
        Connection conn = DBConnection.getConnection();
        String[] tables = {
            "incidents", "ambulances", "responders",
            "dispatch_log", "patients", "equipment",
            "triage_records", "hospitals"
        };
        while (true) {
            System.out.println("\n--- VIEW TABLES ---");
            for (int i = 0; i < tables.length; i++)
                System.out.printf("%d. %s%n", i + 1, tables[i]);
            System.out.println("0. Back");
            System.out.print("Choice: ");
            int choice = sc.nextInt();
            if (choice == 0) return;
            if (choice < 1 || choice > tables.length) {
                System.out.println("Invalid choice.");
                continue;
            }
            printTable(conn, tables[choice - 1]);
        }
    }

    static void printTable(Connection conn, String table) throws SQLException {
        try (Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery("SELECT * FROM " + table)) {

            ResultSetMetaData meta = rs.getMetaData();
            int cols = meta.getColumnCount();

            String[] headers = new String[cols];
            for (int c = 1; c <= cols; c++)
                headers[c - 1] = meta.getColumnName(c);

            List<String[]> rows = new ArrayList<>();
            while (rs.next()) {
                String[] row = new String[cols];
                for (int c = 1; c <= cols; c++) {
                    String val = rs.getString(c);
                    row[c - 1] = (val == null) ? "NULL" : val;
                }
                rows.add(row);
            }

            int[] widths = new int[cols];
            for (int c = 0; c < cols; c++)
                widths[c] = headers[c].length();
            for (String[] row : rows)
                for (int c = 0; c < cols; c++)
                    widths[c] = Math.max(widths[c], row[c].length());

            StringBuilder fmt = new StringBuilder();
            int totalWidth = 0;
            for (int w : widths) { fmt.append("%-").append(w).append("s  "); totalWidth += w + 2; }
            String fmtStr = fmt.toString();

            System.out.println("\n=== " + table.toUpperCase() + " ===");
            System.out.printf((fmtStr) + "%n", (Object[]) headers);
            System.out.println("-".repeat(totalWidth));
            if (rows.isEmpty()) {
                System.out.println("(no rows)");
            } else {
                for (String[] row : rows)
                    System.out.printf((fmtStr) + "%n", (Object[]) row);
            }
            System.out.println("-".repeat(totalWidth));
            System.out.printf("%d row(s)%n", rows.size());
        }
    }
}