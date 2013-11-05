/* 
 * The Fascinator - Common Library
 * Copyright (C) 2008 University of Southern Queensland
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
package com.googlecode.fascinator.common.storage;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Properties;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.googlecode.fascinator.api.storage.DigitalObject;
import com.googlecode.fascinator.api.storage.Payload;
import com.googlecode.fascinator.api.storage.Storage;
import com.googlecode.fascinator.api.storage.StorageException;

/**
 * Storage API utility methods.
 * 
 * @author Oliver Lucido
 * @author Andrew Brazzatti
 * @author Jianfeng Li
 */

public class StorageUtils {

    /** Default host name */
    public static final String DEFAULT_HOSTNAME = "localhost";

    /** Logging */
    @SuppressWarnings("unused")
    private static final Logger log = LoggerFactory
            .getLogger(StorageUtils.class);

    /**
     * Generates a Object identifier for a given file
     * 
     * @param file the File to store
     * @return a String object id
     */
    public static String generateOid(File file) {
        // MD5 hash the file path,
        String path = FilenameUtils.separatorsToUnix(file.getAbsolutePath());
        String hostname = "localhost";
        try {
            hostname = InetAddress.getLocalHost().getCanonicalHostName();
        } catch (UnknownHostException uhe) {
        }
        String username = System.getProperty("user.name", "anonymous");
        // log.debug("Generating OID path:'{}' hostname:'{}' username:'{}'",
        // new String[] { path, hostname, username });
        return DigestUtils.md5Hex(path + hostname + username);
    }

    /**
     * Hash the internal contents of a file.
     * 
     * @param file The File to hash
     * @return String Hash of the file's contents
     * @throws IOException If there was an error accessing the file
     */
    private static String hashFile(File file) throws IOException {
        return DigestUtils.md5Hex(FileUtils.readFileToString(file));
    }

    /**
     * Generates a Payload identifier for a given file
     * 
     * @param file the File to store
     * @return a String payload id
     */
    public static String generatePid(File file) {
        return FilenameUtils.separatorsToUnix(file.getName());
    }

    /**
     * This method stores a copy of a File as a DigitalObject into the specified
     * Storage
     * 
     * @param storage a Storage instance
     * @param file the File to store
     * @return a DigitalObject
     * @throws StorageException if there was an error storing the file
     */
    public static DigitalObject storeFile(Storage storage, File file)
            throws StorageException {
        return storeFile(storage, file, false);
    }

    /**
     * This method stores a link to a File as a DigitalObject into the specified
     * Storage
     * 
     * @param storage a Storage instance
     * @param file the File to store
     * @return a DigitalObject
     * @throws StorageException if there was an error storing the file
     */
    public static DigitalObject linkFile(Storage storage, File file)
            throws StorageException {
        return storeFile(storage, file, true);
    }

    /**
     * This method stores a File as a DigitalObject into the specified Storage.
     * The File can be stored as a linked Payload if specified.
     * 
     * @param storage a Storage instance
     * @param file the File to store
     * @param linked set true to link to the original file, false to copy
     * @return a DigitalObject
     * @throws StorageException if there was an error storing the file
     */
    public static DigitalObject storeFile(Storage storage, File file,
            boolean linked) throws StorageException {
        DigitalObject object = null;
        Payload payload = null;
        String oid = generateOid(file);
        String pid = generatePid(file);
        try {
            try {
                object = getDigitalObject(storage, oid);
                if (linked) {
                    try {
                        String path = FilenameUtils.separatorsToUnix(file
                                .getAbsolutePath());
                        payload = createLinkedPayload(object, pid, path);
                    } catch (StorageException se) {
                        payload = object.getPayload(pid);
                    }
                } else {
                    payload = createOrUpdatePayload(object, pid,
                            new FileInputStream(file));
                }
            } catch (StorageException se) {
                throw se;
            }
        } catch (FileNotFoundException fnfe) {
            throw new StorageException("File not found '" + oid + "'");
        } finally {
            if (payload != null) {
                payload.close();
            }
        }
        return object;
    }

    /**
     * Gets a DigitalObject from the specified Storage instance. If the object
     * does not exist, this method will attempt to create it.
     * 
     * @param storage a Storage instance
     * @param oid the object identifier to get (or create)
     * @return a DigitalObject
     * @throws StorageException if the object could not be retrieved or created
     */
    public static DigitalObject getDigitalObject(Storage storage, String oid)
            throws StorageException {
        DigitalObject object = null;
        try {
            // try to create a new object
            object = storage.createObject(oid);
        } catch (StorageException ex) {
            // object exists, try and get it
            try {
                object = storage.getObject(oid);
            } catch (StorageException ex1) {
                // could not be created and not found
                throw new StorageException(ex1);
            }
        }
        return object;
    }

