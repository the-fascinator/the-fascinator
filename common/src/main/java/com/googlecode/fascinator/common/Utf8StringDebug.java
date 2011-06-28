/*
 * The Fascinator - UTF-8 String Debugger
 * Copyright (C) 2008-2010 University of Southern Queensland
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

import java.io.UnsupportedEncodingException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A class purely for debugging UTF-8 Strings.
 * 
 * @author Greg Pendlebury
 */
public class Utf8StringDebug {
    private static Logger log = LoggerFactory.getLogger(Utf8StringDebug.class);

    /**
     * Simple entry point, Looks for only multi-byte characters
     * 
     * @param newString String to be debugged
     */
    public static void debugString(String newString) {
        debugString(newString, 1);
    }

    /**
     * Optional 'limit' can be set to 0 to show all characters
     * 
     * @param newString String to be debugged
     * @param limit number of characters to be printed
     */
    public static void debugString(String newString, int limit) {
        log.debug("Decoding String\n=============\n" + newString
                + "\n=============\n" + displayString(newString, limit));
    }

    /**
     * Used recursively to pull the string apart
     * 
     * @param newString String to be debugged
     * @param limit number of characters to be printed
     */
    public static String displayString(String newString, int limit) {
        // Test to end recursion
        if (newString.length() == 0) {
            return "";
        }
        String output = "";

        // Display the first character
        String firstChar = newString.substring(0, 1);
        try {
            byte[] utf8Bytes = firstChar.getBytes("UTF8");
            output += printBytes(utf8Bytes, firstChar, limit);
        } catch (UnsupportedEncodingException ex) {
            log.error("Error decoding character '" + firstChar + "' : ", ex);
        }

        // Now keep going with the rest of the string
        output += displayString(newString.substring(1), limit);
        return output;
    }

    /**
     * Print the bytes
     * 
     * @param array byte
     * @param name
     * @param limit number of characters to be printed
     * @return
     */
    public static String printBytes(byte[] array, String name, int limit) {
        String output = "";
        // Only display multi-byte characters
        if (array.length > limit) {
            output += "'" + name + "' =>";
            for (byte element : array) {
                output += " 0x" + byteToHex(element);
            }
            output += "\n";
        }
        return output;
    }

    /**
     * Convert byte to hex
     * 
     * @param b in byte
     * @return converted String
     */
    public static String byteToHex(byte b) {
        char hexDigit[] = { '0', '1', '2', '3', '4', '5', '6', '7', '8', '9',
                'A', 'B', 'C', 'D', 'E', 'F' };
        char[] array = { hexDigit[(b >> 4) & 0x0f], hexDigit[b & 0x0f] };
        return new String(array);
    }
}
