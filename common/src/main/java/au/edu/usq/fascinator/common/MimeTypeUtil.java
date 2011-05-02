/* 
 * The Fascinator - Common Library
 * Copyright (C) 2008-2009 University of Southern Queensland
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
package au.edu.usq.fascinator.common;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

import org.apache.commons.lang.StringUtils;
import org.semanticdesktop.aperture.mime.identifier.MimeTypeIdentifier;
import org.semanticdesktop.aperture.util.IOUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Utility class to determine MIME type
 * 
 * @author Oliver Lucido
 */
public class MimeTypeUtil {

    /** Default Mime Type */
    public static final String DEFAULT_MIME_TYPE = "application/octet-stream";

    /** Logging */
    private static Logger log = LoggerFactory.getLogger(MimeTypeUtil.class);

    /** Identifier */
    private static MimeTypeIdentifier identifier = new MagicMimeTypeIdentifierWrapper();

    /**
     * Gets the MIME type for the specified file name
     * 
     * @param filename a file name
     * @return MIME type
     */
    public static String getMimeType(String filename) {
        File file = new File(filename);
        if (file.exists()) {
            return getMimeType(file);
        }
        String mimeType = identifier.identify(null, filename, null);
        if (mimeType == null) {
            mimeType = DEFAULT_MIME_TYPE;
        }
        return mimeType;
    }

    /**
     * Gets the MIME type for the specified file
     * 
     * @param file a file
     * @return MIME type
     */
    public static String getMimeType(File file) {
        InputStream in = null;
        try {
            in = new FileInputStream(file);
            byte[] inBytes = IOUtil.readBytes(in, identifier
                    .getMinArrayLength());
            in.close();
            return identifier.identify(inBytes, file.getName(), null);
        } catch (IOException ioe) {
            log.warn("Failed to detect MIME type (File): {}", toPrintable(ioe));
        } finally {
            if (in != null) {
                try {
                    in.close();
                } catch (IOException ioe) {
                }
            }
        }
        return DEFAULT_MIME_TYPE;
    }

    /**
     * Gets the MIME type for the specified input stream
     * 
     * @param in an input stream
     * @return MIME type
     */
    public static String getMimeType(InputStream in) {
        try {
            byte[] inBytes = IOUtil.readBytes(in, identifier
                    .getMinArrayLength());
            in.close();
            return identifier.identify(inBytes, null, null);
        } catch (IOException ioe) {
            log.warn("Failed to detect MIME type (InputStream): {}",
                    toPrintable(ioe));
        }
        return DEFAULT_MIME_TYPE;
    }

    /**
     * Gets the MIME type for the specified byte array and filename
     * 
     * @param bytes a byte array
     * @param filename a file name
     * @return MIME type
     */
    public static String getMimeType(byte[] bytes, String filename) {
        return identifier.identify(bytes, filename, null);
    }

    /**
     * Print Exception in readable format
     * 
     * @param e Exception
     * @return Readable version of Exception
     */
    private static String toPrintable(Exception e) {
        String msg = e.getMessage();
        if (StringUtils.isAsciiPrintable(msg)) {
            return msg;
        }
        return StringUtils.left(msg.replaceAll("[^\\p{ASCII}\\n]", "."), 32);
    }
}
