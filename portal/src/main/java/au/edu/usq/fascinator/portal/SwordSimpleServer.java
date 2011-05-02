/* 
 * The Fascinator - Portal
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
package au.edu.usq.fascinator.portal;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;
import java.util.UUID;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.purl.sword.base.Collection;
import org.purl.sword.base.Deposit;
import org.purl.sword.base.DepositResponse;
import org.purl.sword.base.ErrorCodes;
import org.purl.sword.base.SWORDAuthenticationException;
import org.purl.sword.base.SWORDEntry;
import org.purl.sword.base.SWORDException;
import org.purl.sword.base.Service;
import org.purl.sword.base.ServiceDocument;
import org.purl.sword.base.ServiceDocumentRequest;
import org.purl.sword.base.ServiceLevel;
import org.purl.sword.base.Workspace;
import org.purl.sword.client.Client;
import org.purl.sword.client.PostDestination;
import org.purl.sword.client.PostMessage;
import org.purl.sword.server.SWORDServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3.atom.Author;
import org.w3.atom.Content;
import org.w3.atom.Contributor;
import org.w3.atom.Generator;
import org.w3.atom.InvalidMediaTypeException;
import org.w3.atom.Link;
import org.w3.atom.Source;
import org.w3.atom.Summary;
import org.w3.atom.Title;

/**
 * Simple SWORD server. Manages a single anonymous collection.
 * 
 * @author Ron Ward
 * @author Oliver Lucido
 */
public class SwordSimpleServer implements SWORDServer {

    private Logger log = LoggerFactory.getLogger(SwordSimpleServer.class);

    private static int counter = 0;

    private String depositUrl;

    public SwordSimpleServer(String depositUrl) {
        this.depositUrl = depositUrl;
    }

    public Client getClient() {
        return new Client();
    }

    public PostMessage getPostMessage() {
        return new PostMessage();
    }

    public PostDestination getPostDestination() {
        return new PostDestination();
    }

    public ServiceDocument getServiceDocument() {
        return new ServiceDocument();
    }

    public ServiceDocumentRequest getServiceDocumentRequest() {
        // username, password, onBehalfOf, IPAddress
        return new ServiceDocumentRequest();
    }

    public Deposit getDeposit() {
        return new Deposit();
    }

    /**
     * Provides a simple service document - it contains an anonymous workspace
     * and collection.
     * 
     * @throws SWORDAuthenticationException
     */
    public ServiceDocument doServiceDocument(ServiceDocumentRequest sdr)
            throws SWORDAuthenticationException {
        // Authenticate the user
        String username = sdr.getUsername();
        String password = sdr.getPassword();
        if (username == null) {
            username = "anonymous";
        }
        if (password == null) {
            password = "?";
        }
        if (!username.equals(password)) {
            // User not authenticated
            throw new SWORDAuthenticationException("Bad credentials");
        }
        // Create and return a ServiceDocument
        ServiceDocument document = new ServiceDocument();
        Service service = new Service(ServiceLevel.ZERO, true, true);
        Workspace workspace = new Workspace();
        Collection collection = new Collection();

        document.setService(service);
        workspace.setTitle("The Fascinator SWORD Workspace");
        collection.setTitle("Default collection");
        collection.setLocation(depositUrl);
        collection.addAccepts("application/zip");
        // collection.setAbstract("An abstract goes in here");
        // collection.setCollectionPolicy("A collection policy");
        // collection.setMediation(true);
        // collection.setTreatment("treatment in here too");
        workspace.addCollection(collection);
        service.addWorkspace(workspace);
        return document;
    }

    public DepositResponse doDeposit(Deposit deposit)
            throws SWORDAuthenticationException, SWORDException {
        // Authenticate the user
        String username = deposit.getUsername();
        String password = deposit.getPassword();
        if ((username != null)
                && (password != null)
                && (((username.equals("")) && (password.equals(""))) || (!username
                        .equalsIgnoreCase(password)))) {
            // User not authenticated
            throw new SWORDAuthenticationException("Bad credentials");
        }
        // Get the filenames
        StringBuffer filenames = new StringBuffer("Deposit file contained: ");
        if (deposit.getFilename() != null) {
            filenames.append("(filename = " + deposit.getFilename() + ") ");
        }
        if (deposit.getSlug() != null) {
            filenames.append("(slug = " + deposit.getSlug() + ") ");
        }
        try {
            ZipInputStream zip = new ZipInputStream(deposit.getFile());
            ZipEntry ze;
            while ((ze = zip.getNextEntry()) != null) {
                filenames.append(" " + ze.toString());
                log.info("  zipEntry '{}'", ze.toString());
            }
        } catch (IOException ioe) {
            throw new SWORDException("Failed to open deposited zip file", null,
                    ErrorCodes.ERROR_CONTENT);
        }
        // Handle the deposit
        if (!deposit.isNoOp()) {
            counter++;
        }
        // SWORD Atom Entry
        SWORDEntry se = getSwordEntry(deposit, filenames.toString(), username);
        DepositResponse dr = new DepositResponse(Deposit.ACCEPTED);
        dr.setEntry(se);
        return dr;
    }

    public SWORDEntry getSwordEntry(Deposit deposit, String filenames,
            String username) {
        SWORDEntry se = new SWORDEntry(); // SWORD Atom Entry
        Title t = new Title(); // Atom
        t.setContent("SimpleServer Deposit: #" + counter);
        se.setTitle(t);
        se.addCategory("Category");
        if (deposit.getSlug() != null) {
            // se.setId(deposit.getSlug() + " - ID: " + counter);
            se.setId(deposit.getSlug() + ":" + UUID.randomUUID().toString());
        } else {
            // se.setId("ID: " + counter);
            se.setId(UUID.randomUUID().toString());
        }
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");
        TimeZone utc = TimeZone.getTimeZone("UTC");
        sdf.setTimeZone(utc);
        String milliFormat = sdf.format(new Date());
        se.setUpdated(milliFormat);
        Summary s = new Summary(); // Atom
        s.setContent(filenames);
        se.setSummary(s);
        Author a = new Author(); // Atom
        if (username != null) {
            a.setName(username);
        } else {
            a.setName("unknown");
        }
        se.addAuthors(a);
        Link e = new Link(); // Atom
        e.setRel("edit");
        e.setHref(depositUrl);
        se.addLink(e); // multi
        if (deposit.getOnBehalfOf() != null) {
            Contributor c = new Contributor();
            c.setName(deposit.getOnBehalfOf());
            // c.setEmail(deposit.getOnBehalfOf() + "@usq.edu.au");
            se.addContributor(c);
        }
        Source source = new Source(); // Atom
        Generator generator = new Generator();
        generator.setContent(getClass().getCanonicalName());
        source.setGenerator(generator);
        se.setSource(source);
        Content content = new Content(); // Atom
        try {
            content.setType("application/zip");
        } catch (InvalidMediaTypeException e1) {
            // TODO Auto-generated catch block
            e1.printStackTrace();
        }
        content
                .setSource("http://localhost:9997/portal/default/sword/test/uploads/upload-"
                        + counter + ".zip");
        se.setContent(content);
        // se.setTreatment("Short back and sides");
        if (deposit.isVerbose()) {
            // se.setVerboseDescription("I've done a lot of hard work to get this far!");
        }
        se.setNoOp(deposit.isNoOp());
        // se.setFormatNamespace("http://www.loc.gov/METS/"); //
        // http://www.imsglobal.org/xsd/imscp_v1p2
        return se;
    }
}
