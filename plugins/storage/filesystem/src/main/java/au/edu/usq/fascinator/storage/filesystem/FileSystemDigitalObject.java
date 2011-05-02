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

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.util.Map;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import au.edu.usq.fascinator.api.storage.Payload;
import au.edu.usq.fascinator.api.storage.PayloadType;
import au.edu.usq.fascinator.api.storage.StorageException;
import au.edu.usq.fascinator.common.DummyFileLock;
import au.edu.usq.fascinator.common.storage.impl.GenericDigitalObject;

public class FileSystemDigitalObject extends GenericDigitalObject {

    private Logger log = LoggerFactory.getLogger(FileSystemDigitalObject.class);
    private static String METADATA_SUFFIX = ".meta";
    private static String MANIFEST_LOCK_FILE = "manifest.lock";
    private File homeDir;
    private File lockFile;
    private DummyFileLock manifestLock;

    public FileSystemDigitalObject(File homeDir, String oid) {
        super(oid);
        this.homeDir = homeDir;
        buildLock();

        lockManifest();
        buildManifest();
        unlockManifest();
        //log.debug("Object instantiation complete : " + oid);
    }

    // Unit testing
    public String getPath() {
        return homeDir.getAbsolutePath();
    }

    private void buildLock() {
        try {
            String lockPath = getPath() + File.separator + "manifest.lock";
            lockFile = new File(lockPath);
            if (!lockFile.exists()) {
                // log.debug("Creating new lock file : "
                // + lockFile.getAbsolutePath());
                lockFile.getParentFile().mkdirs();
                lockFile.createNewFile();
            }
            manifestLock = new DummyFileLock(lockPath);
        } catch (IOException ex) {
            log.error("Failed accessing manifest lock", ex);
        }
    }

    private void lockManifest() {
        try {
            // log.debug(" * Locking Manifest : " + getId());
            manifestLock.getLock();
            // log.debug(" * Manifest locked : " + getId());
        } catch (IOException ex) {
            log.error("Failed acquiring manifest lock : ", ex);
        }
    }

    private void unlockManifest() {
        try {
            // log.debug(" * Unlocking Manifest : " + getId());
            manifestLock.release();
            // log.debug(" * Manifest unlocked : " + getId());
        } catch (IOException ex) {
            log.error("Failed releasing manifest lock : ", ex);
        }
    }

    private void buildManifest() {
        Map<String, Payload> manifest = getManifest();
        readFromDisk(manifest, homeDir, 0);
    }

    private void readFromDisk(Map<String, Payload> manifest, File dir, int depth) {
        File[] files = dir.listFiles(new FilenameFilter() {
            @Override
            public boolean accept(File dir, String name) {
                if (name.endsWith(METADATA_SUFFIX)) {
                    return false;
                }
                if (name.endsWith(MANIFEST_LOCK_FILE)) {
                    return false;
                }
                return true;
            }
        });
        if (files != null) {
            for (File file : files) {
                if (file.isFile()) {
                    FileSystemPayload payload = null;
                    File payloadFile = file;
                    if (depth > 0) {
                        File parentFile = file.getParentFile();
                        String relPath = "";
                        for (int i = 0; i < depth; i++) {
                            relPath = parentFile.getName() + File.separator
                                    + relPath;
                            parentFile = parentFile.getParentFile();
                        }
                        payloadFile = new File(relPath, file.getName());
                        payload = new FileSystemPayload(relPath
                                + file.getName(), new File(homeDir, payloadFile
                                .getPath()));
                    } else {
                        // log.debug("File found on disk : " +
                        // payloadFile.getName());
                        payload = new FileSystemPayload(payloadFile.getName(),
                                payloadFile);
                    }
                    payload.readExistingMetadata();
                    if (payload.getType().equals(PayloadType.Source)) {
                        setSourceId(payload.getId());
                    }
                    manifest.put(payload.getId(), payload);
                } else if (file.isDirectory()) {
                    readFromDisk(manifest, file, depth + 1);
                }
            }
        }
        // log.debug("New Manifest : " + manifest);
    }

    @Override
    public Payload createStoredPayload(String pid, InputStream in)
            throws StorageException {
        Payload payload = createPayload(pid, in, false);
        return payload;
    }

    @Override
    public Payload createLinkedPayload(String pid, String linkPath)
            throws StorageException {
        try {
            ByteArrayInputStream in = new ByteArrayInputStream(linkPath
                    .getBytes("UTF-8"));
            Payload payload = createPayload(pid, in, true);
            return payload;
        } catch (UnsupportedEncodingException ex) {
            throw new StorageException(ex);
        }
    }

