/* 
 * The Fascinator - StorageDataUtil
 * Copyright (C) 2013 Queensland Cyber Infrastructure Foundation (http://www.qcif.edu.au/)
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
 */

package com.googlecode.fascinator.common;

import java.text.DateFormat;
import java.text.DecimalFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.SortedMap;
import java.util.TimeZone;
import java.util.TreeMap;

import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.ObjectUtils;
import org.apache.commons.lang.StringEscapeUtils;
import org.apache.commons.lang.StringUtils;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.json.simple.JSONValue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.googlecode.fascinator.api.storage.DigitalObject;
import com.googlecode.fascinator.api.storage.Payload;
import com.googlecode.fascinator.api.storage.Storage;
import com.googlecode.fascinator.api.storage.StorageException;
import com.googlecode.fascinator.common.storage.StorageUtils;

import javax.xml.bind.DatatypeConverter;

/**
 * Utility class for stored data in JSON format
 *
 * @author Linda Octalina
 * @author Andrew Brazzatti
 * @author Jianfeng Li
 */
public class StorageDataUtil {

    /**
     * Logger
     */
    static Logger log = LoggerFactory.getLogger(StorageDataUtil.class);

    /**
     * Get a playload in JsonSimple format in the specified Storage instance by
     * its object ID and the name of payload. Useful for loading payloads which
     * are in JSON format
     *
     * @param storage     : Storage object
     * @param oid         : object ID
     * @param payloadName : name of palyload
     * @return JsonSimple or null
     */
    public JsonSimple getPayloadJsonSimple(Storage storage, String oid,
                                           String payloadName) {
        if (storage == null) {
            return null;
        }
        try {
            Payload payload = StorageUtils
                    .getPayload(storage, oid, payloadName);
            return getPayloadJsonSimple(payload);
        } catch (Exception e) {
            log.error("Failed to retrive payload. Name = {}, more: {}",
                    payloadName, e.getMessage());
            return null;
        }
    }

    /**
     * Get a playload in JsonSimple format from the payload instance
     *
     * @param payload : Payload object
     * @return JsonSimple or null
     */
    public JsonSimple getPayloadJsonSimple(Payload payload) {
        JsonSimple jsonSimple = null;
        if (payload == null) {
            return null;
        }
        try {
            jsonSimple = new JsonSimple(payload.open());
        } catch (Exception e) {
            log.error("Failed to retrive payload. ID of payload: {}, more: {}",
                    payload.getId(), e.getMessage());
        }
        return jsonSimple;
    }

    /**
     * Getlist method to get the values of key from the sourceMap
     *
     * @param sourceMap Map container
     * @param baseKey   field to searchclass
     * @return list of value based on baseKey
     */
    public Map<String, Object> getList(Map<String, Object> sourceMap,
                                       String baseKey) {
        SortedMap<String, Object> valueMap = new TreeMap<String, Object>();
        Map<String, Object> data;

        if (baseKey == null) {
            log.error("NULL baseKey provided!");
            return valueMap;
        }
        if (!baseKey.endsWith(".")) {
            baseKey = baseKey + ".";
        }
        if (sourceMap == null) {
            log.error("NULL sourceMap provided!");
            return valueMap;
        }

        for (String key : sourceMap.keySet()) {
            if (key.startsWith(baseKey)) {

                String value = sourceMap.get(key).toString();
                String field = baseKey;
                if (key.length() >= baseKey.length()) {
                    field = key.substring(baseKey.length(), key.length());
                }

                String index = field;
                if (field.indexOf(".") > 0) {
                    index = field.substring(0, field.indexOf("."));
                }

                if (valueMap.containsKey(index)) {
                    data = (Map<String, Object>) valueMap.get(index);
                } else {
                    data = new LinkedHashMap<String, Object>();
                    valueMap.put(index, data);
                }

                if (value.length() == 1) {
                    value = String.valueOf(value.charAt(0));
                }

                data.put(
                        field.substring(field.indexOf(".") + 1, field.length()),
                        value);

            }
        }

        return valueMap;
    }