    /**
     * Get a payload object in the specified Storage instance by its object ID
     * and the name of payload
     * 
     * @param storage : Storage object
     * @param oid : ID of a Digital object
     * @param payloadName : name of payload
     * @return a Payload object
     * @throws StorageException
     */
    public static Payload getPayload(Storage storage, String oid,
            String payloadName) throws StorageException {
        DigitalObject digitalObject = getDigitalObject(storage, oid);

        Payload payload = digitalObject.getPayload(payloadName);
        return payload;
    }

    /**
     * Ensure the provided harvest file is up-to-date in storage.
     * 
     * @param storage a Storage instance
     * @param file to check in storage
     * @return a DigitalObject
     * @throws StorageException if the object could not be retrieved or created
     */
    public static DigitalObject checkHarvestFile(Storage storage, File file)
            throws StorageException {
        String oid = generateOid(file);
        String lastMod = String.valueOf(file.lastModified());
        DigitalObject object;
        Properties metadata;

        try {
            // Get the object from storage
            object = storage.getObject(oid);
            try {
                // Check when it was last saved
                metadata = object.getMetadata();
                String oldMod = metadata.getProperty("lastModified");

                // Quick test - has it been changed?
                if (oldMod == null || !oldMod.equals(lastMod)) {
                    // Hash the file contents
                    String oldHash = metadata.getProperty("fileHash");
                    String fileHash = hashFile(file);
                    // Thorough test - have the contents changed?
                    if (oldHash == null || !oldHash.equals(fileHash)) {
                        // Update the file
                        FileInputStream in = new FileInputStream(file);
                        object.updatePayload(object.getSourceId(), in);
                        // Update the metadata
                        metadata.setProperty("lastModified", lastMod);
                        metadata.setProperty("fileHash", fileHash);
                        // Close and return
                        object.close();
                        return object;
                    }
                }
            } catch (FileNotFoundException ex1) {
                throw new StorageException("Harvest file not found: ", ex1);
            } catch (IOException ex1) {
                throw new StorageException("Error reading harvest file: ", ex1);
            } catch (StorageException ex1) {
                throw new StorageException("Error storing harvest file: ", ex1);
            }

        } catch (StorageException ex) {
            // It wasn't found in storage
            try {
                // Store it
                object = storeFile(storage, file);
                // Update its metadata
                metadata = object.getMetadata();
                metadata.setProperty("lastModified", lastMod);
                metadata.setProperty("fileHash", hashFile(file));
                // Close and return
                object.close();
                return object;
            } catch (IOException ex1) {
                throw new StorageException("Error reading harvest file: ", ex1);
            } catch (StorageException ex1) {
                throw new StorageException("Error storing harvest file: ", ex1);
            }
        }
        return null;
    }

    /**
     * Create or update a stored Payload in the specified DigitalObject
     * 
     * @param object the DigitalObject to create the Payload in
     * @param pid the Payload ID
     * @param in the InputStream for the Payload's data
     * @return a Payload
     * @throws StorageException if the Payload could not be created
     */
    public static Payload createOrUpdatePayload(DigitalObject object,
            String pid, InputStream in) throws StorageException {
        return createOrUpdatePayload(object, pid, in, null);
    }

    public static Payload createOrUpdatePayload(DigitalObject object,
            String pid, InputStream in, String filePath)
            throws StorageException {
        Payload payload = null;
        try {
            if (filePath == null) {
                payload = object.createStoredPayload(pid, in);
            } else {
                payload = object.createLinkedPayload(pid, filePath);
            }
        } catch (StorageException ex) {
            try {
                payload = object.updatePayload(pid, in);
            } catch (StorageException ex1) {
                throw ex1;
            }
        }
        return payload;
    }

    /**
     * Creates a linked Payload in the specified DigitalObject
     * 
     * @param object the DigitalObject to create the Payload in
     * @param pid the Payload ID
     * @param path the absolute path to the file the Payload links to
     * @return a Payload
     * @throws StorageException if the Payload could not be created
     */
    public static Payload createLinkedPayload(DigitalObject object, String pid,
            String path) throws StorageException {
        Payload payload = null;
        try {
            payload = object.createLinkedPayload(pid, path);
        } catch (StorageException ex) {
            throw ex;
        }
        return payload;
    }
}
