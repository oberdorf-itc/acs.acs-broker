package de.oberdorf_itc.acs.mqtt;

import de.oberdorf_itc.acs.model.AccessEvent;
import de.oberdorf_itc.acs.model.StatusEvent;

public class JsonUtil {

    private JsonUtil() {}

    /**
     * Serializes an AccessEvent to JSON.
     *
     * Output matches "Access message format when opening a door" schema
     * from mqtt-message-reference.md.
     * @param e the AccessEvent to serialize
     * @return a JSON string representing the AccessEvent
     */
    public static String toJson(AccessEvent e) {
        return "{"
            + jsonStr("timestamp",            e.getTimestamp())           + ","
            + jsonStr("entrypoint_ip",        e.getEntrypoint_ip())       + ","
            + jsonStr("entrypoint_location",  e.getEntrypoint_location()) + ","
            + jsonStr("transponder_uid",      e.getTransponder_uid())     + ","
            + jsonStr("user_id",              e.getUser_id())             + ","
            + jsonStr("user_dn",              e.getUser_dn())             + ","
            + jsonStr("user_display_name",    e.getUser_display_name())   + ","
            + jsonStr("status",               e.getStatus())              + ","
            + jsonBool("notification",        e.isNotification())
            + "}";
    }

    /**
     * Serializes a StatusEvent to JSON.
     *
     * Output matches "ACS Server status" schema from mqtt-message-reference.md.
     * @param e the StatusEvent to serialize
     * @return a JSON string representing the StatusEvent
     */
    public static String toJson(StatusEvent e) {
        return "{"
            + jsonStr("timestamp",   e.getTimestamp())    + ","
            + jsonStr("severity",    e.getSeverity())     + ","
            + jsonStr("status",      e.getStatus())       + ","
            + jsonStr("description", e.getDescription())
            + "}";
    }

    /**
     * Formats a key-value pair as a JSON string, escaping special characters in the key and value.
     * @param key the JSON key
     * @param value the JSON value
     * @return a JSON string in the format "key":"value", with special characters escaped
     */
    private static String jsonStr(String key, String value) {
        return "\"" + escape(key) + "\":\"" + escape(value) + "\"";
    }

    /**
     * Formats a key-boolean pair as a JSON string, escaping special characters in the key.
     * @param key the JSON key
     * @param value the boolean value
     * @return a JSON string in the format "key":value, with special characters in the key escaped
     */
    private static String jsonBool(String key, boolean value) {
        return "\"" + escape(key) + "\":" + value;
    }

    /**
     * Escapes special characters in a string for safe inclusion in JSON.
     *
     * @param s the input string to escape
     * @return the escaped string, with special characters replaced by their JSON escape sequences
     */
    private static String escape(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }
}
