/*
 * $Id: JSONObject.java,v 1.1 2006/04/15 14:10:48 platform Exp $
 * Created on 2006-4-10
 */
package com.googlecode.fascinator.common;

import java.io.IOException;
import java.io.Writer;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;

import org.json.simple.JSONAware;
import org.json.simple.JSONStreamAware;
import org.json.simple.JSONValue;

/**
 * <p>
 * This class is and all code is a direct copy of the org.json.simple.JSONObject
 * implementation found here:
 * http://json-simple.googlecode.com/svn/trunk/src/org
 * /json/simple/JSONObject.java
 * </p>
 * 
 * <p>
 * It has been duplicated for the sole purpose of moving to a LinkedHashMap to
 * preserve order. All credit must go to the original authors.
 * </p>
 * 
 * <p>
 * Because JSONValue.escape() is inaccessible from outside the original package
 * it needed to be added to the end of the class as well.
 * </p>
 * 
 * A JSON object. Key value pairs are unordered. JSONObject supports
 * java.util.Map interface.
 * 
 * @author FangYidong<fangyidong@yahoo.com.cn>
 * 
 */
public class JsonObject extends LinkedHashMap<Object, Object> implements
        Map<Object, Object>, JSONAware, JSONStreamAware {
    /** Serializable - required */
    private static final long serialVersionUID = 1L;

    public JsonObject() {
        super();
    }

    public JsonObject(Map<?, ?> map) {
        super(map);
    }

    public static void writeJSONString(Map<?, ?> map, Writer out)
            throws IOException {
        if (map == null) {
            out.write("null");
            return;
        }

        boolean first = true;
        Iterator<?> iter = map.entrySet().iterator();

        out.write('{');
        while (iter.hasNext()) {
            if (first) {
                first = false;
            } else {
                out.write(',');
            }
            Map.Entry<?, ?> entry = (Map.Entry<?, ?>) iter.next();
            out.write('\"');
            out.write(escape(String.valueOf(entry.getKey())));
            out.write('\"');
            out.write(':');
            JSONValue.writeJSONString(entry.getValue(), out);
        }
        out.write('}');
    }

    @Override
    public void writeJSONString(Writer out) throws IOException {
        writeJSONString(this, out);
    }

    public static String toJSONString(Map<?, ?> map) {
        if (map == null) {
            return "null";
        }

        StringBuffer sb = new StringBuffer();
        boolean first = true;
        Iterator<?> iter = map.entrySet().iterator();

        sb.append('{');
        while (iter.hasNext()) {
            if (first) {
                first = false;
            } else {
                sb.append(',');
            }

            Map.Entry<?, ?> entry = (Map.Entry<?, ?>) iter.next();
            toJSONString(String.valueOf(entry.getKey()), entry.getValue(), sb);
        }
        sb.append('}');
        return sb.toString();
    }

    @Override
    public String toJSONString() {
        return toJSONString(this);
    }

    private static String toJSONString(String key, Object value, StringBuffer sb) {
        sb.append('\"');
        if (key == null) {
            sb.append("null");
        } else {
            escape(key, sb);
        }
        sb.append('\"').append(':');

        sb.append(JSONValue.toJSONString(value));

        return sb.toString();
    }

    @Override
    public String toString() {
        return toJSONString();
    }

    public static String toString(String key, Object value) {
        StringBuffer sb = new StringBuffer();
        toJSONString(key, value, sb);
        return sb.toString();
    }

    /**
     * Escape quotes, \, /, \r, \n, \b, \f, \t and other control characters
     * (U+0000 through U+001F). It's the same as JSONValue.escape() only for
     * compatibility here.
     * 
     * @see org.json.simple.JSONValue#escape(String)
     * 
     * @param s
     * @return
     */
    public static String escape(String s) {
        return JSONValue.escape(s);
    }

    static void escape(String s, StringBuffer sb) {
        for (int i = 0; i < s.length(); i++) {
            char ch = s.charAt(i);
            switch (ch) {
            case '"':
                sb.append("\\\"");
                break;
            case '\\':
                sb.append("\\\\");
                break;
            case '\b':
                sb.append("\\b");
                break;
            case '\f':
                sb.append("\\f");
                break;
            case '\n':
                sb.append("\\n");
                break;
            case '\r':
                sb.append("\\r");
                break;
            case '\t':
                sb.append("\\t");
                break;
            case '/':
                sb.append("\\/");
                break;
            default:
                // Reference: http://www.unicode.org/versions/Unicode5.1.0/
                if ((ch >= '\u0000' && ch <= '\u001F')
                        || (ch >= '\u007F' && ch <= '\u009F')
                        || (ch >= '\u2000' && ch <= '\u20FF')) {
                    String ss = Integer.toHexString(ch);
                    sb.append("\\u");
                    for (int k = 0; k < 4 - ss.length(); k++) {
                        sb.append('0');
                    }
                    sb.append(ss.toUpperCase());
                } else {
                    sb.append(ch);
                }
            }
        }
    }
}
