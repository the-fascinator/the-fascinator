/*
 * The Fascinator - Plugin - Transformer - Ims
 * Copyright (C) 2010-2011 University of Southern Queensland
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
package au.edu.usq.fascinator.transformer.ims;

import au.edu.usq.fascinator.api.PluginDescription;
import au.edu.usq.fascinator.api.PluginException;
import au.edu.usq.fascinator.api.storage.DigitalObject;
import au.edu.usq.fascinator.api.storage.Payload;
import au.edu.usq.fascinator.api.storage.PayloadType;
import au.edu.usq.fascinator.api.storage.StorageException;
import au.edu.usq.fascinator.api.transformer.Transformer;
import au.edu.usq.fascinator.api.transformer.TransformerException;
import au.edu.usq.fascinator.common.JsonSimple;
import au.edu.usq.fascinator.common.JsonSimpleConfig;
import au.edu.usq.fascinator.common.MimeTypeUtil;
import au.edu.usq.fascinator.common.storage.StorageUtils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Enumeration;
import java.util.Properties;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * <p>
 * This plugin is used for harvesting <a
 * href="http://en.wikipedia.org/wiki/IMS_Global">IMS packages</a> by relying on
 * imsmanifest.xml file. The content of the file will be unzipped and displayed
 * by using <a
 * href="https://fascinator.usq.edu.au/trac/wiki/Paquete">Paquete</a>.
 * </p>
 * 
 * <h3>Configuration</h3> Standard configuration table:
 * <table border="1">
 * <tr>
 * <th>Option</th>
 * <th>Description</th>
 * <th>Required</th>
 * <th>Default</th>
 * </tr>
 * 
 * <tr>
 * <td>id</td>
 * <td>Transformer Id</td>
 * <td><b>Yes</b></td>
 * <td>ims</td>
 * </tr>
 * 
 * </table>
 * 
 * <h3>Examples</h3>
 * <ol>
 * <li>
 * IMS transformer attached to the transformer list in The Fascinator
 * 
 * <pre>
 *      "ims": {
 *             "id": "ims"
 *         }
 * </pre>
 * 
 * </li>
 * </ol>
 * 
 * <h3>Wiki Link</h3>
 * <p>
 * None
 * </p>
 * 
 * @author Ron Ward, Linda Octalina
 */
public class ImsTransformer implements Transformer {

    /** Logging **/
    private static Logger log = LoggerFactory.getLogger(ImsTransformer.class);

    /** Json config file **/
    private JsonSimpleConfig config;

    /** ims Manifest file **/
    private String manifestFile = "imsmanifest.xml";

    /** Flag for first execution */
    private boolean firstRun = true;

    // private boolean isImsPackage = false;

    /**
     * ImsTransformer constructor
     */
    public ImsTransformer() {

    }

    /**
     * Init method to initialise Ims transformer
     * 
     * @param jsonFile
     * @throws IOException
     * @throws PluginException
     */
    @Override
    public void init(File jsonFile) throws PluginException {
        try {
            config = new JsonSimpleConfig(jsonFile);
            reset();
        } catch (IOException e) {
            log.error("Error reading config: ", e);
        }
    }

    /**
     * Init method to initialise Ims transformer
     * 
     * @param jsonString
     * @throws IOException
     * @throws PluginException
     */
    @Override
    public void init(String jsonString) throws PluginException {
        try {
            config = new JsonSimpleConfig(jsonString);
            reset();
        } catch (IOException e) {
            log.error("Error reading config: ", e);
        }
    }

    /**
     * Reset the transformer in preparation for a new object
     */
    private void reset() throws TransformerException {
        if (firstRun) {
            firstRun = false;
            // Does IMS need this?
        }
    }