    /**
     * Getlist method to get the values of key from the sourceMap
     *
     * @param sourceMap Map container
     * @param baseKey   field to search
     * @return list of value based on baseKey
     */
    public Map<String, Object> getList(JsonSimple json, String baseKey) {
        SortedMap<String, Object> valueMap = new TreeMap<String, Object>();
        Map<String, Object> data;

        if (baseKey == null) {
            log.error("NULL baseKey provided!");
            return valueMap;
        }
        if (!baseKey.endsWith(".")) {
            baseKey = baseKey + ".";
        }

        if (json == null) {
            log.error("NULL JSON object provided!");
            return valueMap;
        }

        // Look through the top level nodes
        for (Object oKey : json.getJsonObject().keySet()) {
            // If the key matches
            String key = (String) oKey;
            if (key.startsWith(baseKey)) {
                // Find our data
                String value = json.getString(null, key);
                String field = baseKey;

                if (key.length() >= baseKey.length()) {
                    field = key.substring(baseKey.length(), key.length());
                }

                String index = field;
                if (field.indexOf(".") > 0) {
                    index = field.substring(0, field.indexOf("."));
                }

                if (valueMap.containsKey(index)) {
                    data = (Map<String, Object>) valueMap.get(index);
                } else {
                    data = new LinkedHashMap<String, Object>();
                    valueMap.put(index, data);
                }

                if (value.length() == 1) {
                    value = String.valueOf(value.charAt(0));
                }

                data.put(
                        field.substring(field.indexOf(".") + 1, field.length()),
                        value);

            }
        }

        // log.info("{}: {}", baseKey, valueMap);
        return valueMap;
    }

    /**
     * getJavaList method to reconstruct an list of JSONObjects of a key from a
     * JsonSimple object
     *
     * @param json:    JsonSimple object of source
     * @param baseKey: field to search
     * @return List: a list of JsonObject based on baseKey
     */
    public List<JsonObject> getJavaList(JsonSimple json, String baseKey) {
        List<JsonObject> valueList = new ArrayList<JsonObject>();

        if (baseKey == null) {
            log.error("NULL baseKey provided!");
            return valueList;
        }
        if (!baseKey.endsWith(".")) {
            baseKey = baseKey + ".";
        }

        if (json == null) {
            log.error("NULL JSON object provided!");
            return valueList;
        }

        for (Object oKey : json.getJsonObject().keySet()) {
            String key = (String) oKey;
            if (key.startsWith(baseKey)) {
                String value = json.getString(null, key);
                String field = baseKey;

                if (key.length() >= baseKey.length()) { // It is an pseudo-JSON
                    // array
                    field = key.substring(baseKey.length(), key.length());
                }

                String indexString = field;
                String suffix = "value"; // Default JSON key in returned
                // JsonObject if it is a simple string
                if (field.indexOf(".") > 0) { // This is not a simple string,
                    // get the key of it
                    indexString = field.substring(0, field.indexOf("."));
                    suffix = field.substring(field.indexOf(".") + 1,
                            field.length());
                }
                int index = Integer.parseInt(indexString) - 1;

                if (valueList.size() <= index) {
                    while (valueList.size() < index + 1) {
                        valueList.add(new JsonObject());
                    }
                }

                if (value != null && value.length() == 1) {
                    value = String.valueOf(value.charAt(0));
                }

                valueList.get(index).put(suffix, value);
            }
        }
        return valueList;
    }

    /**
     * getStringList method to reconstruct an list of String of a key from a
     * JsonSimple object
     *
     * @param json:    JsonSimple object of source
     * @param baseKey: field to search
     * @return List: a List of JsonObject based on baseKey
     */
    public List<String> getStringList(JsonSimple json, String baseKey) {
        List<JsonObject> jsonList = getJavaList(json, baseKey);
        List<String> valueList = new ArrayList<String>(jsonList.size());
        if (jsonList.size() == 0) {
            return valueList;
        }
        for (int i = 0; i < jsonList.size(); i++) {
            String t = (String) jsonList.get(i).get("value");
            if (t.equals("null")) {
                valueList.add("");
            } else {
                valueList.add(t);
            }
        }
        return valueList;
    }

