package de.oberdorf_itc.acs.model;

/**
 * Represents an RFID access control event published to:
 *   [..]/oitc-acs/entrypoints/<reader-ip>/access
 *
 * JSON schema source: mqtt-message-reference.md  "Access message format when opening a door"
 *
 * Required fields:
 *   timestamp, entrypoint_ip, entrypoint_location, transponder_uid,
 *   user_id, user_dn, user_display_name, status, notification
 */
public class AccessEvent {

    /** ISO 8601 timestamp of when the access event occurred. */
    private String timestamp;

    /** IPv4 address of the RFID reader. */
    private String entrypoint_ip;

    /** Human-readable name of the entry point location. */
    private String entrypoint_location;

    /**
     * RFID transponder UID as a hexadecimal string (8–20 hex characters).
     * Pattern: ^[A-Fa-f0-9]{8,20}$
     */
    private String transponder_uid;

    /** Unique identifier of the user. */
    private String user_id;

    /**
     * LDAP distinguished name of the user object.
     * Must start with UID= or uid=
     */
    private String user_dn;

    /** Human-readable display name of the user. */
    private String user_display_name;

    /** "granted" or "denied" */
    private String status;

    /** Whether a notification should be sent for this event. */
    private boolean notification;

    // ---- Constructor ----

    public AccessEvent(String timestamp, String entrypointIp, String entrypointLocation,
                       String transponderUid, String userId, String userDn,
                       String userDisplayName, String status, boolean notification) {
        this.timestamp           = timestamp;
        this.entrypoint_ip       = entrypointIp;
        this.entrypoint_location = entrypointLocation;
        this.transponder_uid     = transponderUid;
        this.user_id             = userId;
        this.user_dn             = userDn;
        this.user_display_name   = userDisplayName;
        this.status              = status;
        this.notification        = notification;
    }

    // ---- Getters / Setters ----

    public String getTimestamp()           { return timestamp; }
    public void   setTimestamp(String v)   { this.timestamp = v; }

    public String getEntrypoint_ip()               { return entrypoint_ip; }
    public void   setEntrypoint_ip(String v)       { this.entrypoint_ip = v; }

    public String getEntrypoint_location()             { return entrypoint_location; }
    public void   setEntrypoint_location(String v)     { this.entrypoint_location = v; }

    public String getTransponder_uid()             { return transponder_uid; }
    public void   setTransponder_uid(String v)     { this.transponder_uid = v; }

    public String getUser_id()             { return user_id; }
    public void   setUser_id(String v)     { this.user_id = v; }

    public String getUser_dn()             { return user_dn; }
    public void   setUser_dn(String v)     { this.user_dn = v; }

    public String getUser_display_name()           { return user_display_name; }
    public void   setUser_display_name(String v)   { this.user_display_name = v; }

    public String getStatus()              { return status; }
    public void   setStatus(String v)      { this.status = v; }

    public boolean isNotification()        { return notification; }
    public void    setNotification(boolean v) { this.notification = v; }
}
