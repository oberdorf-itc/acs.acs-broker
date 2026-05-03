package de.oberdorf_itc.acs.model;

/**
 * Represents a status / KeepAlive event published to:
 *   [..]/oitc-acs/entrypoints/<reader-ip>/status
 *
 * JSON schema source: mqtt-message-reference.md  "ACS Server status"
 *
 * Required fields: timestamp, severity, status, description
 */
public class StatusEvent {

    /** ISO 8601 timestamp of when the event occurred. */
    private String timestamp;

    /** Severity level: "info", "warning", or "error" */
    private String severity;

    /** Short description of the status (max 100 chars). */
    private String status;

    /** Detailed status message. */
    private String description;

    // ---- Constructor ----

    public StatusEvent(String timestamp, String severity, String status, String description) {
        this.timestamp   = timestamp;
        this.severity    = severity;
        this.status      = status;
        this.description = description;
    }

    // ---- Getters / Setters ----

    public String getTimestamp()           { return timestamp; }
    public void   setTimestamp(String v)   { this.timestamp = v; }

    public String getSeverity()            { return severity; }
    public void   setSeverity(String v)    { this.severity = v; }

    public String getStatus()              { return status; }
    public void   setStatus(String v)      { this.status = v; }

    public String getDescription()         { return description; }
    public void   setDescription(String v) { this.description = v; }
}