    /**
     * Transform method
     * 
     * @param object : DigitalObject to be transformed
     * @param jsonConfig : String containing configuration for this item
     * @return DigitalObject The object after being transformed
     * @throws TransformerException
     */
    @Override
    public DigitalObject transform(DigitalObject in, String jsonConfig)
            throws TransformerException {
        // Purge old data
        reset();

        JsonSimple itemConfig;
        try {
            itemConfig = new JsonSimple(jsonConfig);
        } catch (IOException ex) {
            throw new TransformerException("Invalid configuration! '{}'", ex);
        }

        String sourceId = in.getSourceId();
        String ext = FilenameUtils.getExtension(sourceId);

        // We are only interested in ZIP files
        if (!ext.equalsIgnoreCase("zip")) {
            return in;
        }

        File inFile;
        String filePath = itemConfig.getString(null, "sourceFile");
        if (filePath != null) {
            inFile = new File(filePath);
        } else {
            try {
                inFile = File.createTempFile("ims", ".zip");
                inFile.deleteOnExit();
                FileOutputStream tempFileOut = new FileOutputStream(inFile);
                // Payload from storage
                Payload tempPayload = in.getPayload(sourceId);
                // Copy and close
                IOUtils.copy(tempPayload.open(), tempFileOut);
                tempPayload.close();
                tempFileOut.close();
            } catch (IOException ex) {
                log.error("Error writing temp file : ", ex);
                return in;
            } catch (StorageException ex) {
                log.error("Error accessing storage data : ", ex);
                return in;
            }
        }

        if (inFile.exists()) {
            log.info("Unpacking IMS Package: '{}'", inFile.getName());
            try {
                in = createImsPayload(in, inFile);
            } catch (StorageException e) {
                log.error("Error accessing storage: ", e);
            } catch (IOException e) {
                log.error("Error reading ZIP: ", e);
            } finally {
                inFile.delete();
            }
        } else {
            log.info("not found inFile {}", inFile);
        }
        return in;
    }

    /**
     * Create Payload method for Ims files
     * 
     * @param object : DigitalObject that store the payload
     * @param file : File to be stored as payload
     * @return transformed DigitalObject
     * @throws StorageException
     * @throws IOException
     */
    public DigitalObject createImsPayload(DigitalObject object, File file)
            throws StorageException, IOException {
        boolean htmlPreview = false;

        ZipFile zipFile = new ZipFile(file);
        ZipEntry manifestEntry = zipFile.getEntry(manifestFile);
        if (manifestEntry != null) {
            Enumeration<? extends ZipEntry> entries = zipFile.entries();
            while (entries.hasMoreElements()) {
                ZipEntry entry = entries.nextElement();
                if (!entry.isDirectory()) {
                    String name = entry.getName();
                    Payload imsPayload = StorageUtils.createOrUpdatePayload(
                            object, name, zipFile.getInputStream(entry));
                    // Look for our preview
                    if (name.startsWith("index.htm")) {
                        imsPayload.setType(PayloadType.Preview);
                        log.info("New preview found: '{}'", name);
                        htmlPreview = true;
                        // Everything else is an 'enrichment'
                    } else {
                        imsPayload.setType(PayloadType.Enrichment);
                    }
                    imsPayload.setLabel(name);
                    imsPayload.setContentType(MimeTypeUtil.getMimeType(name));
                    // Trigger write on metadata
                    imsPayload.close();
                }
            }

            if (htmlPreview) {
                Properties prop = object.getMetadata();
                prop.setProperty("displayType", "html");
                prop.setProperty("previewType", "html");
                object.close();
            }
        } else {
            log.debug("No manifest entry: '{}'", manifestFile);
        }
        return object;
    }

    /**
     * Get Transformer ID
     * 
     * @return id
     */
    @Override
    public String getId() {
        return "ims";
    }

    /**
     * Get Transformer Name
     * 
     * @return name
     */
    @Override
    public String getName() {
        return "IMS Transformer";
    }

    /**
     * Gets a PluginDescription object relating to this plugin.
     * 
     * @return a PluginDescription
     */
    @Override
    public PluginDescription getPluginDetails() {
        return new PluginDescription(this);
    }

    /**
     * Shut down the transformer plugin
     */
    @Override
    public void shutdown() throws PluginException {

    }

    /**
     * Get File extension method
     * 
     * @param fileName
     * @return file extension
     */
    public String getFileExt(File fileName) {
        return fileName.getName()
                .substring(fileName.getName().lastIndexOf('.'));
    }

}
