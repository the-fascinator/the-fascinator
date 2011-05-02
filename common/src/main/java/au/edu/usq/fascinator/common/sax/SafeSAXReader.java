/*
 * The Fascinator - Common Library
 * Copyright (C) 2008 University of Southern Queensland
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
package au.edu.usq.fascinator.common.sax;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.StringWriter;
import java.io.Writer;

import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.Node;
import org.dom4j.io.OutputFormat;
import org.dom4j.io.SAXReader;
import org.dom4j.io.XMLWriter;
import org.xml.sax.SAXException;

/**
 * A basic util class to instantiate a SAX Reader and parse documents whilst
 * retaining original escaped characters.
 * 
 * <p>
 * Credit due :
 * http://asfak.wordpress.com/2009/08/29/escaping-or-unescaping-special-
 * characters-while-writing-xml-files-using-dom4j/
 * </p>
 * 
 * @author Greg Pendlebury
 */

public class SafeSAXReader {

    /** Feature url */
    private String feature = "http://apache.org/xml/features/nonvalidating/load-external-dtd";

    /** SAX Reader */
    private SAXReader reader;

    /** Constructor */
    public SafeSAXReader() {
        reader = new SAXReader(new SafeSAXParser());
        reader.setValidation(false);
        try {
            reader.setFeature(feature, false);
        } catch (SAXException ex) {
            // Do we care?
        }
    }

    /**
     * Load Document from specified String
     * 
     * @param inDoc document to be read
     * @return Document
     * @throws DocumentException if fail to read the document
     */
    public Document loadDocument(String inDoc) throws DocumentException {
        return reader.read(inDoc);
    }

    /**
     * Load Document from specified Input Stream
     * 
     * @param inStream input stream to be read
     * @return Document
     * @throws DocumentException if fail to read the document
     */
    public Document loadDocumentFromStream(InputStream inStream)
            throws DocumentException {
        return reader.read(inStream);
    }

    /**
     * Convert node to string
     * 
     * @param outDoc Node to be converted
     * @return String of the converted node
     * @throws IOException if the conversion fail
     */
    public String docToString(Node outDoc) throws IOException {
        Writer osw = new StringWriter();
        OutputFormat opf = new OutputFormat("", false, "UTF-8");
        opf.setSuppressDeclaration(true);
        opf.setExpandEmptyElements(true);
        XMLWriter writer = new XMLWriter(osw, opf);
        writer.setEscapeText(false);
        writer.write(outDoc);
        writer.close();

        return osw.toString();
    }

    /**
     * Convert node to stream
     * 
     * @param outDoc Node to be converted
     * @param outStream output stream of the converted node
     * @throws IOException if the conversion fail
     */
    public void docToStream(Node outDoc, OutputStream outStream)
            throws IOException {
        OutputFormat opf = new OutputFormat("", false, "UTF-8");
        opf.setSuppressDeclaration(true);
        opf.setExpandEmptyElements(true);
        XMLWriter writer = new XMLWriter(outStream, opf);
        writer.setEscapeText(false);
        writer.write(outDoc);
        writer.close();
    }
}