    private Payload createPayload(String pid, InputStream in, boolean linked)
            throws StorageException {
        //log.debug("createPayload(" + pid + ")");
        // Manifest check
        lockManifest();
        Map<String, Payload> manifest = getManifest();
        // log.debug("FileSystem Manifest : " + manifest);
        if (manifest.containsKey(pid)) {
            unlockManifest();
            throw new StorageException("ID '" + pid
                    + "' already exists in manifest.");
        }

        // File creation
        File newFile = new File(homeDir, pid);
        if (newFile.exists()) {
            unlockManifest();
            throw new StorageException("ID '" + pid
                    + "' already exists on disk.");
        } else {
            newFile.getParentFile().mkdirs();
            try {
                newFile.createNewFile();
            } catch (IOException ex) {
                unlockManifest();
                log.error("Error creating file (" + newFile.getAbsolutePath()
                        + ")");
                throw new StorageException("Failed to create file", ex);
            }
        }

        // File storage
        try {
            // log.debug("Copying {}", newFile);
            FileOutputStream out = new FileOutputStream(newFile);
            IOUtils.copy(in, out);
            in.close();
            out.close();
        } catch (FileNotFoundException ex) {
            log.error("Failed saving payload to disk", ex);
        } catch (IOException ex) {
            log.error("Failed saving payload to disk", ex);
        }

        // Payload creation
        FileSystemPayload payload = new FileSystemPayload(pid, newFile);
        if (getSourceId() == null) {
            payload.setType(PayloadType.Source);
            setSourceId(pid);
        } else {
            payload.setType(PayloadType.Enrichment);
        }
        payload.setLinked(linked);
        payload.writeMetadata();
        manifest.put(pid, payload);

        unlockManifest();
        return payload;
    }

    @Override
    public Payload getPayload(String pid) throws StorageException {
        //log.debug("getPayload(" + pid + ")");
        lockManifest();
        unlockManifest();

        Map<String, Payload> man = getManifest();
        if (man.containsKey(pid)) {
            return man.get(pid);
        } else {
            //throw new StorageException("ID '" + pid + "' does not exist.");
            buildManifest();
            man = getManifest();
            if (man.containsKey(pid)) {
                return man.get(pid);
            } else {
                throw new StorageException("ID '" + pid + "' does not exist.");
            }
        }
    }

    @Override
    public void removePayload(String pid) throws StorageException {
        //log.debug("removePayload(" + pid + ")");
        lockManifest();
        Map<String, Payload> manifest = getManifest();
        if (!manifest.containsKey(pid)) {
            throw new StorageException("pID '" + pid + "' not found.");
        }

        // Close the payload first in case
        manifest.get(pid).close();
        File realFile = new File(homeDir, pid);
        File metaFile = new File(homeDir, pid + METADATA_SUFFIX);

        boolean result = false;
        if (realFile.exists()) {
            result = FileUtils.deleteQuietly(realFile);
            if (!result) {
                System.out.println("Deleting : " + realFile.getAbsolutePath());
                throw new StorageException("Failed to delete : "
                        + realFile.getAbsolutePath());
            }
        }
        if (metaFile.exists()) {
            result = FileUtils.deleteQuietly(metaFile);
            if (!result) {
                System.out.println("Deleting : " + realFile.getAbsolutePath());
                throw new StorageException("Failed to delete : "
                        + metaFile.getAbsolutePath());
            }
        }

        manifest.remove(pid);
        unlockManifest();
    }

    @Override
    public Payload updatePayload(String pid, InputStream in)
            throws StorageException {
        //log.debug("updatePayload(" + pid + ")");
        lockManifest();
        File oldFile = new File(homeDir, pid);
        if (!oldFile.exists()) {
            throw new StorageException("pID '" + pid + "': file not found");
        }

        // File update
        try {
            FileOutputStream out = new FileOutputStream(oldFile);
            IOUtils.copy(in, out);
            in.close();
            out.close();
        } catch (FileNotFoundException ex) {
            log.error("Failed saving payload to disk", ex);
        } catch (IOException ex) {
            log.error("Failed saving payload to disk", ex);
        }
        unlockManifest();

        // Payload update
        FileSystemPayload payload = (FileSystemPayload) getPayload(pid);
        payload.writeMetadata();
        return payload;
    }

    @Override
    public String toString() {
        return String.format("%s [%s]", getId(), homeDir);
    }

    @Override
    public void close() throws StorageException {
        try {
            super.close();
        } catch(StorageException ex) {
            // It's ok, we expect this, the metadata payload won't exist
            // in the second part of super.close()
        }
        this.unlockManifest();
    }
}
