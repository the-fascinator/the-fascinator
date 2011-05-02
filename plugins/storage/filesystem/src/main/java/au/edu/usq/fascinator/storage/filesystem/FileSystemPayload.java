/* 
 * The Fascinator - File System storage plugin
 * Copyright (C) 2009 University of Southern Queensland
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
package au.edu.usq.fascinator.storage.filesystem;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Reader;
import java.util.Properties;

import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import au.edu.usq.fascinator.api.storage.PayloadType;
import au.edu.usq.fascinator.api.storage.StorageException;
import au.edu.usq.fascinator.common.MimeTypeUtil;
import au.edu.usq.fascinator.common.storage.impl.GenericPayload;

public class FileSystemPayload extends GenericPayload {

    /** Logger */
    private Logger log = LoggerFactory.getLogger(FileSystemPayload.class);

    /** Default type to use if not specified */
    private static PayloadType DEFAULT_PAYLOAD_TYPE = PayloadType.Enrichment;

    /** File extension to use storing metadata */
    private static String METADATA_SUFFIX = ".meta";

    /** The file on disk holding real data */
    private File dataFile;

    /** The file on disk holding metadata */
    private File metaFile;

    public FileSystemPayload(String id, File payloadFile) {
        super(id);
        dataFile = payloadFile;
        metaFile = new File(dataFile.getParentFile(), dataFile.getName()
                + METADATA_SUFFIX);
        //log.debug("Payload instantiation complete : " + id);
    }

    /**
     * Read metadata from disk into memory if it exists
     *
     */
    public void readExistingMetadata() {
        if (metaFile.exists()) {
            Properties props = new Properties();
            Reader metaReader;
            try {
                metaReader = new FileReader(metaFile);
                props.load(metaReader);
                metaReader.close();
            } catch (FileNotFoundException ex) {
                log.error("Failed reading metadata file", ex);
            } catch (IOException ex) {
                log.error("Failed accessing metadata file", ex);
            }

            setId(props.getProperty("id", getId()));
            String type = props.getProperty("payloadType", DEFAULT_PAYLOAD_TYPE
                    .toString());
            setType(PayloadType.valueOf(type));
            setLabel(props.getProperty("label", getId()));

            String link = props.getProperty("linked", String
                    .valueOf(isLinked()));
            setLinked(Boolean.parseBoolean(link));
            if (isLinked()) {
                try {
                    long fileSize = dataFile.length();
                    // Arbitrary test for length of link. We have observed
                    //  instances of real data being treated as a link.
                    if (fileSize > 2000) {
                        log.debug("Linked file '{}' is unusually large. It is" +
                                " most likely not really linked and is corrupt",
                                dataFile.getAbsolutePath());
                    } else {
                        String linkPath = FileUtils.readFileToString(dataFile);
                        File linkFile = new File(linkPath);
                        if (linkFile.exists()) {
                            setContentType(MimeTypeUtil.getMimeType(linkFile));
                        } else {
                            log.debug("Linked file '{}' no longer exists!",
                                    linkFile.getAbsolutePath());
                        }
                    }
                } catch (IOException ioe) {
                    log.warn("Failed to get linked file", ioe);
                }
            } else {
                setContentType(MimeTypeUtil.getMimeType(dataFile));
            }
        } else {
            writeMetadata();
        }
    }

    /**
     * Write metadata to disk, calculating/retrieving if it is not known yet
     *
     */
    public void writeMetadata() {
        if (getLabel() == null) {
            setLabel(dataFile.getName());
        }
        if (getType() == null) {
            setType(PayloadType.Source);
        }
        if (getContentType() == null) {
            setContentType(MimeTypeUtil.getMimeType(dataFile));
        }

        try {
            if (!metaFile.exists()) {
                metaFile.getParentFile().mkdirs();
                metaFile.createNewFile();
            }
            Properties props = new Properties();
            OutputStream metaOut = new FileOutputStream(metaFile);

            props.setProperty("id", getId());
            props.setProperty("payloadType", getType().toString());
            props.setProperty("label", getLabel());
            props.setProperty("linked", String.valueOf(isLinked()));
            // Sometimes we just can't get it
            if (getContentType() != null) {
                props.setProperty("contentType", getContentType());
            }
            props.store(metaOut, "Payload metadata for "
                    + dataFile.getAbsolutePath());
            metaOut.close();
        } catch (IOException ioe) {
            log.warn("Failed to read/write metaFile", ioe);
        }
    }

    /**
     * Gets the input stream to access the content for this payload
     *
     * @return an input stream
     * @throws IOException if there was an error reading the stream
     */
    @Override
    public InputStream open() throws StorageException {
        // Linked files
        if (isLinked()) {
            try {
                String linkPath = FileUtils.readFileToString(dataFile);
                File linkFile = new File(linkPath);
                if (!linkFile.exists()) {
                    throw new StorageException("External file not found : "
                            + linkFile.getAbsolutePath());
                }
                InputStream in = new FileInputStream(linkFile);
                return in;
            } catch (IOException ex) {
                throw new StorageException(ex);
            }

            // Stored files
        } else {
            try {
                InputStream in = new FileInputStream(dataFile);
                return in;
            } catch (FileNotFoundException ex) {
                throw new StorageException(ex);
            }
        }
    }

    /**
     * Close the input stream for this payload
     *
     * @throws StorageException if there was an error closing the stream
     */
    @Override
    public void close() throws StorageException {
        super.close();
        if (hasMetaChanged()) {
            writeMetadata();
        }
    }

    /**
     * Return the timestamp when the payload was last modified
     *
     * @returns Long: The last modified date of the payload, or NULL if unknown
     */
    @Override
    public Long lastModified() {
        // Linked files
        if (isLinked()) {
            try {
                String linkPath = FileUtils.readFileToString(dataFile);
                File linkFile = new File(linkPath);
                if (!linkFile.exists()) {
                    log.error("Error! File does not exist: '{}'",
                            linkFile.getAbsolutePath());
                    return null;
                }
                return linkFile.lastModified();
            } catch (IOException ex) {
                log.error("Error reading file from disk: ", ex);
                return null;
            }

        // Stored files
        } else {
            return dataFile.lastModified();
        }
    }

    /**
     * Return the size of the payload in byte
     *
     * @returns Integer: The file size in bytes, or NULL if unknown
     */
    @Override
    public Long size() {
        // Linked files
        if (isLinked()) {
            try {
                String linkPath = FileUtils.readFileToString(dataFile);
                File linkFile = new File(linkPath);
                if (!linkFile.exists()) {
                    log.error("Error! File does not exist: '{}'",
                            linkFile.getAbsolutePath());
                    return null;
                }
                return linkFile.length();
            } catch (IOException ex) {
                log.error("Error reading file from disk: ", ex);
                return null;
            }

        // Stored files
        } else {
            return dataFile.length();
        }
    }
}