    /**
     * Trivial wrapper for call into JSON Library. Removes the difficulty of
     * dealing with a null argument and a vararg from Velocity.
     *
     * @param json:  The JSON object to get from
     * @param field: The field in the JSON object to get
     * @return String: The data in the field, possibly NULL
     */
    public String get(JsonSimple json, Object... field) {
        if (json == null) {
            log.error("NULL JSON object provided!");
            return "";
        }

        return json.getString(null, field);
    }

    /**
     * Similar to get Method but return empty string instead of null
     *
     * @param json:  The JSON object to get from
     * @param field: The field in the JSON object to get
     * @return String: The data in the field, possibly NULL
     */
    public String getEmptyIfNull(JsonSimple json, Object... field) {
        return getDefaultValueIfNull(json, "", field);
    }

    /**
     * Similar to get Method but return a string supplied by caller if cannot
     * get the field
     *
     * @param json:         The JSON object to get from
     * @param defaultValue: The default value of the field
     * @param field:        The field in the JSON object to get
     * @return String: The data in the field, possibly NULL
     */
    public String getDefaultValueIfNull(JsonSimple json, String defaultValue,
                                        Object... field) {
        String value = get(json, field);
        return value == null ? defaultValue : value;
    }

    /**
     * Cleanup the supplied datetime value into a W3C format.
     *
     * @param dateTimeText Datetime text to clean
     * @return String The cleaned value
     */
    public String getW3CDateTime(String dateTimeInput) {
        String dateTimeOutput = StringUtils.EMPTY;
        if (StringUtils.isNotBlank(dateTimeInput)) {
            dateTimeOutput = new DateTime(dateTimeInput).toString();
            log.debug("date text was: " + dateTimeInput);
            log.debug("w3c(ISO8601) date time text is: " + dateTimeOutput);
        }
        return dateTimeOutput;
    }

    /**
     * Cleanup the supplied datetime value into a W3C format.
     *
     * @param dateTimeInput Datetime to clean
     * @return String The cleaned value
     */
    public String getDateTime(String dateTimeInput, String outputFormat) {
        String formattedDateTimeOutput = StringUtils.EMPTY;
        if (StringUtils.isNotBlank(dateTimeInput)) {
            DateTime dateTime = new DateTime(dateTimeInput);
            if (StringUtils.isNotBlank(outputFormat)) {
                formattedDateTimeOutput = dateTime.toString(outputFormat);
            } else {
                formattedDateTimeOutput = dateTime.toString();
            }
            log.debug("date text was: " + dateTimeInput);
            log.debug("date time output format is: " + outputFormat);
            log.debug("Returning date time: " + formattedDateTimeOutput);
        }
        return formattedDateTimeOutput;
    }

    /**
     * Utility method for accessing object properties. Since null testing is
     * awkward in velocity, an unset property is changed to en empty string ie.
     * ("").
     *
     * @param object: The object to extract the property from
     * @param field:  The field name of the property
     * @return String: The value of the property, or and empty string.
     */
    public String getMetadata(DigitalObject object, String field) {
        if (object == null) {
            log.error("NULL object provided!");
            return "";
        }

        try {
            Properties metadata = object.getMetadata();
            String result = metadata.getProperty(field);
            if (result == null) {
                return "";
            } else {
                return result;
            }
        } catch (StorageException ex) {
            log.error("Error accessing object metadata: ", ex);
            return "";
        }
    }

    /**
     * Safely escape the supplied string for use in XML.
     *
     * @param value: The string to escape
     * @return String: The escaped string
     */
    public String encodeXml(String value) {
        return StringEscapeUtils.escapeXml(value);
    }

    /**
     * Safely escape the supplied string for use in JSON.
     *
     * @param value: The string to escape
     * @return String: The escaped string
     */
    public String encodeJson(String value) {
        return JSONValue.escape(value);
    }

}
